package com.hopkins.nand2tetris.project6

import java.io.FileInputStream
import java.io.FileOutputStream

fun main(args: Array<String>) {
    // Parse the arguments
    if (args.isEmpty()) {
        println("Usage: HackAssembler <input_file>")
        return
    }
    val inputFileName = args[0].trim()
    val extension = inputFileName.substringAfterLast(".")
    val outputFileName = inputFileName.substringBeforeLast(".") + ".hack"

    // Read the input file
    val inputFile = FileInputStream(inputFileName)
    val lines = inputFile.bufferedReader().readLines()

    // Step 1: Initialize the symbol table
    val symbolTable = SymbolTable()
    initializeStaticSymbols(symbolTable)

    // Step 2: Parse the lines into instructions
    val parsedLines: List<Line> = lines.map { line -> parseLine(line) }

    println("Instructions")
    println("============")
    parsedLines.forEach { println(it)}
    println()

    // Step 3: Pass 1: define all the labels
    var index = 0
    for (line in parsedLines) {
        when (line) {
            is Line.Whitespace -> {
                // noop
            }
            is Line.Label -> symbolTable.define(line.label, index)
            is Line.Computation, is Line.Address -> index++
        }
    }

    println("Labels")
    println("============")
    println(symbolTable.dump())
    println()


    // Pass 2: parse the instructions
    val instructions: List<Instruction> =
        parsedLines.mapNotNull { line ->
            when (line) {
                is Line.Address -> line.resolve(symbolTable)
                is Line.Computation -> line.parse()
                else -> null
            }
        }

    println("Symbol Table")
    println("============")
    println(symbolTable.dump())
    println()

    // Write the output file
    FileOutputStream(outputFileName).bufferedWriter().use { writer ->
        for (instruction in instructions) {
            writer.appendLine(instruction.toBinaryCode())
        }
    }
}

fun parseLine(inputLine: String): Line {
    val line = inputLine.substringBefore("//").trim()
    if (line.isBlank()) {
        return Line.Whitespace
    } else if (line.startsWith("(")) {
        return parseLabel(line)
    } else if (line.startsWith("@")) {
        return parseAddress(line)
    } else {
        return parseComputation(line)
    }
}

fun initializeStaticSymbols(table: SymbolTable) {
    table.define("SP", 0)
    table.define("LCL", 1)
    table.define("ARG", 2)
    table.define("THIS", 3)
    table.define("THAT", 4)
    // Registers
    for (i in 0 until 16) {
        table.define("R$i", i)
    }
    // Hardware mapped I/O devices
    table.define("SCREEN", 16384)
    table.define("KBD", 24576)
}

class SymbolTable {
    private val symbols: MutableMap<String, Int> = mutableMapOf()
    private var nextValue: Int = 16

    fun define(name: String, value: Int) {
        symbols.put(name, value)
    }

    fun lookupOrCreateSymbol(name: String): Int {
        if (!symbols.containsKey(name)) {
            symbols.put(name, nextValue++)
        }
        return symbols[name]!!
    }

    fun dump(): String {
        return symbols.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }
}

fun parseLabel(line: String): Line.Label {
    check(line.startsWith("("))
    check(line.endsWith(")"))
    return Line.Label(line.drop(1).dropLast(1))
}

fun parseAddress(line: String): Line.Address {
    check(line.startsWith("@"))
    return Line.Address(line.drop(1))
}

fun parseComputation(line: String): Line.Computation {
    val equalsParts = line.split("=")
    val destination = if (equalsParts.size > 1) { equalsParts[0] } else { "" }
    val rest = equalsParts.last()
    val semiColonParts = rest.split(";")
    val computation = semiColonParts.first()
    val jump = if (semiColonParts.size > 1) { semiColonParts[1] } else { "" }

    return Line.Computation(destination, computation, jump)
}

fun parseDestination(input: String): DestType {
    if (input == "") {
        return DestType.NULL
    }
    return DestType.entries.drop(1).firstOrNull { it.name == input } ?: throw IllegalArgumentException("Unexpected Destination: $input")
}

