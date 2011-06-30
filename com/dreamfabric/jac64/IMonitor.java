/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 * @author jblok
 */

public interface IMonitor {

  public void init(MOS6510Core cpu);

  public void setEnabled(boolean b);

  public boolean isEnabled();//test for expensive operations

  public void info(Object o);

  public void warning(Object o);

  public void error(Object o);

  public int getLevel();

  public void setLevel(int level);

  public void disAssemble(int[] memory, int pc, int acc, int x, int y,
			  byte status, int interruptInExec,
			  int lastInterrupt);
}
