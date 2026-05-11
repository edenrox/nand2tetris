// Name: Mult
// Description:
//   A program which multiples R2 = R0 * R1
// Pseudo-code
// Initialization:
// R2 = 0
// i = 0
// while (i < R1) {
//  r2 += r0
//  i++
// }
// end


// Initialization
// R2 = 0
@R2
M=0
// i = 0
@i
M=0


// if (R1 >= 0) { goto loop }
@R1
D=M
@loop
D;JGE

// R0 = -R0
@R0
M=-M
// R1 = -R1
@R1
M=-M

// while (i < R1)
// if (i - R1 >= 0) { goto end }
(loop)
@i
D=M
@R1
D=D-M
@end
D;JGE

// Loop body
// r2 += r0
@R0
D=M
@R2
M=D+M

// i++
@i
M=M+1

@loop
D;JMP

// Halt
(end)
@end
D;JMP