fun parseJump(jump: String): JumpType {
    if (jump == "") {
        return JumpType.NULL
    }
    return JumpType.entries.drop(1).firstOrNull { it.name == jump } ?: throw IllegalArgumentException("Unexpected Jump: $jump")
}

fun parseComputationSegment(input: String): ComputationSegment? =
    when (input) {
        // Literal
        "0" -> ComputationSegment(input, "0101010")
        "1" -> ComputationSegment(input, "0111111")
        "-1" -> ComputationSegment(input, "0111010")
        // Register
        "D" -> ComputationSegment(input, "0001100")
        "A" -> ComputationSegment(input, "0110000")
        "M" -> ComputationSegment(input, "1110000")
        // Not
        "!D" -> ComputationSegment(input, "0001101")
        "!A" -> ComputationSegment(input, "0110001")
        "!M" -> ComputationSegment(input, "1110001")
        // Negate
        "-D" -> ComputationSegment(input, "0001111")
        "-A" -> ComputationSegment(input, "0110011")
        "-M" -> ComputationSegment(input, "1110011")
        // Increment
        "D+1" -> ComputationSegment(input, "0011111")
        "A+1" -> ComputationSegment(input, "0110111")
        "M+1" -> ComputationSegment(input, "1110111")
        // Decrement
        "D-1" -> ComputationSegment(input, "0001110")
        "A-1" -> ComputationSegment(input, "0110010")
        "M-1" -> ComputationSegment(input, "1110010")
        // Add
        "D+A" -> ComputationSegment(input, "0000010")
        "D+M" -> ComputationSegment(input, "1000010")
        // Subtract
        "D-A" -> ComputationSegment(input, "0010011")
        "D-M" -> ComputationSegment(input, "1010011")
        "A-D" -> ComputationSegment(input, "0000111")
        "M-D" -> ComputationSegment(input, "1000111")
        // And
        "D&A" -> ComputationSegment(input, "0000000")
        "D&M" -> ComputationSegment(input, "1000000")
        // Or
        "D|A" -> ComputationSegment(input, "0010101")
        "D|M" -> ComputationSegment(input, "1010101")
        else -> null
    }

sealed interface Line {
    object Whitespace : Line

    data class Label(val label: String): Line

    data class Address(val address: String): Line {
        fun resolve(symbolTable: SymbolTable): Instruction.Address {
            val resolvedValue = address.toIntOrNull() ?: symbolTable.lookupOrCreateSymbol(address)
            return Instruction.Address(resolvedValue)
        }
    }

    data class Computation(val destination: String, val computation: String, val jump: String): Line {
        fun parse(): Instruction.Computation {
            val computationSegment = parseComputationSegment(computation)
                ?: throw IllegalArgumentException("Error parsing computation segment: $computation")
            return Instruction.Computation(
                parseDestination(destination),
                computationSegment,
                parseJump(jump))
        }
    }
}

class ComputationSegment(val literal: String, val binary: String) {
    init {
        check(binary.length == 7)
    }
}

sealed interface Instruction {
    /** Returns the binary representation of this instruction. */
    fun toBinaryCode(): String

    data class Address(val value: Int) : Instruction {
        override fun toBinaryCode(): String {
            return buildString {
                append("0")
                append(value.toString(radix = 2).padStart(15, '0'))
            }
        }
    }

    data class Computation(val dest: DestType, val computation: ComputationSegment, val jump: JumpType): Instruction {
        override fun toBinaryCode(): String {
            return buildString {
                append("111")
                append(computation.binary)
                append(dest.code)
                append(jump.code)
            }
        }
    }
}

enum class DestType(val code: String) {
    NULL("000"),
    M("001"),
    D("010"),
    MD("011"),
    A("100"),
    AM("101"),
    AD("110"),
    AMD("111")
}

enum class JumpType(val code: String) {
    NULL("000"),
    JGT("001"),
    JEQ("010"),
    JGE("011"),
    JLT("100"),
    JNE("101"),
    JLE("110"),
    JMP("111")
}