package com.dreamfabric.c64utils;

import java.util.Hashtable;
import java.util.Vector;
import com.dreamfabric.jac64.*;
import java.io.InputStream;

/**
 * A simple assembler for JaC64 (C64) / 6502
 * Joakim Eriksson, JaC64.com / Dreamfabric.com
 * ----------------------------------------------
 * Should be almost compatible (?) with a65 and
 * some other C64 assemblers
 * ----------------------------------------------
 * To get full functionality for loading binaries
 * you need to override openBinary!
 *
 * Created: Fri Dec 29 09:52:38 2006
 * 
 * @author Joakim Eriksson
 * @version 1.0
 */
public class Assembler {

  public static final int DEBUG_LEVEL = 0;

  public static final int CODE = 1;
  public static final int COMMENT = 2;

  public static final int REF_WORD = 0;
  public static final int REF_BYTE = 1;
  public static final int REF_BYTE_LO = 2;
  public static final int REF_BYTE_HI = 3;
  public static final int REF_RELATIVE = 4;

  public static final int[] REF_SIZE = new int[] {
    2,1,1,1,1
  };
  private int mode = CODE;
  // pos => address
  private int pos = 0;
  private int lastPos = 0;

  private Hashtable labels = new Hashtable();

  private Vector references = new Vector();

  private String currentLine;
  private String[] tokens;
  private int[] memory = new int[0x10000];
  private int lineNo;
  protected String workingDir;


  /**
   * Creates a new <code>Assembler</code> instance.
   *
   */
  public Assembler() {
  }

  public void setMemory(int[] memory) {
    this.memory = memory;
  }

  public void setWorkingDir(String dir) {
    workingDir = dir;
    if (!workingDir.endsWith("/")) {
      workingDir += "/";
    }
    System.out.println("Set working dir to " + workingDir);
  }

  public int[] assemble(String s, int start) {
    String[] lines = split(s, "\n", false);
    labels.clear();
    references.removeAllElements();

    setPos(start);
    mode = CODE;
    // First pass...
    for (int i = 0, n = lines.length; i < n; i++) {
      currentLine = lines[i];
      if (assembleLine(lines[i], memory, pos) == 1) break;
      lineNo++;
    }

    resolve();

    return memory;
  }

  private void setPos(int start) {
    pos = start;
    lastPos = pos;
    System.out.println("Location set to: " + start);
  }

