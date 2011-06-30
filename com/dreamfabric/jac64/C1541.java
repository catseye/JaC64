package com.dreamfabric.jac64;

import java.io.ByteArrayOutputStream;
import java.util.Observable;

/**
 * class C1541 - simple SerialBus and C1541 emulation.
 * - will not support everything but the usual simplest
 * CBM dos stuff. No fastloading, etc.
 * Might add full 1541 emulation later with CPU + IO chip
 * emulation.
 *
 * Created: Tue Mar 28 22:06:31 2006
 *
 * @author Joakim Eriksson, Dreamfabric.com
 * joakime@sics.se
 * @version 1.0
 */
public class C1541 extends Observable {

  public static final int IO_OFFSET = CPU.IO_OFFSET;

  public static final boolean DEBUG = true; //false;

  public static final int SERIAL_ATN = (1 << 3);
  public static final int SERIAL_CLK_OUT = (1 << 4);
  public static final int SERIAL_DATA_OUT = (1 << 5);
  public static final int SERIAL_CLK_IN = (1 << 6);
  public static final int SERIAL_DATA_IN = (1 << 7);

  public static final int TALK = 0x40;
  public static final int LISTEN = 0x20;
  public static final int DATA = 0x60;
  public static final int OPEN = 0xf0;
  public static final int CLOSE = 0xe0;

  // Cpu memory
  private int[] memory;
  // Disk (.d64) reader
  private C64Reader reader;

  private static final int IDLE = 0;
  private static final int ATN = 1;
  private static final int RECEIVING = 2;
  private static final int SENDING = 3;
  private static final int READ_BIT = 4;
  private static final int WAIT_BIT = 5;
  private static final int READ_BYTE = 6;
  private static final int WRITE_BYTE = 7;

  private static final int LOAD_FILE = 1;
  private static final int SAVE_FILE = 2;
  private static final int LOGICAL_CHANNEL = 3;

  private static final int ATN_SEEN = 10;
  private static final int ATN_READ_BIT = 11;
  private static final int ATN_WAIT_BIT = 12;

  private static final int WAIT_LISTENER_READY = 13;
  private static final int WRITE_BIT_CLK1 = 14;
  private static final int WRITE_BIT_CLK2 = 15;
  private static final int WRITE_END = 16;
  private static final int WAIT_LISTENER_EOI_HANDSHAKE = 17;

  private static final int READ_FILENAME = 1;

  private DiskChannel channel[] = new DiskChannel[16];

  private boolean atnLast = false;
  private int mode = IDLE;
  private long eoiTimeout = 0;
  private int eoi;
  private boolean lastChar = false;

  private int role = 0;
  private int floppyMode = 0;
  private int floppyChannel = 0;
  private String filename;

  // For example when reading bytes and waits for EOI
  private long waitTimeout = 0;

  private int readMode = READ_FILENAME;
  private String tmpFilename = "";
  /**
   * Creates a new <code>C1541</code> instance.
   *
   */
  public C1541(int[] memory) {
    this.memory = memory;
    reader = new C64Reader();
    for (int i = 0, n = 16; i < n; i++) {
      channel[i] = new DiskChannel(i);
    }
  }

  public void reset() {
    mode = IDLE;
    role = 0;
    rbState = 0;
    rbByte = 0;
    floppyMode = 0;
    floppyChannel = 0;
    clockHi();
  }

  public C64Reader getReader() {
    return reader;
  }

  public void tick(long cycles) {
    if (waitTimeout != 0 && waitTimeout < cycles) {
      System.out.print(".");
      tick(cycles, true);
    }
  }

