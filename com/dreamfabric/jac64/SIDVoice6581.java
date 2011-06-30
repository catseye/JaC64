/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 *
 *
 * @author  Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 1.3 $, $Date: 2005/11/21 07:01:33 $
 */
public class SIDVoice6581 extends SIDVoice {

  public static final int UPDATE_CYCLES = 1000; // 2 x each ms
  public static final int PER_SEC = 1000000 / UPDATE_CYCLES;
  public static final boolean DEBUG = false;

  private int[] memory;
  private int sidbase = 0;

  // Frq  = (Freg * Fclk / 16777216) Hz
  // public static final double FRQCONV = 0.060975609;
  // PAL Clock speed:
  public static final int SAMPLE_RATE = 44000;
  public static final double FRQCONV = 985248.4 / 16777216;

  public static final int TRIANGLE = 0x1;
  public static final int SAW = 0x2;
  public static final int PULSE = 0x4;
  public static final int NOISE = 0x8;
  public static final int NONE = 0x0;

  private int[] ST_LOOKUP = RS6581Waves.wave6581__ST;
  private int[] PST_LOOKUP = RS6581Waves.wave6581_PST;
  private int[] PT_LOOKUP = RS6581Waves.wave6581_P_T;
  private int[] PS_LOOKUP = RS6581Waves.wave6581_PS_;

  public static final String[] WAVE = new String[] {
    "NONE", "TRIANGLE", "SAW", "TRI_SAW",
    "PULSE", "TRI_PULSE", "SAW_PULSE", "TRI_SAW_PULSE",
    "NOISE", "NOISE_T", "NOISE_S", "NOISE_TS",
    "NOISE_P", "NOISE_TP", "NOISE_SP", "NOISE_TSP"};

  // Should be public when used in software synths...
  private final int ATTACK = 1;
  private final int DECAY = 2;
  private final int SUSTAIN = 3;
  private final int RELEASE = 4;
  private final int FINISHED = 5;

  public static final String[] ADSR_PHASES = new String[]
    {"-", "Attack", "Decay", "Sustain", "Relase", "Finished"};

  final static int waveLen = 44000;

  // The wavebuffers for the precalculated waves (shared between SIDs)
  private static int[] sawWave;
  private static int[] triangleWave;
  private static int[] triangleWaveD2;
  private static int[] pulseWave;

  public final static int GENLEN = 44000 / PER_SEC;
  public static final int GENS_PER_IRQ = (SAMPLE_RATE / 50) / GENLEN;
  public static final int SAMPLE_BITS = 12;

  // Maybe for 1/50th of a second???
  public static final int MAXGENLEN = 10 * GENLEN;

  public static final double SAMPLES_PER_MICRO = SAMPLE_RATE / 1000000.0;
  public static final int CYCLES_PER_SAMPLE = 1000000 / SAMPLE_RATE;

  public static final int VOLUME_SIZE = 4096;
  public static final int DELAY_LEN = 44000/50;

  byte[] buffer = new byte[4096];

  int delPos = 0;
  int diffMin = 0;
  int diffDt = 1;

  int delay[] = new int[DELAY_LEN];

  int generated;
  int smp;

  int pwid = waveLen / 2;

  long nextSample = 0;
  long next_nextSample;
  int noiseData = 0;

  long noise_reg = 0x7ffff8;

  boolean debugGen = false;

  boolean sync = false;
  boolean ring = false;
  boolean testBit = false;
  int testZero = 0;
  private int[] outBuffer;

  // Start stop ADSR stuff!! (not yet implemented, and how to do with gain?)
  // Number of adsr microseconds (cycles)
  long attackTime[] = { 2000,    8000,    16000,   24000,
		  38000,   56000,   68000,   80000,
		  100000,  250000,  500000,  800000,
		  1000000, 3000000, 5000000, 8000000};

  public static final int ADSR_BITS = 22;
  public static final int ADSR_RATE_BITS = 32;
  long ATTACK_MAX = 255L << ADSR_BITS;
  long RELEASE_FINISH_LEVEL = (1L << ADSR_BITS) - 1;

  int ATTACK_FUZZ = 128; // For the comparison so that we do not overshoot!

  long attackDelta[] = new long[attackTime.length];