  private int assembleLine(String lineOrig, int[] memory, int adr) {
    String line = lineOrig;
    if (DEBUG_LEVEL > 1)
      System.out.println("Assembling line: '" + line + "' at " + hex4(pos));
    if (mode == COMMENT) {
      if (line.endsWith("*/"))
	mode = CODE;
      return 0;
    }
    if (line.startsWith("/*")) {
      mode = COMMENT;
      return 0;
    }

    tokens = split(line, " \t", true);
    if (tokens == null || tokens.length == 0) return 0;
    String label = tokens[0].trim();
    if ((tokens.length == 0) || ((tokens.length == 1) && (label.length() == 0)))
      return 0;

    if (label.equals(".end")) {
      if (DEBUG_LEVEL > 0) {
	System.out.println("*** Assembly file ended (.end) ");
	System.out.println("-------------------------------");
	System.out.println("Total code size: " + currentLine + " lines");
	System.out.println("Ending position: " + hex4(pos));
	System.out.println("-------------------------------");
      }
      return 1;
    }

    String op = null;
    String operand = null;

    if (tokens.length > 1) op = tokens[1];
    if (tokens.length > 2) operand = tokens[2];

    if (label.length() > 0) {
      // Add label can "consume" this line
      if (addLabel(label, op, operand)) return 0;
    }

//     System.out.println("OP:" + op + " Operand: " + operand);

    char c = op.charAt(0);
    if (c == '.') {
      if (op.equals(".word")) {
	pos += setValue(REF_WORD, pos, operand, true);
      } else if (op.equals(".byte")) {
	pos += setValue(REF_BYTE, pos, operand, true);
      } else if (op.equals(".org")) {
	// label free...
	setPos(parseInt(operand));
      } else if (op.equals(".align")) {
	// label free...
	int al = parseInt(operand);
	System.out.println("Aligning to " + al);
	setPos(pos - (pos & (al - 1)) + al);
      } else if (op.equals(".binary")) {
	// label free...
	loadBinary(operand);
      } else if (op.equals(".wdir")) {
	// label free...
	setWorkingDir(operand);
      } else {
	error("unhandled operation '" + op + "'");
      }
    } else if (op.equals("*=")) {
      // label free...
      setPos(resolve(operand, pos));
    } else if (op.startsWith(";")) {
      // Ignore
      return 0;
    } else {
      // Here is an op?
      int opI = MOS6510Ops.lookup(op);
      // System.out.println("### OP:" + opI + " " + line);
      switch (opI) {
      case MOS6510Ops.BNE:
      case MOS6510Ops.BEQ:
      case MOS6510Ops.BCC:
      case MOS6510Ops.BCS:
      case MOS6510Ops.BVC:
      case MOS6510Ops.BPL:
      case MOS6510Ops.BMI:
	setBranch(MOS6510Ops.lookup(opI, MOS6510Ops.RELATIVE), operand);
	break;
	// ops
      case MOS6510Ops.PLA:
      case MOS6510Ops.PLP:
      case MOS6510Ops.PHA:
      case MOS6510Ops.PHP:
      case MOS6510Ops.TAY:
      case MOS6510Ops.TAX:
      case MOS6510Ops.TYA:
      case MOS6510Ops.TXA:
      case MOS6510Ops.TXS:
      case MOS6510Ops.TSX:
      case MOS6510Ops.RTS:
      case MOS6510Ops.RTI:
      case MOS6510Ops.SEI:
      case MOS6510Ops.CLI:
      case MOS6510Ops.CLC:
      case MOS6510Ops.CLD:
      case MOS6510Ops.CLV:
      case MOS6510Ops.SEC:
      case MOS6510Ops.SED:
      case MOS6510Ops.INX:
      case MOS6510Ops.INY:
      case MOS6510Ops.DEX:
      case MOS6510Ops.DEY:
      case MOS6510Ops.BRK:
      case MOS6510Ops.NOP:
	memory[pos++] = MOS6510Ops.lookup(opI, 0);
	//System.out.println("#### Single OP: " + MOS6510Ops.lookup(opI, 0));
	break;
      case MOS6510Ops.JMP:
      case MOS6510Ops.LDA:
      case MOS6510Ops.STA:
      case MOS6510Ops.LDX:
      case MOS6510Ops.STX:
      case MOS6510Ops.LDY:
      case MOS6510Ops.STY:
      case MOS6510Ops.EOR:
      case MOS6510Ops.ORA:
      case MOS6510Ops.AND:
      case MOS6510Ops.INC:
      case MOS6510Ops.DEC:
      case MOS6510Ops.CMP:
      case MOS6510Ops.CPY:
      case MOS6510Ops.CPX:
      case MOS6510Ops.ROL:
      case MOS6510Ops.ROR:
      case MOS6510Ops.LSR:
      case MOS6510Ops.ASL:
      case MOS6510Ops.ADC:
      case MOS6510Ops.SBC:
      case MOS6510Ops.BIT:
	c = operand.charAt(0);
	if (c == '#') {
	  // Immediate!
	  setOP(opI, MOS6510Ops.IMMEDIATE, pos++);
	  pos += setValue(REF_BYTE, pos, operand.substring(1));
	} else if (c == '(') {
	  String oplow = operand.toLowerCase();
	  int adrMode = MOS6510Ops.INDIRECT;
	  int size = REF_WORD;
	  if (oplow.endsWith(")")) {
	    operand = operand.substring(1, operand.length() - 1);
	  } else if (oplow.endsWith(",x)")) {
	    adrMode = MOS6510Ops.INDIRECT_X;
	    size = REF_BYTE;
	    operand = operand.substring(1, operand.length() - 3);
	  } else if (oplow.endsWith("),y")) {
	    adrMode = MOS6510Ops.INDIRECT_Y;
	    size = REF_BYTE;
	    operand = operand.substring(1, operand.length() - 3);
	  } else error("Illegal syntax on indirection op " + op);
	  setOP(opI, adrMode, pos++);
	  pos += setValue(size, pos, operand);
	} else {
	  String oplow = operand.toLowerCase();
	  int adrMode = MOS6510Ops.ABSOLUTE;
	  int adrModeZ = MOS6510Ops.ZERO;
	  int size = REF_WORD;
	  if (oplow.endsWith(",x")) {
	    operand = operand.substring(0, operand.length() - 2);
	    adrMode = MOS6510Ops.ABSOLUTE_X;
	    adrModeZ = MOS6510Ops.ZERO_X;
	  } else if (oplow.endsWith(",y")) {
	    operand = operand.substring(0, operand.length() - 2);
	    adrMode = MOS6510Ops.ABSOLUTE_Y;
	    adrModeZ = MOS6510Ops.ZERO_Y;
	  }
	  if (byteSize(operand)) {
	    size = REF_BYTE;
	    adrMode = adrModeZ;
	  }

	  setOP(opI, adrMode, pos++);
	  pos += setValue(size, pos, operand);
	}
	break;
      case MOS6510Ops.JSR:
	setOP(opI, 0, pos++);
	pos += setValue(REF_WORD, pos, operand);
	break;
      default:
	error("Unhandled OP: '" + op + "' at " + hex4(pos));
      }

    }


    if (DEBUG_LEVEL > 1) {
      System.out.print("Assembled line " + lineNo + ": " + lineOrig + " => ");
      for (int i = lastPos, n = pos; i < n; i++) {
	if (i > lastPos) System.out.print(", ");
	System.out.print(hex2(memory[i]));
      }
      System.out.println();
    }
    lastPos = pos;

    return 0;
  }

