/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (JaC64.com Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.jac64.com/ 
 * http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.jac64;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

public class AudioDriverSE extends AudioDriver {

  private SourceDataLine dataLine;
  private FloatControl volume;
  private int vol = 0;
  private boolean soundOn = true;
  private boolean fullSpeed = false;
  
  public int available() {
    if (dataLine == null)
      return 0;
    return dataLine.available();
  }

  public int getMasterVolume() {
    return vol;
  }

  public long getMicros() {
    if (dataLine == null)
      return 0;
    return dataLine.getMicrosecondPosition();
  }

  public boolean hasSound() {
    return dataLine != null;
  }

  public void init(int sampleRate, int bufferSize) {
//  Allocate Audio resources
    AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, false);
    DataLine.Info dli =
      new DataLine.Info(SourceDataLine.class, af, bufferSize);
    try {
      dataLine = (SourceDataLine) AudioSystem.getLine(dli);
      if (dataLine == null)
        System.out.println("DataLine: not existing...");
      else {
        System.out.println("DataLine allocated: " + dataLine);
        dataLine.open(dataLine.getFormat(), bufferSize);
        volume = (FloatControl)
        dataLine.getControl(FloatControl.Type.MASTER_GAIN);
        setMasterVolume(100);

        // Startup the dataline
        dataLine.start();
      }
    } catch (Exception e) {
      System.out.println("Problem while getting data line ");
      e.printStackTrace();
      dataLine = null;
    }  
  }
  
  public void setMasterVolume(int v) {
    if (volume != null) {
      volume.setValue(-10.0f + 0.1f * v);
    }
    vol = v;
  }

  public void shutdown() {
    dataLine.close();
  }

  public void write(byte[] buffer) {
    if (dataLine == null)
      return;
    int bsize = buffer.length;
    if (!fullSpeed) {
      while (dataLine.available() < bsize)
        try {
          Thread.sleep(1);
        } catch (Exception e) {
        }
    } else if (dataLine.available() < bsize) {
      return;
    }
    if (!soundOn) {
      // Kill sound!!!
      for (int i = 0; i < buffer.length; i++) {
        buffer[i] = 0;
      }
    }
    dataLine.write(buffer, 0, bsize);
  }

  public void setSoundOn(boolean on) {
    soundOn = on;
  }

  public void setFullSpeed(boolean full) {
    fullSpeed = full;
  }

  public boolean fullSpeed() {
    return fullSpeed;
  }
}