  long decayExp[] = {
    4216751472L,4275278366L,4285111523L,4288394265L
    ,4290814737L,4292149050L,4292646253L,4292994329L
    ,4293388850L,4294335848L,4294651560L,4294769958L
    ,4294809425L,4294914671L,4294935721L,4294947561L
  }; // was new long[attackTime.length];

  long adsrLevel22 = 0;
  long adsrSusLevel22 = 0;
  long decayFactor = 0;
  long releaseFactor = 0;
  // Used while DECAY & RELEASE!
  long currentDecayFactor = 0;
  long currentAttackRate = 0;

  int adsrPhase = ATTACK;
  int adsrBug = 0;
  int ad;
  int sr;

  // 8 => div by 2...
  // 32 => mul by 2... // Same for all SID Channels!!!
  public static int frqFac = 16;

  // Stuff for the viewers...
  public double adsrLevel;
  long lastAttackTime = 0;

  boolean soundOn = false;

  // Maybe frq should not be integer?
  public static final int FRQ_BITS = 4;
  public static final long WAVELEN = waveLen << FRQ_BITS;
  public static final long WAVELEN_HALF = WAVELEN / 2;
  public long frq = 1;
  public double trueFrq = 0;
  public int wave;

  public long next_frq; // FRQ for modulation, etc

  public SIDVoice6581 next;

  public SIDVoice6581(int mem[], int sb) {
    intBuffer = new int[2048];
    outBuffer = new int[2048];

    memory = mem;
    sidbase = sb;
    System.out.println("SIDBASE: " + sidbase);
    System.out.println("GENLEN: " + GENLEN);

    for (int i = 0, n = attackTime.length; i < n; i++) {
      int noSamples = (int) ((SAMPLE_RATE * attackTime[i]) / 1000000L);
      // From zero to 255
      long attack = (255L << ADSR_BITS) / noSamples;
      attackDelta[i] = (long)(attack);
    }

    // No log/exp in J2ME
//     System.out.println("Decay exp:");
//     for (int i = 0, n = attackTime.length; i < n; i++) {
//       int noSamples = (int) ((SAMPLE_RATE * attackTime[i] * 3) / 1000000L);
//       double decayFactor = Math.exp(DECAY_LOG/noSamples);
//       decayExp[i] = (long)((1L << ADSR_RATE_BITS) * decayFactor);
//       System.out.println("," + decayExp[i]);
//     }

    // ensure that all arrays exists
    if (sawWave == null) {
      sawWave = new int[waveLen];
      triangleWave = new int[waveLen];
      triangleWaveD2 = new int[waveLen];
      pulseWave = new int[2 * waveLen];
    }

    // -------------------------------------------------------------------
    // This will create similar waves as was created using the 24 bit
    // accumulator in the sid (which upper 12 bits for the sound gen.)
    // -------------------------------------------------------------------

    // Create all the waves!
    double val = 0;
    double dval = 0xfff / (double) waveLen;

    // Create SAW
    for (int i = 0; i < waveLen; i++) {
      sawWave[i] = (int) val;
      val = val + dval;
    }

    // Create PULSE (according to re-sid it is starting high!)
    for (int i = 0; i < waveLen; i++) {
      pulseWave[i] = 0xfff;
      pulseWave[i + waveLen] = 0x0;
    }

    // Create Triangle
    val = 0;
    dval = 0xfff * 2 / (double) waveLen;
    for (int i = 0; i < waveLen / 2; i++) {
      triangleWave[i] = (int) val;
      triangleWave[i + waveLen / 2] = (int) (0xfff - val);
      triangleWaveD2[i] = (int) (val/2);
      triangleWaveD2[i + waveLen / 2] = (int) ((0xfff - val) /2);
      val = val + dval;
    }

  }

  public void init() {
    wave = NONE;
    sync = false;
    testBit = false;
    ring = false;
    nextSample = 0;
    next_nextSample = 0;
    next_frq = 0;
    setControl(0,0);
  }

