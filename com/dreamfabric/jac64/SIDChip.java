/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (JaC64.com Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.jac64.com/ 
 * http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 * SIDChip - implements all neccessary control and set-up for the SID
 * chip emulation.
 * @author Joakim
 */
public class SIDChip extends ExtChip {

  public static final boolean SOUND_AVAIABLE = true;
  public static final int IO_OFFSET = CPU.IO_OFFSET;

  SIDVoice6581 sid[];
  SIDMixer mixer;
  private int sidUpdate = 1000;
  private boolean killEvent = false;
  
  private TimeEvent sidEvent = new TimeEvent(0, "JaC SID Chip") {
    public void execute(long cycles) {
      mixer.updateSound(cycles);
      time = time + sidUpdate; // Each milli second (1000) at 50 fps
      if (time < cpu.cycles)
        time = cpu.cycles + 10;
      if (!killEvent) 
        cpu.scheduler.addEvent(this);
    }
  };
  
  public SIDChip(MOS6510Core cpu, AudioDriver driver) {
    init(cpu);
    if (SOUND_AVAIABLE) {
      try {
        System.out.println("Creating SID configuration");
        sid = new SIDVoice6581[3];
        sid[0] = new SIDVoice6581(cpu.memory, IO_OFFSET + 0xd400 );
        sid[0].init();
        sid[1] = new SIDVoice6581(cpu.memory, IO_OFFSET + 0xd400 + 7);
        sid[1].init();
        sid[2] = new SIDVoice6581(cpu.memory, IO_OFFSET + 0xd400 + 14);
        sid[2].init();
        sid[0].next = sid[2];
        sid[1].next = sid[0];
        sid[2].next = sid[1];
        mixer = new SIDMixer(sid, null, driver);
        driver.setMasterVolume(100);
        sidEvent.time = cpu.cycles + 10;
        cpu.scheduler.addEvent(sidEvent);
      } catch (Throwable e) {
        e.printStackTrace();
        sid = null;
      }
    }
  }
  
  public void clock(long cycles) {
  }

  public int performRead(int address, long cycles) {
    switch (address - IO_OFFSET) {
    case 0xd41b: //
      return (sid[2].lastSample()) & 0xff;
    case 0xd41c: //
      return sid[2].adsrVol & 0xff;
      // Pot XY taken care of in C64Screen for now...
    }
    return 0;
  }

  public void performWrite(int address, int data, long cycles) {    
    switch(address - IO_OFFSET) {
    case 0xd404 :
      sid[0].setControl(data, cpu.cycles);
      break;
    case 0xd400 + 5:
      sid[0].setAD(data, cpu.cycles);
      break;
    case 0xd400 + 6:
      sid[0].setSR(data, cpu.cycles);
      break;
    case 0xd402:
    case 0xd403:
      sid[0].updatePulseWidth(cycles);
      break;

    case 0xd40b :
      sid[1].setControl(data, cpu.cycles);
      break;
    case 0xd407 + 5:
      sid[1].setAD(data, cpu.cycles);
      break;
    case 0xd407 + 6:
      sid[1].setSR(data, cpu.cycles);
      break;
    case 0xd402 + 7:
    case 0xd403 + 7:
      sid[1].updatePulseWidth(cycles);
      break;

    case 0xd412 :
      sid[2].setControl(data, cpu.cycles);
      break;
    case 0xd40e + 5:
      sid[2].setAD(data, cpu.cycles);
      break;
    case 0xd40e + 6:
      sid[2].setSR(data, cpu.cycles);
      break;

    case 0xd402 + 14:
    case 0xd403 + 14:
      sid[2].updatePulseWidth(cycles);
      break;


      // Controls for the SID Mixer
    case 0xd415:
      mixer.setFilterCutoffLO(data & 7);
      break;
    case 0xd416:
      mixer.setFilterCutoffHI(data);
      break;
    case 0xd417:
      mixer.setFilterResonance(data >> 4);
      mixer.setFilterOn(data & 0x0f);
      break;
    case 0xd418 :
      mixer.setVolume(data & 0x0f, cpu.cycles);
      mixer.setFilterCtrl(data);
      break;
   }
  }
    
  public void reset() {
    mixer.reset();
    sidEvent.time = cpu.cycles;
  }

  public void stop() {
    killEvent = true;
  }
}
