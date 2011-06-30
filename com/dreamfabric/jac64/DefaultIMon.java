/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 * Describe class DefaultIMon here.
 *
 *
 * Created: Tue Aug 02 09:21:17 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */

public class DefaultIMon implements IMonitor {
  int level = 0;
  String prefix = "";

 // -------------------------------------------------------------------
  // Imonitor - ignore for now...
  // -------------------------------------------------------------------
  public void init(MOS6510Core cpu) {
  }

  public void setEnabled(boolean b) {
  }

  public boolean isEnabled() {
    return false;
  }

  public void info(Object o) {
    output((String) o);
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public void warning(Object o) {
    output((String) o);
  }

  public void error(Object o) {
    output((String) o);
  }

  private void output(String s) {
    if (prefix != null) {
      if (s.startsWith(prefix)) {
	System.out.println(s);
      }
    } else {
      System.out.println(s);
    }
  }

  public void disAssemble(int[] memory, int pc, int acc, int x, int y,
			  byte status, int interruptInExec, int lastI) {}
}
