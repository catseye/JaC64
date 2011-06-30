package com.dreamfabric.jac64;
/**
 * Describe class C1541Chips here.
 *
 *
 * Created: Tue Aug 01 13:08:47 2006
 *
 * @author Joakim Eriksson
 * @version 1.0

GCR Coding Table
	Nybble		GCR
	0  0	0000	01010	 a 10
	1  1	0001	01011	 b 11
	2  2	0010	10010	12 18
	3  3	0011	10011	13 19
	4  4	0100	01110	 e 14
	5  5	0101	01111	 f 15
	6  6	0110	10110	16 22
	7  7	0111	10111	17 23
	8  8	1000	01001	 9  9
	9  9	1001	11001	19 25
	a 10	1010	11010	1a 26
	b 11	1011	11011	1b 27
	c 12	1100	01101	 d 13
	d 13	1101	11101	1d 29
	e 14	1110	11110	1e 30
	f 15	1111	10101	15 21

 */


public class C1541Chips extends ExtChip implements DiskListener {

  // GCR_SECTOR_SIZE => 354
  public static final int GCR_SECTOR_SIZE = 1 + 10 + 9 + 1 + 325 + 8;

  public static final boolean DEBUG = false;
  public static final boolean DEBUG_IRQ = false;
  public static final boolean DEBUG_WRITE = false;
  public static final boolean DEBUG_GCR = false;
  public static final boolean DEBUG_IEC = false;

  public static final Object LED_MOTOR = new Object();
  public static final Object HEAD_MOVED = new Object();
  public static final Object SECTOR_UPDATE = new Object();

  // GCR conversion table - used for converting ordinary byte to 10-bits
  // (or 4 bits to 5)
  public static final int[] GCR = new int[] {
    0x0a, 0x0b, 0x12, 0x13,
    0x0e, 0x0f, 0x16, 0x17,
    0x09, 0x19, 0x1a, 0x1b,
    0x0d, 0x1d, 0x1e, 0x15
  };

  // 5 bits > 4 bits (0xff => invalid)
  public static final int[] GCR_REV = new int[] {
    0xff, 0xff, 0xff, 0xff, // 0 - 3invalid...
    0xff, 0xff, 0xff, 0xff, // 4 - 7 invalid...
    0xff, 0x08, 0x00, 0x01, // 8 invalid... 9 = 8, a = 0, b = 1
    0xff, 0x0c, 0x04, 0x05, // c invalid... d = c, e = 4, f = 5

    0xff, 0xff, 0x02, 0x03, // 10-11 invalid...
    0xff, 0x0f, 0x06, 0x07, // 14 invalid...
    0xff, 0x09, 0x0a, 0x0b, // 18 invalid...
    0xff, 0x0d, 0x0e, 0xff, // 1c, 1f invalid...
  };

  private C1541Emu cpu;

  private int diskID1 = 0;
  private int diskID2 = 0;

  private int track = 1;
  private int hTrack = 2;
  private int sector = 0;
  private int sectorPos = 0;
  private int currentTrackSize = 21;

  // The current GCR sector
  private int[] gcrSector = new int[GCR_SECTOR_SIZE];
  private int[] gcrWriteSector = new int[GCR_SECTOR_SIZE];

  // Max 40 tracks each 21 sectors
  private int[][][] gcrCacheSector = new int[40][21][];

  private int via1PB;
  private int via1PA;
  private int via1CB;
  private int via1CA;
  private int via1T1Ctr;
  private int via1T1Latch;
  private int via1T2Ctr;
  private int via1T2Latch;
  private int via1SerialRegister;
  private int via1AuxControl;
  private int via1PerControl;
  private int via1IFlag;
  private int via1IEnable;

  private int via2PB;
  private int via2PA;
  private int via2CB;
  private int via2CA;
  private int via2T1Ctr;
  private int via2T1Latch;
  private int via2T2Ctr;
  private int via2T2Latch;
  private int via2SerialRegister;
  private int via2AuxControl;
  int via2PerControl; // Needed by the C1541Emu
  private int via2IFlag;
  private int via2IEnable;

