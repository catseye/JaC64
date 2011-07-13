/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import javax.swing.JPanel;

/**
 * Implements the VIC chip + some other HW
 *
 * @author  Joakim Eriksson (joakime@sics.se) / main developer, still active
 * @author  Jan Blok (jblok@profdata.nl) / co-developer during ~2001
 * @version $Revision: 1.11 $, $Date: 2006/05/02 16:26:26 $
 */

public class C64Screen extends ExtChip implements Observer, MouseListener,
MouseMotionListener {
  public static final String version = "1.11";

  public static final int SERIAL_ATN = (1 << 3);
  public static final int SERIAL_CLK_OUT = (1 << 4);
  public static final int SERIAL_DATA_OUT = (1 << 5);
  public static final int SERIAL_CLK_IN = (1 << 6);
  public static final int SERIAL_DATA_IN = (1 << 7);

  public static final int RESID_6581 = 1;
  public static final int RESID_8580 = 2;
  public static final int JACSID = 3;

  public static final boolean IRQDEBUG = false;
  public static final boolean SPRITEDEBUG = false;
  public static final boolean IODEBUG = false;
  public static final boolean VIC_MEM_DEBUG = false;
  public static final boolean BAD_LINE_DEBUG = false;
  public static final boolean STATE_DEBUG = false;
  public static final boolean DEBUG_IEC = false;

  public static final boolean DEBUG_CYCLES = false;

  public static final int IO_UPDATE = 37;
  // This is PAL speed! - will be called each scan line...

  private static final int VIC_IRQ = 1;

  // This might be solved differently later!!!
  public static final int CYCLES_PER_LINE = VICConstants.SCAN_RATE;

  // ALoow the IO to write in same as RAM
  public static final int IO_OFFSET = CPU.IO_OFFSET;
  public static final boolean SOUND_AVAIABLE = true;

  public static final Color TRANSPARENT_BLACK = new Color(0, 0, 0x40, 0x40);
  public static final Color DARKER_0 = new Color(0, 0, 0x40, 0x20);
  public static final Color LIGHTER_0 = new Color(0xe0, 0xe0, 0xff, 0x30);
  public static final Color DARKER_N = new Color(0, 0, 0x40, 0x70);
  public static final Color LIGHTER_N = new Color(0xe0, 0xe0, 0xff, 0xa0);

  public static final Color LED_ON = new Color(0x60, 0xdf, 0x60, 0xc0);
  public static final Color LED_OFF = new Color(0x20, 0x60, 0x20, 0xc0);
  public static final Color LED_BORDER = new Color(0x40, 0x60, 0x40, 0xa0);


  public static final int LABEL_COUNT = 32;
  private Color[] darks = new Color[LABEL_COUNT];
  private Color[] lites = new Color[LABEL_COUNT];
  private int colIndex = 0;

  // This is the screen width and height used...
  private final static int SC_WIDTH = 384; //403;
  private final static int SC_HEIGHT = 284;
  private final int SC_XOFFS = 32;
  // Done: this should be - 24!
  private final int SC_SPXOFFS = SC_XOFFS - 24;
  private final int FIRST_VISIBLE_VBEAM = 15;
  private final int SC_SPYOFFS = FIRST_VISIBLE_VBEAM + 1;


  private IMonitor monitor;

  private int targetScanTime = 20000;
  private int actualScanTime = 20000;
  private long lastScan = 0;
  private long nextIOUpdate = 0;
  private boolean DOUBLE = false;
  private int reset = 100;
  private C64Canvas canvas;

  private int[] memory;

  private Keyboard keyboard;

  ExtChip sidChip;

  CIA cia[];
  //  C1541 c1541;
  C1541Chips c1541Chips;

  TFE_CS8900 tfe;

  int iecLines = 0;
  // for disk emulation...
  int cia2PRA = 0;
  int cia2DDRA = 0;

  AudioClip trackSound = null;
  AudioClip motorSound = null;

  private int lastTrack = 0;
  private int lastSector = 0;
  private boolean ledOn = false;
  private boolean motorOn = false;

  // This is an IEC emulation (non ROM based)
  boolean emulateDisk = false; //true; //!CPU.EMULATE_1541; // false;

  private int[] cbmcolor = VICConstants.COLOR_SETS[0];

  // -------------------------------------------------------------------
  // VIC-II variables
  // -------------------------------------------------------------------
  public int vicBank;
  public int charSet;
  public int videoMatrix;
  public int videoMode;

  // VIC Registers
  int irqMask = 0;
  int irqFlags = 0;
  int control1 = 0;
  int control2 = 0;
  int sprXMSB = 0;
  int sprEN = 0;
  int sprYEX = 0;
  int sprXEX = 0;
  int sprPri = 0;
  int sprMul = 0;
  int sprCol = 0;
  int sprBgCol = 0;
  int sprMC0 = 0;
  int sprMC1 = 0;
  int vicMem = 0;
  int vicMemDDRA = 0;
  int vicMemDATA = 0;
  // Read for debugging on other places...
  public int vbeam = 0; // read at d012
  public int raster = 0;
  int bCol = 0;
  int bgCol[] = new int[4];

  private int vicBase = 0;
  private boolean badLine = false;
  private int spr0BlockSel;

  // New type of position in video matrix - Video Counter (VIC II docs)
  int vc = 0;
  int vcBase = 0;
  int rc = 0;
  int vmli = 0;
  // The current vBeam pos - 9... => used for keeping track of memory
  // position to write to...
  int vPos = 0;
  int mpos = 0;

  int displayWidth = SC_WIDTH;
  int displayHeight = SC_HEIGHT;
  int offsetX = 0;
  int offsetY = 0;

  // Cached variables...
  boolean gfxVisible = false;
  boolean paintBorder = false;
  boolean paintSideBorder = false;
  
  int borderColor = cbmcolor[0];
  int bgColor = cbmcolor[1];

  private boolean extended = false;
  private boolean multiCol = false;
  private boolean blankRow = false;
  private boolean hideColumn = false;

  int multiColor[] = new int[4];

  // 48 extra for the case of an expanded sprite byte
  int collissionMask[] = new int[SC_WIDTH + 48];

  Sprite sprites[] = new Sprite[8];

  private Color colors[] = null;

  private int horizScroll = 0;
  private int vScroll = 0;

  private Image image;
  private Graphics g2;

  // The font is in a copy in "ROM"...
  private int charMemoryIndex = 0;

  // Caching all 40 chars (or whatever) each "bad-line"
  private int[] vicCharCache = new int[40];
  private int[] vicColCache = new int[40];

  public Image screen = null;
  private MemoryImageSource mis = null;
  private AudioDriver audioDriver;

  // The array to generate the screen in Extra rows for sprite clipping
  // And for clipping when scrolling (smooth)
  int mem[] = new int[SC_WIDTH * (SC_HEIGHT + 10)];

  int rnd = 754;
  String message;
  String tmsg = "";

  int frame = 0;
  private boolean updating = false;
  boolean displayEnabled = true;
  boolean irqTriggered = false;
  long lastLine = 0;
  long firstLine = 0;
  long lastIRQ = 0;

  int potx = 0;
  int poty = 0;
  boolean button1 = false;
  boolean button2 = false;

  // This variable changes when Kernal has installed
  // a working ISR that is reading the keyboard
  private boolean isrRunning = false;
  private int     ciaWrites = 0;

  public C64Screen(IMonitor m, boolean dob) {
    monitor = m;
    DOUBLE = dob;

    setScanRate(50);
    makeColors(darks, DARKER_0, DARKER_N);
    makeColors(lites, LIGHTER_0, LIGHTER_N);
  }

  public void setAutoscale(boolean val) {
    DOUBLE = val;
    canvas.setAutoscale(val);
  }

  private void makeColors(Color[] colors, Color c1, Color c2) {
    int a0 = c1.getAlpha();
    int r0 = c1.getRed();
    int g0 = c1.getGreen();
    int b0 = c1.getBlue();
    int an = c2.getAlpha();
    int rn = c2.getRed();
    int gn = c2.getGreen();
    int bn = c2.getBlue();
    int lc = LABEL_COUNT / 2;
    for (int i = 0, n = lc; i < n; i++) {
      colors[i] =
        colors[LABEL_COUNT - i - 1] =
          new Color(((lc - i) * r0 + (i * rn)) / lc,
              ((lc - i) * g0 + (i * gn)) / lc,
              ((lc - i) * b0 + (i * bn)) / lc,
              ((lc - i) * a0 + (i * an)) / lc);
    }
  }

  public void setColorSet(int c) {
    if (c >= 0 && c < VICConstants.COLOR_SETS.length) {
      cbmcolor = VICConstants.COLOR_SETS[c];
      borderColor = cbmcolor[bCol];
      bgColor = cbmcolor[bgCol[0]];
      for (int i = 0, n = 8; i < n; i++) {
        sprites[i].color[0] = bgColor;
        sprites[i].color[1] = cbmcolor[sprMC0];
        sprites[i].color[3] = cbmcolor[sprMC1];
      }
    }
  }

  public CIA[] getCIAs() {
    return cia;
  }

  public void setSID(int sid) {
    switch (sid) {
    case RESID_6581:
    case RESID_8580:
      if (!(sidChip instanceof RESIDChip)) {
	if (sidChip != null) sidChip.stop();
        sidChip = new RESIDChip(cpu, audioDriver);
      }
      ((RESIDChip) sidChip).setChipVersion(sid);
      break;
    case JACSID:
      if (!(sidChip instanceof SIDChip)) {
	if (sidChip != null) sidChip.stop();
        sidChip = new SIDChip(cpu, audioDriver);
      }
      break;
    }
  }

  public void setScanRate(double hertz) {
    // Scan time for 10 scans...
    targetScanTime = (int) (1000000 / hertz);
    float diff = 1.0f * VICConstants.SCAN_RATE / 65;
  }

  public int getScanRate() {
    return (1000000 / targetScanTime);
  }

  public int getActualScanRate() {
    // This should be calculated... if it is too slow it will be
    // shown here
    return (1000000 / actualScanTime);
  }

  public void setIntegerScaling(boolean yes) {
    canvas.setIntegerScaling(yes);
  }

  public JPanel getScreen() {
    return canvas;
  }

  public AudioDriver getAudioDriver() {
    return audioDriver;
  }

  public boolean ready() {
    return isrRunning;
  }

  public void setDisplayFactor(double f) {
    displayWidth = (int) (SC_WIDTH * f);
    displayHeight = (int) (SC_HEIGHT * f);
    crtImage = null;
  }

  public void setDisplayOffset(int x, int y) {
    offsetX = x;
    offsetY = y;
  }

  public void dumpGfxStat() {
    monitor.info("Char MemoryIndex: 0x" +
        Integer.toString(charMemoryIndex, 16));
    monitor.info("CharSet adr: 0x" +
        Integer.toString(charSet, 16));
    monitor.info("VideoMode: " + videoMode);
    monitor.info("Vic Bank: 0x" +
        Integer.toString(vicBank, 16));
    monitor.info("Video Matrix: 0x" +
        Integer.toString(videoMatrix, 16));

    monitor.info("Text: extended = " + extended +
        " multicol = " + multiCol);

    monitor.info("24 Rows on? " +
        (((control1 & 0x08) == 0) ? "yes" : "no"));

    monitor.info("YScroll = " + (control1 & 0x7));
    monitor.info("$d011 = " + control1);

    monitor.info("IRQ Latch: " +
        Integer.toString(irqFlags, 16));
    monitor.info("IRQ  Mask: " +
        Integer.toString(irqMask, 16));
    monitor.info("IRQ RPos : " + raster);

    for (int i = 0, n = 8; i < n; i++) {
      monitor.info("Sprite " + (i + 1) + " pos = " +
          sprites[i].x + ", " + sprites[i].y);
    }

    monitor.info("IRQFlags: " + getIRQFlags());
    monitor.info("NMIFlags: " + getNMIFlags());
    monitor.info("CPU IRQLow: " + cpu.getIRQLow());
    monitor.info("CPU NMILow: " + cpu.NMILow);
    monitor.info("Current CPU cycles: " + cpu.cycles);
    monitor.info("Next IO update: " + nextIOUpdate);
  }

  public void setSoundOn(boolean on) {
   audioDriver.setSoundOn(on);
  }

  public void setStick(boolean one) {
    keyboard.setStick(one);
  }

  public void registerHotKey(int key, int mod, String script, Object o) {
    keyboard.registerHotKey(key, mod, script, o);
  }

  public void setKeyboardEmulation(boolean extended) {
    monitor.info("Keyboard extended: " + extended);

    keyboard.stickExits = !extended;
    keyboard.extendedKeyboardEmulation = extended;
  }

  public void init(CPU cpu) {
    super.init(cpu);

    this.memory = cpu.getMemory();

    c1541Chips = cpu.getDrive().chips;
    c1541Chips.initIEC2(this);
    c1541Chips = cpu.getDrive().chips;
    c1541Chips.setObserver(this);


    for (int i = 0, n = sprites.length; i < n; i++) {
      sprites[i] = new Sprite();
      sprites[i].spriteNo = i;
    }

    cia = new CIA[2];
    cia[0] = new CIA(cpu, IO_OFFSET + 0xdc00, this);
    cia[1] = new CIA(cpu, IO_OFFSET + 0xdd00, this);

    tfe = new TFE_CS8900(IO_OFFSET + 0xde00);

//  c1541 = new C1541(memory);
//  c1541.addObserver(this);

    keyboard = new Keyboard(this, cia[0], memory);
    canvas = new C64Canvas(this, DOUBLE, keyboard);
    canvas.addMouseMotionListener(this);
    canvas.addMouseListener(this);

    audioDriver = new AudioDriverSE();
    audioDriver.init(44000, 22000);
    setSID(RESID_6581);
    charMemoryIndex = CPU.CHAR_ROM2;

    for (int i = 0; i < SC_WIDTH * SC_HEIGHT; i++) {
      mem[i] = cbmcolor[6];
    }

    mis = new MemoryImageSource(SC_WIDTH, SC_HEIGHT, mem, 0, SC_WIDTH);

    mis.setAnimated(true);
    mis.setFullBufferUpdates(true);
    screen = canvas.createImage(mis);

    //to fix bug http://developer.java.sun.com/developer/bugParade/bugs/4464723.html
//  setRequestFocusEnabled(true);
//  addMouseListener(new MouseAdapter() {
//  public void mousePressed(MouseEvent e) {
//  requestFocus();
//  }
//  });
    initUpdate();
  }

  public void update(Object src, Object data) {
    if (src != c1541Chips) {
      // Print some kind of message...
      message = (String) data;
    } else {
      updateDisk(src, data);
    }
  }

  void restoreKey(boolean down) {
    if (down) setNMI(KEYBOARD_NMI);
    else clearNMI(KEYBOARD_NMI);
  }

  // Should be checked up!!!
  private static final int[] IO_ADDRAND = new int[] {
    0xd03f, 0xd03f, 0xd03f, 0xd03f,
    0xd41f, 0xd41f, 0xd41f, 0xd41f,
    0xd8ff, 0xd9ff, 0xdaff, 0xdbff, // Color ram
    0xdc0f, 0xdd0f, 0xdeff, 0xdfff, // CIA + Expansion...
  };

  public int performRead(int address, long cycles) {
    // dX00 => and address
    // d000 - d3ff => &d063
    int pos = (address >> 8) & 0xf;
    //    monitor.info("Address before: " + address);
    address = address & IO_ADDRAND[pos];
    int val = 0;
    switch (address) {
    case 0xd000:
    case 0xd002:
    case 0xd004:
    case 0xd006:
    case 0xd008:
    case 0xd00a:
    case 0xd00c:
    case 0xd00e:
      return sprites[(address - 0xd000) >> 1].x & 0xff;
    case 0xd001:
    case 0xd003:
    case 0xd005:
    case 0xd007:
    case 0xd009:
    case 0xd00b:
    case 0xd00d:
    case 0xd00f:
      return sprites[(address - 0xd000) >> 1].y;
    case 0xd010:
      return sprXMSB;
    case 0xd011:
      return control1 & 0x7f | ((vbeam & 0x100) >> 1);
    case 0xd012:
      return vbeam & 0xff;
      // Sprite collission registers - zeroed after read!
    case 0xd013:
    case 0xd014:
      // Lightpen x/y
        return 0;
    case 0xd015:
      return sprEN;
    case 0xd016:
      return control2;
    case 0xd017:
      return sprYEX;
    case 0xd018:
      return vicMem;
    case 0xd019:
      if (SPRITEDEBUG)
        monitor.info("Reading d019: " + memory[address + IO_OFFSET]);
      return irqFlags;
    case 0xd01a:
      return irqMask;
    case 0xd01b:
      return sprPri;
    case 0xd01c:
      return sprMul;
    case 0xd01d:
      return sprXEX;
    case 0xd01e:
      val = sprCol;
      if (SPRITEDEBUG)
        monitor.info("Reading sprite collission: " +
            Integer.toString(address, 16) + " => " + val);
      sprCol = 0;
      return val;
    case 0xd01f:
      val = sprBgCol;
      if (SPRITEDEBUG)
        monitor.info("Reading sprite collission: " +
            Integer.toString(address, 16) + " => " + val);

      sprBgCol = 0;
      return val;
    case 0xd020:
      return bCol | 0xf0;
    case 0xd021:
    case 0xd022:
    case 0xd023:
    case 0xd024:
        return bgCol[address - 0xd021] | 0xf0;
    case 0xd025:
        return sprMC0 | 0xf0;
    case 0xd026:
        return sprMC1 | 0xf0;
    case 0xd027:
    case 0xd028:
    case 0xd029:
    case 0xd02a:
    case 0xd02b:
    case 0xd02c:
    case 0xd02d:
    case 0xd02e:
      return sprites[address - 0xd027].col | 0xf0;
    case 0xd41b:
    case 0xd41c:
      return sidChip.performRead(IO_OFFSET + address, cycles);
    case 0xd419:
      return potx;
    case 0xd41A:
      return poty;
    case 0xdc00:
      return keyboard.readDC00(cpu.lastReadOP);
    case 0xdc01:
      return keyboard.readDC01(cpu.lastReadOP);
    case 0xdd00:
      //       System.out.print("Read dd00 IEC1: ");
      // Try the frodo way... again...
      val = (cia2PRA | ~cia2DDRA) & 0x3f
      | iecLines & c1541Chips.iecLines;

      val &= 0xff;
      return val;
    default:
      if (pos == 0x4) {
        return sidChip.performRead(address + IO_OFFSET, cycles);
      } else if (pos == 0xd) {
        return cia[1].performRead(address + IO_OFFSET, cycles);
      } else if (pos == 0xc) {
        return cia[0].performRead(address + IO_OFFSET, cycles);
      } else if (pos == 0xe) {
        return tfe.performRead(address + IO_OFFSET, cycles);
      } else if (pos >= 0x8) {
        return memory[IO_OFFSET + address] | 0xf0;
      }
      return 0xff;
    }
  }

  public void performWrite(int address, int data, long cycles) {
    int pos = (address >> 8) & 0xf;
    address = address & IO_ADDRAND[pos];

//  monitor.info("Wrote to Chips at " + Integer.toString(address, 16)
//  + " = " + Integer.toString(data, 16));

    // Store in the memory given by "CPU"
    memory[address + IO_OFFSET] = data;

//  if (address >= 0xd800 && address < 0xdc00) {
//  int p = address - 0xd800;
//  System.out.println("### Write to color ram: " + (p % 40) + "," + p/40 +
//  " = " + data);
//  }

    switch (address) {
    // -------------------------------------------------------------------
    // VIC related
    // -------------------------------------------------------------------
    case 0xd000:
    case 0xd002:
    case 0xd004:
    case 0xd006:
    case 0xd008:
    case 0xd00a:
    case 0xd00c:
    case 0xd00e:
      int sprite = (address - 0xd000) >> 1;
      sprites[sprite].x &= 0x100;
      sprites[sprite].x += data;
      break;
    case 0xd001:
    case 0xd003:
    case 0xd005:
    case 0xd007:
    case 0xd009:
    case 0xd00b:
    case 0xd00d:
    case 0xd00f:
      sprites[(address - 0xd000) >> 1].y = data;
//      System.out.println("Setting sprite " + (address - 0xd000)/2 + " to " + data);
      break;
    case 0xd010:
      sprXMSB = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].x &= 0xff;
        sprites[i].x |= (data & m) != 0 ? 0x100 : 0;
      }
      break;
      // d011 -> high address of raster pos
    case 0xd011 :
      raster = (raster & 0xff) | ((data << 1) & 0x100);
      control1 = data;
      //       monitor.info("Setting blank: " +
      // 			 ((memory[ + 0xd011] & 0x08) == 0) +
      // 			 " at " + vbeam);

      if (vScroll != (data & 7)) {
        // update vScroll and badLine!
        vScroll = data & 0x7;
        boolean oldBadLine = badLine;
        badLine =
          (displayEnabled && vbeam >= 0x30 && vbeam <= 0xf7) &&
          (vbeam & 0x7) == vScroll;
        if (BAD_LINE_DEBUG && oldBadLine != badLine) {
          monitor.info("#### BadLC diff@" + vbeam + " => " +
              badLine + " vScroll: " + vScroll +
              " vmli: " + vmli + " vc: " + vc +
              " rc: " + rc + " cyc line: " +
              (cpu.cycles - lastLine) +
              " cyc IRQ: " + (cpu.cycles - lastIRQ));
        }
      }

      extended = (data & 0x40) != 0;
      blankRow = (data & 0x08) == 0;

      // 000 => normal text, 001 => multicolor text
      // 010 => extended text, 011 => illegal mode...
      // 100 => hires gfx, 101 => multi hires
      // 110, 111 => ?
      videoMode = (extended ? 0x02 : 0)
      | (multiCol ? 0x01 : 0) | (((data & 0x20) != 0) ? 0x04 : 0x00);

//    System.out.println("Extended set to: " + extended + " at " +
//    vbeam + " d011: " + Hex.hex2(data));

      if (VIC_MEM_DEBUG || BAD_LINE_DEBUG) {
        monitor.info("d011 = " + data + " at " + vbeam +
            " => YScroll = " + (data & 0x7) +
            " cyc since line: " + (cpu.cycles-lastLine) +
            " cyc since IRQ: " + (cpu.cycles-lastIRQ));
      }
      if (IRQDEBUG)
        monitor.info("Setting raster position (hi) to: " +
            (data & 0x80));

      break;

      // d012 -> raster position
    case 0xd012 :
      raster = (raster & 0x100) | data;
      if (IRQDEBUG)
        monitor.info("Setting Raster Position (low) to " + data);
      break;
    case 0xd013:
    case 0xd014:
      // Write to lightpen...
      break;
    case 0xd015:
      sprEN = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].enabled = (data & m) != 0;
      }