  public void setControl(int data, long cycles) {
    wave = data >> 4;
    if (DEBUG) {
      System.out.println(getSIDNo() +
			 "  Setting Wave: " + WAVE[wave] + " at " + cycles + "  lastGen: " + lastCycles + " Gate: " + (data&1));
    }
    boolean oldTest = testBit;
    testBit = (data & 0x08) != 0;
    if (oldTest && !testBit) {
      // When cleared -> reset sample play...
//       System.out.println(getSIDNo() + "  Testbit cleared!!!>>> " + cycles + " zeroed:" +
// 			 testZero);
      testZero = 0;
      nextSample = 0;
      noise_reg = 0x7ffff8;
    }
//     if (!oldTest && testBit) {
//       System.out.println(getSIDNo() + "  Testbit set!!!>>>" + cycles);
//     }

    if ((data & 1) > 0)
      soundOn(cycles);
    else
      soundOff(cycles);
    sync = (data & 2) != 0;
    ring = (data & 4) != 0;
  }

  public void soundOn(long cycles){
    if(!soundOn) {
      // Set the data for the ADSR_12
      if ((sr & 0xf) > (ad >> 4)) {
	adsrBug = (int) (0x8000 * SAMPLES_PER_MICRO);
	if (DEBUG)
	  System.out.println(getSIDNo() +
			     "  CTRATK_ADSR BUG Triggered for " + adsrBug +
			     " samples" + " level: " +
			     (adsrLevel22 >> ADSR_BITS));

      }

      currentAttackRate = attackDelta[(memory[sidbase + 5] & 0xf0) >> 4];
      decayFactor = decayExp[(memory[sidbase + 5] & 0x0f)];
      releaseFactor = decayExp[(memory[sidbase + 6] & 0x0f)];
      adsrSusLevel22 = (memory[sidbase + 6] & 0xf0 + 8) << ADSR_BITS;
      currentDecayFactor = decayFactor;
      adsrPhase = ATTACK;
//       System.out.println(getSIDNo() + "  Starting ATTACK: AD=" +
// 			 memory[sidbase + 5] + " SR="+ memory[sidbase + 6] +
// 			 " adsrLevel: " + (adsrLevel22 >> ADSR_BITS) + " " + cycles);

      // ADSR Level should not be reset???
      //      adsrLevel22 = 0;
      lastAttackTime = cycles;
      soundOn = true;
    }
  }


  public void soundOff(long cycles){
    adsrPhase = RELEASE;
    soundOn = false;
    debugGen = false;

    currentDecayFactor = releaseFactor;
    adsrSusLevel22 = RELEASE_FINISH_LEVEL;
//     System.out.println(getSIDNo() + "  RELEASE: " + memory[sidbase + 6] +
// 		       " diff from attack: " + (cycles - lastAttackTime));
  }


  public void setAD(int data, long cycles) {
    // ADSR_12 just update the rates - nothing more to do!
    currentAttackRate = attackDelta[(memory[sidbase + 5] & 0xf0) >> 4];
    decayFactor = decayExp[(memory[sidbase + 5] & 0x0f)];

    if (DEBUG) {
      System.out.println(getSIDNo() + "  Setting AD: " +
			 memory[sidbase + 5] + " " + cycles + " diff " +
			 (cycles - lastAttackTime) + " ADSRLv: " +
			 (adsrLevel22 >> ADSR_BITS));
    }

    ad = data;
    // If we are currently decaying... change the current decay factor!
    if (adsrPhase != RELEASE) {
      if (currentDecayFactor > decayFactor) {
	adsrBug = (int) (0x8000 * SAMPLES_PER_MICRO);
	if (DEBUG) {
	  System.out.println(getSIDNo() +
			     "  DEC_ADSR BUG Triggered for " + adsrBug +
			     " samples" + " level: " +
			     (adsrLevel22 >> ADSR_BITS));
	}
      }
      currentDecayFactor = decayFactor;
    }
  }