  private boolean diskChanged = true;
  private boolean writeProtected = true;

  public boolean ledOn;
  public boolean motorOn;
  public int currentTrack;
  public int currentSector;
  public int headOutBeyond = 0;
  private int bytesWritten = 0;
  private int currentByte = 0;

  private C64Reader reader;

  boolean byteReadyOverflow = false;
  boolean diskModeWrite = false;

  C64Screen cia2;
  int iecLines;

  public C1541Chips(C1541Emu emu) {
    cpu = emu;
    init(cpu);
  }

  public void initIEC2(C64Screen s) {
    cia2 = s;
  }

  public void setReader(C64Reader reader) {
    log("Setting reader...");
    this.reader = reader;
    reader.setDiskListener(this);
  }

  public final int performRead(int address, long cycles) {
    switch (address) {
    case 0x1800: // VIA1 PB - IEEE Serial Bus / IEC
      return (via1PB & 0x1a
          | ((iecLines & cia2.iecLines) >> 7) & 0x01    // DATA
          | ((iecLines & cia2.iecLines) >> 4) & 0x04    // CLK
          | (cia2.iecLines << 3) & 0x80) ^ 0x85;        // ATN
    case 0x1801: // VIA1 PA
    case 0x180f: // VIA1 PA
      via1IFlag &= ~2;
      checkInterrupt(1, "read from pa");
      return 0xff; // via1PA;
    case 0x1802: // VIA1 CB
      return via1CB;
    case 0x1803: // VIA1 CA
      return via1CA;
    case 0x1804:
      via1IFlag &= 0xbf;
      checkInterrupt(1, "read T1 low");
      return via1T1Ctr & 0xff;
    case 0x1805:
      return via1T1Ctr >> 8;
          case 0x1806:
            return via1T1Latch & 0xff;
          case 0x1807:
            return via1T1Latch >> 8;
          case 0x1808:
            via1IFlag &= 0xdf;
            checkInterrupt(1, "read T2 low");
            return via1T2Ctr & 0xff;
          case 0x1809:
            return via1T2Ctr >> 8;
          case 0x180a:
            return via1SerialRegister;
          case 0x180b:
            return via1AuxControl;
          case 0x180c:
            return via1PerControl;
          case 0x180d:
            return via1IFlag;
          case 0x180e:
            return via1IEnable;


          case 0x1c00: // VIA2 PB
            return (via2PB & 0x6f) | sync() | writeProtect();
          case 0x1c01: // VIA2 PA - read from disk!
          case 0x1c0f:
            return readByte();
          case 0x1c02: // VIA2 CB
            return via2CB;
          case 0x1c03: // VIA2 CA
            return via2CA;
          case 0x1c04:
            via2IFlag &= 0xbf;
            checkInterrupt(1, "read T1 low");
            return via2T1Ctr & 0xff;
          case 0x1c05:
            return via2T1Ctr >> 8;
          case 0x1c06:
            return via2T1Latch & 0xff;
          case 0x1c07:
            return via2T1Latch >> 8;
          case 0x1c08:
            via2IFlag &= 0xdf;
//          System.out.println("Updated via2IFlag: " +
//          Integer.toHexString(via2IFlag));
            checkInterrupt(2, "read T2 low");
            return via2T2Ctr & 0xff;
          case 0x1c09:
            return via2T2Ctr >> 8;
          case 0x1c0a:
            return via2SerialRegister;
          case 0x1c0b:
            return via2AuxControl;
          case 0x1c0c:
            return via2PerControl;
          case 0x1c0d:
            return via2IFlag;
          case 0x1c0e:
            return via2IEnable;

    }

    return 0;
  }