//      System.out.println("Setting sprite enable to " + data);

      break;
    case 0xd016:
      control2 = data;
      horizScroll = data & 0x7;
      multiCol = (data & 0x10) != 0;

//      if (hideColumn != ((data & 0x08) == 0)) {
//        System.out.println("38 chars on: " + hideColumn + " at " + vbeam + " cycle: " +
//            (cycles - lastLine) + " borderstate:" + borderState);
//      }
      
      hideColumn = (data & 0x08) == 0;

      // Set videmode...
      videoMode = (extended ? 0x02 : 0)
      | (multiCol ? 0x01 : 0) | (((control1 & 0x20) != 0)
          ? 0x04 : 0x00);

//    System.out.println("HorizScroll set to: " + horizScroll + " at "
//    + vbeam);

//    System.out.println("MultiColor set to: " + multiCol + " at " + vbeam);
      break;

    case 0xd017:
      sprYEX = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].expandY = (data & m) != 0;
      }
      break;

    case 0xd018:
      vicMem = data;
      setVideoMem();
      break;

    case 0xd019 : {
      if ((data & 0x80) != 0) data = 0xff;
      int latchval = 0xff ^ data;
      if (IRQDEBUG)
        monitor.info("Latching VIC-II: " + Integer.toString(data, 16)
            + " on " + Integer.toString(irqFlags, 16) +
            " latch: " + Integer.toString(latchval, 16));

      irqFlags &= latchval;

      // Is this "flagged" off?
      if ((irqMask & 0x0f & irqFlags) == 0) {
        clearIRQ(VIC_IRQ);
      }
    }
    break;
    case 0xd01a:
      irqMask = data;

      // Check if IRQ should trigger or clear!
      if ((irqMask & 0x0f & irqFlags) != 0) {
        irqFlags |= 0x80;
        setIRQ(VIC_IRQ);
      } else {
        clearIRQ(VIC_IRQ);
      }

      if (IRQDEBUG) {
        monitor.info("Changing IRQ mask to: " +
            Integer.toString(irqMask, 16) + " vbeam: " + vbeam);
      }
      break;

    case 0xd01b:
      sprPri = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].priority = (data & m) != 0;
      }
      break;
    case 0xd01c:
      sprMul = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].multicolor = (data & m) != 0;
      }
      break;
    case 0xd01d:
      sprXEX = data;
      for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
        sprites[i].expandX = (data & m) != 0;
      }
      break;

    case 0xd020:
      borderColor = cbmcolor[bCol = data & 15];
      break;
    case 0xd021:
      bgColor = cbmcolor[bgCol[0] = data & 15];
      for (int i = 0, n = 8; i < n; i++) {
        sprites[i].color[0] = bgColor;
      }
      break;
    case 0xd022:
    case 0xd023:
    case 0xd024:
      bgCol[address - 0xd021] = data & 15;
      break;
    case 0xd025:
      sprMC0 = data & 15;
      for (int i = 0, n = 8; i < n; i++) {
        sprites[i].color[1] = cbmcolor[sprMC0];
      }
      break;
    case 0xd026:
      sprMC1 = data & 15;
      for (int i = 0, n = 8; i < n; i++) {
        sprites[i].color[3] = cbmcolor[sprMC1];
      }
      break;
    case 0xd027:
    case 0xd028:
    case 0xd029:
    case 0xd02a:
    case 0xd02b:
    case 0xd02c:
    case 0xd02d:
    case 0xd02e:
      sprites[address - 0xd027].color[2] = cbmcolor[data & 15];
      sprites[address - 0xd027].col = data & 15;
