/**
 * encoding: UTF-8
 * JaC64 - Application for JaC64 emulator
 * A Swing UI for the JaC64 emulator for download to Java enabled
 * Desktop computers.
 * Created: Sat Dec 08 23:27:15 2007
 *
 * @author Joakim Eriksson, Dreamfabric / joakime@sics.se
 * @version 1.0
 */
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.net.*;
import com.dreamfabric.jac64.*;
import com.dreamfabric.c64utils.*;

public class JaC64 implements ActionListener, KeyEventDispatcher {

  private static final String ABOUT_MESSAGE = 
    "JaC64 version: " + C64Screen.version + "\n" +
    "JaC64 is a Java-based C64 emulator by Joakim Eriksson\n" +
    "The SID emulation use the resid Java port by Ken Händel\n\n" +
    "For more information see: http://www.jac64.com/";

  private C64Reader reader;
  private C64Screen scr;
  private boolean fullscreen = false;

  private CPU cpu;
  private JFrame C64Win;
  private KeyListener c64Canvas;
  private FileDialog fileDialog;

  private JMenuItem load;
  private JTable fileTable;
  private JDialog loadFile;
  private DirEntry[] dirEntries;

  private static final String[] SID_TYPES = new String[] {"SID: resid MOS 6581", 
    "SID: resid MOS 8580", "SID: JaC64 Original"};
  
  private static final String[] JOYSTICK = new String[] {"Joystick in port 1",
    "Joystick in port 2"};
  
  private TableModel dataModel = new AbstractTableModel() {
    public final String[] NAMES = new String[] {"File name", "Size", "Type"};
    public int getColumnCount() { return 3;}
    public int getRowCount() { return (dirEntries != null ? dirEntries.length : 0);}
    public Object getValueAt(int row, int col) { 
      if (col == 0) return dirEntries[row].name;
      if (col == 1) return new Integer(dirEntries[row].size);
      if (col == 2) return dirEntries[row].getTypeString();
      return "-";
    }
    public String getColumnName(int col) {
      return NAMES[col];
    }
  };
  
  public JaC64() {
    SIDMixer.DL_BUFFER_SIZE = 16384;
    Debugger monitor = new Debugger();

    cpu = new CPU(monitor, "", new SELoader());
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

    C64Win = new JFrame("JaC64 - A Java C64 Emulator");
    C64Win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    JMenuBar jbar = new JMenuBar();
    C64Win.setJMenuBar(jbar);
    JMenu filem;
    JMenuItem mi;
    jbar.add(filem = new JMenu("File"));
    filem.add(mi = new JMenuItem("Open File/Disk"));
    mi.addActionListener(this);
    filem.add(load = new JMenuItem("Load File"));
    load.addActionListener(this);
    filem.add(mi = new JMenuItem("Reset"));
    mi.addActionListener(this);
    filem.add(mi = new JMenuItem("Hard Reset"));
    mi.addActionListener(this);
    filem.add(mi = new JMenuItem("About JaC64"));
    mi.addActionListener(this);

    jbar.add(filem = new JMenu("Settings"));
    JMenu subm;
    filem.add(subm = new JMenu("Color Set"));
    createRadioMenu(subm, new String[] {"Color Set 1 - JaC64 original",
        "Color Set 2 - darker", "Color Set 3 - softer", 
        "Color Set 4 - Win VICE"}, 0);

    filem.add(subm = new JMenu("SID Emulation"));
    createRadioMenu(subm, SID_TYPES, 0);
    
    filem.add(subm = new JMenu("Joystick Port"));
    createRadioMenu(subm, JOYSTICK, 0);
    
    C64Win.setBackground(Color.black);
    C64Win.setForeground(Color.black);
    C64Win.setLayout(new BorderLayout());
    C64Win.getContentPane().add(scr.getScreen(),
				BorderLayout.CENTER);
    C64Win.setFocusable(true);

    C64Win.pack(); //	C64Scr.setSize(380,300);
    C64Win.setSize(386 * 2 + 10, 284 * 2 + 70);
    C64Win.setResizable(true);
    C64Win.setVisible(true);
    
    KeyboardFocusManager.
      getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);