  public final void performWrite(int address, int data, long cycles) {
    if (DEBUG_WRITE) {
      log("Writing to " + Integer.toHexString(address)
          + " = " + Integer.toHexString(data) + " pc = " +
          Integer.toHexString(cpu.pc));
    }
    switch (address) {

    // VIA 1
    case 0x1800: // VIA1 PB
      via1PB = data;
      // Frodo style (another test...)
      updateIECLines();
      break;
    case 0x1801: // VIA1 PA
      // Clear flag...
      via1IFlag &= ~2;
      checkInterrupt(1, "wrote pa");

      via1PA = data;
      break;
    case 0x1802: // VIA1 CB
      via1CB = data;
      updateIECLines();
      break;
    case 0x1803: // VIA1 CA
      via1CA = data;
      break;
    case 0x1804:
      via1T1Latch = (via1T1Latch & 0xff00) | data;
      break;
    case 0x1805: // T1 High
      // 'Reset' timer 1
      via1T1Latch = (via1T1Latch & 0xff) | (data << 8);
      via1IFlag &= 0xbf;
      via1T1Ctr = via1T1Latch;
      checkInterrupt(1, "write T1 high");
      break;
    case 0x1806:
      via1T1Latch = (via1T1Latch & 0xff00) | data;
      break;
    case 0x1807:
      via1T1Latch = (via1T1Latch & 0xff) | (data << 8);
      break;
    case 0x1808:
      via1T2Latch = (via1T2Latch & 0xff00) | data;
      break;
    case 0x1809: // T2 high
      // 'Reset' timer 2
      via1T2Latch = (via1T2Latch & 0xff) | (data << 8);
      via1IFlag &= 0xdf;
      via1T2Ctr = via1T1Latch;
      checkInterrupt(1, "write T2 high");
      break;
    case 0x180a:
      via1SerialRegister = data;
      break;
    case 0x180b:
      via1AuxControl = data;
      break;
    case 0x180c:
      via1PerControl = data;
      break;
    case 0x180d:
      via1IFlag &= ~data;
      checkInterrupt(1, "write IFlag");
      break;
    case 0x180e:
      if ((data & 0x80) == 0x80)
        via1IEnable |= data & 0x7f;
      else
        via1IEnable &= ~data;
      if (DEBUG_WRITE) System.out.println("Wrote IE CIA1: " + via1IEnable);
      checkInterrupt(1, "write IE");
      break;

      // VIA 2
    case 0x1c00: // VIA2 PB
      boolean lastLed = ledOn;
      boolean lastMotor = motorOn;
      ledOn = ((data & 0x08) != 0);
      motorOn = ((data & 0x04) != 0);

      if (lastLed != ledOn | lastMotor != motorOn) {
        update(this, LED_MOTOR);
      }

//    System.out.println("C1541: LedON: " + ledOn);

      // If step motor value changed - check if it is out or in!
      if (((via2PB ^ data) & 0x3) != 0) {
        if ((via2PB & 0x3) == ((data + 1) & 0x3))
          headOut();
        else if ((via2PB & 0x3) == ((data - 1) & 0x3))
          headIn();
      }

      via2PB = data;
      break;
    case 0x1c01: // VIA2 PA
      via2PA = data;
      writeByte(data);
      break;
    case 0x1c02: // VIA2 CB
      via2CB = data;
      break;
    case 0x1c03: // VIA2 CA
      via2CA = data;
      break;
    case 0x1c04:
      via2T1Latch = (via2T1Latch & 0xff00) | data;
      break;
    case 0x1c05:
      // 'Reset' timer 1
      via2T1Latch = (via2T1Latch & 0xff) | (data << 8);
      via2IFlag &= 0xbf;
//    System.out.println("Updated via2IFlag: " +
//    Integer.toHexString(via2IFlag));
      via2T1Ctr = via2T1Latch;
      checkInterrupt(2, "write T1 high: " + data + " latch: " + via2T1Latch);
//    System.out.println("C1541: Wrote via2 T1H(05) Latch: " + data + " => " +
//    via2T1Latch);
      break;
    case 0x1c06:
      via2T1Latch = (via2T1Latch & 0xff00) | data;
//    System.out.println("C1541: Wrote via2 T1L Latch: " + data + " => " +
//    via2T1Latch);
      break;
    case 0x1c07:
      via2T1Latch = (via2T1Latch & 0xff) | (data << 8);
//    System.out.println("C1541: Wrote via2 T1H(07) Latch: " + data + " => " +
//    via2T1Latch);
      break;
    case 0x1c08:
      via2T2Latch = (via2T2Latch & 0xff00) | data;
      break;
    case 0x1c09:
      // 'Reset' timer 2
      via2T2Latch = (via2T2Latch & 0xff) | (data << 8);
      via2IFlag &= 0xdf;
//    System.out.println("Updated via2IFlag: " +
//    Integer.toHexString(via2IFlag));
      via2T2Ctr = via2T2Latch;
      checkInterrupt(2, "write T2 high");
      break;
    case 0x1c0a:
      via2SerialRegister = data;
      break;
    case 0x1c0b:
      via2AuxControl = data;
      break;
    case 0x1c0c:
      byteReadyOverflow = (data & 2) == 2;
      diskModeWrite = (data & 0x20) != 0x20;
//    System.out.println("Writing to VIA2 PC/CA:" +
//    Integer.toString(data, 16) + " ByteOverflow: " +
//    byteReadyOverflow + " write: " + diskModeWrite);
      via2PerControl = data;
      if (!diskModeWrite) currentByte = -1;
      break;
    case 0x1c0d:
      via2IFlag &= ~data;
//    System.out.println("Updated via2IFlag: " +
//    Integer.toHexString(via2IFlag));
      checkInterrupt(2, "write IFlag");
      break;
    case 0x1c0e:
      if ((data & 0x80) == 0x80)
        via2IEnable |= data & 0x7f;
      else
        via2IEnable &= ~data;
      checkInterrupt(2, "write IE");
      break;
    }
  }

