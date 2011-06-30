/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 * @author jblok
 */
public interface M6510Ops
{
  public final static int BRK = 0x00;
  public final static int ORA_INDX = 0x01; // (Indirect, X)
  public final static int ORA_Z = 0x05; // Zero Page
  public final static int ASL_Z = 0x06;   // Zero Page
  public final static int PHP = 0x08;
  public final static int ORA_I = 0x09; // Immediate
  public final static int ASL_ACC = 0x0A; // Accumulator
  public final static int ORA = 0x0D; // Absolute
  public final static int ASL = 0x0E; // Absolute

  public final static int BPL = 0x10;
  public final static int ORA_INDY = 0x11; // (Indirect),Y
  public final static int ORA_ZX = 0x15; // Zero Page,X
  public final static int ASL_ZX = 0x16; // Zero Page,X
  public final static int CLC = 0x18;
  public final static int ORA_Y = 0x19; // Absolute,Y
  public final static int ORA_X = 0x1D; // Absolute,X
  public final static int ASL_X = 0x1E; // Absolute,X

  public final static int JSR = 0x20;
  public final static int AND_INDX = 0x21; // (Indirect,X)
  public final static int BIT_Z = 0x24; // Zero Page
  public final static int AND_Z = 0x25; // Zero Page
  public final static int ROL_Z = 0x26; // Zero Page
  public final static int PLP = 0x28; //
  public final static int AND_I = 0x29; // Immediate
  public final static int ROL_ACC = 0x2A; // Accumulator
  public final static int BIT = 0x2C; // Absolute
  public final static int AND = 0x2D; // Absolute
  public final static int ROL = 0x2E; // Absolute
  public final static int BMI = 0x30; //
  public final static int AND_INDY = 0x31; // (Indirect),Y
  public final static int AND_ZX = 0x35; // Zero Page,X
  public final static int ROL_ZX = 0x36; // Zero Page,X
  public final static int SEC = 0x38; //
  public final static int AND_Y = 0x39; // Absolute,Y
  public final static int AND_X = 0x3D; // Absolute,X
  public final static int ROL_X = 0x3E; // Absolute,X

  public final static int RTI = 0x40; //
  public final static int EOR_INDX = 0x41; // (Indirect,X)
  public final static int EOR_Z = 0x45; // Zero Page
  public final static int LSR_Z = 0x46; // Zero Page
  public final static int PHA = 0x48; //
  public final static int EOR_I = 0x49; // Immediate
  public final static int LSR_ACC = 0x4A; // Accumulator
  public final static int JMP = 0x4C; // Absolute
  public final static int EOR = 0x4D; // Absolute
  public final static int LSR = 0x4E; // Absolute

  public final static int BVC = 0x50; //
  public final static int EOR_INDY = 0x51; // (Indirect),Y
  public final static int EOR_ZX = 0x55; // Zero Page,X
  public final static int LSR_ZX = 0x56; // Zero Page,X
  public final static int CLI = 0x58; //
  public final static int EOR_Y = 0x59; // Absolute,Y
  public final static int EOR_X = 0x5D; // Absolute,X
  public final static int LSR_X = 0x5E; // Absolute,X

  public final static int RTS = 0x60;
  public final static int ADC_INDX = 0x61; // (Indirect,X)
  public final static int ADC_Z = 0x65; // Zero Page
  public final static int ROR_Z = 0x66; // Zero Page
  public final static int PLA = 0x68;
  public final static int ADC_I = 0x69; // Immediate
  public final static int ROR_ACC = 0x6A; // Accumulator
  public final static int JMP_IND = 0x6C; // Indirect
  public final static int ADC = 0x6D; // Absolute
  public final static int ROR = 0x6E; // Absolute

  public final static int BVS = 0x70; //
  public final static int ADC_INDY = 0x71; // (Indirect),Y
  public final static int ADC_ZX = 0x75; // Zero Page,X
  public final static int ROR_ZX = 0x76; // Zero Page,X
  public final static int SEI = 0x78; //
  public final static int ADC_Y = 0x79; // Absolute,Y
  public final static int ADC_X = 0x7D; // Absolute,X
  public final static int ROR_X = 0x7E; // Absolute,X

  public final static int STA_INDX = 0x81; // (Indirect,X)
  public final static int STY_Z = 0x84; // Zero Page
  public final static int STA_Z = 0x85; // Zero Page
  public final static int STX_Z = 0x86; // Zero Page
  public final static int DEY = 0x88; //
  public final static int TXA = 0x8A; //
  public final static int STY = 0x8C; // Absolute
  public final static int STA = 0x8D; // Absolute
  public final static int STX = 0x8E; // Absolute
  public final static int BCC = 0x90; //
  public final static int STA_INDY = 0x91; // (Indirect),Y
  public final static int STY_ZX = 0x94; // Zero Page,X
  public final static int STA_ZX = 0x95; // Zero Page,X
  public final static int STX_ZY = 0x96; // Zero Page,Y
  public final static int TYA = 0x98; //
  public final static int STA_Y = 0x99; // Absolute,Y
  public final static int TXS = 0x9A; //
  public final static int STA_X = 0x9d; // Absolute,X

