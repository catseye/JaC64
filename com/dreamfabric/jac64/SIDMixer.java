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
 * This mixes four channels of SID sounds (or three)
 *
 * 0 - 2 => 16 bits channels from the SID chip
 *
 * 3 => 8 bits from PSID sample player
 *
 *
 * Created: Fri Oct 28 17:45:19 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public class SIDMixer {

  public static int BYTES_PER_SAMPLE = 2;

  public static final boolean NO_SOUND = false;

  public static final boolean DEBUG = false;
  // 16 bits, 1 second buffer
  public static int DL_BUFFER_SIZE = SIDVoice6581.SAMPLE_RATE * 2;
  // public static final int SYNCH_TIME = 1000 / 50;
  public static final int SYNCH_BUFFER = ((SIDVoice6581.SAMPLE_RATE * 2) / 50);

  public static final int EFX_NONE = 0;
  public static final int EFX_FLANGER_1 = 1;
  public static final int EFX_FLANGER_2 = 2;
  public static final int EFX_FLANGER_3 = 3;
  public static final int EFX_PHASER_1 = 4;
  public static final int EFX_PHASER_2 = 5;
  public static final int EFX_CHORUS_1 = 6;
  public static final int EFX_CHORUS_2= 7;
  public static final int EFX_ECHO_1 = 8;
  public static final int EFX_ECHO_2 = 9;
  public static final int EFX_REV_SMALL = 10;
  public static final int EFX_REV_MED = 11;
  public static final int EFX_REV_LARGE = 12;
  public static final int EFX_FSWEEP = 13;
  public static final int EFX_FSWEEP_RES = 14;

  private String[] efxNames = {
      "EFX_NONE",
      "EFX_FLANGER_1",
      "EFX_FLANGER_2",
      "EFX_FLANGER_3",
      "EFX_PHASER_1",
      "EFX_PHASER_2",
      "EFX_CHORUS_1",
      "EFX_CHORUS_2",
      "EFX_ECHO_1",
      "EFX_ECHO_2",
      "EFX_REV_SMALL",
      "EFX_REV_MED",
      "EFX_REV_LARGE",
      "EFX_FSWEEP",
      "EFX_FSWEEP_RES",
  };

  // if set this will never block...
  public boolean fullSpeed = false;

  private SIDVoice psid;
  private SIDVoice6581[] channels;
  private boolean soundOn = true;

  private SIDMixerListener listener = null;
  private AudioDriver driver;

  // Fixed size buffers
  byte[] buffer = new byte[SIDVoice6581.GENLEN * 2];
  byte[] syncBuffer = new byte[4096];
  int[] intBuffer = new int[SIDVoice6581.GENLEN];
  int[] noFltBuffer = new int[SIDVoice6581.GENLEN];



  // Effects buffers and variables
  boolean effects = false;
//boolean moog = false;
  public static final int LFO_WAVELEN = 500;

  int[] echo;
  int echoSize = 0;
  int echoPos = 0;
  int echoLFODiff = 0;
  int echoLFODiffMax = 0;
  int echoLFODepth = 50;
  int echoFeedback = 0;
  int echoLFOSpeed = 0;
  int echoLFOPos = 0;
  int echoDW = 50;

  int maxefx;
  int minefx;

  int sidVol = 15;

  int sidVolArr[] = new int[SIDVoice6581.GENLEN];
  long lastCycles = 0;

  // Filter (some stuff public for viewing )
  int filterVal = 0;
  public int cutoff = 0;
  public int resonance = 0;
  int filterOn = 0;

  int masterVolume = 100;

  boolean lpOn = false;
  boolean hpOn = false;
  boolean bpOn = false;

  // SIDFilter
  long vlp;
  long vhp;
  long vbp;
  long w0;
  long div1024Q;

  // ExtFilter
  long exVlp;
  long exVhp;
  long exVo;
  long exw0lp;
  long exw0hp;


  int irq = 0;

  int[] sine10Hz;

//private MoogFilter moogFilter;
//private int moogResonance = 0;
//private int moogCutoff = 0;
//private int mdep = 0;

  public SIDMixer() {
  }

  /**
   * Creates a new <code>SIDMixer</code> instance.
   *
   *
   */
  public SIDMixer(SIDVoice6581[] channels, SIDVoice sample, AudioDriver driver) {
    init(channels, sample, driver);
  }

  public void init(SIDVoice6581[] channels, SIDVoice sample, AudioDriver driver) {
    this.channels = channels;
    psid = sample;
    this.driver = driver;

//  moogFilter = new MoogFilter(SID6581.SAMPLE_RATE);
//  moogFilter.setFilterParams(120, 0.5);

    System.out.println("Micros per SIDGen: " + getMicrosPerGen());

    // From resid... (~1 Mhz clock...) Extfilter
    exw0hp = 105;
    exw0lp = 104858;

    sine10Hz = new int[LFO_WAVELEN];
    for (int i = 0, n = LFO_WAVELEN; i < n; i++) {
      sine10Hz[i] = (int)
      (500 + (500 * Math.sin(i * 2 * 3.1415 / LFO_WAVELEN)));
    }

    setEFX(EFX_ECHO_1);
    setEFX(EFX_NONE);
  }

  public void setListener(SIDMixerListener front) {
    listener = front;
  }

  // -------------------------------------------------------------------
  // Sound effects after the mixer!
  // -------------------------------------------------------------------
  public void setEchoTime(int millisDelay) {
    int sampleSize = millisDelay * SIDVoice6581.SAMPLE_RATE / 1000;
    System.out.println("SamplesDelay: " + sampleSize);
    echoSize = sampleSize;
    echo = new int[echoSize];
    echoLFODiffMax = (echoSize * echoLFODepth) / 110;
    echoLFODiff = 0;
    echoPos = 0;
  }

  public int getEchoTime() {
    return (1000 * echoSize) / SIDVoice6581.SAMPLE_RATE;
  }

  public int getEFXCount() {
    return efxNames.length;
  }

  public String getEFXName(int efx) {
    return efxNames[efx];
  }

  public void setEchoFeedback(int percen) {
    echoFeedback = percen;
  }

  public int getEchoFeedback() {
    return echoFeedback;
  }

  public void setEchoLFOSpeed(int hzd10) {
    // 10Hz wave => hz * 10 for speed in that...
    echoLFOSpeed = hzd10;
  }

  public int getEchoLFOSpeed() {
    return echoLFOSpeed;
  }

  public void setEchoDW(int percent) {
    echoDW = percent;
  }

  public int getEchoDW() {
    return echoDW;
  }

  public void setEchoLFODepth(int percent) {
    echoLFODepth = percent;
    echoLFODiffMax = (echoSize * echoLFODepth) / 110;
  }

  public int getEchoLFODepth() {
    return echoLFODepth;
  }

  public boolean getEffectsOn() {
    return effects;
  }

  public void setEFX(int fx) {
    fx = fx % efxNames.length;
    effects = true;
//  moog = false;
    switch (fx) {
    case  EFX_NONE:
      effects = false;
      break;
    case EFX_FLANGER_1:
      setEchoTime(5);
      setEchoFeedback(75);
      setEchoLFOSpeed(1);
      setEchoLFODepth(35);
      setEchoDW(33);
      break;
    case EFX_FLANGER_2:
      setEchoTime(15);
      setEchoFeedback(70);
      setEchoLFOSpeed(5);
      setEchoLFODepth(35);
      setEchoDW(35);
      break;
    case EFX_FLANGER_3:
      setEchoTime(2);
      setEchoFeedback(85);
      setEchoLFOSpeed(3);
      setEchoLFODepth(55);
      setEchoDW(30);
      break;
    case EFX_PHASER_1:
      setEchoTime(10);
      setEchoFeedback(0);
      setEchoLFOSpeed(1);
      setEchoLFODepth(75);
      setEchoDW(50);
      break;
    case EFX_PHASER_2:
      setEchoTime(3);
      setEchoFeedback(0);
      setEchoLFOSpeed(2);
      setEchoLFODepth(85);
      setEchoDW(40);
      break;
    case EFX_CHORUS_1:
      setEchoTime(25);
      setEchoFeedback(60);
      setEchoLFOSpeed(1);
      setEchoLFODepth(35);
      setEchoDW(35);
      break;
    case EFX_CHORUS_2:
      setEchoTime(30);
      setEchoFeedback(50);
      setEchoLFOSpeed(5);
      setEchoLFODepth(25);
      setEchoDW(35);
      break;
    case EFX_ECHO_1:
      setEchoTime(150);
      setEchoFeedback(0);
      setEchoLFOSpeed(0);
      setEchoLFODepth(0);
      setEchoDW(33);
      break;
    case EFX_ECHO_2:
      setEchoTime(300);
      setEchoFeedback(33);
      setEchoLFOSpeed(0);
      setEchoLFODepth(0);
      setEchoDW(45);
      break;
    case EFX_REV_SMALL:
      setEchoTime(70);
      setEchoFeedback(40);
      setEchoLFOSpeed(0);
      setEchoLFODepth(0);
      setEchoDW(33);
      break;
    case EFX_REV_MED:
      setEchoTime(130);
      setEchoFeedback(50);
      setEchoLFOSpeed(0);
      setEchoLFODepth(0);
      setEchoDW(33);
      break;
    case EFX_REV_LARGE:
      setEchoTime(100);
      setEchoFeedback(70);
      setEchoLFOSpeed(0);
      setEchoLFODepth(0);
      setEchoDW(40);
      break;
//    case EFX_FSWEEP:
//    effects = false;
//    moog = true;
//    moogResonance = 30;
//    mdep = 60;
//    mfrq = 1;
//    moogCutoff = 1000;
//    break;
//    case EFX_FSWEEP_RES:
//    effects = false;
//    moog = true;
//    moogResonance = 75;
//    mdep = 70;
//    mfrq = 4;
//    moogCutoff = 500;
//    break;
    }

    if (listener != null) {
      listener.updateValues();
    }
  }

  public void setEffectsOn(boolean fx) {
    effects = fx;
  }

  public boolean isEffectsOn() {
    return effects;
  }

  // -------------------------------------------------------------------
  // Control for the SID Mixer
  // -------------------------------------------------------------------

  public void setFilterCutoffLO(int data) {
    cutoff = cutoff & 0xff8 | (data & 0x07);
    recalcFilter();
  }

  public void setFilterCutoffHI(int data) {
    cutoff = cutoff & 0x07 | (data << 3);
    recalcFilter();
  }

  public void setFilterResonance(int data) {
    resonance = data;
    recalcFilter();
  }

  public void setFilterCtrl(int data) {
    lpOn = (data & 0x10) > 0;
    bpOn = (data & 0x20) > 0;
    hpOn = (data & 0x40) > 0;
    if (DEBUG) {
      System.out.println("Filter: " + data + " => " + (lpOn ? "LP " : "") +
          (bpOn ? "BP " : "") + (hpOn ? "HP " : ""));
    }
    recalcFilter();
  }

  public void setFilterOn(int on) {
    filterOn = on;
  }

  public void setMoogFilterOn(boolean on) {
//  moog = on;
  }

  public boolean isMoogFilterOn() {
    return false; // moog;
  }

  public void setMoogResonance(int percent) {
//  moogResonance = percent;
  }

  public int getMoogResonance() {
    return 0; //moogResonance;
  }

  public void setMoogCutoff(int frq) {
//  moogCutoff = frq;
  }

  public int getMoogCutoff() {
    return 0;// moogCutoff;
  }

  public void setMoogSpeed(int hz10) {
//  mfrq = hz10;
  }

  public int getMoogSpeed() {
    return 0;//mfrq;
  }

  public void setMoogDepth(int dep) {
    //mdep = dep;
  }

  public int getMoogDepth() {
    return 0; //mdep;
  }

  public void setVolume(int vol) {
    sidVol = vol;
  }

  public void setVolume(int vol, long cycles) {
    if (lastCycles > 0) {
      int pos = (int) ((cycles - lastCycles) /
          (SIDVoice6581.CYCLES_PER_SAMPLE + 10));
      sidVolArr[pos % SIDVoice6581.GENLEN] = vol;
    } else {
      sidVol = vol;
    }
  }

  private void recalcFilter() {
    int fCutoff = 30 + (12000 * cutoff / 2048);
    w0 = (long) (2 * Math.PI * fCutoff * 1.048576);
    div1024Q = (int) (1024.0 / (0.707 + 1.0 * resonance / 15));
  }


  public void stop() {
    setVolume(0);
  }

  public void reset() {
    exVo = 0;
    exVhp = 0;
    exVlp = 0;

    cutoff = 0;
    resonance = 0;
    w0 = 0;
    div1024Q = 0;
    filterOn = 0;

    maxefx = 0;
    minefx = 0;

    for (int i = 0, n = SIDVoice6581.GENLEN; i < n; i++) {
      sidVolArr[i] = -1;
    }
    setVolume(15);
    recalcFilter();
  }


  public void printStatus() {
    System.out.println("SIDMixer  ----------------------------");
    System.out.println("Volume: " + sidVol);
    System.out.println("FilterOn: " + filterOn);
    System.out.println("Cutoff: " + cutoff);
    System.out.println("Resonance: " + resonance);

    System.out.println("Max Efx:" + maxefx);
    System.out.println("Min Efx:" + minefx);
//  moogFilter.printStatus();

  }

  public void setFullSpeed(boolean fs) {
    System.out.println("Set full speed: " + fs);
    fullSpeed = fs;
  }

  public boolean fullSpeed() {
    return fullSpeed;
  }

  public int[] getBuffer() {
    return intBuffer;
  }



  public static final int SLEEP_SYNC = 1;
  private int sleep = 100;
  private int syncMode = 0;//SLEEP_SYNC;
  private double avg = 10;
  private long lastTime = System.currentTimeMillis();
  private long micros = 0;

  public boolean updateSound(long cycles) {
    // For all the normal SID generator channels generate the sound!
    // and mix it into the intBuffer

    // should return true when irq!!!
    irq++;
    boolean trueIRQ = (irq % SIDVoice6581.GENS_PER_IRQ) == 0;

    // How do we delay the short time that we should delay here???
    if (trueIRQ) {
      if (syncMode == SLEEP_SYNC && !fullSpeed) {
        long elapsed = System.currentTimeMillis() - lastTime;
        lastTime = System.currentTimeMillis();
        avg = 0.99 * avg + 0.01 * elapsed;
        // avg should be 20 =>
        // 20 = sleep + X
        // avg = sleep + X
        // sleep = avg - X
        if (avg < 20) sleep++;
        if (avg > 20) sleep--;
        System.out.println("Avg: " + avg + " Sleep: " + sleep / 10.0 + " " +
            (driver.getMicros() - micros));
        double slm = 19.75 - ((driver.getMicros() - micros) / 1000.0);
        try {
          Thread.sleep((int) slm);
        } catch (Exception e) {
          e.printStackTrace();
        }
        micros = driver.getMicros();
      }
    }


    if (!driver.hasSound()) {
      // No sound - no continue...
      return trueIRQ;
    }

    // If either fullspeed or SLEEP_SYNC then check if there are room
    // for more samples!
    if (fullSpeed || syncMode == SLEEP_SYNC) {
      if (driver.available() < SIDVoice6581.GENLEN * 2) {
        return false;
      }
    }

    lastCycles = cycles;

    if (soundOn) {
      int [] tBuf;
      if (psid != null) {
        tBuf = psid.generateSound(cycles);
        for (int j = 0, m = SIDVoice6581.GENLEN; j < m; j++) {
          intBuffer[j] = 0;
          // From a byte array (low 8 bits)
          noFltBuffer[j] = (tBuf[j] << 4);
        }
      } else {
        for (int j = 0, m = SIDVoice6581.GENLEN; j < m; j++) {
          intBuffer[j] = 0;
          noFltBuffer[j] = 0;
        }
      }

      for (int i = 0, n = channels.length; i < n; i++) {
        int[] buf = channels[i].generateSound(cycles);

        if ((filterOn & (1 << i)) > 0)
          tBuf = intBuffer;
        else
          tBuf = noFltBuffer;

        for (int j = 0, m = SIDVoice6581.GENLEN; j < m; j++) {
          tBuf[j] += buf[j] >> 2;
        }
      }

      // Internal SID emulation processing
      // Filters...

      for (int i = 0, n = SIDVoice6581.GENLEN; i < n; i++) {
        int inval = intBuffer[i];

        // SID filter - filter shuold be fed even if not on...
        if (filterOn > 0) {
          // 8 + 8 + 7 => 23 which is the number of cycles per sample
          vbp -= 8 * w0 * vhp >> 20;
      vlp -= 8 * w0 * vbp >> 20;
          vhp = (vbp * div1024Q >> 10) - vlp - inval;

          vbp -= 8 * w0 * vhp >> 20;
    vlp -= 8 * w0 * vbp >> 20;
    vhp = (vbp * div1024Q >> 10) - vlp - inval;

    vbp -= 7 * w0 * vhp >> 20;
    vlp -= 7 * w0 * vbp >> 20;
    vhp = (vbp * div1024Q >> 10) - vlp - inval;

    inval = (int)
    ((bpOn ? vbp : 0) + (hpOn ? vhp : 0) + (lpOn ? vlp : 0));
        }

        inval += noFltBuffer[i];
        // 13 bits * sidVol => 13 + 4 = 17 bits... take it down to 14...
        if (sidVolArr[i] != -1) {
          sidVol = sidVolArr[i];
          sidVolArr[i] = -1;
        }
        inval = (inval * sidVol) >> 2;

        // External filter
        // run 8 + 8 + 7 cycles
        exVlp += (8 * exw0lp >> 8) * (inval - exVlp) >> 12;
        exVhp += exw0hp * 8 * (exVlp - exVhp) >> 20;
        exVlp += (8 * exw0lp >> 8) * (inval - exVlp) >> 12;
        exVhp += exw0hp * 8 * (exVlp - exVhp) >> 20;

        exVo = exVlp - exVhp;
        exVlp += (7 * exw0lp >> 8) * (inval - exVlp) >> 12;
        exVhp += exw0hp * 7 * (exVlp - exVhp) >> 20;

        intBuffer[i] = (int) exVo;
      }

//    if (moog)
//    moogFilter.performFilter(intBuffer, SID6581.GENLEN);

      if (effects) {
        for (int i = 0, n = SIDVoice6581.GENLEN; i < n; i++) {
          int exVal = intBuffer[i];
          int echoRead = (echoPos + echoLFODiff) % echoSize;
          int out = (exVal * (100 - echoDW) + echo[echoRead] * echoDW) / 100;
          if (out > 32767) out = 32767;
          if (out < -32767) out = -32767;

          intBuffer[i] = out;


          exVal = exVal + ((echo[echoRead] * echoFeedback) / 100);

          if (exVal > maxefx) maxefx = exVal;
          if (exVal < minefx) minefx = exVal;

          if (exVal > 32767) exVal = 32767;
          if (exVal < -32767) exVal = -32767;

          echo[echoPos] = exVal;
          echoPos = (echoPos + 1) % echoSize;
        }
      }

      int bIndex = 0;
      if (BYTES_PER_SAMPLE == 2) {
        for (int i = 0, n = SIDVoice6581.GENLEN; i < n; i++) {
          buffer[bIndex++] = (byte) (intBuffer[i] & 0xff);
          buffer[bIndex++] = (byte) ((intBuffer[i] >> 8));
        }
      } else {
        for (int i = 0, n = SIDVoice6581.GENLEN; i < n; i++) {
          buffer[bIndex++] = (byte) ((intBuffer[i] >> 8));
        }
      }
    }

    driver.write(buffer);

    if (trueIRQ) {
      echoLFODiff = (echoLFODiffMax * sine10Hz[echoLFOPos]) / 1000;
      echoLFOPos = (echoLFOPos + echoLFOSpeed) % LFO_WAVELEN;
    }
    return trueIRQ;
  }

  public int getMicrosPerGen() {
    long mpg = 1000000l * SIDVoice6581.GENLEN;
    return (int) (mpg / SIDVoice6581.SAMPLE_RATE);
  }

  public boolean soundOn() {
    return soundOn;
  }

  public void setSoundOn(boolean on) {
    soundOn = on;
    if (soundOn == false) {
      for (int i = 0, n = buffer.length; i < n; i++) {
        buffer[i] = 0;
      }
    }
  }
}
