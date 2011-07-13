/**
 * @(#)C64Applet.java   Created date: 99-8-20
 * Last update - 2005-01-01
 *
 */
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import com.dreamfabric.jac64.*;
import com.dreamfabric.c64utils.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.applet.AudioClip;

/**
 *
 *
 * @author  Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 1.14 $, $Date: 2006/05/01 14:57:57 $
 */
public class C64Applet extends Applet implements Runnable, PatchListener {

  private CPU cpu;
  private int[] memory;
  private boolean started = false;
  private boolean stopping = false;
  private C64Reader reader;
  private String currentDisk;
  private String loadFile;
  private boolean stick = true; // Emulate joystick 1
  private C64Screen screen;
  private C64Canvas canvas;
  private Vector files;
  private boolean require1541 = false;

  private IMonitor imon = new DefaultIMon();

  private static Color lblue = new Color(VICConstants.COLOR_SETS[0][14]);

  private Thread thread;
  private String autostartDisk;
  private String autostartProgram;
  private String autoText;

  private int autostartID = -1;
  private int defaultStick = 0;
  private int soundOn = 0;
  private int doubleScreen = 0;


  private boolean fullscreen = false;
  private JFrame fullFrame = null;
  private JWindow fullWin = null;
  // -------------------------------------------------------------------
  // Applet methods
  // -------------------------------------------------------------------

  public void init() {
    System.out.println("###### Applet init() #######");
    started = false;
    stopping = false;
    currentDisk = null;

    if (cpu == null) {
      SIDMixer.DL_BUFFER_SIZE = 16384;

      System.out.println("starting CPU");
      cpu = new CPU(imon, getCodeBase().toString(), new SELoader());

      System.out.println("Status: initializing");

      doubleScreen = getParameterAsInt("doubleScreen", 0);
      int freescale = getParameterAsInt("freescale", 0);

      screen = new C64Screen(imon, doubleScreen > 0);
      cpu.init(screen);
      screen.init(cpu);

      if (freescale != 0) {
    	  screen.setIntegerScaling(false);
      }

      // Not when emulating 1541!!
      //      cpu.patchROM(this);

      memory = cpu.getMemory();

      setLayout(new BorderLayout());
      setBackground(Color.black);
      setForeground(lblue);

      // Get the diskdrive reader!!!
      reader = new C64Reader();
      reader.setCPU(cpu);

      canvas = (C64Canvas) screen.getScreen();

      fullscreen(fullscreen);

      screen.registerHotKey(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK |
			 KeyEvent.ALT_DOWN_MASK
			 , "reset()", cpu);

      screen.registerHotKey(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK
			    , "toggleFullscreen()", this);


      repaint();
      validate();

      addKeyListener(canvas);
      canvas.requestFocus();

      // A test... for real 1541 emulation...
      cpu.getDrive().setReader(reader);

      AudioClip trackSound = null;
      AudioClip motorSound = null;
      URL url = getClass().getResource("sounds/track.wav");
      System.out.println("Audio URL:" + url);
      if (url != null) trackSound = Applet.newAudioClip(url);
      url = getClass().getResource("sounds/motor.wav");
      if (url != null) motorSound = Applet.newAudioClip(url);
      screen.setSounds(trackSound, motorSound);


      setColorSet(getParameterAsInt("colorset", 0));
      int rq1541 = getParameterAsInt("require1541", 0);
      require1541 = (rq1541 == 1);

      for (int i = 0, n = 12; i < n; i++) {
    	  String f1 = getParameter("hotkey-f" + (i + 1));
    	  if (f1 != null && f1.length() > 0) {
    		  // 0 - 11 => ALT-F1-12
    		  screen.registerHotKey(KeyEvent.VK_F1 + i,
    				  KeyEvent.ALT_DOWN_MASK, f1, this);
    	  }
      }
      System.out.println("*** INIT END ***");
    }
  }

  public void toggleFullscreen() {
    fullscreen(!fullscreen);
  }

