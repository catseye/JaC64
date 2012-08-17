/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

/**
 * @(#)C64Test.java	Created date: 99-7-06
 *
 */
import java.net.URL;
import java.applet.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import com.dreamfabric.jac64.*;
import com.dreamfabric.c64utils.*;


/**
 * A test program that starts the CPU and loads files into its RAM
 *
 * @author  Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 1.15 $, $Date: 2006/05/01 14:57:57 $
 */
public class C64Test implements ActionListener, Runnable {
  private C64Reader reader;
  private C64Screen scr;
  private boolean fullscreen = false;

  private static final boolean FULLSCREEN = true; // false;

  // Set the antialiasing to get the right look!
  public final static RenderingHints AALIAS =
    new RenderingHints(RenderingHints.KEY_ANTIALIASING,
		       RenderingHints.VALUE_ANTIALIAS_ON);

  private JFrame window;
  private JPanel infoWindow;
  private FileDialog fileDialog;
  private JToggleButton debug;
  private JToggleButton joy;
  private JToggleButton sound;
  private JToggleButton speed;
  private JButton pause;
  private JButton load;
  private JButton normal;
  private JButton full;
  private JButton save;
  private JButton vicdump;
  private JButton color;
  private JButton saveDisk;
  private JButton openAssembler;

  private ArrayList dirNames = new ArrayList();
  private JList list;
  private JTextField txt;
  private JTextField prefix;
  private JTextField file;
  private JTextField fileName; // For save...

  private JFrame asmFrame;
  private JTextArea asmText;
  private JTextField asmAdr;
  private JButton asm;
  private Assembler assembler;

  private JLabel stext;
  private int sprnum = 0;

  private Image sprite = null;
  private MemoryImageSource mis = null;
  private int[] sprMem = new int[24 * 21]; // Sprite size
  private JPanel sprPan;
  private CPU cpu;
  private int[] memory;
  private int cset = 0;

  private JWindow C64Scr;
  private JFrame C64Win;

  private Color lblue;
  private Debugger monitor;

  public C64Test(String file) {

//     System.out.println("Locale: " + l);
//     Locale.setDefault(Locale.US);
//    l = Locale.getDefault();

    // Set the buffer for SID chip to lower than 1 second...
    SIDMixer.DL_BUFFER_SIZE = 16384;

    monitor = new Debugger();
    //    monitor.setEnabled(true);
    cpu = new CPU(monitor, "", new SELoader());

    monitor.init(cpu);

    memory = cpu.getMemory();
    scr = new C64Screen(monitor, true);

    cpu.init(scr);

    // Reader available after init!
    scr.init(cpu);
    scr.registerHotKey(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK |
		       KeyEvent.ALT_DOWN_MASK
		       , "reset()", cpu);

    scr.registerHotKey(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK
		       , "toggleFullScreen()", this);

    reader = new C64Reader(); // scr.getDiskDrive().getReader();
    reader.setCPU(cpu);

    cpu.getDrive().setReader(reader);

    if (FULLSCREEN) {
      JFrame jf = new JFrame("nada");
      jf.setVisible(true);
      C64Scr = new JWindow(jf); // new JFrame("JaC64 - A Java C64 Emulator");
    } else {
      C64Win = new JFrame("JaC64 - A Java C64 Emulator");
    }


//     JMenuBar jbar = new JMenuBar();
//     C64Scr.setJMenuBar(jbar);
//     JMenu filem;
//     jbar.add(filem = new JMenu("File"));
//     filem.add(new JMenuItem("Open file"));
    if (FULLSCREEN) {
      C64Scr.setBackground(Color.black);
      C64Scr.setForeground(lblue = new Color(VICConstants.COLOR_SETS[0][14]));
      C64Scr.setLayout(new BorderLayout());
      C64Scr.add(scr.getScreen(), BorderLayout.CENTER);
      C64Scr.setSize(386 * 2, 284 * 2);
    } else {
      C64Win.setBackground(Color.black);
      C64Win.setForeground(lblue = new Color(VICConstants.COLOR_SETS[0][14]));
      C64Win.setLayout(new BorderLayout());
      C64Win.add(scr.getScreen(), BorderLayout.CENTER);
      C64Win.setFocusable(true);
    }

    if (!FULLSCREEN) {
      C64Win.pack(); //	C64Scr.setSize(380,300);
      C64Win.setSize(386 * 2, 284 * 2);
      C64Win.setResizable(true);
      C64Win.setVisible(true);
    } else {
      C64Scr.setVisible(true);
    }



    for (int i = 0; i < 24 * 21; i++)
      sprMem[i] = 0xfff04040 + i;

    mis = new MemoryImageSource(24, 21, sprMem, 0, 24);

    mis.setAnimated(true);
    mis.setFullBufferUpdates(true);
    if (FULLSCREEN) {
      sprite = C64Scr.createImage(mis);
    } else {
      sprite = C64Win.createImage(mis);
    }

    AudioClip trackSound = null;
    AudioClip motorSound = null;
    URL url = getClass().getResource("sounds/track.wav");
    System.out.println("Audio URL:" + url);
    if (url != null) trackSound = Applet.newAudioClip(url);
    url = getClass().getResource("sounds/motor.wav");
    if (url != null) motorSound = Applet.newAudioClip(url);
    else {
      System.out.println("Could not load file... motor.wav");
    }
    scr.setSounds(trackSound, motorSound);
    if (motorSound != null) {
      motorSound.play();
    }
  }

