/**
 * Describe class DCheckBox here.
 * (c) 2005 Joakim Eriksson
 *
 * Created: Sat Nov 05 19:31:11 2005
 *
 */

package com.dreamfabric.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class DCheckBox extends JComponent {


  private static final long serialVersionUID = -8566093291874184047L;

// Set the antialiasing to get the right look!
  private final static RenderingHints AALIAS =
    new RenderingHints(RenderingHints.KEY_ANTIALIASING,
		       RenderingHints.VALUE_ANTIALIAS_ON);

  private final static Dimension MIN_SIZE = new Dimension(14, 14);

  private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);
  private static final Color COLOR_LIGHT = new Color(0xe0,0xe0,0xe0,0x60);
  private static final Color COLOR_SELECTED = new Color(0xf0,0x40,0x40,0x80);
  private static final Color COLOR_SELECTED_LIGHT =
    new Color(0xff,0x60,0x60,0xa0);
  private Color focusColor;

  private ChangeEvent changeEvent = null;
  private EventListenerList listenerList = new EventListenerList();


  private boolean selected = false;
  /**
   * Creates a new <code>DCheckBox</code> instance.
   *
   */
  public DCheckBox() {

    setFocusable(true);

    focusColor = DEFAULT_FOCUS_COLOR;

    // Handle focus so that the knob gets the correct focus highlighting.
    addFocusListener(new FocusListener() {
	public void focusGained(FocusEvent e) {
	  repaint();
	}
	public void focusLost(FocusEvent e) {
	  repaint();
	}
      });

    // Let the user control the selection with the keyboard
    addKeyListener(new KeyListener() {

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
	  int k = e.getKeyCode();
	  if (k == KeyEvent.VK_SPACE) {
	    selected = !selected;
	    repaint();
	    fireChangeEvent();
	  }
	}
      });

    // Activate focus when clicked
    addMouseListener(new MouseAdapter() {
	public void mousePressed(MouseEvent me) {
	  requestFocus();
	  if (me.getX() < 14 && me.getY() < 14) {
	    selected = !selected;
	    fireChangeEvent();
	  }
	  repaint();
	}

	public void mouseClicked(MouseEvent me) {
	}
      });


    setPreferredSize(MIN_SIZE);

  }

  public void setSelected(boolean sel ) {
    if (selected != sel) {
      selected = sel;
      repaint();
    }
  }

  public boolean isSelected() {
    return selected;
  }

  public void paint(Graphics g) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D) g;

      g2d.setBackground(getParent().getBackground());
      g2d.addRenderingHints(AALIAS);

      g2d.translate(1,1);
      // Draw the Check box LED
      drawCheckBox(g2d);
    }
  }

  void drawCheckBox(Graphics2D g2d) {
    if (hasFocus()) {
      g2d.setColor(focusColor);
      g2d.fillOval(0,0,12,12);
    }
    g2d.setColor(Color.black);
    g2d.fillOval(2,2,8,8);

    if (selected) {
      g2d.setColor(COLOR_SELECTED);
      g2d.fillOval(1,1,10,10);
      g2d.setColor(COLOR_SELECTED_LIGHT);
      g2d.fillOval(3,3,6,6);
    }

    g2d.setColor(COLOR_LIGHT);
    g2d.fillOval(3,3,5,5);
    g2d.fillOval(4,4,2,2);

  }


  // -------------------------------------------------------------------
  // Change listener management
  // -------------------------------------------------------------------

  public void addChangeListener(ChangeListener cl) {
    listenerList.add(ChangeListener.class, cl);
  }

  public void removeChangeListener(ChangeListener cl) {
    listenerList.remove(ChangeListener.class, cl);
  }

  protected void fireChangeEvent() {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == ChangeListener.class) {
	// Lazily create the event:
	if (changeEvent == null)
	  changeEvent = new ChangeEvent(this);
	((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
      }
    }
  }


}