  // reads a string constand from the input string
  // FIX THIS!!! - is not 100% good since some op's can be made on strings...
  // see A65.txt...
  private int handleStringConstant(int pos, String operand) {
    char c = operand.charAt(0);
    int num = 0;
    if (c == '"') { // What is the stuffing method here?
      for (int i = 1, n = operand.length(); i < n; i++) {
	c = operand.charAt(i);
	if (c != '"') {
	  memory[pos] = c;
	  pos++;
	  num++;
	}
      }
    } else if (c == '\'') {
      boolean stuffed = false;
      for (int i = 1, n = operand.length(); i < n; i++) {
	c = operand.charAt(i);
	if (c != '\'' || stuffed) {
	  memory[pos] = c;
	  pos++;
	  num++;
	  stuffed = false;
	} else {
	  stuffed = true;
	}
      }
    }
    return num;
  }

  public InputStream openBinary(String binary) {
    return null;
    //    return new FileInputStream(binary);
  }

  private void loadBinary(String binary) {
    try {
      if (workingDir == null) workingDir = "";
      InputStream fs = openBinary(workingDir + binary);
      if (fs == null) return;
      int data = 0;
      int initPos = pos;
      while ((data = fs.read()) != -1) {
	memory[pos++] = data & 0xff;
      }
      System.out.println("Loaded binary file at: " + hex4(initPos) + " len: " +
			 (pos - initPos));
      fs.close();
    } catch (Exception e) {
      error("Could not load binary file " + binary);
    }
  }

  private void setOP(int opI, int mode, int pos) {
    int opR = MOS6510Ops.lookup(opI, mode);
    if (opR == -1) error(MOS6510Ops.modeString(mode) +
			 " mode not available for " + MOS6510Ops.INS_STR[opI]);
    memory[pos] = opR;
  }

  // This could handle arrays also!!! - and therefore also increase pos...
  private int setValue(int type, int pos, String value) {
    return setValue(type, pos, value, false);
  }
  private int setValue(int type, int pos, String value, boolean allowArrays) {
    if (allowArrays) {
      char c = value.charAt(0);
      if (c == '\'' || c == '"') {
	int lp = currentLine.indexOf(c);
	return handleStringConstant(pos, currentLine.substring(lp));
      } else {
	// handle ordinary array...
	// Can either be just picked from tokens or from something else...
	// The normal value is from tokens[2], here we can either have
	// multiple more tokens, or possibly one token with , as separator
	if (tokens.length > 3) {
	  return setValueArr(type, pos, tokens, 2);
	} else if (value.indexOf(',') > 0) {
	  String[] tok2 = split(value, ",", true);
	  return setValueArr(type, pos, tok2, 0);
	} else {
	  if (DEBUG_LEVEL > 0) System.out.println("Array? : value = " + value);
	}
      }
    }

    int val = parseInt(value);
    if (val != -1) {
      setValue(type, pos, val);
      return REF_SIZE[type];
    } else {
      // Label
      createRef(type, pos, value);
      return REF_SIZE[type];
    }
  }

