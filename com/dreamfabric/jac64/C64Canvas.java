/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

import java.awt.event.KeyListener;
import java.awt.event.FocusListener;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.awt.Font;
import javax.swing.JPanel;

/**
 * The actual AWT component that shows the C64 Screen.
 *
 * Created: Tue Aug 02 08:45:10 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public class C64Canvas extends JPanel implements KeyListener, FocusListener {
  private static final long serialVersionUID = 5124260828376559537L;

  boolean integerScale = true;
  C64Screen scr;
  Keyboard keyboard;
  boolean autoScale;
  int w;
  int h;

  public C64Canvas(C64Screen screen, boolean dob, Keyboard keyboard) {
    super();
    autoScale = dob;
    scr = screen;
    this.keyboard = keyboard;
    setFont(new Font("Monospaced", Font.PLAIN, 11));
    setFocusTraversalKeysEnabled(false);
    addFocusListener(this);
    addKeyListener(this);
  }

  public void setAutoscale(boolean val) {
    autoScale = val;
  }

  public void setIntegerScaling(boolean yes) {
    integerScale = yes;
  }

  public void update(Graphics g) {
    // No clearing of paint area...
    paint(g);
  }

  public void paint(Graphics g) {
    // All the paintcode should be here!!!
    if (autoScale) {
      if (w != getWidth() ||
	  h != getHeight()) {
	w = getWidth();
	h = getHeight();
	double fac = (1.0 * w) / C64Screen.IMG_TOTWIDTH;
	if (fac > (1.0 * h) / C64Screen.IMG_TOTHEIGHT) {
	  fac = (1.0 * h) / C64Screen.IMG_TOTHEIGHT;
	}
	if (integerScale && fac > 1.0) fac = (int) fac;
	scr.setDisplayFactor(fac);
	scr.setDisplayOffset((int) (w - fac * C64Screen.IMG_TOTWIDTH) / 2,
			     (int) (h - fac * C64Screen.IMG_TOTHEIGHT) / 2);
      }
    }
    scr.paint(g);
  }

  public void keyPressed(KeyEvent event) {
    keyboard.keyPressed(event);
  }

  public void keyReleased(KeyEvent event) {
    keyboard.keyReleased(event);
  }

  public void keyTyped(KeyEvent event) {
    char chr = event.getKeyChar();
    if (chr == 'w') {
      if ((event.getModifiers() & KeyEvent.ALT_MASK) != 0) {
        scr.getAudioDriver().setFullSpeed(!scr.getAudioDriver().fullSpeed());
      }
    }
  }

  // -------------------------------------------------------------------
  // Focus listener
  // -------------------------------------------------------------------

  public void focusGained(FocusEvent evt) {
    keyboard.reset();
  }

  public void focusLost(FocusEvent evt) {
    keyboard.reset();
  }

  public boolean isFocusable() {
    // Allows the user to move the focus to the canvas
    // by pressing the tab key.
    return true;
  }

}