//      System.out.println("Sprite " + (address - 0xd027) + " color set to: " + (data & 15));
      break;
      // CIA 1 & 2 - 'special' addresses
    case 0xdc00:
    case 0xdc01:
    case 0xdc02:
    case 0xdc03:
      //       monitor.println("////////////==> Set Keyboard:" +
      // 		      Integer.toString(address - ,16) + " = " + data +
      // 		      " => ~" + ((~data) & 0xff));
      cia[0].performWrite(address + IO_OFFSET, data, cpu.cycles);
      if (!isrRunning) {
        if (ciaWrites++ > 20) {
          isrRunning = true;
          ciaWrites = 0;
        } else {
          System.out.println("startup CIA write# " + ciaWrites + ": set " + address + " to " + data);
        }
      }
      break;
    case 0xdd00:
      if (DEBUG_IEC)
        monitor.info("C64: IEC Write: " + Integer.toHexString(data));

//    if (emulateDisk) {
//    c1541.handleDisk(data, cpu.cycles);
//    }

      if (VIC_MEM_DEBUG)
        System.out.println("Set dd00 to " + Integer.toHexString(data));

      cia[1].performWrite(address + IO_OFFSET, data, cpu.cycles);
      cia2PRA = data;

      data = ~cia2PRA & cia2DDRA;
      int oldLines = iecLines;
      iecLines = (data << 2) & 0x80	// DATA
      | (data << 2) & 0x40		// CLK
      | (data << 1) & 0x10;		// ATN

      if (((oldLines ^ iecLines) & 0x10) != 0) {
        c1541Chips.atnChanged((iecLines & 0x10) == 0);
      }
      c1541Chips.updateIECLines();

      if (DEBUG_IEC) printIECLines();
      setVideoMem();
      break;

    case 0xdd02:
      cia2DDRA = data;
