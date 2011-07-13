/**
 * encoding: UTF-8
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 *
 *
 */

package com.dreamfabric.jac64;
import com.dreamfabric.c64utils.*;
import java.awt.event.*;
import java.util.*;

/**
 * The keyboard and joystick emulation
 *
 *
 * Created: Wed Jun 14 17:39:15 2006
 *
 * @author Joakim Eriksson, joakime@sics.se
 * @version 1.0
 */
public class Keyboard {

  public static final int IO_OFFSET = CPU.IO_OFFSET;

  public static final int AUTO_SHIFT = 1024;
  public static final int AUTO_CTRL = 2048;
  public static final int AUTO_COMMODORE = 4096;
  public static final int MIN_AUTO = AUTO_SHIFT;

  public static final int STICK_UPDOWN = 1 | 2;
  public static final int STICK_LEFTRIGHT = 4 | 8;

  public static final int STICK_UP = 1;
  public static final int STICK_DOWN = 2;
  public static final int STICK_LEFT = 4;
  public static final int STICK_RIGHT = 8;
  public static final int STICK_FIRE = 16;

  public static final char[][] KEYMAPS = new char[][] {
    { 's','v',  // Keymap for language "sv"
      ';', 'ö',
      '\'','ä',
      '[','å',
      '`', '§',
      '\\', (char) 222,
      '/', '-',
      ']', (char) 135,
      '-', (char) KeyEvent.VK_PLUS,
      '=', (char) 129
    },
    { 'd','e',  // Keymap for language "de"
      ';', 'ö',
      '\'','ä',
      '[','ü',
      '`', '^',
      '\\', (char) 520,
      '/', '-',
      ']', (char) 521,
      '-', (char) 223,
      '=', (char) 129
    }
  };


  boolean extendedKeyboardEmulation = false;
  boolean stickExits = false;

  int joystick1 = 255;
  int bval = 255;
  boolean lastUp = false;
  boolean lastLeft = false;

  private int joy1 = 0xff;
  private int joy2 = 0xff;

  int stick = IO_OFFSET + 0xdc00; // stick 0;
  private int keyPressed = 0;
  private int lastKey;

  private C64Script c64script;
  private ArrayList hotkeyScript;

  int keyrow[] = new int[8];
  int keycol[] = new int[8];

  // Some keys have high value (+ => 521 for example)
  int keytable[][] = new int[1024][3];

  // For handling autoshifts...
  int keyShift = 0;

