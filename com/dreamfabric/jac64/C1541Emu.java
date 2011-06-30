package com.dreamfabric.jac64;

import java.io.BufferedReader;

import java.net.URL;

import java.io.DataInputStream;
import java.io.InputStreamReader;

/**
 * Describe class C1541Emu here.
 *
 *
 * Created: Tue Aug 01 12:29:57 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class C1541Emu extends MOS6510Core {

  public static final boolean DEBUG = false; //true;
  public static final boolean IODEBUG = false;

  public static final int C1541ROM = 0xc000;
  public static final int RESET_VECTOR = 0xfffc;
  public C1541Chips chips;

  public C1541Emu(IMonitor m, String cb) {
    super(m, cb);
    // Only single area of RAM
    memory = new int[0x10000];
    chips = new C1541Chips(this);
    init(chips);
    loadDebug("c1541dbg.txt");
    // For avoiding CPU debug info.
    debug = false;
  }

  public String getName() {
    return "C1541 CPU";
  }

  public void setReader(C64Reader reader) {
    chips.setReader(reader);
  }

  // Reads the memory with all respect to all flags...
  protected final int fetchByte(int adr) {
	cycles++;
    if (adr < 0x800 || adr >= 0xc000)
      return memory[adr];
    int c = adr & 0xff00;
    if (c == 0x1800 || c == 0x1c00) {
      int data = chips.performRead(adr, cycles);

      if (IODEBUG)
    	  System.out.println("C1541: Reading from " + Integer.toHexString(adr)
    			  + " PC = " + Integer.toHexString(pc) +
    			  " => " + Integer.toHexString(data));

      return data;
    }

    return 0;
  }

  // A byte is written directly to memory or to ioChips
  protected final void writeByte(int adr, int data) {
    cycles++;
    if (adr < 0x800) {
      memory[adr] = data;
    }
    int c = adr & 0xff00;
    if (c == 0x1800 || c == 0x1c00)
      chips.performWrite(adr, data, cycles);
  }

  public void reset() {
    super.reset();
    pc = memory[RESET_VECTOR] | (memory[RESET_VECTOR + 1] << 8);
    System.out.println("C1541: Reset to " + Integer.toHexString(pc));
  }


  boolean byteReady = false;
  void triggerByteReady() {
    byteReady = true;
  }

  public void tick(long c64Cycles) {
    while (cycles < c64Cycles) {
      // Run one instruction! - with special overflow "patch" -
      // Always fake 'byte ready' for fast read!
//    boolean o = overflow;
      if (byteReady && chips.byteReadyOverflow) {
        // Set overflow and clear byte ready!
        overflow = true;
        byteReady = false;
      }
      //      overflow = (chips.via2PerControl & 0x0e) == 0x0e ? true : o;

      // Debugging?
      if (DEBUG) {
        String msg;
        if ((msg = getDebug(pc)) != null) {
          System.out.println("C1541: " + Integer.toHexString(pc) +
              "****** " + msg + " Data: " +
              Integer.toHexString(memory[0x85]) +
              " => '" + (char) memory[0x85] + '\'');
        }
      }
      if (DEBUG && (monitor.isEnabled() || interruptInExec > 0)) {
        monitor.disAssemble(memory,pc,acc,x,y,
            (byte)getStatusByte(),interruptInExec,
            lastInterrupt);
      }
      emulateOp();

      if (chips.nextCheck < cycles) {
        chips.clock(cycles);
      }
      // Back to what it was...
//    overflow = o;
    }
  }

  public void patchROM(PatchListener list) {
  }

  public void loadDebug(String resource) {
    try {
      URL url = getClass().getResource(resource);
      monitor.info("Loading debug from URL: " + url);
      if (url == null) url = new URL(codebase + resource);
      BufferedReader reader =
	new BufferedReader(new InputStreamReader(url.openConnection().
						 getInputStream()));
      String line = "";
      while ((line = reader.readLine()) != null) {
	String[] parts = line.split("\t");
	int adr = -1;
	try {
	  adr = Integer.parseInt(parts[0].trim(), 16);
	} catch (Exception e) {
	}
	if (adr != -1) {
// 	  System.out.println("Found data for address: " +
// 			     Integer.toHexString(adr) + " => " +
// 			     parts[1].trim());
	  setDebug(adr, parts[1].trim());
	}
      }
    } catch(Exception e) {
      System.out.println("Failed to load debug text: " + resource);
    }
  }


  protected void readROM(String resource, int startMem, int len) {
    try {
      URL url = getClass().getResource(resource);
      monitor.info("URL: " + url);
      monitor.info("Read ROM " + resource);
      if (url == null) url = new URL(codebase + resource);
      loadROM(new DataInputStream(url.openConnection().getInputStream()),
	      startMem, len);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void installROMS() {
    readROM("/roms/c1541.rom" , C1541ROM, 0x4000);
  }

}