  public void fullscreen(boolean full) {
    fullscreen = full;
    if (full) {
      // Add opening a parent frame first... otherwise no keyinput
      // will work...
      remove(canvas);

      if (fullFrame == null) {
	fullFrame = new JFrame("-");
	fullWin = new JWindow(fullFrame);
	fullWin.addKeyListener(canvas);
      }

      screen.setAutoscale(true);
      fullWin.add(canvas);
      fullWin.setSize(100, 100);
      System.out.println("Setting visible to true!!!");
      fullWin.setVisible(true);
      fullFrame.setVisible(true);

      java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
	getDefaultScreenDevice().setFullScreenWindow(fullWin);
      fullWin.setFocusable(true);
    } else {
      if (fullFrame != null) {
	fullFrame.setVisible(false);
	fullWin.setVisible(false);
      }
      java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
	  getDefaultScreenDevice().setFullScreenWindow(null);
      add(canvas, BorderLayout.CENTER);
      validate();
    }
  }

  private void autoload() {
    Thread t = new Thread(new Runnable() {
	public void run() {
	  // Autostart from disk/prg
	  autoText = getParameter("autostartCode");
	  autostartDisk = getParameter("autostartDisk");
	  if (autostartDisk != null) {
	    autostartProgram = getParameter("autostartPGM");
	    if (autostartProgram == null) {
	      autostartProgram = getParameter("autostartProgram");
	    }
	  } else {
	    autostartProgram = getParameter("autostartPGM");
	    autostartID = getParameterAsInt("autostartProgram", -1);
	  }

	  defaultStick = getParameterAsInt("joystick", 0);
	  soundOn = getParameterAsInt("soundOn", 1);

	  if (getParameterAsInt("extendedKeyboard", 0) != 0) {
	    screen.setKeyboardEmulation(true);
	    System.out.println("Extended keyboard emulation on!");
	  }

	  loadGamesList();

	  screen.setSoundOn(soundOn == 1);
	  screen.setStick(defaultStick == 0);
	  if (autostartDisk != null) {
	    if (autostartProgram != null) {
	      loadGame(autostartDisk, autostartProgram);
	    } else {
	      insertDisk(autostartDisk);
              waitForKernal();
	      enterText(autoText);
	    }
	  } else if (autostartProgram != null) {
	    System.out.println("Autostart program:" + autostartProgram);
	    if (files != null && autostartProgram.equals("random")) {
	      int randomId = (int) (Math.random() * (files.size() / 2));
	      loadGame(randomId);
	    } else {
	      loadPGM(autostartProgram);
	    }
	  }
	  if (autostartID != -1) {
	    System.out.println("AutostartID: " + autostartID);
	    loadGame(autostartID);
	  }
	  started = true;
	}
      });
    t.start();
  }