  public final static int LDY_I = 0xA0; //
  public final static int LDA_INDX = 0xA1; // (Indirect,X)
  public final static int LDX_I = 0xA2; // Immediate
  public final static int LDY_Z = 0xA4; // Zero Page
  public final static int LDA_Z = 0xA5; // Zero Page
  public final static int LDX_Z = 0xA6; // Zero Page
  public final static int TAY = 0xA8; //
  public final static int LDA_I = 0xA9; // Immediate
  public final static int TAX = 0xAA; //
  public final static int LDY = 0xAC; // Absolute
  public final static int LDA = 0xAD; // Absolute
  public final static int LDX = 0xAE; // Absolute
  public final static int BCS = 0xB0; //
  public final static int LDA_INDY = 0xB1; // (Indirect),Y
  public final static int LDY_ZX = 0xB4; // Zero Page,X
  public final static int LDA_ZX = 0xB5; // Zero Page,X
  public final static int LDX_ZY = 0xB6; // Zero Page,Y
  public final static int CLV = 0xB8; // Clear Overflov
  public final static int LDA_Y = 0xB9; // Absolute,Y
  public final static int TSX = 0xBA; //
  public final static int LDY_X = 0xBC; // Absolute,X
  public final static int LDA_X = 0xBD; // Absolute,X
  public final static int LDX_Y = 0xBE; // Absolute,Y

  public final static int CPY_I = 0xC0; // Immediate
  public final static int CMP_INDX = 0xC1; // (Indirect,X)
  public final static int CPY_Z = 0xC4; // Zero Page
  public final static int CMP_Z = 0xC5; // Zero Page
  public final static int DEC_Z = 0xC6; // Zero Page
  public final static int INY = 0xC8; //
  public final static int CMP_I = 0xC9; // Immediate
  public final static int DEX = 0xCA; //
  public final static int CPY = 0xCC; // Absolute
  public final static int CMP = 0xCD; // Absolute
  public final static int DEC = 0xCE; // Absolute
  public final static int BNE = 0xD0; //
  public final static int CMP_INDY = 0xD1; // (Indirect),Y
  public final static int CMP_ZX = 0xD5; // Zero Page,X
  public final static int DEC_ZX = 0xD6; // Zero Page,X
  public final static int CLD = 0xD8; //
  public final static int CMP_Y = 0xD9; // Absolute,Y
  public final static int CMP_X = 0xDD; // Absolute,X
  public final static int DEC_X = 0xDE; // Absolute,X

  public final static int CPX_I = 0xE0; // Immediate
  public final static int SBC_INDX = 0xE1; // (Indirect,X)
  public final static int CPX_Z = 0xE4; // Zero Page
  public final static int SBC_Z = 0xE5; // Zero Page
  public final static int INC_Z = 0xE6; // Zero Page
  public final static int INX = 0xE8; //
  public final static int SBC_I = 0xE9; // Immediate
  public final static int NOP = 0xEA; //
  public final static int SBC_I_01 = 0xEB; // Absolute
  public final static int CPX = 0xEC; // Absolute
  public final static int SBC = 0xED; // Absolute
  public final static int INC = 0xEE; // Absolute
  public final static int BEQ = 0xF0; //
  public final static int SBC_INDY = 0xF1; // (Indirect),Y
  public final static int SBC_ZX = 0xF5; // Zero Page,X
  public final static int INC_ZX = 0xF6; // Zero Page,X
  public final static int SED = 0xF8; //
  public final static int SBC_Y = 0xF9; // Absolute,Y
  public final static int SBC_X = 0xFD; // Absolute,X
  public final static int INC_X = 0xFE; // Absolute,X

  // Non documented 6510 instructions
  // Specs from winc64 code
  // ASO == SLO == (ASL, ORA)
  //
  public final static int SLO_INDX = 0x03; // (Indirect, X)
  public final static int RLA_INDX = 0x23; // (Indirect, X)
  public final static int SRE_INDX = 0x43; // (Indirect, X)
  public final static int RRA_INDX = 0x63; // (Indirect, X)
  public final static int SAX_INDX = 0x83; // (Indirect, X)
  public final static int LAX_INDX = 0xA3; // (Indirect, X)
  public final static int DCP_INDX = 0xC3; // (Indirect, X)
  public final static int ISB_INDX = 0xE3; // (Indirect, X)