  public void setSR(int data, long cycles) {
    int tmp = ((memory[sidbase + 6] & 0xf0) + 8) << ADSR_BITS;
    releaseFactor = decayExp[(memory[sidbase + 6] & 0x0f)];

    if (DEBUG) {
      System.out.println(getSIDNo() + "  Setting SR: " +
			 Integer.toString(memory[sidbase + 6], 16) +
			 " " + cycles + " diff " +
			 (cycles - lastAttackTime) + " ADSRLv: " +
			 (adsrLevel22 >> ADSR_BITS));
    }
    // Restart decay if sustain is reached, but we have lowered sustain level!!!
    // ???

    sr = data;
    // If we are currently releasing...
    if (adsrPhase == RELEASE) {
      if (currentDecayFactor > releaseFactor) {
	adsrBug = (int) (0x8000 * SAMPLES_PER_MICRO);
	if (DEBUG) {
	  System.out.println(getSIDNo() +
			     "  REL_ADSR BUG Triggered for " + adsrBug +
			     " samples" + " level: " +
			     (adsrLevel22 >> ADSR_BITS));
	}
      }
      currentDecayFactor = releaseFactor;
    } else if (adsrPhase == SUSTAIN) {
      if (tmp < adsrSusLevel22) {
	currentDecayFactor = releaseFactor;
	adsrPhase = RELEASE;
      }
    }
    adsrSusLevel22 = tmp;
  }


  public void reset() {
    init();
    soundOff(0);
  }

  public void printStatus() {
    System.out.println("SID: " + getSIDNo() + " ----------------------------");
    System.out.println("Wave: " + WAVE[wave]);
    System.out.println("Frequency: " + frq + "  PWid:" + pwid + " " + trueFrq);
    System.out.println("ADSRLevel: " + (adsrLevel22 >> ADSR_BITS));
    System.out.println("ADSRPhase: " + ADSR_PHASES[adsrPhase]);
    System.out.println("AD reg: " + Integer.toString(memory[sidbase + 5], 16));
    System.out.println("SR reg: " + Integer.toString(memory[sidbase + 6], 16));
    System.out.println("WavePos: " + nextSample + " pulse => " + ((nextSample < pwid) ? "0xfff" : "0"));
    if (ring)System.out.println("RING MODULATION");
    if (sync)System.out.println("SYNCHRONIZATION");
    if (testBit) System.out.println("TEST BIT SET!");
  }


  private int getSIDNo() {
    return ((sidbase & 0xf) / 7);
  }

  private long lastCycles = 0;
  private int decayTimes = 0;
  // last sample
  public int lastSample = 0;
  // ADSR is 8 bits
  public int adsrVol = 0;

  long nanos;
  long total;

  int pwidArr[] = new int[SIDVoice6581.GENLEN];

  // This is a crappy method... should not be used later...
  public int[] generateSound(long cycles) {
    updateSound(cycles);
    // Cheat down generated to 0
    generated = 0;
    return outBuffer;
  }

  public void updatePulseWidth(long cycles) {
    if ((wave & PULSE) != 0 && lastCycles > 0) {
      int pos = (int) ((cycles - lastCycles) /
		       (SIDVoice6581.CYCLES_PER_SAMPLE + 10));
      pwidArr[pos % SIDVoice6581.GENLEN] =
	((((memory[sidbase + 3]& 0xf) << 8) +
	  memory[sidbase + 2]) * waveLen) / 4095;
    } else {
      pwid = ((((memory[sidbase + 3]& 0xf) << 8) +
	       memory[sidbase + 2]) * waveLen) / 4095;
    }
  }