  public void start() {
    System.out.println("###### Applet start() #######");

    // Start the thread as late as possible so that other init is done
    // before!
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
      try {
	  Thread.sleep(1000);
      } catch (Exception e) {
      }
      autoload();
    } else {
      unpause();
      started = true;
    }
  }

  public void stop() {
    System.out.println("###### Applet stop() #######");
    shutdown();
  }

  public void destroy() {
    System.out.println("###### Applet destroy() #######");
    if (screen != null && screen.getAudioDriver() != null)
        screen.getAudioDriver().shutdown();
    shutdown();
  }

  private void shutdown() {
    stopping = true;
    if (cpu != null)
        cpu.stop();
    if (screen != null) {
        screen.deleteInterruptManagers();
        screen.motorSound(false);
    }
    cpu = null;
    screen = null;
    removeKeyListener(canvas);
    canvas = null;
  }

  public boolean isStarted() {
    return started;
  }

  // -------------------------------------------------------------------
  // Run loop!
  // -------------------------------------------------------------------

  public void run() {
    if (started && !stopping) {
      System.out.println( "Status: running");
      cpu.start();
    } else {
      cpu.start();
    }
    stopping = false;
    thread = null;
  }

  // -------------------------------------------------------------------
  // Utils
  // -------------------------------------------------------------------


  private int getParameterAsInt(String paramName, int defVal) {
    String val = getParameter(paramName);
    System.out.println(paramName + " = " + val);
    if (val != null) {
      try {
	return Integer.parseInt(val);
      } catch (Exception e) {
	System.out.println("Can not parse value: " + val);
      }
    }
    return defVal;
  }

  private void loadGamesList() {
    System.out.println("Trying to load games list");
    try {
      URL url = getResource("games.txt");
      LineNumberReader reader =
	new LineNumberReader(new InputStreamReader(url.openConnection().getInputStream()));

      // game loop...
      String games;
      String disk;
      files = new Vector();
      while ((disk = reader.readLine()) != null) {
	disk = disk.trim();
	if (disk.toLowerCase().endsWith(".prg") ||
	    disk.toLowerCase().endsWith(".p00")) {
//        System.out.println("Adding PGM file: " + disk);
	  files.addElement(disk);
	  files.addElement(disk);
	} else {
//        System.out.println("reading games from disk: " + disk);
	  games = reader.readLine();
	  if (games != null) {
	    games = games.trim();
	    StringTokenizer stok = new StringTokenizer(games, ",");
	    while (stok.hasMoreElements()) {
	      String game = stok.nextToken();
	      files.addElement(disk);
	      files.addElement(game);
	      System.out.println("Adding: " + game);
	    }
	  }
	}
      }
    } catch (Exception e) {
      System.out.println("Can not load games..." + e);
      e.printStackTrace();
      System.out.println( "No games to load...");
    }
  }

  // -------------------------------------------------------------------
  // For scripting!
  // -------------------------------------------------------------------

  public void setColorSet(int i) {
    i = i % VICConstants.COLOR_SETS.length;
    screen.setColorSet(i);
  }

  public void poke(int address, int data) {
    if (address < 0xd000 || address >= 0xe000) {
      int[] memory = cpu.getMemory();
      memory[address & 0xffff] = data & 0xff;
    } else {
      cpu.poke(address & 0xffff, data & 0xff);
    }
  }

  public int peek(int address) {
    int[] memory = cpu.getMemory();
    return memory[address & 0xffff];
  }

  public void pause() {
    cpu.setPause(true);
  }

  public void unpause() {
    cpu.setPause(false);
  }

  private void loadProgram(int item) {
    String disk = (String) files.elementAt(item * 2);
    String name = (String) files.elementAt(item * 2 + 1);

    System.out.println("Index:" + item + " -> " + disk + " " + name);
    loadProgram(disk, name);
  }

  private boolean loadProgram(String disk, String name) {
    boolean em1541 = require1541;
    if (disk.startsWith("@")) {
      disk = disk.substring(1);
      em1541 = true;
    }
    if (disk != currentDisk) {
      URL url = getResource(disk);
      currentDisk = disk;
      disk = disk.toLowerCase();

      if (disk.endsWith(".d64")) {
	if (!reader.readDiskFromURL(url))
	  System.out.println("Status: problem while loading disk");
      } else if (disk.endsWith(".t64")) {
	if (!reader.readTapeFromURL(url))
	  System.out.println("Status: problem while loading tape");
      } else if (disk.endsWith(".prg") || disk.endsWith(".p00")) {
	if (!reader.readPGM(url, -1))
	  System.out.println("Status: problem while loading pgm");
	else System.out.println("Status: loaded " + disk);
	// Already read into memory!!!
	return false;
      }
    }

    if (em1541) {
      System.out.println("Loading with C1541 emulation...");
      enterText("load \"" + name + "\",8~");
      enterText("run~");
      return true;
    } else {
      for (int i = name.length(); i<16; i++)
	name = name + " ";
      if (reader.readFile(name) != null)
	System.out.println("Status: loaded " + name);
      else
	System.out.println("Status: error while loading " + name);
      return false;
    }
  }

  public void loadPGM(String pgm) {
    waitForKernal();
    URL url = getResource(pgm);
    if (!reader.readPGM(url, -1))
      System.out.println("Status: problem while loading pgm");
    cpu.runBasic();
    canvas.requestFocus();
  }

  public void insertDisk(String urlstr) {
    if (urlstr.startsWith("@")) {
      urlstr = urlstr.substring(1);
    }

    URL url = getResource(urlstr);
    if (!reader.readDiskFromURL(url)) {
      System.out.println("Status: problem while inserting disk: " + url);
    }
  }

  public void enterText(String txt) {
    cpu.enterText(txt);
  }

  public void loadPGM(String disk, String game) {
    loadGame(disk, game);
  }

  public void loadGame(String disk, String game) {
    waitForKernal();
    System.out.println("Loading " + game + " from " + disk);
    // load program returns true if autostarting!
    if (!loadProgram(disk, game))
      cpu.runBasic();
    canvas.requestFocus();
  }

  private void waitForKernal() {
    while(!screen.ready()) {
      try {
	Thread.sleep(100);
      } catch (Exception e2) {
	System.out.println("Exception while sleeping... C64Applet");
      }
    }
  }

  public void loadGame(int item) {
    waitForKernal();
    // Does not always work....
    loadProgram(item);
    cpu.runBasic();
    canvas.requestFocus();
  }

  // 0 -> stick 1 other -> stick 2
  public void setStick(int stick) {
    System.out.println("Setting stick: one ? " + (stick == 0));
    screen.setStick(stick == 0);
    canvas.requestFocus();
  }

  public void setSoundOn(boolean on) {
    screen.setSoundOn(on);
  }

  public void setScanRate(int rate) {
    screen.setScanRate(rate);
  }

  public void reset() {
    System.out.println("Reset - no kill");
    cpu.reset();
    screen.reset();
    canvas.requestFocus();
  }

  private URL getResource(String urls) {
    URL url = this.getClass().getResource(urls);
    if (url == null) try {
      url = new URL(getCodeBase().toString() + urls);
    } catch (Exception e) {}
    return url;
  }


  public void setEffect(int id) {
//    screen.getMixer().setEFX(id);
  }

  // Where should this be stored???
  public void saveFile(String name, String author, String description) {
    String data = "";
    if (description == null)
      description = "";
    try {
      data = "name=" + URLEncoder.encode(name, "UTF-8") +
        "&description=" + URLEncoder.encode(description, "UTF-8") +
        "&author=" + URLEncoder.encode(author, "UTF-8") +
        "&file=" + reader.saveFile();
    } catch (UnsupportedEncodingException uee) {
      uee.printStackTrace();
      return;
    }

    System.out.println("Saving file: " + data);

    // Make a post!
    try {
      URL url = getResource("prgup.php");
      URLConnection urlc = url.openConnection();
      urlc.setDoOutput(true);
      urlc.setUseCaches(false);
      HttpURLConnection httpConnection = (HttpURLConnection) urlc;

      httpConnection.setRequestMethod("POST");
      httpConnection
	.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
      DataOutputStream out =
	new DataOutputStream(httpConnection.getOutputStream());
      out.writeBytes(data);
      out.flush();
      out.close();

      InputStream is = httpConnection.getInputStream();

      System.out.println("Read back:");
      int c;
      while ((c = is.read()) != -1) {
	System.out.print((char) c);
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  // Patch listener
  public boolean readFile(String str, int adr) {
    str = str.trim();
    System.out.println("Should load: \"" + str + "\"");

    if ("$".equals(str)) {
      // Should enter basic program for dir listing...
      System.out.println("Entering basic data");

      ArrayList vc = reader.getDirNames();

      int pos = 2048;
      int nextPos;
      memory[pos++] = 0;
      for (int i = 0, n = vc.size(); i < n; i++)
      {
	DirEntry ent = reader.getDirEntry((String) vc.get(i));
	String name = ent.name;
	nextPos = pos + 5 + name.length();

	// Next position
	System.out.println("Name:  " + name + " " + name.length());
	System.out.println("Next:  " + nextPos);
	System.out.println("Pos:  " + pos);
	memory[pos++] = nextPos & 0xff;
	memory[pos++] = nextPos >> 8;

	// Row number
	memory[pos++] = ent.size & 0xff;
	memory[pos++] = ent.size >> 8;

	for (int j = 0; j < name.length(); j++)
	  memory[pos++] = name.charAt(j);

	memory[pos++] = 0;
      }
      return true;
    } else {
      for (int i = str.length(); i < 16; i++)
	str = str + " ";
      return reader.readFile(str, adr) != null;
    }
  }

    // 1 -> resid 6580, 2 -> resid 8580, 3 -> jac64
  public void setSIDEmulation(int type) {
      screen.setSID(type);
  }

  // -------------------------------------------------------------------
  // For "cheating"
  // Should have more than one AutoStore!
  // -------------------------------------------------------------------
  public void enableAutoStore(int max) {
    cpu.setCheatEnabled(max);
  }

  public void setAutoStore(int index, String prefix) {
    AutoStore as = new AutoStore(prefix);
    cpu.setAutoStore(index, as);
  }

  public void protect(int address, int value) {
    cpu.protect(address, value);
  }

  public void monitorRead(int address) {
    cpu.monitorRead(address);
  }

  public void monitorWrite(int address) {
    cpu.monitorWrite(address);
  }

  public void addAutoStoreRule(int index, String rule) {
    cpu.getAutoStore(index).addRule(rule);
  }

  public void addAutoStoreStore(int index, int adr, int len, String name) {
    cpu.getAutoStore(index).addStore(adr, len, name);
  }
}
