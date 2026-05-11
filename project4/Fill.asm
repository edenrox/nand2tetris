// Name: Fill
// Description:
//   Fills the screen black if a key is pressed, white if not
// Pseudo-code
// while {
//    color = 0 (white)
//    if (kbd != 0) {
//      color = -1 (black)
//    }
//    paintColor(color)
// }
// fun paintColor(color) {
//   i = 0
//   screenMax = 8192
//   screenPoint = SCREEN
//   while (i < screenSize) {
//     M[screenPointer] = color
//     i++
//     screenPointer++
//   }
//   return;
// }



(start)
//  color = 0 (white)
@color
M=0

//  if (KBD == 0) { goto kbdElse }
@KBD
D=M
@kbdElse
D;JEQ
//  color = -1 (black)
@color
M=-1

// kbdElse:
(kbdElse)
// paintColor()
@afterPaint
D=A
@paintColor
D;JMP

// afterPaint
(afterPaint)

// goto start
@start
D;JMP


// fun paintColor()
(paintColor)
  @return
  M=D
// i = 0
  @i
  M=0
// screenMax = 8192
  @8192
  D=A
  @screenMax
  M=D
// screenPointer = SCREEN
  @SCREEN
  D=A
  @screenPointer
  M=D

// paintLoop:
(paintLoop)

// if (i - screenMax >= 0) { goto end }
  @i
  D=M
  @screenMax
  D=D-M
  @return
  A=M
  D;JGE

// Loop body
// M[screenPointer] = color
  @color
  D=M
  @screenPointer
  A=M
  M=D

// i++
  @i
  M=M+1
// screenPointer++
  @screenPointer
  M=M+1
// goto paintLoop
  @paintLoop
  D;JMP
