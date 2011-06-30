package com.dreamfabric.jac64;
/**
 * Describe class Hex here.
 *
 *
 * Created: Thu Jun 15 08:15:29 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class Hex {

  public static final String hex2(int n) {
    if (n < 16) return "0" + Integer.toString(n, 16);
    return Integer.toString(n, 16);
  }

}
