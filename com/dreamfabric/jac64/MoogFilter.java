/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

/**
 * Describe class MoogFilter here.
 *
 *
 * Created: Thu Nov 03 07:55:19 2005
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class MoogFilter {

  private int samplingFrq;
  private double f;
  private double k;
  private double p;
  private double r;

  // The filter execution registers
  private double y1;
  private double y2;
  private double y3;
  private double y4;
  private double oldx;
  private double oldy1;
  private double oldy2;
  private double oldy3;

  private double yMax;
  private double yMin;
  // -------------------------------------------------------------------
  // CSound code - Moog filter - try it out!!!
  // -------------------------------------------------------------------

  /**
   * Creates a new <code>MoogFilter</code> instance.
   *
   */
  public MoogFilter(int sfrq) {
    samplingFrq = sfrq;
  }

  public void printStatus() {
    System.out.println("yMax: " + yMax);
    System.out.println("yMin: " + yMin);
  }

  public void setFilterParams(int cutoff, double res) {
    f = 2.0 * cutoff / samplingFrq; //[0 - 1]
    k = 3.6 * f - 1.6 * f * f - 1.0; //(Empirical tunning)
    p = (k + 1) * 0.5;
    double scale = Math.exp(( (1 - p) * 1.386249));
    r = res * scale;
  }

  public void performFilter(int buffer[], int len) {
    for (int i = 0, n = len; i < n; i++) {
      //--Inverted feed back for corner peaking
      double x = (buffer[i] / 32768.0) - r * y4;
      if (buffer[i] != 0) {
// 	System.out.println("B[i]=" + buffer[i] + " x= " + x + " y1:" + y1);
      }

      //Four cascaded onepole filters (bilinear transform)
      y1 = x * p + oldx * p - k * y1;
      y2 = y1 * p + oldy1 * p - k * y2;
      y3 = y2 * p + oldy2 * p - k * y3;
      y4 = y3 * p + oldy3 * p - k * y4;

      //Clipper band limited sigmoid
      y4 = y4 - (y4 * y4 * y4) / 6.0;

      oldx = x;
      oldy1 = y1;
      oldy2 = y2;
      oldy3 = y3;

      // Ensure no-wrap-overs.
      if (y4 > yMax) yMax = y4;
      if (y4 < yMin) yMin = y4;

      // Dist
//       y4 = y4 * 2.3;

      if (y4 > 0.9) {
	y4 = 0.9;
      }
      if (y4 < -0.9) {
	y4 = -0.9;
      }

      buffer[i] = (int) (y4 * 32768.0);
    }
  }
}