  private int setValueArr(int type, int pos, String[] tokens, int start) {
    int len = 0;
    System.out.println("*** Possible array!!!");
    for (int i = start, n = tokens.length; i < n; i++) {
      if (tokens[i].startsWith(";")) return len;
      if (tokens[i].indexOf(',') >= 0) {
	len += setValueArr(type, pos + len, split(tokens[i], ",", true), 0);
      } else {
	len += setValue(type, pos + len, tokens[i], false);
	System.out.println("Arr[" + (i - start) + "] = " + tokens[i]);
      }
    }
    return len;
  }

  private void setValue(int type, int pos, int value) {
    switch (type) {
    case REF_WORD:
      setWordValue(pos, value);
      break;
    case REF_BYTE_HI:
      setByteValue(pos, value >> 8);
      break;
    case REF_BYTE_LO:
    case REF_BYTE:
      setByteValue(pos, value & 0xff);
      break;
    case REF_RELATIVE:
      setRelValue(pos, value);
      break;
    }
  }

  private void error(String s) {
    throw new IllegalArgumentException(s + " at line " + lineNo);
  }

  private void resolve() {
    if (DEBUG_LEVEL > 0) {
      System.out.println("Resolving.... " + references.size());
    }
    for (int i = 0, n = references.size(); i < n; i += 2) {
      String name = (String) references.elementAt(i);
      int[] data = (int[]) references.elementAt(i + 1);
      int type = data[0];
      int pos = data[1];

      if (DEBUG_LEVEL > 0) {
	System.out.print("Resolving " + name);
      }
      int val = resolve(name, pos);
      if (DEBUG_LEVEL > 0) {
	System.out.println(" to " + hex4(val) + " at pos:" + hex4(pos));
      }
      setValue(type, pos, val);
    }
  }

  private int resolve(String name, int pos) {
    char c = name.charAt(0);
    if (c == '<') {
      return resolve(name.substring(1), pos) & 0xff;
    } else if (c == '>') {
      return resolve(name.substring(1), pos) >> 8;
    }

    // split on "special" chars (+-/*, etc)
    for (int i = 0, n = name.length(); i < n; i++) {
      c = name.charAt(i);
      if (c == '+') {
	return resolve(name.substring(0, i), pos) +
	  resolve(name.substring(i + 1), pos);
      } else if (c == '-') {
	return resolve(name.substring(0, i), pos) -
	  resolve(name.substring(i + 1), pos);
      } else if (c == '/') {
	return resolve(name.substring(0, i), pos) /
	  resolve(name.substring(i + 1), pos);
      } else if (c == '*') {
	if (i == 0) {
	  // return the position - 1 since op started at -1
	  if (n == 1) return (pos - 1);
	  // if not just a star => it is an expression... let it
	  // but cut i pieces...
	} else {
	  return resolve(name.substring(0, i), pos) *
	    resolve(name.substring(i + 1), pos);
	}
      }
    }

    int adr = getLabelAddress(name);


    if (adr == -1) {
      // Here we should try parsing as an integer also...
      int val = parseInt(name);
      if (val == -1)
	error("### Could not find label " + name);
      return val;
    }

    return adr;
  }


  private void setBranch(int op, String line) {
    memory[pos++] = op;
    setValue(REF_RELATIVE, pos, line);
    pos++;
  }

  private boolean byteSize(String s) {
    int x = parseInt(s);
    if (x != -1 && x < 0x100) return true;
    // Should also check labels, etc...
    return false;
  }

  public static int parseInt(String s) {
    char c = s.charAt(0);
    if (s.endsWith(",")) {
      // Formulated as x, y, z, ...
      s = s.substring(0, s.length() - 1);
      System.out.println("Ends with , => " + s);
    }
    int val = -1;
    try {
      if (c == '$') {
	s = s.substring(1);
	val = Integer.parseInt(s, 16);
      } else if (c == '%') {
	s = s.substring(1);
	val = Integer.parseInt(s, 2);
      } else if (c == '@') {
	s = s.substring(1);
	val = Integer.parseInt(s, 8);
      } else {
	val = Integer.parseInt(s);
      }
    } catch (Exception e) {
    }
    return val;
  }