  public void tick(long cycles, boolean timeout) {
    int data = memory[IO_OFFSET + 0xdd00];
    boolean atn = (data & SERIAL_ATN) != 0;
    boolean dataOut = (data & SERIAL_DATA_OUT) != 0;
    boolean clkOut = (data & SERIAL_CLK_OUT) != 0;
    boolean atnInvoked = atn & !atnLast;

    switch (mode) {
      // IDLE mode - just waiting... set data to high...
    case READ_BYTE:
      if (atnInvoked) {
	mode = IDLE;
	return;
      }

      int b = readByte(data, cycles, false, timeout);
      if (b != 0) {
	mode = IDLE;
	if (atn) {
	  handleATNByte(b);
	} else {
	  System.out.println("//// Read byte: " + Integer.toString(b, 16) +
			     " => " + ((char) b));
	  dataLo();
	  if (readMode == READ_FILENAME) {
	    tmpFilename += (char) b;
	    if (lastChar) {
	      System.out.println("Filename: " + tmpFilename);
	      filename = tmpFilename;
	    }
	  }
	}
      }
      break;
    case WRITE_BYTE:
      if (atnInvoked) {
	mode = IDLE;
	return;
      }
      if (writeByte(data, cycles, timeout)) {
	// When all is read - reset!
	reset();
      }
      break;
    case ATN_SEEN:
      if (atn && !clkOut) {
	dataHi();
	mode = READ_BYTE;
	// Start up for reading a byte...
	readByte(data, cycles, true, false);

      } else if (!atn) {
	mode = IDLE;
      }
      break;
    case IDLE:
      if (atn & clkOut) {
	System.out.println("C1541: ATN Seen...");
	dataLo();
	mode = ATN_SEEN;
      }
      if (!atn && role == TALK) {
	// Here we should talk more...
	if (!clkOut && dataOut) {
	  mode = WRITE_BYTE;
	  initWrite(cycles);
	}
      } else if (!atn && role != 0) {
	if (!clkOut) {
	  mode = READ_BYTE;
	  readByte(data, cycles, true, false);
	}
      }
      break;
    }
    atnLast = atn;
  }

  int rbState;
  int rbByte;
  int rbCtr = 0;
  int eoiCtr = 0;
  // Just pick bits from
  private int readByte(int data, long cycles,
		       boolean restart, boolean timeout) {
    boolean dataOut = (data & SERIAL_DATA_OUT) != 0;
    boolean clkOut = (data & SERIAL_CLK_OUT) != 0;

    if (restart) {
      // Set everything to "low" and go...
      rbCtr = 0;
      rbByte = 0;
      rbState = WAIT_BIT;
      dataHi();
      System.out.println("Start reading byte - data lo");
      waitTimeout = 200 + cycles;
      eoiCtr = 0;
      lastChar = false;
      return 0;
    }

    if (timeout) {
      System.out.println("//// EOI Timeout???");
      if (eoiCtr == 0) {
	System.out.println("//// EOI 1 => dataLo");
	dataLo();
	waitTimeout = 80 + cycles;
      } else {
	System.out.println("///// EOI 2 => dataHi");
	dataHi();
	// No more timeout...
	waitTimeout = 0;
	lastChar = true;
      }
      eoiCtr++;
    }

    if (rbState == WAIT_BIT) {
      if (clkOut)
	rbState = READ_BIT;
    } else {
      if (!clkOut) {
	rbByte |= dataOut ? 0 : (1 << rbCtr);
// 	System.out.println("//// Read bit: " + rbCtr + " => " + rbByte);
	rbState = WAIT_BIT;
	rbCtr++;
	waitTimeout = 0;
      }
    }

    if (rbCtr == 8) {
      return rbByte;
    }
    return 0;
  }


  int wByte;
  int wBitPos;
  int wBytePos;
  int wState;
  long wCyclesWait;
  byte[] bytesToWrite;
  boolean wEOI = false;

  private void initWriteByte(int data, long cycles) {
    int b = data > ' ' ? data : '.';
    if (DEBUG)
      System.out.print("***>> InitW: " +
		       Integer.toString(data & 0xff, 16) + " '" + (char) b
		       + "' ");
    wByte = data;
    wBitPos = 0;

    // at least 100 us between the bytes
    wCyclesWait = cycles + 100;

    // Ensure that we get ticks!!!!
    waitTimeout = cycles + 100;

    wState = WAIT_LISTENER_READY;
  }

