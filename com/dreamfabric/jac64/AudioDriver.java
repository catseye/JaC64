/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (JaC64.com Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.jac64.com/ 
 * http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.jac64;


public abstract class AudioDriver {

  public abstract void init(int sampleRate, int bufferSize);
  public abstract void write(byte[] buffer);
  public abstract long getMicros();
  public abstract boolean hasSound();
  public abstract int available();
  public abstract int getMasterVolume();
  public abstract void setMasterVolume(int v);
  public abstract void shutdown();
  public abstract void setSoundOn(boolean on);
  public abstract void setFullSpeed(boolean full);
  public abstract boolean fullSpeed();

}