  public void updateSound(long cycles) {
//     if (getSIDNo() == 0) {
//       total = System.nanoTime() - nanos;
//     }
//     nanos = System.nanoTime();

    int[] lookup;
    int[] lookupW;
    lastCycles = cycles;

    // Always generate data - if cycle driven?!
    // Maybe make the frq to be << 4 so that it is even more exact than
    // now???
    frq = (int) (0.5 + (1 << FRQ_BITS) * ((memory[sidbase + 1] << 8) +
					  memory[sidbase]) * FRQCONV);
    trueFrq = (1 << FRQ_BITS) *
      ((memory[sidbase + 1] << 8) + memory[sidbase]) * FRQCONV;

    pwid = ((((memory[sidbase + 3]& 0xf) << 8) +
	     memory[sidbase + 2])
	    * waveLen) / 4095;

    if (frqFac != 16) {
      frq = (frq * frqFac) >> 4;
      trueFrq = (trueFrq * frqFac) / 16;
    }

    if (next != null) {
      next_frq = next.frq;
      // 	next_nextSample = next.nextSample;
    }

    int bIndex = generated;

    switch(wave) {
    case NONE:
      for (int i = 0; i < GENLEN; i++) {
	intBuffer[bIndex++] = 0xfff;
	nextSample = (nextSample + frq) % WAVELEN;
      }
      break;
    case TRIANGLE:
      if (ring) {
	boolean msb0 = false;
	for (int i = 0; i < GENLEN; i++) {
	  boolean msb = (msb0 = nextSample >= WAVELEN_HALF) ^
	    (next_nextSample >= WAVELEN_HALF);

	  long samp = nextSample;
	  // Differ => modify position!
	  if (msb0 != msb) {
	    samp += msb0 ? -WAVELEN_HALF : WAVELEN_HALF;
	  }
	  intBuffer[bIndex++] = triangleWave[(int)(samp >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample = (next_nextSample + next_frq) % WAVELEN;
	}
      } else if (!sync) {
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = triangleWave[(int)(nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	}
      } else {
	// SYNCH
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = triangleWave[(int)(nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample += next_frq;
	  if (next_nextSample > WAVELEN) {
	    nextSample = 0;
	    next_nextSample -= WAVELEN;
	  }
	}
      }
      break;
    case SAW:
      if (!sync) {
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = sawWave[(int)(nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	}
      } else {
	// SYNCH
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = sawWave[(int)(nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample += next_frq;
	  if (next_nextSample > WAVELEN) {
	    nextSample = 0;
	    next_nextSample -= WAVELEN;
	  }
	}
      }
      break;
    case SAW | TRIANGLE:
      if (!sync) {
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = ST_LOOKUP[sawWave[(int)
						  (nextSample >> FRQ_BITS)]];
	  nextSample = (nextSample + frq) % WAVELEN;
	}
      } else {
	// SYNCH
	for (int i = 0; i < GENLEN; i++) {
	  intBuffer[bIndex++] = ST_LOOKUP[sawWave[(int)
						  (nextSample >> FRQ_BITS)]];
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample += next_frq;
	  if (next_nextSample > WAVELEN) {
	    nextSample = 0;
	    next_nextSample -= WAVELEN;
	  }
	}
      }
      break;
    case PULSE:
      if (!sync) {
	for (int i = 0; i < GENLEN; i++) {

	  if (pwidArr[i] != -1) {
	    pwid = pwidArr[i];
	    pwidArr[i] = -1;
	  }

	  intBuffer[bIndex++] =
	    pulseWave[pwid + (int)(nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	}
      } else {
	for (int i = 0; i < GENLEN; i++) {

	  if (pwidArr[i] != -1) {
	    pwid = pwidArr[i];
	    pwidArr[i] = -1;
	  }

	  intBuffer[bIndex++] =
	    pulseWave[pwid + (int) (nextSample >> FRQ_BITS)];
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample += next_frq;
	  if (next_nextSample > WAVELEN) {
	    nextSample = 0;
	    next_nextSample -= WAVELEN;
	  }
	}
      }
      break;
    case PULSE | SAW:
    case PULSE | TRIANGLE:
    case PULSE | SAW | TRIANGLE:
      if (wave == (PULSE | SAW)) {
	lookup = PS_LOOKUP;
	lookupW = sawWave;
      } else if (wave == (PULSE | TRIANGLE)) {
	lookup = PT_LOOKUP;
	lookupW = triangleWaveD2;
      } else {
	lookup = PST_LOOKUP;
	lookupW = sawWave;
      }
      if (!sync) {
	for (int i = 0; i < GENLEN; i++) {

	  if (pwidArr[i] != -1) {
	    pwid = pwidArr[i];
	    pwidArr[i] = -1;
	  }


	  int sam1 = pulseWave[pwid + (int) (nextSample >> FRQ_BITS)];
	  intBuffer[bIndex++] = sam1 != 0 ?
	    lookup[lookupW[(int)(nextSample >> FRQ_BITS)]] : 0;
	  nextSample = (nextSample + frq) % WAVELEN;
	}
      } else {
	for (int i = 0; i < GENLEN; i++) {

	  if (pwidArr[i] != -1) {
	    pwid = pwidArr[i];
	    pwidArr[i] = -1;
	  }

	  int sam1 = pulseWave[pwid + (int)(nextSample >> FRQ_BITS)];
	  intBuffer[bIndex++] = sam1 != 0 ?
	    lookup[lookupW[(int)(nextSample >> FRQ_BITS)]] : 0;
	  nextSample = (nextSample + frq) % WAVELEN;
	  next_nextSample += next_frq;
	  if (next_nextSample > WAVELEN) {
	    nextSample = 0;
	    next_nextSample -= WAVELEN;
	  }
	}
      }
      break;

    case NOISE:
    case NOISE | PULSE:
    case NOISE | TRIANGLE:
    case NOISE | SAW:
    case NOISE | PULSE | SAW:
    case NOISE | TRIANGLE | SAW:
    case NOISE | PULSE | TRIANGLE:
    case NOISE | PULSE | TRIANGLE |SAW:
      // Noise generator inspired/adapted from the Re-sid sid emulator!
      // But should be shifted even if noise is not played!!!
      int delay = waveLen / 32;
      for (int i = 0; i < GENLEN; i++) {
	int newNoiseData = 0;
	int loops = 0;
	while (delay < 0) {
	  int bit0 = (int)
	    ((noise_reg >> 22) ^ (noise_reg >> 17)) & 0x1;
	  noise_reg <<= 1;
	  noise_reg &= 0x7fffff;
	  noise_reg |= bit0;

	  if (loops < 4) {
	    newNoiseData += (int) (((noise_reg & 0x400000L) >> 11) |
				   ((noise_reg & 0x100000L) >> 10) |
				   ((noise_reg & 0x010000L) >> 7) |
				   ((noise_reg & 0x002000L) >> 5) |
				   ((noise_reg & 0x000800L) >> 4) |
				   ((noise_reg & 0x000080L) >> 1) |
				   ((noise_reg & 0x000010L) << 1) |
				   ((noise_reg & 0x000004L) << 2));
	    loops++;
	  }
	  delay += waveLen / 32;
	}
	if (loops > 0) {
	  noiseData = (newNoiseData / loops);
	}
	delay -= frq;
	intBuffer[bIndex++] = noiseData;
	// Move register position even if sample not played...
	nextSample = (nextSample + frq) % WAVELEN;
      }
      break;
    default:
      System.out.println("WAVE NOT IMPLEMENTED: " + wave);
    }

    // Afterprocessing after the waves has been generated!
    // Setting ADSR volume
    bIndex = generated;

    for (int i = 0; i < GENLEN; i++) {

      // ADSR_12 update!
      if (adsrBug > 0) {
	// If the ADSR bug have been triggered the ADSR will be "locked"
	adsrBug--;
      } else {
	if (adsrPhase == ATTACK) {
	  adsrLevel22 += currentAttackRate;
	  if (adsrLevel22 + ATTACK_FUZZ > ATTACK_MAX) {
	    adsrLevel22 = ATTACK_MAX;
	    adsrPhase = DECAY;
	  }
	} else if (adsrPhase == DECAY || adsrPhase == RELEASE) {
	  decayTimes++;
	  adsrLevel22 = (adsrLevel22 * currentDecayFactor) >> ADSR_RATE_BITS;
	  if (adsrLevel22 < adsrSusLevel22) {
	    // Do something!!!
	    if (adsrPhase == DECAY) {
	      decayTimes = 0;
	      adsrLevel22 = adsrSusLevel22;
	      adsrPhase = SUSTAIN;
	    } else {
	      adsrSusLevel22 = 0;
	      adsrPhase = FINISHED;
	    }
	  }
	}
      }
      adsrVol = (int) (adsrLevel22 >> ADSR_BITS);

      // For debug!
      adsrLevel = (adsrLevel22 >> ADSR_BITS) / 255.0;

      if (testBit) {
	testZero++;
	outBuffer[bIndex] = 0;
      } else {
	// FIX the sample output buffer to be centered around 0 + add volume
	// How should this be done to really work fine - check resid?
	// Resid goes down to 13 bits before filter (so do I?!)
	outBuffer[bIndex] = 0x400 + ((intBuffer[bIndex] * adsrVol) >> 7);
      }
      bIndex++;
    }

    //       if (getSIDNo() == 0) {
    // 	long diff = System.nanoTime() - nanos;
    // 	System.out.println("Elapsed ns: " + diff + " %: " +
    // 			   (diff * 100.0 / total));
    //       }
  }


  public int lastSample() {
    return (intBuffer[(smp++) % GENLEN] >> 3) & 0xff;
  }

}