  private boolean writeByte(int data, long cycles, boolean timeout) {
    boolean dataOut = (data & SERIAL_DATA_OUT) != 0;

    // Do nothing if we are waiting for something...
    if (wCyclesWait > cycles) {
//       System.out.println("Waiting until: " + wCyclesWait + " now: " +
// 			 cycles);
      return false;
    }
//     System.out.println("wState:" + wState);
    switch (wState) {
    case WAIT_LISTENER_READY:
      clockHi();
      if (!dataOut) {
	// If no file - exit here => file not found error...
	if (bytesToWrite == null) {
	  waitTimeout = 0;
	  return true;
	}
	if (!wEOI) {
	  System.out.print("[R]");
	  wState = WRITE_BIT_CLK2;
	} else {
	  System.out.print("[R(EOI)]");
	  wState = WAIT_LISTENER_EOI_HANDSHAKE;
	}
	// no wait for the first bit! (should call this method again)
      } else
	System.out.print("[-R]");
      break;
    case WAIT_LISTENER_EOI_HANDSHAKE:
      if (dataOut) {
	System.out.println("EOI handshake!!!");
	wEOI = false;
	wState = WAIT_LISTENER_READY;
      }
      break;
    case WRITE_BIT_CLK1:
      // C64 is waiting while the clock is low - as soon as clock
      // is high it will read the data!
      // Set bit and make clock low!
      if ((wByte & (1 << wBitPos)) == 0) {
// 	System.out.println("* Write bit " + wBitPos + " low");
	dataLo();
      } else {
// 	System.out.println("* Write bit " + wBitPos + " high");
	dataHi();
      }
      wBitPos++;
      clockHi();
      wCyclesWait = cycles + 70;
      if (wBitPos < 8) {
	wState = WRITE_BIT_CLK2;
      } else {
	wState = WRITE_END;
      }
      break;
    case WRITE_BIT_CLK2:
      // Set clock back to low - to indicate that another byte
      // is coming up!
      clockLo();
      dataLo();
      wCyclesWait = cycles + 70;
      wState = WRITE_BIT_CLK1;
      break;
    case WRITE_END:
      clockLo();
      if (dataOut) {
	System.out.println("Ack: " +
			   Integer.toString(memory[0xa4], 16));
	wBytePos++;

	if ((wBytePos % 10) == 0) {
	  setChanged();
	  notifyObservers("Loading " + filename + " " + (100 * wBytePos) /
			  bytesToWrite.length + "%");
	}

	if (wBytePos == bytesToWrite.length - 1) {
	  wEOI = true;
	} else if (wBytePos >= bytesToWrite.length) {
	  waitTimeout = 0;
	  wEOI = false;
	  System.out.println("******** Write finished!!!");
	  setChanged();
	  notifyObservers("");
	  return true;
	}
	initWriteByte(bytesToWrite[wBytePos], cycles);
      }
      break;
    }
    return false;
  }

