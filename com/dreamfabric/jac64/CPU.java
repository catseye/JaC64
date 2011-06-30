/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 * This is the CPU file for Commodore 64 with its
 * ROM files and memory management, etc.
 *
 * @(#)cpu.java	Created date: 99-5-17
 *
 */
package com.dreamfabric.jac64;
import com.dreamfabric.c64utils.*;
/**
 * CPU "implements" the C64s 6510 processor in java code. reimplemented from old
 * CPU.java
 *
 * @author Joakim Eriksson (joakime@sics.se)
 * @version $Revision:$, $Date:$
 */
public class CPU extends MOS6510Core {

  public static final boolean DEBUG_EVENT = false;
  // The IO RAM memory at 0x10000 (just since there is RAM there...)
  public static final int IO_OFFSET = 0x10000 - 0xd000;
  public static final int BASIC_ROM2 = 0x1a000;
  public static final int KERNAL_ROM2 = 0x1e000;
  public static final int CHAR_ROM2 = 0x1d000;

  public static final boolean EMULATE_1541 = true;

  public static final int CH_PROTECT = 1;
  public static final int CH_MONITOR_WRITE = 2;
  public static final int CH_MONITOR_READ = 4;

  private int romFlag = 0xa000;

  // Defaults for the ROMs
  public boolean basicROM = true;
  public boolean kernalROM = true;
  public boolean charROM = false;
  public boolean ioON = true;

  // The state of the program (runs if running = true)
  public boolean running = true;
  public boolean pause = false;

  private static final long CYCLES_PER_DEBUG = 10000000;
  public static final boolean DEBUG = false;

  private C1541Emu c1541;
  private Loader loader;
  private int windex = 0;

  private int cheatMon[];
  private AutoStore[] autoStore;

  public CPU(IMonitor m, String cb, Loader loader) {
    super(m, cb);
    memory = new int[0x20000];
    this.loader = loader;
    if (EMULATE_1541) {
      IMonitor d = new DefaultIMon(); // new Debugger();
      c1541 = new C1541Emu(d, cb);
      // d.init(c1541);
      // d.setEnabled(true);
    }
  }


  public C1541Emu getDrive() {
    return c1541;
  }

  private final void schedule(long cycles) {
    chips.clock(cycles);
    while (cycles >= scheduler.nextTime) {
      TimeEvent t = scheduler.popFirst();
      if (t != null) {
        if (DEBUG_EVENT) {
          System.out.println("Executing event: " + t.getShort());
        }
        // Give it the actual time also!!!
        t.execute(cycles);
      } else {
        if (DEBUG_EVENT) System.out.println("Nothign to execute...");
        return;
      }
    }
  }

  // Reads the memory with all respect to all flags...
  protected final int fetchByte(int adr) {
    /* a cycles passes for this read */
    cycles++;

    /* Chips work first, then CPU */
    schedule(cycles);
    while (baLowUntil > cycles) {
      cycles++;  
      schedule(cycles);
    }

    if ((romFlag & adr) == romFlag) {
      return memory[rindex = adr | 0x10000];
    } else if ((adr & 0xf000) == 0xd000) {
      if (ioON) {
        return chips.performRead(rindex = adr, cycles);
      } else if (charROM) {
        return memory[rindex = adr | 0x10000];
      } else {
        return memory[rindex = adr];
      }
    } else {
      return memory[rindex = adr];
    }
  }

  // A byte is written directly to memory or to ioChips
  protected final void writeByte(int adr, int data) {
    cycles++;

    schedule(cycles);
    // Locking only on fetch byte...
    // System.out.println("Writing byte at: " + Integer.toString(adr, 16)
    // + " = " + data);
    if (adr <= 1) {
      memory[adr] = data;
      int p = (memory[0] ^ 0xff) | memory[1];

      kernalROM = ((p & 2) == 2); // Kernal on
      basicROM = ((p & 3) == 3); // Basic on

      charROM = ((p & 3) != 0) && ((p & 4) == 0);
      // ioON is probably not correct!!! Check against table...
      ioON = ((p & 3) != 0) && ((p & 4) != 0);

      if (basicROM)
        romFlag = 0xa000;
      else if (kernalROM)
        romFlag = 0xe000;
      else
        romFlag = 0x10000; // No Rom at all (Basic / Kernal)
    }

    adr &= 0xffff;
    if (ioON && ((adr & 0xf000) == 0xd000)) {
      // System.out.println("IO Write at: " + Integer.toString(adr, 16));
      chips.performWrite(adr, data, cycles);
    } else {
      memory[windex = adr] = data;
    }
  }


  private void fixRindex(int adr) {
    // ROM/RAM address fix
    if ((basicROM && ((adr & 0xe000) == 0xa000))
        || (kernalROM && ((adr & 0xe000) == 0xe000))
        || (charROM && ((adr & 0xf000) == 0xd000))) {
      // Add ROM address for the read!
      adr |= 0x10000;
    }
    rindex = adr;
  }

  public void poke(int address, int data) {
    writeByte(address & 0xffff, data & 0xff);
  }

  public void patchROM(PatchListener list) {
    this.list = list;

    int pos = 0xf49e | 0x10000;
    memory[pos++] = M6510Ops.JSR;
    memory[pos++] = 0xd2;
    memory[pos++] = 0xf5;

    System.out.println("Patched LOAD at: " + Hex.hex2(pos));
    memory[pos++] = LOAD_FILE;
    memory[pos++] = M6510Ops.RTS;
  }

  public void runBasic() {
    memory[631] = (int) 'R';
    memory[632] = (int) 'U';
    memory[633] = (int) 'N';
    memory[634] = 13;// enter
    memory[198] = 4; // length
  }

  public void enterText(String txt) {
    System.out.println("Entering text into textbuffer: " + txt);
    txt = txt.toUpperCase();
    int len = txt.length();
    int pos = 0;
    for (int i = 0, n = len; i < n; i++) {
      char c = txt.charAt(i);
      if (c == '~')
        c = 13;
      memory[631 + pos] = c;
      pos++;
      if (pos == 5) {
        memory[198] = pos;
        pos = 0;
        int tries = 5;
        while (tries > 0 && memory[198] > 0) {
          try {
            Thread.sleep(50);
          } catch (Exception e) {
            e.printStackTrace();
          }
          tries--;
          if (tries == 0) {
            System.out.println("Buffer still full: " + memory[198]);
          }
        }
      }
    }
    memory[198] = pos;
    int tries = 5;
    while (tries > 0 && memory[198] > 0) {
      try {
        Thread.sleep(50);
      } catch (Exception e) {
        e.printStackTrace();
      }
      tries--;
      if (tries == 0) {
        System.out.println("Buffer still full: " + memory[198]);
      }
    }
  }

  protected void installROMS() {
    loadROM(loader.getResourceStream("/roms/kernal.c64"), KERNAL_ROM2,
        0x2000);
    loadROM(loader.getResourceStream("/roms/basic.c64"), BASIC_ROM2, 0x2000);
    loadROM(loader.getResourceStream("/roms/chargen.c64"), CHAR_ROM2,
        0x1000);
  }

  public void run(int address) {
    reset();
    running = true;
    setPC(address);
    loop();
  }

  public void unknownInstruction(int pc, int op) {
    switch (op) {
    case SLEEP:
      cycles += 100;
      break;
    case LOAD_FILE:
      if (acc == 0)
        monitor.info("**** LOAD FILE! ***** PC = " +
            Integer.toString(pc, 16) + " => " +
            Integer.toString(rindex, 16));
      else
        monitor.info("**** VERIFY!    ***** PC = " + pc + " => " + rindex);
      int len;
      int mptr = memory[0xbb] + (memory[0xbc] << 8);
      monitor.info("Filename len:" + (len = memory[0xb7]));
      String name = "";
      for (int i = 0; i < len; i++)
        name += (char) memory[mptr++];
      name += '\n';
      int sec = memory[0xb9];
      monitor.info("name = " + name);
      monitor.info("Sec Address: " +  sec);
      int loadAdr = -1;
      if (sec == 0)
        loadAdr = memory[0x2b] + (memory[0x2c] << 8);
      if (list != null) {
        if (list.readFile(name, loadAdr)) {
          acc = 0;
        }
      }
      pc--;
      break;
    }
  }



  // Takes the thread and loops!!!
  public void start() {
    run(0xfce2); // Power UP reset routine!
    if (pause) {
      while (pause) {
        System.out.println("Entering pause mode...");
        synchronized(this) {
          try {
            wait();
          } catch (Exception e) {
          }
        }
        System.out.println("Exiting pause mode...");
        loop();
      }
    }
  }

  // Should pause the application!
  public synchronized void setPause(boolean p) {
    if (p) {
      pause = true;
      running = false;
    } else {
      pause = false;
      running = true;
    }
    notify();
  }

  public synchronized void stop() {
    // stop completely
    running = false;
    pause = false;
    notify();
  }

  public void reset() {
    writeByte(1, 0x7);
    super.reset();

    if (EMULATE_1541) {
      c1541.reset();
    }
  }

  public void setPC(int startAdress) {
    // The processor flags
    pc = startAdress;
  }

  public String getName() {
    return "C64 CPU";
  }
  /**
   * The main emulation <code>loop</code>.
   *
   * @param startAdress
   *            an <code>int</code> value that represent the starting
   *            address of the emulator
   */
  public void loop() {
    if (cheatMon != null) {
      cheatLoop();
      return;
    }
    long next_print = cycles + CYCLES_PER_DEBUG;
    // How much should this be???
    monitor.info("Starting CPU at: " + Integer.toHexString(pc));
    try {
      while (running) {

        // Debugging?
        if (monitor.isEnabled()) { // || interruptInExec > 0) {
          if (baLowUntil <= cycles) {
            fixRindex(pc); // sets the rindex!
            monitor.disAssemble(memory, rindex, acc, x, y,
                (byte) getStatusByte(), interruptInExec,
                lastInterrupt);
          }
        }

        // Run one instruction!
        emulateOp();

        // Also allow the 1541 to run an instruction!
        if (EMULATE_1541) {
          c1541.tick(cycles);
        }

        nr_ins++;
        if (next_print < cycles) {
          long sec = System.currentTimeMillis() - lastMillis;
          int level = monitor.getLevel();

          if (DEBUG && level > 1) {
            monitor.info("--------------------------");
            monitor.info("Nr ins:" + nr_ins + " sec:" + (sec)
                + " -> " + ((nr_ins * 1000) / sec) + " ins/s"
                + "  " + " clk: " + cycles + " clk/s: "
                + ((CYCLES_PER_DEBUG * 1000) / sec) + "\n"
                + ((nr_irq * 1000) / sec));
            if (level > 2)
              monitor.disAssemble(memory, rindex, acc, x, y,
                  (byte) getStatusByte(), interruptInExec,
                  lastInterrupt);
            monitor.info("--------------------------");
          }
          nr_irq = 0;
          nr_ins = 0;
          lastMillis = System.currentTimeMillis();
          next_print = cycles + CYCLES_PER_DEBUG;
        }
      }
    } catch (Exception e) {
      monitor.error("Exception in loop " + pc + " : " + e);
      e.printStackTrace();
      monitor.disAssemble(memory, rindex, acc, x, y,
          (byte) getStatusByte(), interruptInExec, lastInterrupt);
    }
  }

  // -------------------------------------------------------------------
  // Cheat loop!
  // Protection
  // + rule triggered auto get/store
  // Rule: xpr & xpr & xpr ...
  // rule: int[] adr, cmptype, cmpval ...
  // autostore: int[] adr, len => result in hex! from adr and on!
  // -------------------------------------------------------------------

  public void setAutoStore(int index, AutoStore au) {
    autoStore[index] = au;
  }

  public AutoStore getAutoStore(int index) {
    return autoStore[index];
  }

  public void setCheatEnabled(int maxAutostores) {
    cheatMon = new int[0x10000];
    autoStore = new AutoStore[maxAutostores];
  }

  public void protect(int address, int value) {
    cheatMon[address] = (cheatMon[address] & 0xff) | (value << 8) | CH_PROTECT;
  }

  public void monitorRead(int address) {
    cheatMon[address] |= CH_MONITOR_READ;
  }

  public void monitorWrite(int address) {
    cheatMon[address] |= CH_MONITOR_WRITE;
  }


  public void cheatLoop() {
    int t;
    try {
      while (running) {

        // Run one instruction!
        emulateOp();

        // Processor read from address...
        if (rindex < 0x10000) {
          if ((t = cheatMon[rindex]) != 0) {
            if ((t & CH_MONITOR_READ) != 0) {
              for (int i = 0, n = autoStore.length; i < n; i++) {
                if (autoStore[i] != null)
                  autoStore[i].checkRules(memory);
              }
            }
          }
        }
        if (windex < 0x10000) {
          if ((t = cheatMon[windex]) != 0) {
            if ((t & CH_PROTECT) != 0) {
              // Write back value from then protected...
              memory[windex] = (cheatMon[windex] >> 16) & 0xff;
            }
            if ((t & CH_MONITOR_WRITE) != 0) {
              for (int i = 0, n = autoStore.length; i < n; i++) {
                if (autoStore[i] != null)
                  autoStore[i].checkRules(memory);
              }
            }
          }
        }

        // Also allow the 1541 to run an instruction!
        if (EMULATE_1541) {
          c1541.tick(cycles);
        }
      }
    } catch (Exception e) {
      monitor.error("Exception in loop " + pc + " : " + e);
      e.printStackTrace();
      monitor.disAssemble(memory, rindex, acc, x, y,
          (byte) getStatusByte(), interruptInExec, lastInterrupt);
    }
  }
}