//    System.out.println("C64: Wrote to DDRA (IEC): " +
//    Integer.toHexString(data));
      cia[1].performWrite(address + IO_OFFSET, data, cpu.cycles);
      setVideoMem();
      break;

    default:
      if (pos == 0x4) {
        sidChip.performWrite(address + IO_OFFSET, data, cycles);
      } else if (pos == 0xd) {
        cia[1].performWrite(address + IO_OFFSET, data, cycles);
      } else if (pos == 0xc) {
        cia[0].performWrite(address + IO_OFFSET, data, cycles);
      } else if (pos == 0xe) {
        tfe.performWrite(address + IO_OFFSET , data, cycles);
      }
      // handle color ram!
    }
  }

  private void printIECLines() {
    System.out.print("IEC/F: ");
    if ((iecLines & 0x10) == 0) {
      System.out.print("A1");
    } else {
      System.out.print("A0");
    }

    // The c64 has id = 1
    int sdata = ((iecLines & 0x40) == 0) ? 1 : 0;
    System.out.print(" C" + sdata);
    sdata = ((iecLines & 0x80) == 0) ? 1 : 0;
    System.out.print(" D" + sdata);

    // The 1541 has id = 2
    sdata = ((c1541Chips.iecLines & 0x40) == 0) ? 1 : 0;
    System.out.print(" c" + sdata);
    sdata = ((c1541Chips.iecLines & 0x80) == 0) ? 1 : 0;
    System.out.print(" d" + sdata);

    System.out.println(" => C" +
        ((iecLines & c1541Chips.iecLines & 0x80) == 0 ? 1 : 0)
        + " D" +
        ((iecLines & c1541Chips.iecLines & 0x40) == 0 ? 1 : 0));
  }

  private void setVideoMem() {
    if (VIC_MEM_DEBUG) {
      monitor.info("setVideoMem() cycles since line: " +
          (cpu.cycles - lastLine) +
          " cycles since IRQ: " + (cpu.cycles-lastIRQ) +
          " at " + vbeam);
    }
    // Set-up vars for screen rendering
    vicBank = (~(~cia2DDRA | cia2PRA) & 3) << 14;
    charSet = vicBank | (vicMem & 0x0e) << 10;
    videoMatrix = vicBank | (vicMem & 0xf0) << 6;
    vicBase = vicBank | (vicMem & 0x08) << 10;
    spr0BlockSel = 0x03f8 + videoMatrix;

//    monitor.info("--------------------");
//    monitor.info("0xdd00: 0x"+Integer.toHexString(memory[IO_OFFSET + 0xdd00]));
//    monitor.info("0xd018: 0x"+Integer.toHexString(memory[IO_OFFSET + 0xd018]));
//    monitor.info("vicBank: 0x"+Integer.toHexString(vicBank));
//    monitor.info("videoMatrix: 0x"+Integer.toHexString(videoMatrix-vicBank));
//    monitor.info("charSet: 0x"+Integer.toHexString(charSet-vicBank));

    //check if vic not looking at char rom 1, 2, 4, 8
    // This is not correct! Char Rom is not everywhere!!!! - find out!
    if ( (vicMem & 0x0c) != 4 || (vicBank & 0x4000) == 0x4000) {
      charMemoryIndex = charSet;
    } else {
      charMemoryIndex = (((vicMem & 0x02) == 0) ? 0 : 0x0800) +
        CPU.CHAR_ROM2;
    }
  }

  private void initUpdate() {
    vc = 0;
    vcBase = 0;
    vmli = 0;
    //     rc = 0;
    updating = true;

//  // First rendered line will start at cpu.cycles - no at next_scan!
//  firstLine = nextScanLine;

    for (int i = 0; i < 8; i++) {
      sprites[i].nextByte = 0;
      sprites[i].painting = false;
      sprites[i].spriteReg = 0;
    }

    if (colors == null) {
      colors = new Color[16];
      for (int i = 0; i < 16; i++) {
        colors[i] = new Color(cbmcolor[i]);
      }
    }
    canvas.setBackground(colors[memory[IO_OFFSET + 0xd020] & 15]);
  }

  // -------------------------------------------------------------------
  // Screen rendering!
  // -------------------------------------------------------------------
  // keep track of if the border is to be painted...
  private int borderState = 0;
  private boolean notVisible = false;
  private int xPos = 0;
  private long lastCycle = 0;
  
  public final void clock(long cycles) {
    if (DEBUG_CYCLES || true) {
      if (lastCycle + 1 < cycles) {
        System.out.println("More than one cycle passed: " +
            (cycles - lastCycle) + " at " + cycles + " PC: "
            + Integer.toHexString(cpu.pc));
      }

      if (lastCycle == cycles) {
        System.out.println("No diff since last update!!!: " +
            (cycles - lastCycle) + " at " + cycles + " PC: "
            + Integer.toHexString(cpu.pc));
      }
      lastCycle = cycles;
    }

    // Delta is cycles into the current raster line!
    int vicCycle = (int) (cycles - lastLine);

    if (notVisible) {
      if (vicCycle < 62)
        return;
    }
    
    // Each cycle is 8 pixels (a byte)
    // Cycle 16 (if first cycle is 0) is the first visible gfx cycle
    // Cycle 12 is first visible border cycle => 12 x 8 = 96
    // Last to "draw" is cycle 59 => 59 * 8 = 472 => 376 visible pixels?

    if (badLine) {
      gfxVisible = true;
    }
    
    switch (vicCycle) {
    case 0:
      // Increase the vbeam - rendering is started
      vbeam = (vbeam + 1) % 312;
      if (vbeam == 0) frame++;
      vPos = vbeam - (FIRST_VISIBLE_VBEAM + 1);

      if (vbeam == FIRST_VISIBLE_VBEAM) {
        colIndex++;
        if (colIndex >= LABEL_COUNT) colIndex = 0;
        // Display enabled?
        initUpdate();
      }

      // Check for interrupts, etc...
      // Sprite collission interrupts - why only once a line?
      if (((irqMask & 2) != 0) && (sprBgCol != 0) &&
          (irqFlags & 2) == 0) {
        if (SPRITEDEBUG)
          monitor.info("*** Sprite collission IRQ (d01f): " +
              sprBgCol + " at " + vbeam);
        irqFlags |= 82;
        setIRQ(VIC_IRQ);
      }
      if (((irqMask & 4) != 0) && (sprCol != 0) &&
          (irqFlags & 4) == 0) {
        if (SPRITEDEBUG)
          monitor.info("*** Sprite collission IRQ (d01e): " +
              sprCol + " at " + vbeam);
        irqFlags |= 84;
        setIRQ(VIC_IRQ);
      }


      int irqComp = raster;
      // Not nice... FIX THIS!!!
      if (irqComp > 312) irqComp &= 0xff;

      if ((irqFlags & 1) == 0 && (irqComp == vbeam)) {
        irqFlags |= 0x1;

        if ((irqMask & 1) != 0) {
          irqFlags |= 0x80;
          irqTriggered = true;
          setIRQ(VIC_IRQ);
          lastIRQ = cpu.cycles;
          if (IRQDEBUG)
            monitor.info("Generating IRQ at " + vbeam + " req:" + raster
                + " IRQs:" + cpu.interruptInExec
                + " flags: " + irqFlags + " delta: " +
                (cpu.cycles - lastLine));
        }
      } else {
        irqTriggered = false;
      }
      notVisible = false;
      if (vPos < 0 || vPos >= 284) {
        cpu.baLowUntil = 0;
        notVisible = true;
        if (STATE_DEBUG)
          monitor.info("FINISH next at " + vbeam);
        // Jump directly to VS_FINISH and wait for end of line...
        break;
      }

      // Check if display should be enabled...
      if (vbeam == 0x30) {
        displayEnabled = (control1 & 0x10) != 0;
        if (displayEnabled) {
          borderState &= ~0x04;
        } else {
          borderState |= 0x04;
        }
      }

      badLine =
        (displayEnabled && vbeam >= 0x30 && vbeam <= 0xf7) &&
        (vbeam & 0x7) == vScroll;

      // Clear the collission masks each line... - not needed???
      for (int i = 0, n = SC_WIDTH; i < n; i++) {
        collissionMask[i] = 0;
      }
      break;
    case 1: // Sprite data - sprite 3
      if (sprites[3].dma) {
        sprites[3].readSpriteData(); // reads all 3 bytes here (one should be prev).
      }
      if (sprites[5].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP5;
      }
      break;
    case 2:
      // here some of the bytes for sprite 4 should be read...
      break;
    case 3:
      if (sprites[4].dma) {
        sprites[4].readSpriteData();
      }
      if (sprites[6].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP6;
      }
      break;
    case 4:
      // here some of the bytes for sprite 5 should be read...
      break;
    case 5:
      if (sprites[5].dma) {
        sprites[5].readSpriteData();
      }
      if (sprites[7].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP7;
      }
      break;
    case 6:
      // here some of the bytes for sprite 6 should be read..
      break;
    case 7:
      if (sprites[6].dma) {
        sprites[6].readSpriteData();
      }
      break;
    case 8:
      // here some of the bytes for sprite 7 should be read...
      break;
    case 9:
      if (sprites[7].dma) {
        sprites[7].readSpriteData();
      }

      // Border management! (at another cycle maybe?)
      if (blankRow) {
        if (vbeam == 247) {
          borderState |= 1;
        }
      } else {
        if (vbeam == 251) {
          borderState |= 1;
        }
        if (vbeam == 51) {
          borderState &= 0xfe;

          // Reset sprite data to avoid garbage since they are not painted...
          for (int i = 0, n = 7; i < n; i++) {
            if (!sprites[i].painting) {
              sprites[i].lineFinished = true;
            }
          }
        }
      }
      // No border after vbeam 55 (ever?)
      if (vbeam == 55) {
        borderState &= 0xfe;

        // Reset sprite data to avoid garbage since they are not painted...
        for (int i = 0, n = 7; i < n; i++) {
          if (!sprites[i].painting)
            sprites[i].lineFinished = true;
        }
      }
      break;
    case 10:
      break;
    case 11: // Set badline fetching...
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
      }
      break;
    case 12: // First visible cycle (on screen)
      // calculate mpos before starting the rendering!
      mpos = vPos * SC_WIDTH;
      drawBackground();

      xPos = 16;
      mpos += 8;

      break;
    case 13:
      drawBackground();
      drawSprites();
      mpos += 8;

      // Set vc, reset vmli...
      vc = vcBase;
      vmli = 0;
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        if (BAD_LINE_DEBUG) System.out.println("#### RC = 0 (" + rc + ") at "
            + vbeam + " vc: " + vc);
        rc = 0;
      }
      break;
    case 14:      
      drawBackground();      
      drawSprites();     
      mpos += 8;
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
      }
      break;
    case 15:

      drawBackground();
      drawSprites();
      mpos += 8;

      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
      }

      // Turn off sprite DMA if finished reading!
      for (int i = 0, n = 8; i < n; i++) {
        if (sprites[i].nextByte == 63)
          sprites[i].dma = false;
      }
      
      break;
    case 16:
      if (!hideColumn) {
        borderState &= 0xfd;
      }
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        // Fetch first char into cache! (for the below draw...)
        vicCharCache[vmli] = memory[videoMatrix + (vcBase & 0x3ff)];
        vicColCache[vmli] = memory[IO_OFFSET + 0xd800 + (vcBase & 0x3ff)];
      }

      // Draw one character here!
      drawGraphics(mpos + horizScroll);
      drawSprites();
      if (borderState != 0)
        drawBackground();
      mpos += 8;
      