  // Needs to add some "special keys" - that are handled slightly
  // different => key up / key right = key dwn + shift & key left + shift
  // + some ordinary keys that is hard to get on a normal non c64 keyboard
  // needs to be mapped (+-/*?, etc)
  // The C64 key-table - key to map, then row + col + auto "shift" / flag
  int keytableDef[][] = new int[][] {
      {'A', 1, 2, 0 },
      {'B', 3, 4, 0 },
      {'C', 2, 4, 0 },
      {'D', 2, 2, 0 },
      {'E', 1, 6, 0 },
      {'F', 2, 5, 0 },
      {'G', 3, 2, 0 },
      {'H', 3, 5, 0 },
      {'I', 4, 1, 0 },
      {'J', 4, 2, 0 },
      {'K', 4, 5, 0 },
      {'L', 5, 2, 0 },
      {'M', 4, 4, 0 },
      {'N', 4, 7, 0 },
      {'O', 4, 6, 0 },
      {'P', 5, 1, 0 },
      {'Q', 7, 6, 0 },
      {'R', 2, 1, 0 },
      {'S', 1, 5, 0 },
      {'T', 2, 6, 0 },
      {'U', 3, 6, 0 },
      {'V', 3, 7, 0 },
      {'W', 1, 1, 0 },
      {'X', 2, 7, 0 },
      {'Y', 3, 1, 0 },
      {'Z', 1, 4, 0 },
      {'0', 4, 3, 0 },
      {'1', 7, 0, 0 },
      {'2', 7, 3, 0 },
      {'3', 1, 0, 0 },
      {'4', 1, 3, 0 },
      {'5', 2, 0, 0 },
      {'6', 2, 3, 0 },
      {'7', 3, 0, 0 },
      {'8', 3, 3, 0 },
      {'9', 4, 0, 0 },
      {'\n', 0, 1, 0 },
      {KeyEvent.VK_ENTER, 0, 1, 0 },
      {' ', 7, 4, 0 },
      {',', 5, 7, 0 },
      {'.', 5, 4, 0 },
      // = on the \ pos.
      {'\\', 6, 5, 0},
      // This is actually colon on the C64 ;-)
      {';', 5, 5, 0 },
      {'-', 5, 0, 0 },     // Actually + on the C64... (- => +)
      //    {'-', 5, 3, 0 },
      {'=', 5, 3, 0 },     // Actually - on the C64... (= => -)
      {'`', 7, 1, 0 },
      // This is semi-colon on the C64
      {'\'', 6, 2, 0 },
      {KeyEvent.VK_SUBTRACT, 5, 3, 0 },
      {'[', 5, 6, 0 },
      {']', 6, 1, 0 },
      {KeyEvent.VK_ESCAPE, 7, 1, 0},
      {'/', 6, 7, 0 },
      {KeyEvent.VK_DIVIDE, 6, 7, 0},
      // will be mapped to an auto shift /
      {'?', 6, 7, AUTO_SHIFT},
      {KeyEvent.VK_DELETE, 0, 0, 0 },
      {KeyEvent.VK_BACK_SPACE, 0, 0, 0 },
      // LEFT SHIFT
      {KeyEvent.VK_SHIFT, 1, 7, 0 },
      // RIGHT SHIFT
      {KeyEvent.VK_CAPS_LOCK, 6, 4, 0 },

      // Break
      {19, 7, 7, 0 },
      {KeyEvent.VK_ESCAPE, 7, 7, 0 },
      // Enter + CTRL
      {'\r', 0, 1, 0 },
      // Commodore key on the control keys
      {KeyEvent.VK_CONTROL, 7, 5, KeyEvent.KEY_LOCATION_LEFT},
      {KeyEvent.VK_ENTER, 0, 1, 0 },
      // DOWN     &  RIGHT
      {KeyEvent.VK_DOWN, 0, 7, 0 },
      {KeyEvent.VK_UP, 0, 7, AUTO_SHIFT },
      {KeyEvent.VK_RIGHT, 0, 2, 0 },
      {KeyEvent.VK_LEFT, 0, 2, AUTO_SHIFT },
      // Function keys
      {KeyEvent.VK_F1, 0, 4, 0 },
      {KeyEvent.VK_F3, 0, 5, 0 },
      {KeyEvent.VK_F5, 0, 6, 0 },
      {KeyEvent.VK_F7, 0, 3, 0 },
      {KeyEvent.VK_HOME, 6, 3, 0 },
      {KeyEvent.VK_END, 6, 6, 0},
      {KeyEvent.VK_INSERT, 6, 0, 0},
      {KeyEvent.VK_TAB, 7, 2, 0}
  };

  private int restoreKey = KeyEvent.VK_PAGE_UP;

  public static final int USER_UP = 0;
  public static final int USER_DOWN = 1;
  public static final int USER_LEFT = 2;
  public static final int USER_RIGHT = 3;
  public static final int USER_FIRE = 4;

  private static final int[] ARROWS = new int[] {
    KeyEvent.VK_UP, 0, KeyEvent.VK_DOWN, 0,
    KeyEvent.VK_LEFT, 0, KeyEvent.VK_RIGHT, 0,
    KeyEvent.VK_CONTROL, KeyEvent.KEY_LOCATION_RIGHT};

  private int[] userDefinedStick = ARROWS;

