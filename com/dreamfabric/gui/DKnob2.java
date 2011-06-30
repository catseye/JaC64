/*
 * DKnob2.java
 * (c) 2005 by Joakim Eriksson
 *
 * DKnob is a component similar to JSlider but with
 * round "user interface", a knob.
 */
package com.dreamfabric.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;

public class DKnob2 extends JComponent {
 
  private static final long serialVersionUID = -3944978568068155143L;
  private final static float START = 225;
  private final static float LENGTH = 270;
  private final static float PI = (float) 3.1415;
  private final static float START_ANG = (START/360)*PI*2;
  private final static float LENGTH_ANG = (LENGTH/360)*PI*2;
  private final static float MULTIP = 180 / PI;
  private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);

  public static final Color DARK = new Color(0x40, 0x40, 0x40, 0xe0);
  public static final Color DARK_T = new Color(0x40, 0x40, 0x40, 0x80);
  public static final Color DARK_L = new Color(0x80, 0x80, 0x80, 0xa0);
  public static final Color LIGHT_D = new Color(0xa0, 0xa0, 0xa0, 0xa0);
  public static final Color LIGHT = new Color(0xc0, 0xc0, 0xc0, 0xa0);
  public static final Color LIGHT_T = new Color(0xc0, 0xc0, 0xc0, 0x80);

  private float DRAG_SPEED;
  private float CLICK_SPEED;
  private int size;
  private int middle;

  private String label = "";
  private String type = "";

  public final static int SIMPLE = 1;
  public final static int ROUND  = 2;
  private int dragType = ROUND;

  private Polygon knob = new Polygon();

  private final static Dimension MIN_SIZE = new Dimension(30, 40);
  private final static Dimension PREF_SIZE = new Dimension(40, 60);

  // Set the antialiasing to get the right look!
  private final static RenderingHints AALIAS =
    new RenderingHints(RenderingHints.KEY_ANTIALIASING,
		       RenderingHints.VALUE_ANTIALIAS_ON);

  private ChangeEvent changeEvent = null;
  private EventListenerList listenerList = new EventListenerList();

  private Arc2D hitArc = new Arc2D.Float(Arc2D.PIE);

  private float ang = (float) START_ANG;
  private float val;

  private int lowInt = 0;
  private int highInt = 100;
  private int medInt = 0;
  private int divisor = 0;

  private int dragpos = -1;
  private float startVal;
  private Color focusColor;
  private double lastAng;

  public DKnob2(String label) {
    this(label, "");
  }
  public DKnob2(String label, String type) {
    DRAG_SPEED = 0.01F;
    CLICK_SPEED = 0.01F;
    this.label = label;
    this.type = type;

    for (int i = 0, n = 120; i < n; i++) {
      int y = 20 + (int) (0.5 + (Math.sin(i * 3.141592 / 4) * 1.5 + 20) *
		     Math.sin(i * 3.141592 * 2.0 / n));
      int x = 20 + (int) (0.5 + (Math.sin(i * 3.141592 / 4) * 1.5 + 20) *
		     Math.cos(i * 3.141592 * 2.0 / n));
      knob.addPoint(x, y);
    }

    focusColor = DEFAULT_FOCUS_COLOR;

    setPreferredSize(PREF_SIZE);
    setFocusable(true);

    hitArc.setAngleStart(235); // Degrees ??? Radians???
    addMouseListener(new MouseAdapter() {
	public void mousePressed(MouseEvent me) {
	  dragpos = me.getX() + me.getY();
	  startVal = val;

	  // Fix last angle
	  int xpos = middle - me.getX();
	  int ypos = middle - me.getY();
	  lastAng = Math.atan2(xpos, ypos);
	  requestFocus();
	}

	public void mouseClicked(MouseEvent me) {
	  hitArc.setAngleExtent(-(LENGTH + 20));
	  if  (hitArc.contains(me.getX(), me.getY())) {
	    hitArc.setAngleExtent(MULTIP * (ang-START_ANG)-10);
	    if  (hitArc.contains(me.getX(), me.getY())) {
	      decValue();
	    } else incValue();
	  }
	}
      });

    // Let the user control the knob with the mouse
    addMouseMotionListener(new MouseMotionAdapter() {
	public void mouseDragged(MouseEvent me) {
	  if ( dragType == SIMPLE) {
	    float f = DRAG_SPEED * (float)
	      ((me.getX() + me.getY()) - dragpos);
	    setValue(startVal + f);
	  } else if ( dragType == ROUND) {
	    // Measure relative the middle of the button!
	    int xpos = middle - me.getX();
	    int ypos = middle - me.getY();
	    double ang = Math.atan2(xpos, ypos);
	    double diff = lastAng - ang;
	    setValue((float) (getValue() + (diff / LENGTH_ANG)));

	    lastAng = ang;
	  }
	}

	public void mouseMoved(MouseEvent me) {
	}
      });

    // Let the user control the knob with the keyboard
    addKeyListener(new KeyListener() {

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
	  int k = e.getKeyCode();
	  if (k == KeyEvent.VK_RIGHT) {
		incValue();
	  } else if (k == KeyEvent.VK_LEFT) {
		decValue();
	  }
	  }
    });

    // Handle focus so that the knob gets the correct focus highlighting.
    addFocusListener(new FocusListener() {
	public void focusGained(FocusEvent e) {
	  repaint();
	}
	public void focusLost(FocusEvent e) {
	  repaint();
	}
      });
  }

  public void setDragType(int type) {
    dragType = type;
  }
  public int getDragType() {
    return dragType;
  }

  private void incValue() {
    setValue(val + CLICK_SPEED);
  }

  private void decValue() {
    setValue(val - CLICK_SPEED);
  }

  public float getValue() {
    return val;
  }

  public int getIntValue() {
    if (medInt > 0) {
      if (val > 0.5) {
	return medInt + (int) (2 * (val - 0.5) * (highInt - medInt));
      }
      return lowInt + (int) (2 * val * (medInt - lowInt));
    }

    return lowInt + (int) (val * (highInt - lowInt));
  }

  public void setIntValue(int val) {
    if (medInt > 0) {
      if (val > medInt) {
	setValue(0.5f + 0.5f * (val - medInt) / (highInt - medInt));
      } else {
	setValue(0.5f * (val - lowInt) / (medInt - lowInt));
      }
    } else {
      setValue(1.0f * (val - lowInt) / (highInt - lowInt));
    }
  }

  public void setValue(float val) {
    if (val < 0) val = 0;
    if (val > 1) val = 1;
    this.val = val;
    ang = START_ANG - (float) LENGTH_ANG * val;
    repaint();
    fireChangeEvent();
  }


  public void addChangeListener(ChangeListener cl) {
    listenerList.add(ChangeListener.class, cl);
  }

  public void removeChangeListener(ChangeListener cl) {
    listenerList.remove(ChangeListener.class, cl);
  }

  public Dimension getMinimumSize() {
    return MIN_SIZE;
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


  // Paint the DKnob
  public void paint(Graphics g) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D) g;

      int width = getWidth();
      int height = getHeight() - 10;
      size = Math.min(width, height) - 5;
      middle = size/2;

      g2d.setBackground(getParent().getBackground());
      g2d.addRenderingHints(AALIAS);

      // For the size of the "mouse click" area
      hitArc.setFrame(1, 1, size + 12, size + 12);

      g2d.scale(size / 30.0, size / 30.0);
      drawKnob(g2d);
    }
  }

  private void drawKnob(Graphics2D g2d) {
    g2d.scale(0.5,0.5);
    g2d.drawString(label, 2, 10);
    g2d.scale(2,2);
    g2d.translate(0,6);

    if (hasFocus()) {
      g2d.setColor(focusColor);
      g2d.fill(new Arc2D.Double(0.0, 0.0, 27.0, 27.0,
				90.0, 360.0, Arc2D.OPEN));
    }
    g2d.setColor(DARK);
    g2d.fill(new Arc2D.Double(1.0, 1.0, 25.0, 25.0,
			      90.0, 360.0, Arc2D.OPEN));
    g2d.setColor(DARK_L);
    g2d.fill(new Arc2D.Double(2.0, 2.0, 22.0, 22.0,
				90.0, 360.0, Arc2D.OPEN));
    g2d.setColor(LIGHT_D);
    g2d.draw(new Arc2D.Double(3.0, 3.0, 21.0, 21.0,
			      75.0, 120.0, Arc2D.OPEN));
    g2d.setColor(LIGHT);
    g2d.draw(new Arc2D.Double(4.0, 4.0, 20.0, 20.0,
			      122.5, 25.0, Arc2D.OPEN));

    g2d.setColor(DARK);
    g2d.fill(new Arc2D.Double(5.0, 5.0, 18.0, 18.0,
			      90.0, 360.0, Arc2D.OPEN));

    g2d.setColor(DARK_T);
    g2d.fill(new Arc2D.Double(9.0, 9.0, 18.0, 18.0,
			      90.0, 360.0, Arc2D.OPEN));
    g2d.setColor(DARK_L);
    g2d.fill(new Arc2D.Double(6.0, 6.0, 16.0, 16.0,
			      90.0, 360.0, Arc2D.OPEN));
    g2d.setColor(DARK_T);
    g2d.draw(new Arc2D.Double(7.0, 7.0, 14.0, 14.0,
			      270.0, 90.0, Arc2D.OPEN));
    g2d.setColor(LIGHT_D);
    g2d.draw(new Arc2D.Double(7.0, 7.0, 15.0, 15.0,
			      90.0, 90.0, Arc2D.OPEN));

    g2d.setColor(LIGHT_D);
    g2d.fill(new Arc2D.Double(7.0, 7.0, 14.0, 14.0,
			      180 * ang / 3.141592 - 60, 120.0, Arc2D.OPEN));
    g2d.setColor(DARK);
    g2d.draw(new Arc2D.Double(6.0, 6.0, 16.0, 16.0,
			      180 * ang / 3.141592 - 1, 2.0, Arc2D.PIE));

    g2d.setColor(DARK);
    g2d.draw(new Arc2D.Double(5.0, 5.0, 18.0, 18.0,
			      180 * ang / 3.141592 - 8, 16.0, Arc2D.OPEN));
    g2d.draw(new Arc2D.Double(4.0, 4.0, 20.0, 20.0,
			      180 * ang / 3.141592 - 4, 8.0, Arc2D.OPEN));

    g2d.scale(0.5,0.5);
    if (divisor != 0) {
      g2d.drawString("" +
		     (getIntValue() * 1.0 / divisor)  + " " + type, 10, 64);
    } else {
      g2d.drawString("" + getIntValue()  + " " + type, 10, 64);
    }

  }

  public void setInterval(int low, int high) {
    this.lowInt = low;
    this.highInt = high;
  }


  public void setInterval(int low, int med, int high) {
    this.lowInt = low;
    this.highInt = high;
    this.medInt = med;
  }

  // Just used for printing the label...
  public void setDivisor(int divisor) {
    this.divisor = divisor;
  }

  public static void main(String[] args)  {
    JFrame win = new JFrame("DTest!");
    win.getContentPane().setLayout(new BorderLayout());
    win.setSize(120,140);

    JPanel volumePanel = new JPanel(new BorderLayout());
    volumePanel.setBackground(new Color(180,180,195));
    win.getContentPane().add(volumePanel, BorderLayout.CENTER);

    DKnob2 ts;
    volumePanel.add(ts = new DKnob2("Volume", ""), BorderLayout.CENTER);
    ts.setInterval(0,15);
    win.setVisible(true);
  }
}