  private void writeByte(int data) {
    currentByte = data;
    log("CPU Writes byte: " + Integer.toString(data, 16) +
        " at " + Integer.toString(cpu.pc, 16));
    //    forward();
  }

  private void finishWrite() {
    if (DEBUG) {log("Written bytes: " + bytesWritten + " at " +
        track + "," + sector + "  " + sectorPos);
    }
    if (bytesWritten > 0)
      convertGCRSector();
    bytesWritten = 0;
  }

  private void checkInterrupt(int via, String reason) {
    if (via == 1) {
      if ((via1IFlag & via1IEnable) == 0) {
        if (DEBUG_IRQ && (via1IFlag & 0x80) != 0) {
          System.out.println("C1541: ** IRQ/IEC ** Clearing IRQ CIA 1: " +
              reason);
        }
        via1IFlag &= 0x7f;
        clearIRQ(1);
      } else {
        if (DEBUG_IRQ && (via1IFlag & 0x80) == 0) {
          System.out.println("C1541: ** IRQ/IEC ** Setting IRQ CIA 1: " +
              reason + " iflag: " +
              Integer.toHexString(via1IFlag | 0x80));
        }
        via1IFlag |= 0x80;
        setIRQ(1);
      }
    } else {
      if ((via2IFlag & via2IEnable) == 0) {
        if (DEBUG_IRQ && (via2IFlag & 0x80) != 0) {
          System.out.println("C1541: ** IRQ ** Clearing IRQ CIA 2: " +
              reason);
        }
        via2IFlag &= 0x7f;
        clearIRQ(2);
      } else {
        if (DEBUG_IRQ && (via2IFlag & 0x80) == 0) {
          System.out.println("C1541: ** IRQ ** Setting IRQ CIA 2: " +
              reason + " iflag: " +
              Integer.toHexString(via2IFlag | 0x80));
        }

        via2IFlag |= 0x80;
        setIRQ(2);
      }
    }
  }

  public void updateIECLines() {
    int data = ~via1PB & via1CB;
    iecLines = (data << 6) & ((~data ^ cia2.iecLines) << 3) & 0x80
    | (data << 3) & 0x40;
  }