  private CIA cia;
  private C64Screen screen;

  private void remap(char key, char newkey) {
    for (int i = 0, n = keytableDef.length; i < n; i++) {
      if (keytableDef[i][0] == key) {
        keytableDef[i][0] = newkey;
        System.out.println("Remapped: " + (char) key + " => " + (char) newkey +
            "  key " + (int) key + " => " + (int) newkey);
        return;
      }
    }
  }


  private boolean doMap(String ltarget) {
    for (int i = 0, n = KEYMAPS.length; i < n; i++) {
      char[] map = KEYMAPS[i];
      String lang = "" + map[0] + map[1];
      System.out.println("Checking map for: " + lang);
      if (ltarget.equals(lang)) {
        System.out.println("Found map - mapping...");
        for (int j = 2, m = map.length; j < m; j += 2) {
          remap(map[j], map[j + 1]);
        }
        // Finished!
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a new <code>Keyboard</code> instance.
   *
   */
  public Keyboard(C64Screen screen, CIA cia, int[] memory) {

    Locale locale = Locale.getDefault();
    System.out.println("Locale: " + locale);

    String lang = locale.getLanguage();
    if (!doMap(lang)) {
      System.out.println("Could not find map for keyboard: " + lang);
    }

    this.cia = cia;
    this.screen = screen;

    for (int i = 0; i < keytable.length; i++) {
      keytable[i][0] = -1;
    }

    for (int i = 0; i < keytableDef.length; i++) {
      int key = keytableDef[i][0];
      keytable[key][0] = keytableDef[i][1];
      keytable[key][1] = keytableDef[i][2];
      keytable[key][2] = keytableDef[i][3];
    }

    reset();
  }

  public void registerHotKey(int key, int modflag, String script, Object o) {
    if (hotkeyScript == null) {
      c64script = new C64Script();
      hotkeyScript = new ArrayList();
    }
    hotkeyScript.add(new Object[] {script, o, new int[] {key, modflag}});
  }

  public void setStick(boolean one) {
    if (one)
      stick = IO_OFFSET + 0xdc01;
    else
      stick = IO_OFFSET + 0xdc00;
  }

  private int getUserStick(int key, int location) {
    if (userDefinedStick != null) {
      for (int i = 0, n = userDefinedStick.length; i < n; i += 2) {
        if (key == userDefinedStick[i]) {
          int cmpLoc = userDefinedStick[i + 1];
          if (cmpLoc == 0 || location == userDefinedStick[i + 1]) {
//          System.out.println("Joystick emulation trigger");
            return i / 2;
          }
        }
      }
    }
    return -1;
  }

  // # Chars in char buffer - 0xc6 - need not to be set here...
  public void keyPressed(KeyEvent event) {
//  System.out.println("Key pressed..." + event.getKeyCode());
//  System.out.println("Char pressed..." + event.getKeyChar() + " = " +
//  (int) event.getKeyChar());
    char chr = event.getKeyChar();
    int key = event.getKeyCode();
    int location = event.getKeyLocation();

    if (hotkeyScript != null) {
      int mod = event.getModifiersEx();
//    System.out.println("HotKey: " + key + " mod: " + mod);

      for (int i = 0, n = hotkeyScript.size(); i < n; i++) {
        Object[] hk = (Object[]) hotkeyScript.get(i);
        int[] keys = (int[]) hk[2];
//      System.out.println("Cmp " + keys[0] + " mod: " + keys[1]);
        if (keys[0] == key && ((mod & keys[1]) == keys[1])) {
          c64script.interpretCall((String) hk[0], hk[1]);
        }
      }
    }

    if (key == 0) {
      System.out.println("KeyZero ???");
      key = (int) Character.toLowerCase(chr);
    }

    if (key == restoreKey) {
      screen.restoreKey(true);
    }

    if (key != lastKey) {
      keyPressed++;
    }
    lastKey = key;

    int usr = getUserStick(key, location);
    if (extendedKeyboardEmulation) usr = -1;
    if (usr == -1) {
      usr = getNormalStick(key);
    }

    switch (usr) {
    // CHANGE TO STOCK1LEFT = true; (or similar!)
    case USER_UP:
      joystick1 = joystick1 & (255 - STICK_UP);
      lastUp = true;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_DOWN:
      joystick1 = joystick1 & (255 - STICK_DOWN);
      lastUp = false;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_LEFT:
      joystick1 = joystick1 & (255 - STICK_LEFT);
      lastLeft = true;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_RIGHT:
      joystick1 = joystick1 & (255 - STICK_RIGHT);
      lastLeft = false;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_FIRE:
      joystick1 = joystick1 & (255 - STICK_FIRE);
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    }

    // Change the joystick with F10!
    switch (key) {
    case KeyEvent.VK_F10:
      setStick(stick == IO_OFFSET + 0xdc00);
      break;
    case KeyEvent.VK_F9:
      System.out.println("F9");
      extendedKeyboardEmulation = !extendedKeyboardEmulation;
      stickExits = !extendedKeyboardEmulation;
      break;
    case KeyEvent.VK_F11:
      // Toggle colour set?
      break;
    }

    if (extendedKeyboardEmulation) {
      // Auto shift handling!
      if ((keytable[key][2] & AUTO_SHIFT) != 0) {
        keyShift++;
        if (keyShift == 1) {
          // AutoShift on if first shift! - otherwize not?!
          handleKeyPress(KeyEvent.VK_SHIFT, location);
        }
        // Also increase shift is shift is pressed
      } else if (key == KeyEvent.VK_SHIFT) {
        keyShift++;
      }
      handleKeyPress(key, location);

    } else {
      // No autoshift keys!!!
      if (keytable[key][2] < MIN_AUTO) {
        handleKeyPress(key, location);
      }
    }
  }

  public void keyReleased(KeyEvent event)
  {
    int key = event.getKeyCode();
    char chr = event.getKeyChar();
    int location = event.getKeyLocation();

    if (key == 0) {
      System.out.println("KeyZero???");
      key = (int) Character.toLowerCase(chr);
    }

    if (key == restoreKey) {
      screen.restoreKey(false);
    }


    keyPressed--;
    lastKey = 0;
    if (keyPressed < 0)
      keyPressed = 0;

    int usr = getUserStick(key, location);
    if (usr == -1)
      usr = getNormalStick(key);

    switch (usr) {
    case USER_UP:
      joystick1 = joystick1 | STICK_UP;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_DOWN:
      joystick1 = joystick1 | STICK_DOWN;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_LEFT:
      joystick1 = joystick1 | STICK_LEFT;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_RIGHT:
      joystick1 = joystick1 | STICK_RIGHT;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    case USER_FIRE:
      joystick1 = joystick1 | STICK_FIRE;
      updateJoystick();
      if (stickExits) {
        return;
      }
      break;
    }

    if (extendedKeyboardEmulation) {
      // Auto shift handling!
      if ((keytable[key][2] & AUTO_SHIFT) != 0) {
        keyShift--;
        if (keyShift == 0) {
          // AutoShift on if first shift! - otherwize not?!
          handleKeyRelease(KeyEvent.VK_SHIFT, location);
        }
        // Also decrease shift is shift is released
      } else if (key == KeyEvent.VK_SHIFT) {
        keyShift--;
        // Should not remove shift if autoshift was on?
        if (keyShift > 0)
          return;
      }
      handleKeyRelease(key, location);
    } else {
      if (keytable[key][2] < MIN_AUTO) {
        handleKeyRelease(key, location);
      }
    }
  }

  private int getNormalStick(int key) {
    switch (key) {
    case KeyEvent.VK_NUMPAD8 :
      return USER_UP;
    case KeyEvent.VK_NUMPAD2 :
    case KeyEvent.VK_NUMPAD5 :
      return USER_DOWN;
    case KeyEvent.VK_NUMPAD4 :
      return USER_LEFT;
    case KeyEvent.VK_NUMPAD6 :
      return USER_RIGHT;
    case KeyEvent.VK_NUMPAD0:
      return USER_FIRE;
    }
    return -1;
  }

  private void handleKeyPress(int key, int location) {
    int maprow = keytable[key][0];
    int mapcol = keytable[key][1];

    if (maprow != -1 && mapcol != -1) {
//    System.out.println("Row: " + maprow + " Col: " + mapcol);
      keyrow[maprow] = keyrow[maprow] & (255 - (1 << keytable[key][1]));
      keycol[mapcol] = keycol[mapcol] & (255 - (1 << keytable[key][0]));
    }
  }

  private void handleKeyRelease(int key, int location) {
    int maprow = keytable[key][0];
    int mapcol = keytable[key][1];

    if (maprow != -1 && mapcol != -1) {
      keyrow[maprow] = keyrow[maprow] | (1 << keytable[key][1]);
      keycol[mapcol] = keycol[mapcol] | (1 << keytable[key][0]);
    }
  }

  int readDC00(int pc) {
    // 0xdc00 - why does this not work when it is "correctly" implemented?
    // DC00 a'la VICE which seems to work on StarPost!?
    int val = 0xff;
    int tmp = pc < 0x10000 ? joy1 : 0xff;
    int mask = (cia.prb | ~(cia.ddrb)) & tmp;


    for (int m = 0x1, i = 0; i < 8; m <<= 1, i++) {
      if ((mask & m) == 0)
        val &= keycol[i];
    }

    tmp = pc < 0x10000 ? joy2 : 0xff;
//    System.out.println("Read keyboard via dc00: => " + ((val & (cia.pra | ~(cia.ddra))) & tmp));
    return (val & (cia.pra | ~(cia.ddra))) & tmp;
  }

  void setButtonval(int bval) {
    this.bval = bval;
    updateJoystick();
  }

  int readDC01(int pc) {
    // A'la VICE!
    //    System.out.println("PC: " + Integer.toHexString(pc));

    int val = 0xff;
    int tmp = pc < 0x10000 ? (joy2 & bval) : 0xff;
    int mask = (cia.pra | ~(cia.ddra)) & tmp;

    for (int m = 0x1, i = 0; i < 8; m <<= 1, i++) {
      if ((mask & m) == 0)
        val &= keyrow[i];
    }
    tmp = pc < 0x10000 ? (joy1 & bval) : 0xff;
//    System.out.println("Read keyboard via dc00: " + val + " => " + ((val & (cia.prb | ~(cia.ddrb))) & tmp));

    return (val & (cia.prb | ~(cia.ddrb))) & tmp;
  }
  
  /* Updates $DC00/$DC01 based on recent joystick activity. */
  private void updateJoystick() {

    int jst = joystick1 & bval;
    // both up and down?
    if ((jst & STICK_UPDOWN) == 0) {
      jst = (jst | STICK_UPDOWN) & (0xff - (lastUp ? STICK_UP : STICK_DOWN));
    }

    // both left and rigth?
    if ((jst & STICK_LEFTRIGHT) == 0) {
      jst = (jst | STICK_LEFTRIGHT) & (0xff - (lastLeft ? STICK_LEFT : STICK_RIGHT));
    }

    joy2 = stick == 0xdc00 + IO_OFFSET ? 0xff : jst;
    joy1 = stick == 0xdc00 + IO_OFFSET ? jst : 0xff;
  }

  public void reset() {
    lastKey = 0;
    keyPressed = 0;
    keyShift = 0;
    joystick1 = 255;

    for (int i = 0; i < 8; i++) {
      keyrow[i] = 255;
      keycol[i] = 255;
    }

  }
}