  private void setWordValue(int pos, int val) {
    memory[pos] = val & 0xff;
    memory[pos + 1] = (val >> 8) & 0xff;
  }

  private void setByteValue(int pos, int val) {
    memory[pos] = val & 0xff;
  }

  // Current address (pos) and target address
  // val - pos => branch positive values is forward
  // Note: pos needs to be at position after branch op when
  // calculating this!
  private void setRelValue(int pos, int val) {
    // Branch value!
    memory[pos] = (val - (pos + 1)) & 0xff;
    if (DEBUG_LEVEL > 0) {
      System.out.println("Set branch value of " + memory[pos] + " at " +
			 hex4(pos));
    }
  }

  private void createRef(int type, int pos, String s) {
    s = s.toLowerCase().trim();
    if (DEBUG_LEVEL > 0) {
      System.out.println("*** Creating reference to: '" + s + "' at " +
			 hex4(pos));
    }
    // The labelname + values!
    references.addElement(s);
    references.addElement(new int[] {type, pos});
  }

  // returns true if this line does not need more handling...
  private boolean addLabel(String label, String op, String operand) {
    if (label.startsWith(";")) return true;

    boolean rval = false;
    if (op == null || (op != null && op.startsWith(";"))) {
      rval = true;
    }

    if ("*".equals(label)) {
      if ("=".equals(op)) {
	setPos(parseInt(operand));
	System.out.println("*** New location: " + hex4(pos));
      }
      return true;
    } else {
      int[] lpos = new int[1];
      if ("=".equals(op) || ".equ".equals(op)) {
	lpos[0] = parseInt(operand);
	rval = true;
      } else {
	// special treatment of .org since it needs to change pos
	// before setting the label!
	if (".org".equals(op)) {
	  setPos(parseInt(operand));
	  rval = true;
	}
	lpos[0] = pos;
      }
      labels.put(label, lpos);
      if (DEBUG_LEVEL > 0) {
	System.out.println("*** Added label: '" + label + "' = " +
			   hex4(lpos[0]));
      }
      return rval;
    }
  }

  public int getLabelAddress(String label) {
    int[] lpos;
    lpos = (int[]) labels.get(label);
    if (lpos != null) {
      return lpos[0];
    }
    return -1;
  }

  public void setByteValue(String label, int val) {
    System.out.println("Setting byte value of " + label + " to " +
		       Integer.toString(val, 16));
    int a = getLabelAddress(label);
    if (a != -1) {
      setByteValue(a, val);
    } else throw new IllegalArgumentException("Can not find label: " + label);
  }

  public void setWordValue(String label, int val) {
    System.out.println("Setting word value of " + label + " to " +
		       Integer.toString(val, 16));
    int a = getLabelAddress(label);
    if (a != -1) {
      setWordValue(a, val);
    } else throw new IllegalArgumentException("Can not find label: " + label);
  }

  private String hex4(int pos) {
    String s = null;
    if (pos < 0x10) s = "$000";
    else if (pos < 0x100) s = "$00";
    else if (pos < 0x1000) s = "$0";
    else s = "$";
    return s + Integer.toString(pos, 16);
  }

  private String hex2(int pos) {
    String s = null;
    if (pos < 0x10) s = "$0";
    else s = "$";
    return s + Integer.toString(pos, 16);
  }

  // Splits on chars in the splits string.
  private static String[] split(String data, String splits, boolean trim) {
    // System.out.println("Split called: '" + data + "' <?> " + splits);
    Vector strings = new Vector();
    int lastPos = 0;
    int mode = 0;
    for (int i = 0, n = data.length(); i < n; i++) {
      char c = data.charAt(i);
      if (splits.indexOf(c) != -1) {
	if (mode == 0) {
	  if (lastPos != 0 && ((lastPos + 1) < n))
	    lastPos++;
	  strings.addElement(data.substring(lastPos, i));
	}
	lastPos = i;
	mode = 1;
      } else {
	mode = 0;
      }
    }
    if (lastPos != 0 && lastPos + 1 < data.length())
      lastPos++;
    strings.addElement(data.substring(lastPos, data.length()));

    String[] retval = new String[strings.size()];
    for (int i = 0, n = retval.length; i < n; i++) {
      retval[i] = (String) strings.elementAt(i);
    }
    return retval;
  }
}
