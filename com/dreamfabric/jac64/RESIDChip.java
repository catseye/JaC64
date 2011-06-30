/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (JaC64.com Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.jac64.com/
 * http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.jac64;

import resid.ISIDDefs;
import resid.SID;
import resid.ISIDDefs.sampling_method;

/**
 * SIDChip - implements all neccessary control and set-up for the SID
 * chip emulation.
 * @author Joakim
 */
public class RESIDChip extends ExtChip {

  static final int SAMPLE_RATE = 44000;
  static final int DL_BUFFER_SIZE = 44000;

  int BUFFER_SIZE = 256;
  byte[] buffer = new byte[BUFFER_SIZE * 2];

  SID sid;
  int CPUFrq = 985248;
  int clocksPerSample = CPUFrq / SAMPLE_RATE;
  int clocksPerSampleRest = 0;
  long nextSample = 0;
  long lastCycles = 0;
  private int nextRest = 0;
  private int pos = 0;
  private AudioDriver driver;
  private boolean removeSample = false;

  TimeEvent sampleEvent = new TimeEvent(0, "resid event") {
    public void execute(long cycles) {
      nextSample += clocksPerSample;
      nextRest += clocksPerSampleRest;
      if (nextRest > 1000) {
        nextRest -= 1000;
        nextSample++;
      }
      // Clock resid!
      while(lastCycles < cycles) {
        sid.clock();
        lastCycles++;
      }
      // and take the sample!
      int sample = sid.output();
      buffer[pos++] = (byte) (sample & 0xff);
      buffer[pos++] = (byte) ((sample >> 8));
      if (pos == buffer.length) {
        writeSamples();
      }
      time = nextSample;
      if (!removeSample)
        cpu.scheduler.addEvent(this);
    }
  };

  
  public RESIDChip(MOS6510Core cpu, AudioDriver audio) {
    init(cpu);
    // Assume 44 Khz sample rate for now... later this must be handled...
    driver = audio;
    sid = new SID();
    lastCycles = cpu.cycles;
    nextSample = cpu.cycles;

    sid.set_sampling_parameters(CPUFrq, sampling_method.SAMPLE_FAST, SAMPLE_RATE, -1, 0.97);

    clocksPerSampleRest = (int) ((CPUFrq * 1000L) / SAMPLE_RATE);
    clocksPerSampleRest -= clocksPerSample * 1000;
    System.out.println("ClocksPer Sample: " + clocksPerSample + "." + clocksPerSampleRest);
    sampleEvent.time = cpu.cycles + 5;
    cpu.scheduler.addEvent(sampleEvent);
  }

  public void clock(long cycles) { }

  private void writeSamples() {
    driver.write(buffer);
    pos = 0;
  }

  public int performRead(int address, long cycles) {
    return sid.read(address - CPU.IO_OFFSET - 0xd400);
  }

  public void performWrite(int address, int data, long cycles) {
    sid.write(address - CPU.IO_OFFSET - 0xd400, data);
  }

  public void reset() {
    nextSample = cpu.cycles + 10;
    lastCycles = cpu.cycles;
    cpu.scheduler.addEvent(sampleEvent, nextSample);
    sid.reset();
  }

  public void stop() {
    // Called from any thread!
    removeSample = true;
  }

  public void setChipVersion(int version) {
    if (version == C64Screen.RESID_6581)
      sid.set_chip_model(ISIDDefs.chip_model.MOS6581);
    else
      sid.set_chip_model(ISIDDefs.chip_model.MOS8580);
  }
}
