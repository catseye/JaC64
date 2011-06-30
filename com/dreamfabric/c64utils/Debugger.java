/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.c64utils;
import com.dreamfabric.jac64.*;

/**
 * @author jblok
 * @author Joakim Eriksson (joakime@sics.se
 */
public class Debugger implements IMonitor,M6510Ops
{
  private boolean DEBUG = false;
  private boolean DEBUG_IRQ = true;
  private boolean noinstructions = false;
  private int level = 10;
  String prefix = "";
  public long lastCycles = 0;


  private MOS6510Core cpu;
  private int[] memory;

  public void init(MOS6510Core cpu) {
    this.cpu = cpu;
    memory = cpu.getMemory();
  }

  /**
   * @see IMonitor#isEnabled()
   */
  public boolean isEnabled()
  {
    return DEBUG;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void info(Object o) {
    output((String) o);
  }

  public void warning(Object o) {
    output((String) o);
  }

  public void error(Object o) {
    output((String) o);
  }

  private void output(String s) {
    if (prefix != null) {
      if (s.startsWith(prefix)) {
	System.out.println(s);
      }
    } else {
      System.out.println(s);
    }
  }

  private int fetchByte(int pc) {
    return memory[pc];
  }

  /**
   * Only for debugging purposes
   * @see IMonitor#disAssemble(int, int, int, int, int, byte, boolean)
   */
  public void disAssemble(int[] memory,
			  int pc, int acc, int x, int y,
			  byte status, int interruptInExec,
			  int lastInterrupt) {
    if (DEBUG || (interruptInExec > 0)) {

      int startPC = pc;
      StringBuffer line = new StringBuffer();
      if (!noinstructions) {
	line.append(Integer.toHexString(pc));
	for(int i = 5-line.length(); i >= 0 ;i--) {
	  line.append(" ");
	}

	// Will not be exactly the same as the PC fetch?!
	int data = MOS6510Ops.INSTRUCTION_SET[memory[pc++]];
	int op = data & MOS6510Ops.OP_MASK;
	int addrMode = data & MOS6510Ops.ADDRESSING_MASK;
	int adMode = addrMode >> 8;
	boolean read = (data & MOS6510Ops.READ) != 0;
	boolean write = (data & MOS6510Ops.WRITE) != 0;
	int adr = 0;
	int tmp = 0;

	// fetch first argument (always fetched...) - but not always pc++!!
	int p1 = memory[pc];

	line.append(MOS6510Ops.INS_STR[op]);
	line.append(MOS6510Ops.ADR_STR_PRE[adMode]);

	// Fetch addres, and read if it should be done!
	switch (addrMode) {
	  // never any address when immediate
	case MOS6510Ops.IMMEDIATE:
	  pc++;
	  data = p1;
	  line.append("$" + Hex.hex2(data));
	  break;
	case MOS6510Ops.ABSOLUTE:
	  pc++;
	  adr = (fetchByte(pc++) << 8) + p1;
	  line.append("$" + Hex.hex2(adr));
	  if (read) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.ZERO:
	  pc++;
	  adr = p1;
	  line.append("$" + Hex.hex2(adr));
	  if (read) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.ZERO_X:
	case MOS6510Ops.ZERO_Y:
	  pc++;
	  // Read from wrong address first...
	  fetchByte(p1);
	  if (addrMode == MOS6510Ops.ZERO_X)
	    adr = (p1 + x) & 0xff;
	  else
	    adr = (p1 + y) & 0xff;

	  line.append("$" + Hex.hex2(adr));
	  if (read) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.ABSOLUTE_X:
	case MOS6510Ops.ABSOLUTE_Y:
	  pc++;
	  // Fetch hi byte!
	  adr = fetchByte(pc++) << 8;
	  // add x/y to low byte & possibly faulty fetch!
	  if (addrMode == MOS6510Ops.ABSOLUTE_X)
	    p1 += x;
	  else
	    p1 += y;

	  data = fetchByte(adr + (p1 & 0xff));
	  adr += p1;

	  // If read - a fifth cycle patches the incorrect address...
	  // Always done if RMW!
	  line.append("$" + Hex.hex2(adr));
	  if (read && (p1 > 0xff || write)) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.RELATIVE:
	  pc++;
	  adr = pc + (byte) p1;
	  if (((adr ^ pc) & 0xff00) > 0) {
	    // loose one cycle since adr is on another page...
	    tmp = 2;
	  } else {
	    tmp = 1;
	  }
	  line.append("$" + Hex.hex2(adr));
	  break;
	case MOS6510Ops.ACCUMULATOR:
	  data = acc;
	  write = false;
	  break;
	case MOS6510Ops.INDIRECT_X:
	  pc++;
	  // unneccesary read... fetchByte(p1);
	  tmp = (p1 + x) & 0xff;
	  adr = (fetchByte(tmp + 1) << 8);
	  adr |= fetchByte(tmp);

	  line.append("$" + Hex.hex2(adr));
	  if (read) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.INDIRECT_Y:
	  pc++;
	  // Fetch hi and lo
	  adr = (fetchByte(p1 + 1) << 8);
	  p1 = fetchByte(p1);
	  p1 += y;

	  data = fetchByte(adr + (p1 & 0xff));
	  adr += p1;
	  // If read - a sixth cycle patches the incorrect address...
	  // Always done if RMW!
	  line.append("$" + Hex.hex2(adr));

	  if (read && (p1 > 0xff || write)) {
	    data = fetchByte(adr);
	    line.append("=" + Hex.hex2(data));
	  }
	  break;
	case MOS6510Ops.INDIRECT:
	  pc++;
	  // Fetch pointer
	  adr = (fetchByte(pc) << 8) + p1;
	  // Calculate address
	  tmp = (adr & 0xfff00) | ((adr + 1) & 0xff);
	  // fetch the real address
	  adr = (fetchByte(tmp) << 8) + fetchByte(adr);
	  line.append("$" + Hex.hex2(adr));
	  break;
	}

	line.append(MOS6510Ops.ADR_STR_POST[adMode]);

	StringBuffer bytes = new StringBuffer();
	for (int i = startPC; i < pc; i++) {
	  bytes.append(Integer.toString(memory[i], 16));
	  bytes.append(" ");
	}

	for (int i = 9-bytes.length(); i > 0; i--) {
	  bytes.append(" ");
	}

	if (interruptInExec > 0) {
	  if (lastInterrupt == MOS6510Core.NMI_INT)
	    bytes.append("[NMI] " + interruptInExec + " ");
	  else
	    bytes.append("[IRQ] " + interruptInExec + " ");
	} else {
	  bytes.append("        ");
	}

	if (line.length() > 6) line.insert(6, bytes.toString());

	for (int i = 45-line.length(); i > 0; i--) {
	  line.append(" ");
	}

	if (!noinstructions) {
	  line.append("A:");
	  line.append(Hex.hex2(acc));
	  line.append(" X:");
	  line.append(Hex.hex2(x));
	  line.append(" Y:");
	  line.append(Hex.hex2(y));
	  line.append(" ");

	  boolean carry = ((status & 0x01) != 0);
	  boolean zero = ((status & 0x02) != 0);
	  boolean disableInterupt = ((status & 0x04) != 0);
	  boolean decimal = ((status & 0x08) != 0);
	  boolean brk = ((status & 0x10) != 0);
	  boolean overflow = ((status & 0x40) != 0);
	  boolean sign = ((status & 0x80) != 0);
	  line.append(carry ? "C" : "-");
	  line.append(zero ? "Z" : "-");
	  line.append(disableInterupt ? "I" : "-");
	  line.append(decimal ? "D" : "-");
	  line.append(brk ? "B" : "-");
	  line.append(overflow ? "O" : "-");
	  line.append(sign ? "S" : "-");

	  if (DEBUG_IRQ) {
	    line.append(" ");
	    line.append(cpu.NMILow ? "N" : "-");
	    line.append(cpu.getIRQLow() ? "I" : "-");
	  }
	  line.append(" S:" + Hex.hex2(cpu.getSP()));
	  line.append(" " + Hex.hex2((int) (cpu.cycles - lastCycles)));

	  // Print out system routine names
	  getFunction(memory, startPC, line);
	}
	System.out.println(line);

	lastCycles = cpu.cycles;

      //		    if (line.length() > 0)
      //		    {
      ////				while (line.length() < 40) line.append(" ");
      //
      //				if (noinstructions)
      //				{
      //				    line.append(" pc =");
      //					line.append(Integer.toString(pc, 16));
      //					System.out.println(str);
      //				}
      //				else
      //				{
      //				    line.insert(0,'\n');
      //				    System.out.print(str);
      //				}
      //		    }
      }
    }
  }

  public void getFunction(int[] memory, int startPC, StringBuffer line) {
    if ((memory[1] & 3) == 3)
      switch (startPC) {
      case 0xa000:
	line.append("  % -	Restart Vectors				WORD");
	break;

      case 0xa00c:
	line.append("  % stmdsp	BASIC Command Vectors			WORD");
	break;

      case 0xa052:
	line.append("  % fundsp	BASIC Function Vectors			WORD");
	break;

      case 0xa080:
	line.append("  % optab	BASIC Operator Vectors			WORD");
	break;

      case 0xa09e:
	line.append("  % reslst	BASIC Command Keyword Table		DATA");
	break;

      case 0xa129:
	line.append("  % msclst	BASIC Misc. Keyword Table		DATA");
	break;

      case 0xa140:
	line.append("  % oplist	BASIC Operator Keyword Table		DATA");
	break;

      case 0xa14d:
	line.append("  % funlst	BASIC Function Keyword Table		DATA");
	break;

      case 0xa19e:
	line.append("  % errtab	Error Message Table			DATA");
	break;

      case 0xa328:
	line.append("  % errptr	Error Message Pointers			WORD");
	break;

      case 0xa364:
	line.append("  % okk	Misc. Messages				TEXT");
	break;

      case 0xa38a:
	line.append("  % fndfor	Find FOR/GOSUB Entry on Stack");
	break;

      case 0xa3b8:
	line.append("  % bltu	Open Space in Memory");
	break;

      case 0xa3fb:
	line.append("  % getstk	Check Stack Depth");
	break;

      case 0xa408:
	line.append("  % reason	Check Memory Overlap / Array area overflow check");
	break;

      case 0xa435:
	line.append("  % omerr	Output ?OUT OF MEMORY Error");
	break;

      case 0xa437:
	line.append("  % error	Error Routine");
	break;

      case 0xa469:
	line.append("  % errfin	Break Entry");
	break;

      case 0xa474:
	line.append("  % ready	Restart BASIC");
	break;

      case 0xa480:
	line.append("  % main	Input & Identify BASIC Line");
	break;

      case 0xa49c:
	line.append("  % main1	Get Line Number & Tokenise Text");
	break;

      case 0xa4a2:
	line.append("  % inslin	Insert BASIC Text");
	break;

      case 0xa533:
	line.append("  % linkprg	Rechain Lines");
	break;

      case 0xa560:
	line.append("  % inlin	Input Line Into Buffer");
	break;

      case 0xa579:
	line.append("  % crunch	Tokenise Input Buffer");
	break;

      case 0xa613:
	line.append("  % fndlin	Search for Line Number");
	break;

      case 0xa642:
	line.append("  % scrtch	Perform [new]");
	break;

      case 0xa65e:
	line.append("  % clear	Perform [clr]");
	break;

      case 0xa68e:
	line.append("  % stxpt	Reset TXTPTR");
	break;

      case 0xa69c:
	line.append("  % list	Perform [list]");
	break;

      case 0xa717:
	line.append("  % qplop	Handle LIST Character");
	break;

      case 0xa742:
	line.append("  % for	Perform [for]");
	break;

      case 0xa7ae:
	line.append("  % newstt	BASIC Warm Start");
	break;

      case 0xa7c4:
	line.append("  % ckeol	Check End of Program");
	break;

      case 0xa7e1:
	line.append("  % gone	Prepare to execute statement");
	break;

      case 0xa7ed:
	line.append("  % gone3	Perform BASIC Keyword,Execute command in A");
	break;

      case 0xa81d:
	line.append("  % restor	Perform [restore]");
	break;

      case 0xa82c:
	line.append("  % stop	Perform [stop], [end], break");
	break;

      case 0xa857:
	line.append("  % cont	Perform [cont]");
	break;

      case 0xa871:
	line.append("  % run	Perform [run]");
	break;

      case 0xa883:
	line.append("  % gosub	Perform [gosub]");
	break;

      case 0xa8a0:
	line.append("  % goto	Perform [goto]");
	break;

      case 0xa8d2:
	line.append("  % return	Perform [return]");
	break;

      case 0xa8f8:
	line.append("  % data	Perform [data]");
	break;

      case 0xa906:
	line.append("  % datan	Search for Next Statement / Line");
	break;

      case 0xa928:
	line.append("  % if	Perform [if]");
	break;

      case 0xa93b:
	line.append("  % rem	Perform [rem]");
	break;

      case 0xa94b:
	line.append("  % ongoto	Perform [on]");
	break;

      case 0xa96b:
	line.append("  % linget	Fetch linnum From BASIC");
	break;

      case 0xa9a5:
	line.append("  % let	Perform [let]");
	break;

      case 0xa9c4:
	line.append("  % putint	Assign Integer");
	break;

      case 0xa9d6:
	line.append("  % ptflpt	Assign Floating Point");
	break;

      case 0xa9d9:
	line.append("  % putstr	Assign String");
	break;

      case 0xa9e3:
	line.append("  % puttim	Assign TI$");
	break;

      case 0xaa2c:
	line.append("  % getspt	Add Digit to FAC#1");
	break;

      case 0xaa80:
	line.append("  % printn	Perform [print]#");
	break;

      case 0xaa86:
	line.append("  % cmd	Perform [cmd]");
	break;

      case 0xaa9a:
	line.append("  % strdon	Print String From Memory");
	break;

      case 0xaaa0:
	line.append("  % print	Perform [print]");
	break;

      case 0xaab8:
	line.append("  % varop	Output Variable");
	break;

      case 0xaad7:
	line.append("  % crdo	Output CR/LF");
	break;

      case 0xaae8:
	line.append("  % comprt	Handle comma, TAB(, SPC(");
	break;

      case 0xab1e:
	line.append("  % strout	Output String");
	break;

      case 0xab3b:
	line.append("  % outspc	Output Format Character");
	break;

      case 0xab4d:
	line.append("  % doagin	Handle Bad Data");
	break;

      case 0xab7b:
	line.append("  % get	Perform [get]");
	break;

      case 0xaba5:
	line.append("  % inputn	Perform [input#]");
	break;

      case 0xabbf:
	line.append("  % input	Perform [input]");
	break;

      case 0xabea:
	line.append("  % bufful	Read Input Buffer");
	break;

      case 0xabf9:
	line.append("  % qinlin	Do Input Prompt");
	break;

      case 0xac06:
	line.append("  % read	Perform [read]");
	break;

      case 0xac35:
	line.append("  % rdget	General Purpose Read Routine");
	break;

      case 0xacfc:
	line.append("  % exint	Input Error Messages			TEXT");
	break;

      case 0xad1e:
	line.append("  % next	Perform [next]");
	break;

      case 0xad61:
	line.append("  % donext	Check Valid Loop");
	break;

      case 0xad8a:
	line.append("  % frmnum	Confirm Result");
	break;

      case 0xad9e:
	line.append("  % frmevl	Evaluate Expression in Text");
	break;

      case 0xae83:
	line.append("  % eval	Evaluate Single Term");
	break;

      case 0xaea8:
	line.append("  % pival	Constant - pi				DATA");
	break;

      case 0xaead:
	line.append("  % qdot	Continue Expression");
	break;

      case 0xaef1:
	line.append("  % parchk	Expression in Brackets");
	break;

      case 0xaef7:
	line.append("  % chkcls	Confirm Character");
	break;

	//				case 0xaef7:
	//				line.append("  % -	-test ')'-");
	//				break;

      case 0xaefa:
	line.append("  % -	-test '('-");
	break;

      case 0xaefd:
	line.append("  % -	-test comma-");
	break;

      case 0xaf08:
	line.append("  % synerr	Output ?SYNTAX Error");
	break;

      case 0xaf0d:
	line.append("  % domin	Set up NOT Function");
	break;

      case 0xaf14:
	line.append("  % rsvvar	Identify Reserved Variable");
	break;

      case 0xaf28:
	line.append("  % isvar	Search for Variable");
	break;

      case 0xaf48:
	line.append("  % tisasc	Convert TI to ASCII String");
	break;

      case 0xafa7:
	line.append("  % isfun	Identify Function Type");
	break;

      case 0xafb1:
	line.append("  % strfun	Evaluate String Function");
	break;

      case 0xafd1:
	line.append("  % numfun	Evaluate Numeric Function");
	break;

      case 0xafe6:
	line.append("  % orop	Perform [or], [and]");
	break;

      case 0xb016:
	line.append("  % dorel	Perform <, =, >");
	break;

      case 0xb01b:
	line.append("  % numrel	Numeric Comparison");
	break;

      case 0xb02e:
	line.append("  % strrel	String Comparison");
	break;

      case 0xb07e:
	line.append("  % dim	Perform [dim]");
	break;

      case 0xb08b:
	line.append("  % ptrget	Identify Variable");
	break;

      case 0xb0e7:
	line.append("  % ordvar	Locate Ordinary Variable");
	break;

      case 0xb11d:
	line.append("  % notfns	Create New Variable");
	break;

      case 0xb128:
	line.append("  % notevl	Create Variable");
	break;

      case 0xb194:
	line.append("  % aryget	Allocate Array Pointer Space");
	break;

      case 0xb1a5:
	line.append("  % n32768	Constant 32768 in Flpt			DATA");
	break;

      case 0xb1aa:
	line.append("  % facinx	FAC#1 to Integer in (AC/YR)");
	break;

      case 0xb1b2:
	line.append("  % intidx	Evaluate Text for Integer (convert)");
	break;

      case 0xb1bf:
	line.append("  % ayint	Float (FAC#1) to Positive Integer (convert)");
	break;

      case 0xb1d1:
	line.append("  % isary	Get Array Parameters");
	break;

      case 0xb218:
	line.append("  % fndary	Find Array");
	break;

      case 0xb245:
	line.append("  % bserr	?BAD SUBSCRIPT/?ILLEGAL QUANTITY");
	break;

      case 0xb261:
	line.append("  % notfdd	Create Array");
	break;

      case 0xb30e:
	line.append("  % inlpn2	Locate Element in Array");
	break;

      case 0xb34c:
	line.append("  % umult	Number of Bytes in Subscript");
	break;

      case 0xb37d:
	line.append("  % fre	Perform [fre]");
	break;

      case 0xb391:
	line.append("  % givayf	Convert Integer in (AC/YR) to Flpt");
	break;

      case 0xb39e:
	line.append("  % pos	Perform [pos]");
	break;

      case 0xb3a6:
	line.append("  % errdir	Confirm Program Mode");
	break;

      case 0xb3e1:
	line.append("  % getfnm	Check Syntax of FN");
	break;

      case 0xb3f4:
	line.append("  % fndoer	Perform [fn]");
	break;

      case 0xb465:
	line.append("  % strd	Perform [str$]");
	break;

      case 0xb487:
	line.append("  % strlit	Set Up String");
	break;

      case 0xb4d5:
	line.append("  % putnw1	Save String Descriptor");
	break;

      case 0xb4f4:
	line.append("  % getspa	Allocate Space for String");
	break;

      case 0xb526:
	line.append("  % garbag	Garbage Collection");
	break;

      case 0xb5bd:
	line.append("  % dvars	Search for Next String");
	break;

      case 0xb606:
	line.append("  % grbpas	Collect a String");
	break;

      case 0xb63d:
	line.append("  % cat	Concatenate Two Strings");
	break;

      case 0xb67a:
	line.append("  % movins	Store String in High RAM");
	break;

      case 0xb6a3:
	line.append("  % frestr	Perform String Housekeeping");
	break;

      case 0xb6db:
	line.append("  % frefac	Clean Descriptor Stack");
	break;

      case 0xb6ec:
	line.append("  % chrd	Perform [chr$]");
	break;

      case 0xb700:
	line.append("  % leftd	Perform [left$]");
	break;

      case 0xb72c:
	line.append("  % rightd	Perform [right$]");
	break;

      case 0xb737:
	line.append("  % midd	Perform [mid$]");
	break;

      case 0xb761:
	line.append("  % pream	Pull sTring Parameters");
	break;

      case 0xb77c:
	line.append("  % len	Perform [len]");
	break;

      case 0xb782:
	line.append("  % len1	Exit String Mode");
	break;

      case 0xb78b:
	line.append("  % asc	Perform [asc]");
	break;

      case 0xb79b:
	line.append("  % gtbytc	Evaluate Text to 1 Byte in XR");
	break;

      case 0xb7ad:
	line.append("  % val	Perform [val]");
	break;

      case 0xb7b5:
	line.append("  % strval	Convert ASCII String to Flpt");
	break;

      case 0xb7eb:
	line.append("  % getnum	Get parameters for POKE/WAIT");
	break;

      case 0xb7f7:
	line.append("  % getadr	Convert FAC#1 to Integer in LINNUM");
	break;

      case 0xb80d:
	line.append("  % peek	Perform [peek]");
	break;

      case 0xb824:
	line.append("  % poke	Perform [poke]");
	break;

      case 0xb82d:
	line.append("  % wait	Perform [wait]");
	break;

      case 0xb849:
	line.append("  % faddh	Add 0.5 to FAC#1");
	break;

      case 0xb850:
	line.append("  % fsub	Perform Subtraction");
	break;

      case 0xb862:
	line.append("  % fadd5	Normalise Addition");
	break;

      case 0xb867:
	line.append("  % fadd	Perform Addition");
	break;

      case 0xb947:
	line.append("  % negfac	2's Complement FAC#1");
	break;

      case 0xb97e:
	line.append("  % overr	Output ?OVERFLOW Error");
	break;

      case 0xb983:
	line.append("  % mulshf	Multiply by Zero Byte");
	break;

      case 0xb9bc:
	line.append("  % fone	Table of Flpt Constants			DATA");
	break;

      case 0xb9ea:
	line.append("  % log	Perform [log]");
	break;

      case 0xba28:
	line.append("  % fmult	Perform Multiply");
	break;

      case 0xba59:
	line.append("  % mulply	Multiply by a Byte");
	break;

      case 0xba8c:
	line.append("  % conupk	Load FAC#2 From Memory");
	break;

      case 0xbab7:
	line.append("  % muldiv	Test Both Accumulators");
	break;

      case 0xbad4:
	line.append("  % mldvex	Overflow / Underflow");
	break;

      case 0xbae2:
	line.append("  % mul10	Multiply FAC#1 by 10");
	break;

      case 0xbaf9:
	line.append("  % tenc	Constant 10 in Flpt			DATA");
	break;

      case 0xbafe:
	line.append("  % div10	Divide FAC#1 by 10");
	break;

      case 0xbb07:
	line.append("  % fdiv	Divide FAC#2 by Flpt at (AC/YR)");
	break;

      case 0xbb0f:
	line.append("  % fdivt	Divide FAC#2 by FAC#1");
	break;

      case 0xbba2:
	line.append("  % movfm	Load FAC#1 From Memory");
	break;

      case 0xbbc7:
	line.append("  % mov2f	Store FAC#1 in Memory");
	break;

      case 0xbbfc:
	line.append("  % movfa	Copy FAC#2 into FAC#1");
	break;

      case 0xbc0c:
	line.append("  % movaf	Copy FAC#1 into FAC#2");
	break;

      case 0xbc1b:
	line.append("  % round	Round FAC#1");
	break;

      case 0xbc2b:
	line.append("  % sign	Check Sign of FAC#1");
	break;

      case 0xbc39:
	line.append("  % sgn	Perform [sgn]");
	break;

      case 0xbc58:
	line.append("  % abs	Perform [abs]");
	break;

      case 0xbc5b:
	line.append("  % fcomp	Compare FAC#1 With Memory");
	break;

      case 0xbc9b:
	line.append("  % qint	Convert FAC#1 to Integer");
	break;

      case 0xbccc:
	line.append("  % int	Perform [int]");
	break;

      case 0xbcf3:
	line.append("  % fin	Convert ASCII String to a Float in FAC#1");
	break;

      case 0xbdb3:
	line.append("  % n0999	String Conversion Constants		DATA");
	break;

      case 0xbdc2:
	line.append("  % inprt	Output 'IN' and Line Number");
	break;

      case 0xbddd:
	line.append("  % fout	Convert FAC#1 to ASCII String");
	break;

      case 0xbe68:
	line.append("  % foutim	Convert TI to String");
	break;

      case 0xbf11:
	line.append("  % fhalf	Table of Constants			DATA");
	break;

      case 0xbf71:
	line.append("  % sqr	Perform [sqr]");
	break;

      case 0xbf7b:
	line.append("  % fpwrt	Perform power ($)");
	break;

      case 0xbfb4:
	line.append("  % negop	Negate FAC#1");
	break;

      case 0xbfbf:
	line.append("  % logeb2	Table of Constants			DATA");
	break;

      case 0xbfed:
	line.append("  % exp	Perform [exp]");
	break;

      case 0xe000:
	line.append("  % (exp continues)	EXP continued From BASIC ROM");
	break;

      case 0xe043:
	line.append("  % polyx	Series Evaluation");
	break;

      case 0xe08d:
	line.append("  % rmulc	Constants for RND			DATA");
	break;

      case 0xe097:
	line.append("  % rnd	Perform [rnd]");
	break;

      case 0xe0f9:
	line.append("  % bioerr	Handle I/O Error in BASIC");
	break;

      case 0xe10c:
	line.append("  % bchout	Output Character");
	break;

      case 0xe112:
	line.append("  % bchin	Input Character");
	break;

      case 0xe118:
	line.append("  % bckout	Set Up For Output");
	break;

      case 0xe11e:
	line.append("  % bckin	Set Up For Input");
	break;

      case 0xe124:
	line.append("  % bgetin	Get One Character");
	break;

      case 0xe12a:
	line.append("  % sys	Perform [sys]");
	break;

      case 0xe156:
	line.append("  % savet	Perform [save]");
	break;

      case 0xe165:
	line.append("  % verfyt	Perform [verify / load]");
	break;

      case 0xe1be:
	line.append("  % opent	Perform [open]");
	break;

      case 0xe1c7:
	line.append("  % closet	Perform [close]");
	break;

      case 0xe1d4:
	line.append("  % slpara	Get Parameters For LOAD/SAVE");
	break;

      case 0xe200:
	line.append("  % combyt	Get Next One Byte Parameter");
	break;

      case 0xe206:
	line.append("  % deflt	Check Default Parameters");
	break;

      case 0xe20e:
	line.append("  % cmmerr	Check For Comma");
	break;

      case 0xe219:
	line.append("  % ocpara	Get Parameters For OPEN/CLOSE");
	break;

      case 0xe264:
	line.append("  % cos	Perform [cos]");
	break;

      case 0xe26b:
	line.append("  % sin	Perform [sin]");
	break;

      case 0xe2b4:
	line.append("  % tan	Perform [tan]");
	break;

      case 0xe2e0:
	line.append("  % pi2	Table of Trig Constants			DATA");
	break;

      case 0xe30e:
	line.append("  % atn	Perform [atn]");
	break;

      case 0xe33e:
	line.append("  % atncon	Table of ATN Constants			DATA");
	break;

      case 0xe37b:
	line.append("  % bassft	BASIC Warm Start [RUNSTOP-RESTORE]");
	break;

      case 0xe394:
	line.append("  % init	BASIC Cold Start");
	break;

      case 0xe3a2:
	line.append("  % initat	CHRGET For Zero-page");
	break;

      case 0xe3ba:
	line.append("  % rndsed	RND Seed For zero-page			DATA");
	break;

      case 0xe3bf:
	line.append("  % initcz	Initialize BASIC RAM");
	break;

      case 0xe422:
	line.append("  % initms	Output Power-Up Message");
	break;

      case 0xe447:
	line.append("  % bvtrs	Table of BASIC Vectors (for 0300)	WORD");
	break;

      case 0xe453:
	line.append("  % initv	Initialize Vectors");
	break;

      case 0xe45f:
	line.append("  % words	Power-Up Message			DATA");
	break;

      case 0xe4ad:
	line.append("  % -	Patch for BASIC Call to CHKOUT");
	break;

      case 0xe4b7:
	line.append("  % -	Unused Bytes For Future Patches		EMPTY");
	break;

      case 0xe4da:
	line.append("  % -	Reset Character Colour");
	break;

      case 0xe4e0:
	line.append("  % -	Pause After Finding Tape File");
	break;

      case 0xe4ec:
	line.append("  % -	RS-232 Timing Table -- PAL		DATA");
	break;

      case 0xe500:
	line.append("  % iobase	Get I/O Address");
	break;

      case 0xe505:
	line.append("  % screen	Get Screen Size");
	break;

      case 0xe50a:
	line.append("  % plot	Put / Get Row And Column");
	break;

      case 0xe518:
	line.append("  % cint1	Initialize I/O");
	break;

      case 0xe544:
	line.append("  % -	Clear Screen");
	break;

      case 0xe566:
	line.append("  % -	Home Cursor");
	break;

      case 0xe56c:
	line.append("  % -	Set Screen Pointers");
	break;

      case 0xe59a:
	line.append("  % -	Set I/O Defaults (Unused Entry)");
	break;

      case 0xe5a0:
	line.append("  % -	Set I/O Defaults");
	break;

      case 0xe5b4:
	line.append("  % lp2	Get Character From Keyboard Buffer");
	break;

      case 0xe5ca:
	line.append("  % -	Input From Keyboard");
	break;

      case 0xe632:
	line.append("  % -	Input From Screen or Keyboard");
	break;

      case 0xe684:
	line.append("  % -	Quotes Test");
	break;

      case 0xe691:
	line.append("  % -	Set Up Screen Print");
	break;

      case 0xe6b6:
	line.append("  % -	Advance Cursor");
	break;

      case 0xe6ed:
	line.append("  % -	Retreat Cursor");
	break;

      case 0xe701:
	line.append("  % -	Back on to Previous Line");
	break;

      case 0xe716:
	line.append("  % -	Output to Screen");
	break;

      case 0xe72a:
	line.append("  % -	-unshifted characters-");
	break;

      case 0xe7d4:
	line.append("  % -	-shifted characters-");
	break;

      case 0xe87c:
	line.append("  % -	Go to Next Line");
	break;

      case 0xe891:
	line.append("  % -	Output ");
	break;

      case 0xe8a1:
	line.append("  % -	Check Line Decrement");
	break;

      case 0xe8b3:
	line.append("  % -	Check Line Increment");
	break;

      case 0xe8cb:
	line.append("  % -	Set Colour Code");
	break;

      case 0xe8da:
	line.append("  % -	Colour Code Table");
	break;

      case 0xe8ea:
	line.append("  % -	Scroll Screen");
	break;

      case 0xe965:
	line.append("  % -	Open A Space On The Screen");
	break;

      case 0xe9c8:
	line.append("  % -	Move A Screen Line");
	break;

      case 0xe9e0:
	line.append("  % -	Syncronise Colour Transfer");
	break;

      case 0xe9f0:
	line.append("  % -	Set Start of Line");
	break;

      case 0xe9ff:
	line.append("  % -	Clear Screen Line");
	break;

      case 0xea13:
	line.append("  % -	Print To Screen");
	break;

      case 0xea24:
	line.append("  % -	Syncronise Colour Pointer");
	break;

      case 0xea31:
	line.append("  % -	Main IRQ Entry Point");
	break;

      case 0xea87:
	line.append("  % scnkey	Scan Keyboard");
	break;

      case 0xeadd:
	line.append("  % -	Process Key Image");
	break;

      case 0xeb79:
	line.append("  % -	Pointers to Keyboard decoding tables	WORD");
	break;

      case 0xeb81:
	line.append("  % -	Keyboard 1 -- unshifted			DATA");
	break;

      case 0xebc2:
	line.append("  % -	Keyboard 2 -- Shifted			DATA");
	break;

      case 0xec03:
	line.append("  % -	Keyboard 3 -- Commodore			DATA");
	break;

      case 0xec44:
	line.append("  % -	Graphics/Text Control");
	break;

      case 0xec78:
	line.append("  % -	Keyboard 4 -- Control			DATA");
	break;

      case 0xecb9:
	line.append("  % -	Video Chip Setup Table			DATA");
	break;

      case 0xece7:
	line.append("  % -	Shift-Run Equivalent");
	break;

      case 0xecf0:
	line.append("  % -	Low Byte Screen Line Addresses		DATA");
	break;

      case 0xed09:
	line.append("  % talk	Send TALK Command on Serial Bus");
	break;

      case 0xed0c:
	line.append("  % listn	Send LISTEN Command on Serial Bus");
	break;

      case 0xed40:
	line.append("  % -	Send Data On Serial Bus");
	break;

      case 0xedad:
	line.append("  % -	Flag Errors");
	break;

      case 0xf707:
	line.append("  % -	Status #80 - device not present");
	break;

      case 0xedb0:
	line.append("  % -	Status #03 - write timeout");
	break;

      case 0xedb9:
	line.append("  % second	Send LISTEN Secondary Address");
	break;

      case 0xedbe:
	line.append("  % -	Clear ATN");
	break;

      case 0xedc7:
	line.append("  % tksa	Send TALK Secondary Address");
	break;

      case 0xedcc:
	line.append("  % -	Wait For Clock");
	break;

      case 0xeddd:
	line.append("  % ciout	Send Serial Deferred");
	break;

      case 0xedef:
	line.append("  % untlk	Send UNTALK / UNLISTEN");
	break;

      case 0xee13:
	line.append("  % acptr	Receive From Serial Bus");
	break;

      case 0xee85:
	line.append("  % -	Serial Clock On");
	break;

      case 0xee8e:
	line.append("  % -	Serial Clock Off");
	break;

      case 0xee97:
	line.append("  % -	Serial Output 1");
	break;

      case 0xeea0:
	line.append("  % -	Serial Output 0");
	break;

      case 0xeea9:
	line.append("  % -	Get Serial Data And Clock In");
	break;

      case 0xeeb3:
	line.append("  % -	Delay 1 ms");
	break;

      case 0xeebb:
	line.append("  % -	RS-232 Send");
	break;

      case 0xef06:
	line.append("  % -	Send New RS-232 Byte");
	break;

      case 0xef2e:
	line.append("  % -	'No DSR' / 'No CTS' Error");
	break;

      case 0xef39:
	line.append("  % -	Disable Timer");
	break;

      case 0xef4a:
	line.append("  % -	Compute Bit Count");
	break;

      case 0xef59:
	line.append("  % -	RS-232 Receive");
	break;

      case 0xef7e:
	line.append("  % -	Set Up To Receive");
	break;

      case 0xef90:
	line.append("  % -	Process RS-232 Byte");
	break;

      case 0xefe1:
	line.append("  % -	Submit to RS-232");
	break;

      case 0xf00d:
	line.append("  % -	No DSR (Data Set Ready) Error");
	break;

      case 0xf017:
	line.append("  % -	Send to RS-232 Buffer");
	break;

      case 0xf04d:
	line.append("  % -	Input From RS-232");
	break;

      case 0xf086:
	line.append("  % -	Get From RS-232");
	break;

      case 0xf0a4:
	line.append("  % -	Serial Bus Idle");
	break;

      case 0xf0bd:
	line.append("  % -	Table of Kernal I/O Messages		DATA");
	break;

      case 0xf12b:
	line.append("  % -	Print Message if Direct");
	break;

      case 0xf12f:
	line.append("  % -	Print Message");
	break;

      case 0xf13e:
	line.append("  % getin	Get a byte");
	break;

      case 0xf157:
	line.append("  % chrin	Input a byte");
	break;

      case 0xf199:
	line.append("  % -	Get From Tape / Serial / RS-232");
	break;

      case 0xf1ca:
	line.append("  % chrout	Output One Character");
	break;

      case 0xf20e:
	line.append("  % chkin	Set Input Device");
	break;

      case 0xf250:
	line.append("  % chkout	Set Output Device");
	break;

      case 0xf291:
	line.append("  % close	Close File");
	break;

      case 0xf30f:
	line.append("  % -	Find File");
	break;

      case 0xf31f:
	line.append("  % -	Set File values");
	break;

      case 0xf32f:
	line.append("  % clall	Abort All Files");
	break;

      case 0xf333:
	line.append("  % clrchn	Restore Default I/O");
	break;

      case 0xf34a:
	line.append("  % open	Open File");
	break;

      case 0xf3d5:
	line.append("  % -	Send Secondary Address");
	break;

      case 0xf409:
	line.append("  % -	Open RS-232");
	break;

      case 0xf49e:
	line.append("  % load	Load RAM");
	break;

      case 0xf4b8:
	line.append("  % -	Load File From Serial Bus");
	break;

      case 0xf533:
	line.append("  % -	Load File From Tape");
	break;

      case 0xf5af:
	line.append("  % -	Print 'SEARCHING'");
	break;

      case 0xf5c1:
	line.append("  % -	Print Filename");
	break;

      case 0xf5d2:
	line.append("  % -	Print 'LOADING / VERIFYING'");
	break;

      case 0xf5dd:
	line.append("  % save	Save RAM");
	break;

      case 0xf5fa:
	line.append("  % -	Save to Serial Bus");
	break;

      case 0xf659:
	line.append("  % -	Save to Tape");
	break;

      case 0xf68f:
	line.append("  % -	Print 'SAVING'");
	break;

      case 0xf69b:
	line.append("  % udtim	Bump Clock");
	break;

      case 0xf6dd:
	line.append("  % rdtim	Get Time");
	break;

      case 0xf6e4:
	line.append("  % settim	Set Time");
	break;

      case 0xf6ed:
	line.append("  % stop	Check STOP Key");
	break;

      case 0xf6fb:
	line.append("  % -	'too many files'");
	break;

      case 0xf6fe:
	line.append("  % -	'file open'");
	break;

      case 0xf701:
	line.append("  % -	'file not open'");
	break;

      case 0xf704:
	line.append("  % -	'file not found'");
	break;

      case 0xf70a:
	line.append("  % -	'not input file'");
	break;

      case 0xf70d:
	line.append("  % -	'not output file'");
	break;

      case 0xf710:
	line.append("  % -	'missing filename'");
	break;

      case 0xf713:
	line.append("  % -	'illegal device number'");
	break;

      case 0xf72d:
	line.append("  % -	Find Any Tape Header");
	break;

      case 0xf76a:
	line.append("  % -	Write Tape Header");
	break;

      case 0xf7d0:
	line.append("  % -	Get Buffer Address");
	break;

      case 0xf7d7:
	line.append("  % -	Set Buffer Stat / End Pointers");
	break;

      case 0xf7ea:
	line.append("  % -	Find Specific Tape Header");
	break;

      case 0xf80d:
	line.append("  % -	Bump Tape Pointer");
	break;

      case 0xf817:
	line.append("  % -	Print 'PRESS PLAY ON TAPE'");
	break;

      case 0xf82e:
	line.append("  % -	Check Tape Status");
	break;

      case 0xf838:
	line.append("  % -	Print 'PRESS RECORD...'");
	break;

      case 0xf841:
	line.append("  % -	Initiate Tape Read");
	break;

      case 0xf864:
	line.append("  % -	Initiate Tape Write");
	break;

      case 0xf875:
	line.append("  % -	Common Tape Code");
	break;

      case 0xf8d0:
	line.append("  % -	Check Tape Stop");
	break;

      case 0xf8e2:
	line.append("  % -	Set Read Timing");
	break;

      case 0xf92c:
	line.append("  % -	Read Tape Bits");
	break;

      case 0xfa60:
	line.append("  % -	Store Tape Characters");
	break;

      case 0xfb8e:
	line.append("  % -	Reset Tape Pointer");
	break;

      case 0xfb97:
	line.append("  % -	New Character Setup");
	break;

      case 0xfba6:
	line.append("  % -	Send Tone to Tape");
	break;

      case 0xfbc8:
	line.append("  % -	Write Data to Tape");
	break;

      case 0xfbcd:
	line.append("  % -	IRQ Entry Point");
	break;

      case 0xfc57:
	line.append("  % -	Write Tape Leader");
	break;

      case 0xfc93:
	line.append("  % -	Restore Normal IRQ");
	break;

      case 0xfcb8:
	line.append("  % -	Set IRQ Vector");
	break;

      case 0xfcca:
	line.append("  % -	Kill Tape Motor");
	break;

      case 0xfcd1:
	line.append("  % -	Check Read / Write Pointer");
	break;

      case 0xfcdb:
	line.append("  % -	Bump Read / Write Pointer");
	break;

      case 0xfce2:
	line.append("  % -	Power-Up RESET Entry");
	break;

      case 0xfd02:
	line.append("  % -	Check For 8-ROM");
	break;

      case 0xfd12:
	line.append("  % -	8-ROM Mask '80CBM'			DATA");
	break;

      case 0xfd15:
	line.append("  % restor	Restore Kernal Vectors (at 0314)");
	break;

      case 0xfd1a:
	line.append("  % vector	Change Vectors For User");
	break;

      case 0xfd30:
	line.append("  % -	Kernal Reset Vectors			WORD");
	break;

      case 0xfd50:
	line.append("  % ramtas	Initialise System Constants");
	break;

      case 0xfd9b:
	line.append("  % -	IRQ Vectors For Tape I/O		WORD");
	break;

      case 0xfda3:
	line.append("  % ioinit	Initialise I/O");
	break;

      case 0xfddd:
	line.append("  % -	Enable Timer");
	break;

      case 0xfdf9:
	line.append("  % setnam	Set Filename");
	break;

      case 0xfe00:
	line.append("  % setlfs	Set Logical File Parameters");
	break;

      case 0xfe07:
	line.append("  % readst	Get I/O Status Word");
	break;

      case 0xfe18:
	line.append("  % setmsg	Control OS Messages");
	break;

      case 0xfe21:
	line.append("  % settmo	Set IEEE Timeout");
	break;

      case 0xfe25:
	line.append("  % memtop	Read / Set Top of Memory");
	break;

      case 0xfe34:
	line.append("  % membot	Read / Set Bottom of Memory");
	break;

      case 0xfe43:
	line.append("  % -	NMI Transfer Entry");
	break;

      case 0xfe66:
	line.append("  % -	Warm Start Basic [BRK]");
	break;

      case 0xfebc:
	line.append("  % -	Exit Interrupt");
	break;

      case 0xfec2:
	line.append("  % -	RS-232 Timing Table - NTSC	DATA");
	break;

      case 0xfed6:
	line.append("  % -	NMI RS-232 In");
	break;

      case 0xff07:
	line.append("  % -	NMI RS-232 Out");
	break;

      case 0xff43:
	line.append("  % -	Fake IRQ Entry");
	break;

      case 0xff48:
	line.append("  % -	IRQ Entry");
	break;

      case 0xff5b:
	line.append("  % cint	Initialize screen editor");
	break;

      case 0xff80:
	line.append("  % -	Kernal Version Number [03]	DATA");
	break;

      case 0xff81:
	line.append("  % cint		Init Editor & Video Chips");
	break;

      case 0xff84:
	line.append("  % ioinit		Init I/O Devices, Ports & Timers");
	break;

      case 0xff87:
	line.append("  % ramtas		Init Ram & Buffers");
	break;

      case 0xff8a:
	line.append("  % restor		Restore Vectors");
	break;

      case 0xff8d:
	line.append("  % vector		Change Vectors For User");
	break;

      case 0xff90:
	line.append("  % setmsg		Control OS Messages");
	break;

      case 0xff93:
	line.append("  % secnd		Send SA After Listen");
	break;

      case 0xff96:
	line.append("  % tksa		Send SA After Talk");
	break;

      case 0xff99:
	line.append("  % memtop		Set/Read System RAM Top");
	break;

      case 0xff9c:
	line.append("  % membot		Set/Read System RAM Bottom");
	break;

      case 0xff9f:
	line.append("  % scnkey		Scan Keyboard");
	break;

      case 0xffa2:
	line.append("  % settmo		Set Timeout In IEEE");
	break;

      case 0xffa5:
	line.append("  % acptr		Handshake Serial Byte In");
	break;

      case 0xffa8:
	line.append("  % ciout		Handshake Serial Byte Out");
	break;

      case 0xffab:
	line.append("  % untalk		Command Serial Bus UNTALK");
	break;

      case 0xffae:
	line.append("  % unlsn		Command Serial Bus UNLISTEN");
	break;

      case 0xffb1:
	line.append("  % listn		Command Serial Bus LISTEN");
	break;

      case 0xffb4:
	line.append("  % talk		Command Serial Bus TALK");
	break;

      case 0xffb7:
	line.append("  % readss		Read I/O Status Word");
	break;

      case 0xffba:
	line.append("  % setlfs		Set Logical File Parameters");
	break;

      case 0xffbd:
	line.append("  % setnam		Set Filename");
	break;

      case 0xffc0:
	line.append("  % (iopen)		Open Vector [f34a]");
	break;

      case 0xffc3:
	line.append("  % (iclose)   	Close Vector [f291]");
	break;

      case 0xffc6:
	line.append("  % (ichkin)   	Set Input [f20e]");
	break;

      case 0xffc9:
	line.append("  % (ichkout)	Set Output [f250]");
	break;

      case 0xffcc:
	line.append("  % (iclrch)	Restore I/O Vector [f333]");
	break;

      case 0xffcf:
	line.append("  % (ichrin)	Input Vector, chrin [f157]");
	break;

      case 0xffd2:
	line.append("  % (ichrout)	Output Vector, chrout [f1ca]");
	break;

      case 0xffd5:
	line.append("  % load		Load RAM From Device");
	break;

      case 0xffd8:
	line.append("  % save		Save RAM To Device");
	break;

      case 0xffdb:
	line.append("  % settim		Set Real-Time Clock");
	break;

      case 0xffde:
	line.append("  % rdtim		Read Real-Time Clock");
	break;

      case 0xffe1:
	line.append("  % (istop)		Test-Stop Vector [f6ed]");
	break;

      case 0xffe4:
	line.append("  % (igetin)	Get From Keyboad [f13e]");
	break;

      case 0xffe7:
	line.append("  % (iclall)	Close All Channels And Files [f32f]");
	break;

      case 0xffea:
	line.append("  % udtim		Increment Real-Time Clock");
	break;

      case 0xffed:
	line.append("  % screen		Return Screen Organization");
	break;

      case 0xfff0:
	line.append("  % plot		Read / Set Cursor X/Y Position");
	break;

      case 0xfff3:
	line.append("  % iobase		Return I/O Base Address");
	break;

      case 0xfff6:
	line.append("  % - [5252]");
	break;

      case 0xfff8:
	line.append("  % SYSTEM [5942]");
	break;

      case 0xfffa:
	line.append("  % NMI [fe43]");
	break;

      case 0xfffc:
	line.append("  % RESET [fce2]");
	break;

      case 0xfffe:
	line.append("  % IRQ [ff48]");
	break;

      case 0xbdcd:
	line.append("  % Print number from AX");
	break;
      }
  }


  /**
   * @see IMonitor#setEnabled(boolean)
   */
  public void setEnabled(boolean b) {
    DEBUG = b;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

}