  private JLabel[] sprites = new JLabel[8];

  public void init() {

    window = new JFrame();

    JButton butt;
    window.setSize(320, 650);
    window.setLocation(new Point(400, 100));
    window.getContentPane().setLayout(new BorderLayout(5,5));
    JPanel pan = new JPanel();
    pan.setLayout(new GridLayout(11,2,1,1));
    pan.add(load = new JButton("Load Program"));
    load.addActionListener(this);
    pan.add(save = new JButton("Save Program"));
    save.addActionListener(this);
    pan.add(full = new JButton("Full Screen"));
    full.addActionListener(this);
    pan.add(normal = new JButton("Normal Screen"));
    normal.addActionListener(this);
    pan.add(butt = new JButton("Reset"));
    butt.addActionListener(this);
    pan.add(butt = new JButton("Select Disk"));
    butt.addActionListener(this);

    pan.add(butt = new JButton("Dump Memory"));
    butt.addActionListener(this);
    pan.add(txt = new JTextField("0000"));
    JPanel span = new JPanel(new BorderLayout(5,5));

    span.add(stext = new JLabel("Sprite: 0"), BorderLayout.NORTH);
    span.add(sprPan = new JPanel() {
	public void paint(Graphics g) {
	  g.drawImage(sprite, 0, 0, null);
	}
      }, BorderLayout.CENTER);
    sprPan.setOpaque(true);
    span.add(butt = new JButton("UP"), BorderLayout.EAST);
    butt.addActionListener(this);

    sprPan.setPreferredSize(new Dimension(21, 24));
    pan.add(span);

    pan.add(debug = new JToggleButton("debug:false"));
    debug.addActionListener(this);
    pan.add(joy = new JToggleButton("joy:0"));
    joy.addActionListener(this);

    pan.add(vicdump = new JButton("Vic/SID Dump"));
    vicdump.addActionListener(this);

    pan.add(color = new JButton("ColorSet"));
    color.addActionListener(this);

    pan.add(sound = new JToggleButton("sound: on"));
    sound.addActionListener(this);

    pan.add(speed = new JToggleButton("FPS: 50 Hz"));
    speed.addActionListener(this);

    pan.add(prefix = new JTextField(""));
    prefix.addActionListener(this);

    pan.add(file = new JTextField(""));

    pan.add(openAssembler = new JButton("Jasm64"));
    openAssembler.addActionListener(this);

    pan.add(pause = new JButton("Pause"));
    pause.addActionListener(this);

    pan.add(saveDisk = new JButton("Save Disk"));
    saveDisk.addActionListener(this);
    pan.add(fileName = new JTextField(""));

    list = new JList();
    list.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e) {
	  if (e.getClickCount() == 2) {
	    load.doClick();
	  }
	}
      });
    JScrollPane scroll = new JScrollPane(list);
    scroll.setSize(200, 200);
    window.getContentPane().add(scroll, BorderLayout.CENTER);
    window.getContentPane().add(pan, BorderLayout.SOUTH);

    infoWindow = new JPanel();
    infoWindow.setLayout(new GridLayout(0, 1));
    for (int i = 0; i < 8; i++) {
      infoWindow.add(sprites[i] = new JLabel("0000"));
    }
    window.getContentPane().add(infoWindow, BorderLayout.NORTH);
    //	window.getContentPane().add(scr.getScreen(), BorderLayout.WEST);
    window.pack();
    window.setResizable(true);
    window.setVisible(true);
    window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    window.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });

    new Thread(this).start();

    new Thread(new Runnable() {
	public void run() {
	  while (true) {
	    try {
	      Thread.sleep(100);
	    } catch (Exception e) {
	      System.out.println("Error while sleeping!");
	    }

	    if (!fullscreen) {
	      window.setTitle("Scan rate: " + scr.getActualScanRate() +
			      " $1 = " + Integer.toString(memory[1], 16));

	      int spr0 = 0x03f8 + scr.videoMatrix;
	      int spr = 0;
	      int spr0X = C64Screen.IO_OFFSET + 0xd000;
	      int coll = memory[C64Screen.IO_OFFSET + 0xd01f];
	      int coll2 = memory[C64Screen.IO_OFFSET + 0xd01e];
	      int expy = memory[C64Screen.IO_OFFSET + 0xd017];
	      int expx = memory[C64Screen.IO_OFFSET + 0xd01d];
	      int yscroll = memory[C64Screen.IO_OFFSET + 0xd011] & 7;
	      int xscroll = memory[C64Screen.IO_OFFSET + 0xd016] & 7;
	      String xtra = "";
	      for (int i = 0; i < 8; i++)
	      {
		xtra = "";
		if (i == 0) xtra = "  YScr:" + yscroll;
		if (i == 1) xtra = "  XScr:" + xscroll;
		int spos =
		  scr.vicBank + (64 * (spr = memory[spr0 + i]));
		int xpos = memory[spr0X + i * 2] +
		  ((memory[spr0X + 0x10] & (1 << i)) != 0 ? 256 : 0);
		int ypos = memory[spr0X + 1 + i * 2];
		boolean c1 = (coll & (1 << i)) != 0;
		boolean c2 = (coll2 & (1 << i)) != 0;
		String x1 = (expx & (1 << i)) != 0 ? "X" : "x";
		String y1 = (expy & (1 << i)) != 0 ? "Y" : "y";
		sprites[i].setText("Spr " + i + (c1 ? " #": " _") +
				   (c2 ? "#": "_") + x1 + y1 +
				   " " + istr(xpos, 3) + "," +
				   istr(ypos, 3) + " " +
				   Integer.toString(spr)
				   + " "
				   + Integer.toString(spos, 16) + xtra);
	      }
	      //			infoWindow.repaint();//set text does this
	    }
	  }
	}
      }).start();
  }

  // Slow apepnder of strings...
  private String istr(int v, int t) {
    String s = "" + v;
    while (s.length() < t) {
      s = "0" + s;
    }
    return s;
  }

  private void saveDisk(String filename) {
    try {
      reader.writeDisk(new FileOutputStream(filename));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void readDisk() {
    if (fileDialog == null)
      fileDialog = new FileDialog(window, "Select File to Load");
    fileDialog.show();

    String name = fileDialog.getDirectory() + fileDialog.getFile();
    readDisk(name);
    dirNames = reader.getDirNames();
    list.setListData(dirNames.toArray());
    if (dirNames.size() > 0) list.setSelectedIndex(0);
  }

  private void readDisk(String name) {
    System.out.println("READING FROM: " + name);
    if ((name.toLowerCase()).endsWith(".d64"))
      reader.readDiskFromFile(name);
    else if ((name.toLowerCase()).endsWith(".t64"))
      reader.readTapeFromFile(name);
    else if (name.toLowerCase().endsWith(".prg") ||
	     name.toLowerCase().endsWith(".p00")) {
      cpu.reset();
      while(!scr.ready()) {
	try {
	  Thread.sleep(100);
	}catch (Exception e2) {
	  System.out.println("Exception while sleeping... C64Test");
	}
      }

      reader.readPGM(name, -1);
      cpu.runBasic();
    }
  }

  public void run() {
    cpu.start();
  }

  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == vicdump) {
      scr.dumpGfxStat();
// TODO: Fix printStatus in all ExtChips... or in C64Screen!     
//      scr.getSIDs()[0].printStatus();
//      scr.getSIDs()[1].printStatus();
//      scr.getSIDs()[2].printStatus();

      scr.getCIAs()[0].printStatus();
      scr.getCIAs()[1].printStatus();

    } else if (e.getSource() == joy) {
      scr.setStick(!joy.isSelected());
      joy.setText("joy:"+(joy.isSelected() ? "1":"0"));
    } else if (e.getSource() == prefix) {
      monitor.setPrefix(prefix.getText());
    } else if (e.getSource() == openAssembler) {
      openAssembler();
    } else if (e.getSource() == save) {
      System.out.println(reader.saveFile());
    } else if (e.getSource() == color) {
      scr.setColorSet(cset++ % VICConstants.COLOR_SETS.length);
    } else if (e.getSource() == sound) {
      scr.setSoundOn(sound.isSelected());
      sound.setText("Sound: "+(sound.isSelected() ? "On":"Off"));
    } else if (e.getSource() == pause) {
      cpu.setPause(!cpu.pause);
    } else if (e.getSource() == speed) {
      if (speed.isSelected()) {
	scr.setScanRate(0.1);
      } else {
	scr.setScanRate(50);
      }
      speed.setText("FPS: " + (speed.isSelected() ? "50/s":"1/s"));
    } else if (e.getSource() == debug) {
      monitor.setEnabled(debug.isSelected());
      debug.setText("debug:"+(debug.isSelected() ? "true":"false"));
    } else if (e.getActionCommand().startsWith("Res")) {
      System.out.println("Reset!");
      cpu.reset();
    } else if (e.getSource() == full) {
      setFull(true);
    } else if (e.getSource() == normal) {
      setFull(false);
    } else if (e.getSource() == saveDisk) {
      saveDisk(fileName.getText());
    } else if (e.getActionCommand().startsWith("Loa")) {
      Object o = list.getSelectedValue();
      String str = "";
      if (o instanceof DirEntry) {
        DirEntry dire = (DirEntry) o;
        str = dire.name;
      } else {
        str = (String) o;
      }
      if (str != null)
      {
        System.out.println("Should load: \"" + str + "\"");

        cpu.reset(); // TODO: fix so that reset waits for reset of chips, etc.
        
        try {
          Thread.sleep(10);
        } catch (Exception e2) {
          System.out.println("Exception while sleeping...");
        }

        
        while (!scr.ready()) {
          try {
            Thread.sleep(100);
          } catch (Exception e2) {
            System.out.println("Exception while sleeping...");
          }
        }

	// Does not always work....
	System.out.println("Loading: \"" + str + "\"");
	reader.readFile(str);
	// 				monitor.setEnabled(true);
	cpu.runBasic();
      }
    } else if (e.getActionCommand().startsWith("Sel")) {
      readDisk();
    } else if (e.getActionCommand().startsWith("UP")) {
      sprnum = (sprnum + 1) & 0xff;
      stext.setText("Sprite: " + sprnum);
      int mpos = scr.vicBank + (64 * sprnum);
      System.out.println("VicBank:" + Integer.toString(scr.vicBank, 16));
      System.out.println("VideoMatrix:" +
			 Integer.toString(scr.videoMatrix, 16));
      System.out.println("Raster Pos:" + scr.vbeam + " irq: " + scr.raster);
      System.out.println("CharSet:" + Integer.toString(scr.charSet, 16));
      System.out.println(
			 "Sprite Adr:" + Integer.toString(mpos, 16) + " = " + mpos);
      int spr0 = 0x03f8 + scr.videoMatrix;
      int spos = scr.vicBank + (64 * memory[spr0]);
      System.out.println("Sprite 0:" + Integer.toString(spos, 16));
      spos = scr.vicBank + (64 * memory[spr0 + 1]);
      System.out.println("Sprite 1:" + Integer.toString(spos, 16));
      spos = scr.vicBank + (64 * memory[spr0 + 2]);
      System.out.println("Sprite 2:" + Integer.toString(spos, 16));
      spos = scr.vicBank + (64 * memory[spr0 + 3]);
      System.out.println("Sprite 3:" + Integer.toString(spos, 16));
      spos = scr.vicBank + (64 * memory[spr0 + 4]);
      System.out.println("Sprite 4:" + Integer.toString(spos, 16));
      spos = scr.vicBank + (64 * memory[spr0 + 5]);
      System.out.println("Sprite 5:" + Integer.toString(spos, 16));
      int adr = 0;
      for (int i = 0; i < 21; i++)
	for (int x = 0; x < 3; x++)
	{
	  int data = memory[mpos++];
	  for (int pos = 0; pos < 8; pos++)
	    sprMem[adr++] =
	      (data & (1 << pos)) != 0 ? 0xffffffff : 0xff000000;
	}
      mis.newPixels();

      System.out.println("BasicON: " + cpu.basicROM);
      System.out.println("KernalON: " + cpu.kernalROM);
      System.out.println("charON: " + cpu.charROM);
      System.out.println("ioON: " + cpu.ioON);

      sprPan.repaint();
    } else {
      String strt = txt.getText();
      int i = Integer.parseInt(strt, 16);
      if (i < 0x15df00)
	dumpMemory(i, 256);
    }
  }

  private void openAssembler() {
    if (asmFrame == null) {
      assembler = new Assembler();
      assembler.setMemory(memory);
      asmFrame = new JFrame("Jasm64");
      asmFrame.setBounds(100,100,400,400);
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JScrollPane(asmText = new JTextArea(40, 40)),
		BorderLayout.CENTER);
      panel.add(asmAdr = new JTextField("$c000"), BorderLayout.NORTH);
      panel.add(asm = new JButton("Assemble"), BorderLayout.SOUTH);
      asm.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent event) {
	    assembler.assemble(asmText.getText(),
			       Assembler.parseInt(asmAdr.getText()));
	  }
	});
      asmFrame.getContentPane().add(panel);
    }
    asmFrame.setVisible(true);
  }


  private void dumpMemory(int address, int size) {
    String tmp = "";
    String tmp2 = "";
    String t;
    System.out.println("Dumping memory: " + address);
    for (int i = 0; i < size; i++) {
      t = Integer.toString(memory[i + address] & 0xff, 16);
      if (t.length() < 2)
	t = "0" + t;
      tmp = tmp + t + " ";
      if (i % 4 == 3) tmp += " ";

      if (memory[i + address] > 32 && memory[i + address] < 99)
	tmp2 += (char) memory[i + address];
      else
	tmp2 += ".";

      if (i % 16 == 15) {
	String adr = Integer.toString(address + i - 15, 16);
	while (adr.length() < 4)
	  adr = "0" + adr;
	System.out.println(adr + "  " + tmp + " " + tmp2);
	tmp = "";
	tmp2 = "";
      }
    }
  }

  public void toggleFullScreen() {
    System.out.println("Toggle fullscreen called!");
    setFull(!fullscreen);
  }

  private void setFull(boolean full) {
    JWindow jw = full ? C64Scr : null;
    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
      getDefaultScreenDevice().setFullScreenWindow(jw);
    if (!full) {
      C64Scr.setSize(386 * 2, 284 * 2);
      C64Scr.validate();
    }
    fullscreen = full;
  }

  public static void main(String[] name) {
    String file = "/games/c64-demodiskette.c64";
    if (name.length > 0)
      file = name[0];
    System.out.println("Loading " + file);
    C64Test test = new C64Test(file);
    test.init();
  }
}