  long nextAutoforward;
  void autoForward(long cycles) {
    if (motorOn) {
      if (nextAutoforward < cycles) {
        if (nextAutoforward == 0) {
          nextAutoforward = cycles + 32;
        } else {
          // 30 seems to work (3 x 8 is fastest, 6x8 slowest)
          nextAutoforward += 30;
          forward();
        }
      }
    } else {
      nextAutoforward = cycles + 10000;
    }
  }

  // -------------------------------------------------------------------
  // Emulation of VIA (update of timers, etc)
  // -------------------------------------------------------------------
  long lastCycles = 0;
  long nextCheck = 0;
  public final void clock(long cycles) {
    if (nextCheck > cycles) return;

    autoForward(cycles);

    int delta = (int) (cycles - lastCycles);
    lastCycles = cycles;
    nextCheck = cycles + 13;

    via1T1Ctr = via1T1Ctr - delta;
    if (via1T1Ctr <= 0) {
      if ((via1AuxControl & 0x40) == 0x40) {
        // Increase by latch (to get correct square timing even if not
        // called each cycle!
        via1T1Ctr += via1T1Latch;
      } else {
        // Otherwise wrap around...
        via1T1Ctr &= 0xffff;
      }
      via1IFlag |= 0x40;
      checkInterrupt(1, "clock wrap, T1");
    }

    // Only count in one shot more?
    if ((via1AuxControl & 0x20) == 0) {
      via1T2Ctr = via1T2Ctr - delta;
      if (via1T2Ctr <= 0) {
        via1IFlag |= 0x20;
        via1T2Ctr &= 0xffff;

        checkInterrupt(1, "clock wrap, T2");
      }
    }


    // via 2
    via2T1Ctr = via2T1Ctr - delta;
    if (via2T1Ctr <= 0) {
      if ((via2AuxControl & 0x40) == 0x40) {
        // Increase by latch (to get correct square timing even if not
        // called each cycle!
        via2T1Ctr += via2T1Latch;
      } else {
        // Otherwise wrap around...
        via2T1Ctr &= 0xffff;
      }
      via2IFlag |= 0x40;

      checkInterrupt(2, "clock wrap, T1:  latch:" + via2T1Latch);
    }

    // Only count in one shot more?
    if ((via2AuxControl & 0x20) == 0) {
      via2T2Ctr = via2T2Ctr - delta;
      if (via2T2Ctr <= 0) {
        via2IFlag |= 0x20;
        via2T2Ctr &= 0xffff;

        checkInterrupt(2, "clock wrap, T2");
      }
    }
  }


  // -------------------------------------------------------------------
  // DiskListener
  // -------------------------------------------------------------------
  public void diskChanged() {
//  reset();
    diskChanged = true;

    // Clear cache...
    for (int i = 0, n = 40; i < n; i++) {
      for (int j = 0, m = 21; j < m; j++) {
        gcrCacheSector[i][j] = null;
      }
    }


    byte[] bam = reader.getSector(18, 0);
    diskID1 = bam[162] & 0xff;
    diskID2 = bam[163] & 0xff;
    System.out.println("Disk changed => Disk ID:" + diskID1 + "," + diskID2);
  }

  // Set the via1IFlag for CA1
  public void atnChanged(boolean hi) {
    if (DEBUG_IEC) {
      System.out.println("C1541: *** IEC ATN changed detected!!! Enable: " +
          Integer.toHexString(via1IEnable));
    }
    if (hi) {
      via1IFlag |= 0x02;
      checkInterrupt(1, "atn went high");
      //      cpu.memory[0x7c] = 1;
    }

    // Recalculate the iecLines...
    updateIECLines();
  }


  public void reset() {
    track = 1;
    hTrack = 2;
    sector = 0;
    sectorPos = -1;
    diskChanged = false;
    writeProtected = false;

    currentTrackSize = C64Reader.getSectorCount(track);

    // Read the current track/sector...
    readGCRSector(track, sector);
  }

  // -------------------------------------------------------------------
  //
  // -------------------------------------------------------------------