    // Setup disk sounds
    AudioClip trackSound = null;
    AudioClip motorSound = null;
    URL url = getClass().getResource("sounds/track.wav");
    if (url != null) trackSound = Applet.newAudioClip(url);
    url = getClass().getResource("sounds/motor.wav");
    if (url != null) motorSound = Applet.newAudioClip(url);
    scr.setSounds(trackSound, motorSound);
    c64Canvas = (KeyListener) scr.getScreen();
  }

  private void createRadioMenu(JMenu subm, String[] names, int selected) {
    ButtonGroup group = new ButtonGroup();
    JRadioButtonMenuItem mi;
    for (int i = 0; i < names.length; i++) {
      subm.add(mi = new JRadioButtonMenuItem(names[i]));
      if (i == selected)
        mi.setSelected(true);
      mi.addActionListener(this);
      group.add(mi);      
    }
  }
  
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (C64Win.isFocused()) {
      if (e.getID() == KeyEvent.KEY_PRESSED) {
        c64Canvas.keyPressed(e);
      } else if (e.getID() == KeyEvent.KEY_RELEASED) {
        c64Canvas.keyReleased(e);
      } else if (e.getID() == KeyEvent.KEY_TYPED) {
        c64Canvas.keyTyped(e);
      }
      return true;
    }
    return false;
  }

  public void actionPerformed(ActionEvent ae) {
    String cmd = ae.getActionCommand();
    if ("Open File/Disk".equals(cmd)) {
      if (fileTable == null) {
        // Show the table somewhere!!!
        fileTable = new JTable(dataModel);
        fileTable.getColumnModel().getColumn(1).setMaxWidth(50);
        fileTable.getColumnModel().getColumn(2).setMaxWidth(50);
        fileTable.setShowGrid(false);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      }
      readDisk();
    } else if ("Reset".equals(cmd)){
      cpu.reset();
    } else if ("Hard Reset".equals(cmd)){
      cpu.hardReset();
    } else if ("About JaC64".equals(cmd)) {
      showAbout();
    } else if ("Load File".equals(cmd)) {
      if (loadFile == null) {
        loadFile = new JDialog(C64Win, "Load file from disk");
        loadFile.setAlwaysOnTop(true);
        loadFile.setVisible(true);
        loadFile.setLayout(new BorderLayout());
        loadFile.add(new JScrollPane(fileTable), BorderLayout.CENTER);
        loadFile.add(fileTable.getTableHeader(), BorderLayout.NORTH);
        JPanel jp = new JPanel();
        JButton jb;
        jp.add(jb = new JButton("Load file"));
        jb.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            reader.readFile(dirEntries[fileTable.getSelectedRow()].name);
            loadFile.setVisible(false);
          }
        });
        jp.add(jb = new JButton("Cancel"));
        jb.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            loadFile.setVisible(false);
          }
        });    
        loadFile.add(jp, BorderLayout.SOUTH);
        loadFile.setSize(300, 400);
      }
      loadFile.setVisible(true);
    } else if (cmd.startsWith("Color Set")) {
      int cs = cmd.charAt(10) - '1';
      System.out.println("Color set: " + cs);
      scr.setColorSet(cs);
    } else if (cmd.equals(SID_TYPES[0])) {
      scr.setSID(C64Screen.RESID_6581);
    } else if (cmd.equals(SID_TYPES[1])) {
        scr.setSID(C64Screen.RESID_8580);
    } else if (cmd.equals(SID_TYPES[2])) {
        scr.setSID(C64Screen.JACSID);
    } else if (cmd.equals(JOYSTICK[0])) {
      scr.setStick(true);
    } else if (cmd.equals(JOYSTICK[1])) {
      scr.setStick(false);
    }
  }

  private void showAbout() {
    JOptionPane.showMessageDialog(C64Win, ABOUT_MESSAGE,
        "JaC64 - The Java C64 Emulator", JOptionPane.INFORMATION_MESSAGE);
  }
  
  public void toggleFullScreen() {
    System.out.println("Toggle fullscreen called!");
    setFull(!fullscreen);
  }

  private void readDisk() {
    if (fileDialog == null)
      fileDialog = new FileDialog(C64Win, "Select File/Disk to Load");
    fileDialog.setVisible(true);

    String name = fileDialog.getDirectory() + fileDialog.getFile();
    if (!readDisk(name)) {
      dirEntries = (DirEntry[]) reader.getDirNames().toArray(new DirEntry[0]);
      fileTable.tableChanged(new TableModelEvent(dataModel));
    }
  }

  private boolean readDisk(String name) {
    System.out.println("READING FROM: " + name);
    if ((name.toLowerCase()).endsWith(".d64"))
      reader.readDiskFromFile(name);
    else if ((name.toLowerCase()).endsWith(".t64"))
      reader.readTapeFromFile(name);
    else if (name.toLowerCase().endsWith(".prg") ||
        name.toLowerCase().endsWith(".p00")) {
      cpu.reset();
      try {
        Thread.sleep(10);
      }catch (Exception e2) {
        System.out.println("Exception while sleeping...");
      }
      while(!scr.ready()) {
        try {
          Thread.sleep(100);
        }catch (Exception e2) {
          System.out.println("Exception while sleeping...");
        }
      }
      reader.readPGM(name, -1);
      cpu.runBasic();
      return true;
    }
    return false;
  }

  private void setFull(boolean full) {
//     JWindow jw = full ? C64Scr : null;
//     java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
//       getDefaultScreenDevice().setFullScreenWindow(jw);
//     if (!full) {
//       C64Scr.setSize(386 * 2, 284 * 2);
//       C64Scr.validate();
//     }
//     fullscreen = full;
  }


  public static void main(String[] name) {
    JaC64 test = new JaC64();
    test.cpu.start();
  }
}