//      System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);
      break;
    case 17:
      if (hideColumn) {
        borderState &= 0xfd;
      }

      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        // Fetch a some chars into cache! (for the below draw...)
        vicCharCache[vmli] = memory[videoMatrix + ((vcBase + vmli) & 0x3ff)];
        vicColCache[vmli] = memory[IO_OFFSET + 0xd800 + ((vcBase + vmli) & 0x3ff)];
      }
      // draw the graphics. (should probably handle sprites also??)
      drawGraphics(mpos + horizScroll);
      drawSprites();
      mpos += 8;
      break;
      // Cycle 18 - 53
    default:
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        // Fetch a some chars into cache! (for the below draw...)
        vicCharCache[vmli] = memory[videoMatrix + ((vcBase + vmli) & 0x3ff)];
        vicColCache[vmli] = memory[IO_OFFSET + 0xd800 + ((vcBase + vmli) & 0x3ff)];
      }
      // draw the graphics. (should probably handle sprites also??)
      drawGraphics(mpos + horizScroll);
      drawSprites();

      mpos += 8;
//      System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);
      break;
    case 54:
      // Then Check if it is time to start up the sprites!
      // Does not matter in which order this is done ?=
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        // Fetch a some chars into cache! (for the below draw...)
        vicCharCache[vmli] = memory[videoMatrix + ((vcBase + vmli) & 0x3ff)];
        vicColCache[vmli] = memory[IO_OFFSET + 0xd800 + ((vcBase + vmli) & 0x3ff)];
      }
      int mult = 1;
      int ypos = vPos + SC_SPYOFFS;

      for (int i = 0, n = 8; i < n; i++) {
        Sprite sprite = sprites[i];
        if (sprite.enabled) {
          // If it is time to start drawing this sprite!
          if (sprite.y == (ypos & 0xff) && (ypos < 270)) {
            sprite.nextByte = 0;
            sprite.dma = true;
            sprite.expFlipFlop = true;
            if (SPRITEDEBUG)
              System.out.println("Starting painting sprite " + i + " on "
                  + vbeam + " first visible at " + (ypos + 1));
          }
        }
        mult = mult << 1;
      }
      if (sprites[0].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP0;
      }
      
      drawGraphics(mpos + horizScroll);
      drawSprites();

      mpos += 8;