  private int writeProtect() {
    if (diskChanged) {	// Disk change -> WP sensor strobe
      System.out.println("C1541: //// Disk change detected???!!! ////");
      diskChanged = false;
      //      sectorPos = 0; //(int) (Math.random() * 100);
      return writeProtected ? 0x10 : 0;
    } else {
      return writeProtected ? 0 : 0x10;
    }
  }

  private int sync() {
//  System.out.println("C1541: Checking Sync: sp: " + sectorPos + " = " +
//  Integer.toHexString(gcrSector[sectorPos]) + " " +
//  cpu.cycles);
    // Sync byte on first sector position!
    if (sectorPos == -1) return 0x80;
    if (gcrSector[sectorPos] == 0xff) {
      return 0;
    }

    // forward();
    return 0x80;
  }

  boolean lastSync = false;
  private int readByte() {
    if (sectorPos == -1) return 0;
    int data = gcrSector[sectorPos];
    if (DEBUG_GCR) {
      System.out.println("C1541: Read byte from: " + sectorPos + " => " +
          Integer.toString(data, 16) + " " +
          i2c(data) + " " + cpu.cycles + " pc = " +
          Integer.toString(cpu.pc, 16));
    }
    //    forward();
    return data;
  }

  // Roatet head forward to read next byte!
  private void forward() {
    if (diskModeWrite && (via2CA == 0xff) &&
        currentByte != -1 && sectorPos >= 0) {
      bytesWritten++;
      if (DEBUG) {
        System.out.println("#### Write byte: " +
            Integer.toString(currentByte, 16) + " ("
            + Integer.toString(gcrSector[sectorPos], 16) +
            ")" + (char) currentByte + "  y=" + cpu.y +
            " written: " + bytesWritten +
            " SectorPos: " + sectorPos + " " + cpu.cycles);
      }
      // Should this be written a byte "backwards"??
      gcrWriteSector[sectorPos] = currentByte;
    }
    sectorPos++;
    if (sectorPos == GCR_SECTOR_SIZE) {
      finishWrite();
      sectorPos = -1;
      // Some extra cycles when switching sector?!!
      nextAutoforward += 1000;
      sector = (sector + 1) % currentTrackSize;
      readGCRSector(track, sector);
    }

    update(this, SECTOR_UPDATE);
    // IF not sync now or last time...
    if (diskModeWrite || sync() != 0 || !lastSync) {
      // Only trigger byte ready when not at sync bytes!
      cpu.triggerByteReady();
    }
    lastSync = sync() == 0;
  }

  private void headOut() {
    if (hTrack > 2) {
      hTrack--;
    } else {
      headOutBeyond++;
    }

    System.out.println("1541: Move head In to: " + hTrack);
    updateHTrack();

    update(this, HEAD_MOVED);
  }

  private void headIn() {
    if (hTrack < 70)
      hTrack++;
    System.out.println("1541: Move head Out to: " + hTrack);

    updateHTrack();

    update(this, HEAD_MOVED);
  }

  private void updateHTrack() {
    if (track != hTrack >> 1) {
      // Takes some time before ready for next...
      nextAutoforward = cpu.cycles + 100000;

      finishWrite();
      track = hTrack >> 1;
    sector = 0;
    //      sectorPos = 0;
    sectorPos = -1;
    currentTrackSize = C64Reader.getSectorCount(track);
    System.out.println("1541: New Track " + track + " reading: "
        + track + ", " + sector +
        " s/t: " + currentTrackSize);
    readGCRSector(track, sector);
    }
  }

  // returns a 10 bit GCR from a 8 bit byte
  private static long getGCR(int b) {
    return (GCR[b >> 4] << 5) | GCR[b & 15];
  }

  private static char i2c(int i) {
    if (i < 32) return '.';
    return (char) i;
  }