  public final static int SLO_Z = 0x07; // Zero
  public final static int RLA_Z = 0x27; // Zero
  public final static int SRE_Z = 0x47; // Zero
  public final static int RRA_Z = 0x67; // Zero
  public final static int SAX_Z = 0x87; // Zero
  public final static int LAX_Z = 0xA7; // Zero
  public final static int DCP_Z = 0xC7; // Zero
  public final static int ISB_Z = 0xE7; // Zero

  public final static int SLO_ZX = 0x17; // Zero, X
  public final static int RLA_ZX = 0x37; // Zero, X
  public final static int SRE_ZX = 0x57; // Zero, X
  public final static int RRA_ZX = 0x77; // Zero, X
  public final static int SAX_ZY = 0x97; // Zero, Y
  public final static int LAX_ZY = 0xB7; // Zero, Y
  public final static int DCP_ZX = 0xD7; // Zero, X
  public final static int ISB_ZX = 0xF7; // Zero, X

  public final static int SLO = 0x0F; // Absolute
  public final static int RLA = 0x2F; // Absolute
  public final static int SRE = 0x4F; // Absolute
  public final static int RRA = 0x6F; // Absolute
  public final static int SAX = 0x8F; // Absolute
  public final static int LAX = 0xAF; // Absolute
  public final static int DCP = 0xCF; // Absolute
  public final static int ISB = 0xEF; // Absolute

  public final static int SLO_INDY = 0x13; // (Indirect), Y
  public final static int RLA_INDY = 0x33; // (Indirect), Y
  public final static int SRE_INDY = 0x53; // (Indirect), Y
  public final static int RRA_INDY = 0x73; // (Indirect), Y
  public final static int SHA_INDY = 0x93; // (Indirect), Y
  public final static int LAX_INDY = 0xB3; // (Indirect), Y
  public final static int DCP_INDY = 0xD3; // (Indirect), Y
  public final static int ISB_INDY = 0xF3; // (Indirect), Y

  public final static int SLO_Y = 0x1b; // Absolute, Y
  public final static int RLA_Y = 0x3b; // Absolute, Y
  public final static int SRE_Y = 0x5b; // Absolute, Y
  public final static int RRA_Y = 0x7b; // Absolute, Y
  public final static int SHS_Y = 0x9b; // Absolute, Y
  public final static int LAS_Y = 0xBb; // Absolute, Y
  public final static int DCP_Y = 0xDb; // Absolute, Y
  public final static int ISB_Y = 0xFb; // Absolute, Y

  public final static int ANC_I = 0x0b; // Immediate
  public final static int ANC_I_01 = 0x2b; // Immediate
  public final static int ASR_I = 0x4b; // Immediate
  public final static int ARR_I = 0x6b; // Immediate
  public final static int ANE_I = 0x8b; // Immediate
  public final static int LXA_I = 0xab; // Immediate same as OAL
  public final static int SBX_I = 0xcb; // Immediate
  // public final static int SBC_I = 0xeb; // Immediate, already def...


  public static final int SAY_X = 0x9c; // Absolute X
  public static final int XAS_Y = 0x9e;

  public final static int SLO_X = 0x1f; // (Absolute, X)
  public final static int RLA_X = 0x3f; // (Absolute, X)
  public final static int SRE_X = 0x5f; // (Absolute, X)
  public final static int RRA_X = 0x7f; // (Absolute, X)
  public final static int SHA_Y = 0x9f; // (Absolute, X)
  public final static int LAX_Y = 0xbf; // (Absolute, Y)
  public final static int DCP_X = 0xdf; // (Absolute, X)
  public final static int ISB_X = 0xff; // (Absolute, X)


  public final static int HALT_00 = 0x02; // HALT_00, is now load routine...
  public final static int HALT_01 = 0x12; // not used...
  public final static int HALT_02 = 0x22; // not used...
  public final static int HALT_03 = 0x32; // not used...
  public final static int HALT_04 = 0x42; // not used...
  public final static int HALT_05 = 0x52; // not used...
  public final static int HALT_06 = 0x62; // not used...
  public final static int HALT_07 = 0x72; // not used...
  public final static int HALT_08 = 0x82; // not used...
  public final static int HALT_09 = 0x92; // not used...
  public final static int HALT_10 = 0xB2; // not used...
  public final static int HALT_11 = 0xC2; // not used...
  public final static int HALT_12 = 0xD2; // not used...
  public final static int HALT_13 = 0xE2; // not used...
  public final static int HALT_14 = 0xF2; // not used...

  // Patched instructions!
  public static final int LOAD_FILE = MOS6510Ops.OP_LOAD_FILE; // LOAD!
  public static final int SLEEP = MOS6510Ops.OP_SLEEP;
}