//      System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);
      
      break;
    case 55:
      if (hideColumn) {
        borderState |= 2;
      }
      if (badLine) {
        cpu.baLowUntil = lastLine + VICConstants.BA_BADLINE;
        // Fetch a some chars into cache! (for the below draw...)
        vicCharCache[vmli] = memory[videoMatrix + ((vcBase + vmli) & 0x3ff)];
        vicColCache[vmli] = memory[IO_OFFSET + 0xd800 + ((vcBase + vmli) & 0x3ff)];
      }
      drawGraphics(mpos + horizScroll);
      drawSprites();
      if (borderState != 0)
          drawBackground();
      mpos += 8;
//      System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);

      break;
    case 56:
      if (!hideColumn) {
        borderState |= 2;
      }

      drawBackground();
      drawSprites();
      mpos += 8;

      
      // If time to turn of sprite display...
      for (int i = 0, n = 8; i < n; i++) {
        Sprite sprite = sprites[i];
        if (!sprite.dma) {
          sprite.painting = false;
          if (SPRITEDEBUG)
            System.out.println("Stopped painting sprite " +
                i + " at (after): " + vbeam);
        }
      }

      // Here we should check if sprite dma should start...
      // - probably need to add a dma variable to sprites, and not
      // only use the painting variable for better emulation
      // Bus not available if sp0 or sp1 is painting
      if (sprites[1].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP1;
      }
      break;
    case 57:
      // Paint border, check sprite for display and read sprite 0 data.
      for (int i = 0, n = 8; i < n; i++) {
        Sprite sprite = sprites[i];
        if (sprite.dma)
          sprite.painting = true;
      }

      drawBackground();
      drawSprites();
      mpos += 8;

      
      if (rc == 7) {
        vcBase = vc;
        gfxVisible = false;
        if (BAD_LINE_DEBUG) {
          monitor.info("#### RC7 ==> vc = " + vc + " at " + vbeam +
              " vicCycle = " + vicCycle);
          if (vc == 1000) {
            monitor.info("--------------- last line ----------------");
          }
        }
      }

      if (badLine || gfxVisible) {
        rc = (rc + 1) & 7;
        gfxVisible = true;
      }

      if (sprites[0].painting) {
        sprites[0].readSpriteData();
      }

      if (sprites[2].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP2;
      }

      break;
    case 58:
      drawBackground();
      drawSprites();
      mpos += 8;

      break;
    case 59:
      drawBackground();
      drawSprites();
      mpos += 8;

      if (sprites[1].painting) {
        sprites[1].readSpriteData();
      }
      break;
    case 60:
      drawSprites();
      break;
    case 61:
      if (sprites[2].painting) {
        sprites[2].readSpriteData();
      }
      if (sprites[3].dma) {
        cpu.baLowUntil = lastLine + VICConstants.BA_SP3;
      }
      break;
    case 62:
      // Should this be made??? or should sprite 0 be repaintable 
      // same line?
      // Reset sprites so that they can be repainted again...
      for (int i = 0; i < sprites.length; i++) {
        sprites[i].reset();
      }
      lastLine += VICConstants.SCAN_RATE;
      // Update screen
      if (updating) {
        if (vPos == 285) {
          mis.newPixels();
          canvas.repaint();
          actualScanTime = (actualScanTime * 9 + (int)
              ((audioDriver.getMicros() - lastScan))) / 10;
          lastScan = audioDriver.getMicros();
          updating = false;
        }
      }
      notVisible = false;
      break;
    }
  }

  // Used to draw background where either border or background should be
  // painted...
  private void drawBackground() {
    int bpos = mpos;
    int currentBg = borderState > 0 ? borderColor : bgColor; 
    for (int i = 0; i < 8; i++) {
      mem[bpos++] = currentBg;
    }
  }

  /**
   * <code>drawGraphics</code> - draw the VIC graphics (text/bitmap)
   * Note that sprites are not drawn here... (yet?)
   *
   *
   * @param mpos an <code>int</code> value representing position to
   * draw the graphics from (already fixed with hscroll)
   */
  private final void drawGraphics(int mpos) {
    if (!gfxVisible || paintBorder || (borderState & 1) == 1) {
      // We know that display is not enabled, and that mpos is already
      // at a correct place, except horizScroll...
      mpos -= horizScroll;
      int color = (paintBorder || (borderState > 0)) ? borderColor : bgColor;
      for (int i = mpos, n = mpos + 8; i < n; i++) {
        mem[i] = color;
      }
      // trick to use vmli as a if var even when no gfx.
      vmli++;
      return;
    }

    int collX = (vmli << 3) + horizScroll + SC_XOFFS;
    
    // Paint background if first col (should maybe be made later also...)
    if (vmli == 0) {
      for (int i = mpos - horizScroll, n = i + 8; i < n; i++) {
        mem[i] = bgColor;
      }
    }
    
    int position = 0, data = 0, penColor = 0, bgcol = bgColor;

    if ((control1 & 0x20) == 0) {
      int tmp;
      int pcol;

      // This should be in a cache some where...
      if (multiCol) {
        multiColor[0] = bgColor;
        multiColor[1] = cbmcolor[bgCol[1]];
        multiColor[2] = cbmcolor[bgCol[2]];
      }


      penColor = cbmcolor[pcol = vicColCache[vmli] & 15];
      if (extended) {
        position = charMemoryIndex +
        (((data = vicCharCache[vmli]) & 0x3f) << 3);
        bgcol = cbmcolor[bgCol[(data >> 6)]];
      } else {
        position = charMemoryIndex + (vicCharCache[vmli] << 3);
      }

      data = memory[position + rc];

      if (multiCol && pcol > 7) {
        multiColor[3] = cbmcolor[pcol & 7];
        for (int pix = 0; pix < 8; pix += 2) {
          tmp = (data >> pix) & 3;
          mem[mpos + 6 - pix] = mem[mpos + 7 - pix] = multiColor[tmp];
          // both 00 and 01 => no collission!?
          // but what about priority?
          if (tmp > 0x01) {
            tmp = 256;
          } else {
            tmp = 0;
          }
          collissionMask[collX + 7 - pix] =
            collissionMask[collX + 6 - pix] = tmp;
        }
      } else {
        for (int pix = 0; pix < 8; pix++) {
          if ((data & (1 << pix)) > 0) {
            mem[mpos + 7 - pix] = penColor;
            collissionMask[collX + 7 - pix] = 256;
          } else {
            mem[mpos + 7 - pix] = bgcol;
            collissionMask[collX + 7 - pix] = 0;
          }
        }
      }

      if (multiCol && extended) {
        // Illegal mode => all black!
        for (int pix = 0; pix < 8; pix++) {
          mem[mpos + 7 - pix] = 0xff000000;
        }
      }

      if (BAD_LINE_DEBUG && badLine) {
        for (int pix = 0; pix < 8; pix += 4) {
          mem[mpos + 7 - pix] = (mem[mpos + 7 - pix] & 0xff7f7f7f) | 0x0fff;
        }
      }
    } else {
      // -------------------------------------------------------------------
      // Bitmap mode!
      // -------------------------------------------------------------------
      position = vicBase + (vc & 0x3ff) * 8 + rc;
      if (multiCol) {
        multiColor[0] = bgColor;
      }
      int vmliData = vicCharCache[vmli];
      penColor =
        cbmcolor[(vmliData & 0xf0) >> 4];
      bgcol = cbmcolor[vmliData & 0x0f];

      data = memory[position];

      if (multiCol) {
        multiColor[1] =
          cbmcolor[(vmliData >> 4) & 0x0f];
        multiColor[2] =
          cbmcolor[vmliData & 0x0f];
        multiColor[3] = cbmcolor[vicColCache[vmli] & 0x0f];

        // Multicolor
        int tmp;
        for (int pix = 0; pix < 8; pix += 2) {
          mem[mpos + 6 - pix] = mem[mpos + 7 - pix] =
            multiColor[tmp = (data >> pix) & 3];
          if (tmp > 0x01) {
            tmp = 256;
          } else {
            tmp = 0;
          }
          collissionMask[collX + 7 - pix] =
            collissionMask[collX + 6 - pix] = tmp;
        }
      } else {
        // Non multicolor
        for (int pix = 0; pix < 8; pix++) {
          if ((data & (1 << pix)) > 0) {
            mem[7 - pix + mpos] = penColor;
            collissionMask[collX + 7 - pix] = 256;
          } else {
            mem[7 - pix + mpos] = bgcol;
            collissionMask[collX + 7 - pix] = 0;
          }
        }
      }

      if (extended) {
        // Illegal mode => all black!
        for (int pix = 0; pix < 8; pix++) {
          mem[mpos + 7 - pix] = 0xff000000;
        }
      }

      if (BAD_LINE_DEBUG && badLine) {
        for (int pix = 0; pix < 8; pix += 4) {
          mem[mpos + 7 - pix] = (mem[mpos + 7 - pix] & 0xff3f3f3f) | 0x0fff;
        }
      }
    }
    vc++;
    vmli++;
  }

  // -------------------------------------------------------------------
  // Sprites...
  // -------------------------------------------------------------------
  private final void drawSprites() {
    int smult = 0x100;
    int lastX = xPos - 8;
    
    for (int i = 7; i >= 0; i--) {
      Sprite sprite = sprites[i];
      // Done before the continue...
      smult = smult >> 1;
      if (sprite.lineFinished || !sprite.painting) {
        continue;
      }
      int x = sprite.x + SC_SPXOFFS; // 0 in sprite x => xPos = 8
      int mpos = vPos * SC_WIDTH;

      if (x < xPos) {
        // Ok, we should write some data...
        int minX = lastX > x ? lastX : x;
//        if (i == 0) monitor.info("Writing sprite " + i + " first pixel at " +
//            minX + " vPos = " + vPos);

        for (int j = minX, m = xPos; j < m; j++) {
          int c = sprite.getPixel();
          if (c != 0 && borderState == 0) {
            int tmp = (collissionMask[j] |= smult);
            if (!sprite.priority || (tmp & 0x100) == 0) {
              mem[mpos + j] = sprite.color[c];
            }

            if (tmp != smult) {
              // If collission with bg then notice!
              if ((tmp & 0x100) != 0) {
                sprBgCol |= smult;
//              monitor.info("***** Sprite x Bkg collission!");
              }
              // If collission with sprite, all colls must
              // be registered!
              if ((tmp & 0xff) != smult) {
                sprCol |= tmp & 0xff;
//              monitor.info("***** Sprite x Sprite collission: d01e = " +
//              sprCol + " sprite: " + i + " => " +
//              smult + " at " + j + "," + vbeam);
              }
            }
          }

          if (SPRITEDEBUG) {
            if ((sprite.nextByte == 3) && ((j & 4) == 0)) {
              mem[mpos + j] = 0xff00ff00;
            }
            if ((sprite.nextByte == 63) && ((j & 4) == 0)) {
              mem[mpos + j] = 0xffff0000;
            }

            if (j == x) {
              mem[mpos + j] = 0xff000000 + sprite.pointer;
            }
          }
        }
      }
    }
    xPos += 8;
  }

  public void stop() {
    motorSound(false);
    sidChip.stop();
    audioDriver.shutdown();
  }

  public void reset() {
    // Clear a lot of stuff...???
    initUpdate();
    sidChip.reset();
    lastLine = cpu.cycles;
    nextIOUpdate = cpu.cycles + 47;

    for (int i = 0; i < mem.length; i++) mem[i] = 0;
    reset = 100;

    sprCol = 0;
    sprBgCol = 0;

    cia[0].reset();
    cia[1].reset();
//  c1541.reset();
    keyboard.reset();
    ciaWrites = 0;
    isrRunning = false;

    motorSound(false);
    resetInterrupts();
  }

  public static final int IMG_TOTWIDTH = SC_WIDTH;
  public static final int IMG_TOTHEIGHT = SC_HEIGHT;

  public Image crtImage;

  // Will be called from the c64canvas class
  long repaint = 0;
  public void paint(Graphics g) {
    if (g == null)
      return;

    if (image == null) {
      image = canvas.createImage(IMG_TOTWIDTH, IMG_TOTHEIGHT);
      g2 = image.getGraphics();
      g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
    }

    if (crtImage == null) {
      crtImage = new BufferedImage(displayWidth, displayHeight,
          BufferedImage.TYPE_INT_ARGB);
      Graphics gcrt = crtImage.getGraphics();
      gcrt.setColor(TRANSPARENT_BLACK);
      for (int i = 0, n = displayHeight; i < n; i += 2) {
        gcrt.drawLine(0, i, displayWidth, i);
      }
    }

    // Why is there transparency?
    g2.drawImage(screen, 0, 0,  null);

    if (reset > 0) {
      g2.setColor(darks[colIndex]);
      int xp = 44;
      if (reset < 44) {
        xp = reset;
      }
      g2.drawString("JaC64 " + version + " - Java C64 - www.jac64.com",
          xp + 1, 9);
      g2.setColor(lites[colIndex]);
      g2.drawString("JaC64 " + version + " - Java C64 - www.jac64.com",
          xp, 8);
      reset--;
    } else {
      String msg = "JaC64 ";
      if ((message != null) && (message != "")) {
        msg += message;
      } else {
        colIndex = 0;
      }
      msg += tmsg;

      g2.setColor(darks[colIndex]);
      g2.drawString(msg, 1, 9);
      g2.setColor(lites[colIndex]);
      g2.drawString(msg, 0, 8);

      if (ledOn) {
        g2.setColor(LED_ON);
      } else {
        g2.setColor(LED_OFF);
      }
      g2.fillRect(372, 3, 7, 1);
      g2.setColor(LED_BORDER);
      g2.drawRect(371, 2, 8, 2);
    }

    g.fillRect(0, 0, offsetX, displayHeight + offsetY * 2);
    g.fillRect(offsetX + displayWidth, 0,
        offsetX, displayHeight + offsetY * 2);
    g.fillRect(0, 0, displayWidth + offsetX * 2, offsetY);
    g.fillRect(0, displayHeight + offsetY,
        displayWidth + offsetX * 2, offsetY);
    Graphics2D g2d = (Graphics2D) g;
//  g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.drawImage(image, offsetX, offsetY, displayWidth, displayHeight, null);
//    g.drawImage(crtImage, offsetX, offsetY, displayWidth, displayHeight, null);

//     monitor.info("Repaint: " + (System.currentTimeMillis() - repaint) + " " + memory[IO_OFFSET + 55296 + 6 * 40]);
//     repaint = System.currentTimeMillis();
  }


  // -------------------------------------------------------------------
  // Internal sprite class to handle all data for sprites
  // Just a collection of data registers... so far...
  // -------------------------------------------------------------------
  private class Sprite {

    boolean painting = false; // If sprite is "on" or not (visible)
    boolean dma = false;  // Sprite DMA on/off

    int nextByte;
    int pointer;
    int x;
    int y;

    int spriteNo;
    // Contains the sprite data to be outshifted
    int spriteReg;

    boolean enabled;
    boolean expFlipFlop;
    boolean multicolor = false;
    boolean expandX = false;
    boolean expandY = false;
    boolean priority = false;
    boolean lineFinished = false;

    int pixelsLeft = 0;
    int currentPixel = 0;

    // The sprites color value (col)
    int col;
    // Sprites real colors
    int[] color = new int[4];

    int getPixel() {
      if (lineFinished) return 0;
      pixelsLeft--;
      if (pixelsLeft > 0) return currentPixel;
      // Indicate finished!
      if (pixelsLeft <= 0 && spriteReg == 0) {
        currentPixel = 0;
        lineFinished = true;
        return 0;
      }

      if (multicolor) {
        // The 23rd and 22nd pixel => data!
        currentPixel = (spriteReg & 0xc00000) >> 22;
        spriteReg = (spriteReg << 2) & 0xffffff;
        pixelsLeft = 2;
      } else {
        // Only the 23rd bit is pixel data!
        currentPixel = (spriteReg & 0x800000) >> 22;
        spriteReg = (spriteReg << 1) & 0xffffff;
        pixelsLeft = 1;
      }
      // Double the number of pixels if expanded!
      if (expandX) {
        pixelsLeft = pixelsLeft << 1;
      }

      return currentPixel;
    }

    void reset() {
      lineFinished = false;
    }
    
    void readSpriteData() {
      // Read pointer + the three sprite data pointers...
      pointer = vicBank + memory[spr0BlockSel + spriteNo] * 0x40;
      spriteReg = ((memory[pointer + nextByte++] & 0xff) << 16) |
      ((memory[pointer + nextByte++] & 0xff)  << 8) |
      memory[pointer + nextByte++];

      // For debugging... seems to be err on other place than the
      // Memoryfetch - since this is also looking very odd...???
      // spriteReg = 0xf0f0f0;

      if (!expandY) expFlipFlop = false;

      if (expFlipFlop) {
        nextByte = nextByte - 3;
      }

      expFlipFlop = !expFlipFlop;
      pixelsLeft = 0;

    }
  }

  // -------------------------------------------------------------------
  // Observer (1541) - should probably be in C64 screen later...
  // -------------------------------------------------------------------

  public void updateDisk(Object obs, Object msg) {
    if (msg == C1541Chips.HEAD_MOVED) {
      if (lastTrack != c1541Chips.currentTrack) {
        lastTrack = c1541Chips.currentTrack;
        trackSound();
      } else {
        // Head could not move any more... maybe other sound here?
        trackSound();
      }
    }

    // add head beyond here...
    lastSector = c1541Chips.currentSector;

    if (motorOn != c1541Chips.motorOn) {
      motorSound(c1541Chips.motorOn);
    }

    tmsg = " track: " + lastTrack + " / " + lastSector;

    ledOn = c1541Chips.ledOn;
    motorOn = c1541Chips.motorOn;
  }

  private void trackSound() {
    if (trackSound != null) {
      trackSound.play();
    }
  }

  public void motorSound(boolean on) {
    if (motorSound != null) {
      if (on)
        motorSound.loop();
      else
        motorSound.stop();
    }
  }

  public void setSounds(AudioClip track, AudioClip motor) {
    trackSound = track;
    motorSound = motor;
  }

  // -------------------------------------------------------------------
  // MouseListener
  // -------------------------------------------------------------------
  public void mouseDragged(MouseEvent e) {
    potx = e.getX() & 0xff;
    poty = 0xff - (e.getY() & 0xff);
  }

  public void mouseMoved(MouseEvent e) {
    potx = e.getX() & 0xff;
    poty = 0xff - (e.getY() & 0xff);
  }

  public void mouseClicked(MouseEvent e) {
  }
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      button1 = true;
    } else {
      button2 = true;
    }
    // Emulate stick button
    keyboard.setButtonval(0xff - (button1 | button2 ? 0x10 : 0));
    //keyboard.setButtonval(0xff - (button1 ? 0x4 : 0) - (button2 ? 0x8 : 0));
  }

  public void mouseReleased(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      button1 = false;
    } else {
      button2 = false;
    }
    // Emulate stick button
    keyboard.setButtonval(0xff - (button1 | button2 ? 0x10 : 0));
    //    keyboard.setButtonval(0xff - (button1 ? 0x4 : 0) - (button2 ? 0x8 : 0));
  }
}