  public static int
  makeGCR(int[] gcrBuf, int pos, int b1, int b2, int b3, int b4) {
    int cSum = b1 ^ b2 ^ b3 ^ b4;
    if (DEBUG_GCR) {
      System.out.print("GCR 4 => 5: " + i2c(b1) + i2c(b2) + i2c(b3) + i2c(b4)
          + ", " + b1 + ", " + b2 + ", " + b3 + ", " + b4);
    }
    long gcr = (getGCR(b1) << 30) | (getGCR(b2) << 20) |
    (getGCR(b3) << 10) | getGCR(b4);

    long bits = 32;
    for (int i = 0, n = 5; i < n; i++) {
      gcrBuf[pos++] = (int) ((gcr >> bits) & 0xff);
      bits = bits - 8;
      if (DEBUG_GCR) {
        if (i == 0) System.out.print(" => ");
        else System.out.print(", ");
        System.out.print(gcrBuf[pos - 1]);
      }
    }
    if (DEBUG_GCR) {
      System.out.println("");
    }
    return cSum;
  }

  // Converts on the fly - should probably cache?
  private void readGCRSector(int track, int sector) {
    currentTrack = track;
    currentSector = sector;

    if (gcrCacheSector[track][sector] != null) {
      if (DEBUG_GCR) log("Using GCR Sector cache for " + track + "," + sector);
      gcrSector = gcrCacheSector[track][sector];
      gcrWriteSector = gcrSector;
      return;
    }

    if (DEBUG_GCR) log("Reading and GCR:ing sector: " + track + ", " + sector);
    byte[] sectorBuf = reader.getSector(track, sector);
    int pos = 0;
    int cSum = 0;

    gcrSector = new int[GCR_SECTOR_SIZE];
    // First data is sync!
    gcrSector[pos++] = 0xff;

    makeGCR(gcrSector, pos, 0x08, sector ^ track ^ diskID1 ^ diskID2,
        sector, track);
    pos += 5;
    makeGCR(gcrSector, pos, diskID2, diskID1, 0x0f, 0x0f);
    pos += 5;

    // pos = 11
    for (int i = 0; i < 9; i++) {
      gcrSector[pos++] = 0x55;
    }

    // Another sync - at position 20 (?)
    gcrSector[pos++] = 0xff;
    // pos = 21
    // cancel out first 7 by setting cSum to 7
    cSum = 0x07;
    cSum ^= makeGCR(gcrSector, pos, 0x07, sectorBuf[0] & 0xff,
        sectorBuf[1] & 0xff, sectorBuf[2] & 0xff);
    pos += 5;

    // pos = 26
    for (int i = 3, n = 255; i < n; i += 4) {
      cSum ^= makeGCR(gcrSector, pos, sectorBuf[i] & 0xff,
          sectorBuf[i + 1] & 0xff,
          sectorBuf[i + 2] & 0xff,
          sectorBuf[i + 3] & 0xff);
      pos += 5;
    }

    cSum ^= sectorBuf[255] & 0xff;

    // Verify checksum
//  int s2 = 0;
//  for (int i = 0, n = 256; i < n; i++) {
//  s2 ^= (sectorBuf[i] & 0xff);
//  }
//  System.out.println("### cSum: " + cSum + " = " + s2);


    makeGCR(gcrSector, pos, sectorBuf[255] & 0xff, cSum & 0xff, 0, 0);
    pos += 5;

    for (int i = 0, n = 8; i < n; i++) {
      gcrSector[pos++] = 0x55;
    }

    // Create a similar write sector!?!??
    //System.arraycopy(gcrSector,0, gcrWriteSector, 0, gcrWriteSector.length);
//  System.out.println("Converted to: " + pos + " bytes");

    // Allow writing the sector!!!
    gcrWriteSector = gcrSector;
    gcrCacheSector[track][sector] = gcrSector;
  }

  private void convertGCRSector() {
    int[] buffer = new int[256 + 10];
    int pos = 0;
    int start = 0;
    for (int i = 5, n = 30; i < n; i++) {
      if (gcrSector[i] == 0xff) {
        start = i + 1;
      }
    }
    for (int i = 0, n = 325; i < n; i += 5) {
      convertFromGCR(buffer, pos, i + start);
      pos += 4;
    }
    System.out.println("Converted " + pos + " bytes");
    // First byte is a 7, then last 3 is checksum + 2 zeros => 256 bytes

    byte[] newSector = new byte[256];
    for (int i = 0, n = 255; i < n; i++) {
      newSector[i] = (byte) (buffer[i + 1] & 0xff);
    }
    reader.setSector(track, sector, newSector);
  }