  private void initWrite(long cycles) {
    // Do stuff with all sorts of things...
    // Floppy channel, filename, etc.
    // Filename

    clockLo();
    wBytePos = 0;
    wEOI = false;

    if (floppyMode == LOAD_FILE) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if ((filename = reader.readFile(filename, -1, out)) != null) {
	bytesToWrite = out.toByteArray();
	System.out.println("C1541 have " + bytesToWrite.length +
			   " bytes to write");

	initWriteByte(bytesToWrite[0], cycles);
      } else {
	// Error???
	System.out.println("File not found... should signal error...");
	bytesToWrite = null;
	initWriteByte(0, cycles);
      }
    }
    if (floppyMode == LOGICAL_CHANNEL) {
      System.out.println("Should write logical channel data!");
      bytesToWrite = channel[floppyChannel].getData();

      initWriteByte(bytesToWrite[0], cycles);
    }
  }



  private void handleATNByte(int data) {
    int cmd = data & 0xf0;
    int dev = data & 0x1f;
    int secAdr = data & 0x0f;
    System.out.println("ATN Byte: " + data + " " + Integer.toString(data, 16));
    switch (cmd) {
    case TALK:
    case TALK + 0x10:
      role = 0;
      if (dev == 31) {
	System.out.println("  >> UNTALK!!!");
      } else {
	System.out.println("  Received TALK for dev: " + dev);
	if (dev == 8) {
	  System.out.println("### DEV: 8 ACTIVE as 1541!");
	  role = TALK;
	}
      }
      break;
    case LISTEN:
    case LISTEN + 0x10:
      role = 0;
      if (dev == 31) {
	System.out.println("  >> UNLISTEN!!!");
	if (floppyMode == LOGICAL_CHANNEL) {

	  // Should load and set data too!
	  ByteArrayOutputStream out = new ByteArrayOutputStream();
	  if ((tmpFilename = reader.readFile(tmpFilename, -1, out)) != null) {
	    channel[floppyChannel].setData(out.toByteArray());
	    System.out.println("Setting channel " + floppyChannel +
			       " to " + tmpFilename + " size: " +
			       channel[floppyChannel].getData().length);
	    channel[floppyChannel].setFilename(tmpFilename);
	    filename = tmpFilename;
	  } else {
	    System.out.println("#### File not found error???");
	  }
	}
      } else {
	System.out.println("  Received LISTEN for dev: " + dev);
	if (dev == 8) {
	  System.out.println("### DEV: 8 ACTIVE as 1541!");
	  role = LISTEN;
	}
      }
      break;
    case OPEN:
      System.out.println("### OPEN sec addr: " + secAdr);
      tmpFilename = "";
      readMode = READ_FILENAME;
      if (secAdr == 0) {
	System.out.println("### => LOAD File!");
	floppyMode = LOAD_FILE;
      } else if (secAdr == 1) {
	System.out.println("### => SAVE File!");
	floppyMode = SAVE_FILE;
	readMode = READ_FILENAME;
      } else if (secAdr == 15) {
	System.out.println("### => Error...");
      } else {
	System.out.println("Logical channel: " + secAdr);
	floppyMode = LOGICAL_CHANNEL;
	floppyChannel = secAdr;
      }
    case CLOSE:
      System.out.println("### Close: secAdr: " + secAdr);
      channel[secAdr].close();
      break;
    case DATA:
      System.out.println("### DATA sec addr: " + secAdr);
      // Set current channel to this!
      System.out.println("Setting floppy channel!");
      floppyChannel = secAdr;
      break;
    }
  }

  private void clockLo() {
    memory[IO_OFFSET + 0xdd00] &= ~SERIAL_CLK_IN;
  }

  private void clockHi() {
    memory[IO_OFFSET + 0xdd00] |= SERIAL_CLK_IN;
  }

  public void dataLo() {
    memory[IO_OFFSET + 0xdd00] &= ~SERIAL_DATA_IN;
  }

  public void dataHi() {
    memory[IO_OFFSET + 0xdd00] |= SERIAL_DATA_IN;
  }

  public void handleDisk(int data, long cycles) {
//     System.out.println("---- SerialBus: " + data + " ------");

    if (DEBUG) {
      System.out.print("EMU: ");
      printSerial(data);
    }
    tick(cycles, false);
  }

  public static void printSerial(int data) {
    if ((data & SERIAL_ATN) != 0) {
      System.out.print("A1");
    } else {
      System.out.print("A0");
    }

    int sdata = (data & SERIAL_CLK_OUT) != 0 ? 1 : 0;
    System.out.print(" C" + sdata);
    sdata = (data & SERIAL_DATA_OUT) != 0 ? 1 : 0;
    System.out.print(" D" + sdata);

    sdata = (data & SERIAL_CLK_IN) != 0 ? 1 : 0;
    System.out.print(" c" + sdata);
    sdata = (data & SERIAL_DATA_IN) != 0 ? 1 : 0;
    System.out.println(" d" + sdata + " (iec)");
  }

}
