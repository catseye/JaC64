package com.dreamfabric.jac64;
/**
 * Definitions for the  MOS6510Core
 *
 * Defines the operations and their addressing modes
 *
 * Created: Mon Jun 26 16:17:19 2006
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public class MOS6510Ops {

  // Instructions in order of appearance in below table...
  public static final int BRK = 0;
  public static final int ORA = 1;
  public static final int TRP = 2;
  public static final int SLO = 3;
  public static final int NOP = 4;
  public static final int ASL = 5;
  public static final int PHP = 6;
  public static final int ANC = 7;
  public static final int BPL = 8;
  public static final int CLC = 9;
  public static final int JSR = 10;
  public static final int AND = 11;
  public static final int RLA = 12;
  public static final int BIT = 13;
  public static final int ROL = 14;
  public static final int PLP = 15;
  public static final int BMI = 16;
  public static final int SEC = 17;
  public static final int RTI = 18;
  public static final int EOR = 19;
  public static final int SRE = 20;
  public static final int LSR = 21;
  public static final int PHA = 22;
  public static final int ASR = 23;
  public static final int JMP = 24;
  public static final int BVC = 25;
  public static final int CLI = 26;
  public static final int RTS = 27;
  public static final int ADC = 28;
  public static final int RRA = 29;
  public static final int ROR = 30;
  public static final int PLA = 31;
  public static final int ARR = 32;
  public static final int BVS = 33;
  public static final int SEI = 34;
  public static final int SAX = 35;
  public static final int STA = 36;
  public static final int STY = 37;
  public static final int STX = 38;
  public static final int DEY = 39;
  public static final int TXA = 40;
  public static final int ANE = 41;
  public static final int BCC = 42;
  public static final int SHA = 43;
  public static final int TYA = 44;
  public static final int TXS = 45;
  public static final int SHS = 46;
  public static final int SHY = 47;
  public static final int SHX = 48;
  public static final int LDY = 49;
  public static final int LDA = 50;
  public static final int LDX = 51;
  public static final int LAX = 52;
  public static final int TAX = 53;
  public static final int LXA = 54;
  public static final int TAY = 55;
  public static final int BCS = 56;
  public static final int CLV = 57;
  public static final int TSX = 58;
  public static final int LAS = 59;
  public static final int CPY = 60;
  public static final int CMP = 61;
  public static final int DCP = 62;
  public static final int DEC = 63;
  public static final int INY = 64;
  public static final int DEX = 65;
  public static final int SBX = 66;
  public static final int BNE = 67;
  public static final int CLD = 68;
  public static final int CPX = 69;
  public static final int SBC = 70;
  public static final int ISB = 71;
  public static final int INC = 72;
  public static final int INX = 73;
  public static final int BEQ = 74;
  public static final int SED = 75;

  public static final int LOAD_FILE = 76; // Load at 256!
  public static final int SLEEP = 77; // Load at 256!

  public static final int OP_LOAD_FILE = 0x100;
  public static final int OP_SLEEP = 0x101;

  public static final int ADDRESSING_MASK = 0xf00;
  public static final int ADDRESSING_SHIFT = 8;
  public static final int OP_MASK = 0x0ff;

  public static final int IMMEDIATE = 0x100;
  public static final int ZERO = 0x200;
  public static final int ABSOLUTE = 0x300;
  public static final int ZERO_X = 0x400;
  public static final int ZERO_Y = 0x500;
  public static final int ABSOLUTE_X = 0x600;
  public static final int ABSOLUTE_Y = 0x700;
  public static final int RELATIVE = 0x800;
  public static final int INDIRECT_X = 0x900; // From zeropage
  public static final int INDIRECT_Y = 0xa00; // From zeropage
  public static final int ACCUMULATOR = 0xb00;
  public static final int INDIRECT = 0xc00;

  public static final int MODE_MASK = 0xf000;
  public static final int MODE_SHIFT = 12;

  public static final int READ = 0x1000;
  public static final int WRITE = 0x2000;
  public static final int RMW = 0x3000;


  public static final String[] INS_STR = {
    "BRK", "ORA", "TRP", "SLO", "NOP", "ASL", "PHP", "ANC",
    "BPL", "CLC", "JSR", "AND", "RLA", "BIT", "ROL", "PLP",
    "BMI", "SEC", "RTI", "EOR", "SRE", "LSR", "PHA", "ASR",
    "JMP", "BVC", "CLI", "RTS", "ADC", "RRA", "ROR", "PLA",
    "ARR", "BVS", "SEI", "SAX", "STA", "STY", "STX", "DEY",
    "TXA", "ANE", "BCC", "SHA", "TYA", "TXS", "SHS", "SHY",
    "SHX", "LDY", "LDA", "LDX", "LAX", "TAX", "LXA", "TAY",
    "BCS", "CLV", "TSX", "LAS", "CPY", "CMP", "DCP", "DEC",
    "INY", "DEX", "SBX", "BNE", "CLD", "CPX", "SBC", "ISB",
    "INC", "INX", "BEQ", "SED", "X-LOAD_FILE", "X-SLEEP" };

  public static final String[] ADR_STR_PRE = {
    " ", " #", " Z ", " ", " Z ",
    " Z ", " ", " ", " ",
    " (", " (", " ACC", " ("
  };

  public static final String[] ADR_STR_POST = {
    "", "", "", "", ",X",
    ",Y", ",X", ",Y", "",
    ",X)", "),Y", "", ")"
  };

  public static final int[] ADR_LEN = {
    1, 2, 2, 3, 2, 2, 3, 3, 2, 2, 2, 1, 3
  };


  // Instruction set table
  public static final int[] INSTRUCTION_SET = {
    BRK, ORA, TRP, SLO, NOP, ORA, ASL, SLO,
    PHP, ORA, ASL, ANC, NOP, ORA, ASL, SLO,
    BPL, ORA, TRP, SLO, NOP, ORA, ASL, SLO,
    CLC, ORA, NOP, SLO, NOP, ORA, ASL, SLO,

    JSR, AND, TRP, RLA, BIT, AND, ROL, RLA,
    PLP, AND, ROL, ANC, BIT, AND, ROL, RLA,
    BMI, AND, TRP, RLA, NOP, AND, ROL, RLA,
    SEC, AND, NOP, RLA, NOP, AND, ROL, RLA,

    RTI, EOR, TRP, SRE, NOP, EOR, LSR, SRE,
    PHA, EOR, LSR, ASR, JMP, EOR, LSR, SRE,
    BVC, EOR, TRP, SRE, NOP, EOR, LSR, SRE,
    CLI, EOR, NOP, SRE, NOP, EOR, LSR, SRE,

    RTS, ADC, TRP, RRA, NOP, ADC, ROR, RRA,
    PLA, ADC, ROR, ARR, JMP, ADC, ROR, RRA,
    BVS, ADC, TRP, RRA, NOP, ADC, ROR, RRA,
    SEI, ADC, NOP, RRA, NOP, ADC, ROR, RRA,

    NOP, STA, NOP, SAX, STY, STA, STX, SAX,
    DEY, NOP, TXA, ANE, STY, STA, STX, SAX,
    BCC, STA, TRP, SHA, STY, STA, STX, SAX,
    TYA, STA, TXS, SHS, SHY, STA, SHX, SHA,

    LDY, LDA, LDX, LAX, LDY, LDA, LDX, LAX,
    TAY, LDA, TAX, LXA, LDY, LDA, LDX, LAX,
    BCS, LDA, TRP, LAX, LDY, LDA, LDX, LAX,
    CLV, LDA, TSX, LAS, LDY, LDA, LDX, LAX,

    CPY, CMP, NOP, DCP, CPY, CMP, DEC, DCP,
    INY, CMP, DEX, SBX, CPY, CMP, DEC, DCP,
    BNE, CMP, TRP, DCP, NOP, CMP, DEC, DCP,
    CLD, CMP, NOP, DCP, NOP, CMP, DEC, DCP,

    CPX, SBC, NOP, ISB, CPX, SBC, INC, ISB,
    INX, SBC, NOP, SBC, CPX, SBC, INC, ISB,
    BEQ, SBC, TRP, ISB, NOP, SBC, INC, ISB,
    SED, SBC, NOP, ISB, NOP, SBC, INC, ISB,
    // 0x100, 0x101
    LOAD_FILE, SLEEP
  };

  public static final int[] READ_INS = {
    LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC,
    CMP, CPX, CPY, BIT, LAX, LAS, NOP
  };

  public static final int[] WRITE_INS = {
    STA, STX, STY, SAX, SHA, SHX, SHY, SHS
  };

  public static final int[] RMW_INS = {
    ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE,
    RLA, RRA, ISB, DCP
  };

  static {
    init();
  }

  public static void init() {
    for (int i = 0, n = INSTRUCTION_SET.length; i < n; i++) {
      int mode = i & 0x1f;
      int pos = i >> 5;
      int instruction = INSTRUCTION_SET[i];
      if (i < 256) {
	INSTRUCTION_SET[i] |= getAdrMode(pos, mode);
	INSTRUCTION_SET[i] |= getOpMode(instruction);
      }
    }
  }

  public static int lookup(String instr) {
    instr = instr.toUpperCase();
    for (int i = 0, n = INS_STR.length; i < n; i++) {
      if (INS_STR[i].equals(instr)) return i;
    }
    return -1;
  }

  public static int lookup(int instr, int adrMode) {
    for (int i = 0, n = INSTRUCTION_SET.length; i < n; i++) {
      int op = INSTRUCTION_SET[i];
      int adr = (op & ADDRESSING_MASK);
      op = op & OP_MASK;
      if (op == instr && adr == adrMode) return i;
    }
    return -1;
  }

  public static String modeString(int mode) {
    switch(mode) {
    case IMMEDIATE:
      return "immediate";
    case ZERO:
      return "zero";
    case ABSOLUTE:
      return "absolute";
    case ZERO_X:
      return "zero,x";
    case ZERO_Y:
      return "zero,y";
    case ABSOLUTE_X:
      return "absolute,x";
    case ABSOLUTE_Y:
      return "absolute,y";
    case RELATIVE:
      return "relative";
    case INDIRECT_X:
      return "indirect,x";
    case INDIRECT_Y:
      return "indirect,y";
    case ACCUMULATOR:
      return "accumulator";
    case INDIRECT:
      return "indirect";
    case 0:
      return "implied";
    default:
      return "";
    }
  }

  public static String toString(int instr) {
    int op = INSTRUCTION_SET[instr];
    int adr = (op & ADDRESSING_MASK) >> 8;
    op = op & OP_MASK;
    return INS_STR[op] + ADR_STR_PRE[adr] + ADR_STR_POST[adr];
  }

  public static String toString(int instr, boolean rmw) {
    int i = INSTRUCTION_SET[instr];
    int adr = (i & ADDRESSING_MASK) >> 8;
    int op = i & OP_MASK;
    if (!rmw)
      return INS_STR[op] + ADR_STR_PRE[adr] + ADR_STR_POST[adr];


    String s = INS_STR[op] + ADR_STR_PRE[adr] + ADR_STR_POST[adr];
    int mode = i & MODE_MASK;
    if (mode == READ) return s + " read";
    if (mode == WRITE) return s + " write";
    if (mode == RMW) return s + " rmw";
    return s;
  }


  private static int getAdrMode(int pos, int m) {
    switch(m) {
    case 0: case 2:
      if (pos > 4) return IMMEDIATE;
      return 0;
    case 1: case 3:
      return INDIRECT_X;
    case 4: case 5: case 6: case 7:
      return ZERO;
    case 9: case 0xb:
      return IMMEDIATE;
    case 0xa:
      if (pos < 4) return ACCUMULATOR;
      return 0;
    case 0xc: case 0xd: case 0xe: case 0xf:
      if (m == 0x0c && pos == 3) return INDIRECT;// Only instruction for this
      return ABSOLUTE;
    case 0x10:
      return RELATIVE;
    case 0x11: case 0x13:
      return INDIRECT_Y;
    case 0x14: case 0x15:
      return ZERO_X;
    case 0x16: case 0x17:
      if (pos == 4 || pos == 5) return ZERO_Y;
      return ZERO_X;
    case 0x19: case 0x1b:
      return ABSOLUTE_Y;
    case 0x1c: case 0x1d:
      return ABSOLUTE_X;
    case 0x1e: case 0x1f:
      if (pos == 4 || pos == 5) return ABSOLUTE_Y;
      return ABSOLUTE_X;
    }
    return 0;
  }

  private static int getOpMode(int i) {
    for (int j = 0, m = READ_INS.length; j < m; j++) {
      if (READ_INS[j] == i) return READ;
    }
    for (int j = 0, m = WRITE_INS.length; j < m; j++) {
      if (WRITE_INS[j] == i) return WRITE;
    }
    for (int j = 0, m = RMW_INS.length; j < m; j++) {
      if (RMW_INS[j] == i) return RMW;
    }
    //System.out.println("OP: " + i + " not found...");
    return 0;
  }

  public static void main(String[] args) {
    init();


    for (int i = 0, n = 256 + 2; i < n; i++) {
      System.out.println(Hex.hex2(i) + " => " + toString(i, true) + " IS:"
			 + Hex.hex2(INSTRUCTION_SET[i]));
    }

    int b = lookup(BEQ, RELATIVE);
    System.out.println("BEQ # => " + b);

  }

}