  private void convertFromGCR(int[] buff, int pos, int posW) {
    long data = convertFromGCR(gcrWriteSector[posW],
        gcrWriteSector[posW + 1],
        gcrWriteSector[posW + 2],
        gcrWriteSector[posW + 3],
        gcrWriteSector[posW + 4]);

    // bits 19.15 + 14.10   9.5 + 4.0
    System.out.println("Converting (" + posW + "): " +
        Long.toString(data, 16));

    buff[pos++] = ((int)(data >> 24)) & 0xff;
    buff[pos++] = ((int)(data >> 16)) & 0xff;
    buff[pos++] = ((int)(data >> 8)) & 0xff;
    buff[pos++] = ((int)(data)) & 0xff;

    for (int i = 0, n = 4; i < n; i++) {
      int c = buff[pos - 4 + i];
      System.out.println("Converted: " + c + " => " + (char) c);
    }
  }

  public static long
  convertFromGCR(long b1, long b2, long b3, long b4, long b5) {
    long data = (b1 << 32) + (b2 << 24) + (b3 << 16) + (b4 << 8)+ b5;
    System.out.println("Converting from: " + Long.toString(data, 16));
    long d1 = GCR_REV[((int) (data >> 35)) & 0x1f] << 4 |
    GCR_REV[((int)(data >> 30)) & 0x1f];
    long d2 = GCR_REV[((int)(data >> 25)) & 0x1f] << 4 |
    GCR_REV[((int)(data >> 20)) & 0x1f];
    long d3 = GCR_REV[((int)(data >> 15)) & 0x1f] << 4 |
    GCR_REV[((int)(data >> 10)) & 0x1f];
    long d4 = GCR_REV[((int)(data >> 5)) & 0x1f] << 4 |
    GCR_REV[((int)data) & 0x1f];
    return (d1 << 24) + (d2 << 16) + (d3 << 8) + d4;
  }


  public static void main(String[] args) {
    String text = args[0];
    int[] gcrEncoded = new int[text.length() * 2];
    int gcrPos = 0;
    int gcrVal = 0;
    int gcrBits = 0;
    // GCR Encode
    for (int i = 0, n = text.length(); i < n; i++) {
      int c = text.charAt(i) & 0xff;
      gcrVal |= ((GCR[c >> 4] << 5 ) | GCR[c & 0xf]) << gcrBits;
      gcrBits += 10;

      // Output the GCR encoded data
      while (gcrBits >= 8) {
        gcrEncoded[gcrPos++] = gcrVal & 0xff;
        gcrBits -= 8;
        gcrVal = gcrVal >> 8;
      }
    }


    System.out.println("GCR Data:");
    for (int i = 0, n = gcrPos; i < n; i++) {
      if (i % 16 == 0) {
        System.out.println("");
      }
      System.out.print(Integer.toHexString(gcrEncoded[i]) + " ");
    }

    // Decode
    gcrVal = 0;
    gcrBits = 0;
    // Just printout...
    System.out.println("Decoded: " );
    for (int i = 0, n = gcrPos; i < n; i++) {
      gcrVal |= gcrEncoded[i] << gcrBits;
      gcrBits += 8;

      if (gcrBits >= 10) {
        int val = gcrVal & 0x3ff;
        int bval = (GCR_REV[val >> 5] << 4) | (GCR_REV[val & 0x1f]);
        gcrVal = gcrVal >> 10;
        gcrBits -= 10;
        System.out.print((char) bval);
      }
    }
    System.out.println("");

    // GCR Decode
  }

  public void stop() {
  }

  public void log(String text) {
    System.out.println("C1541: " + text);
  }

}
