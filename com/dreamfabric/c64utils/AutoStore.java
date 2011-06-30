package com.dreamfabric.c64utils;
/**
 * Describe class AutoStore here.
 *
 *
 * Created: Sat Feb 17 00:48:42 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
import java.util.ArrayList;
import java.net.URLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;

public class AutoStore {

  public static final int CMP_EQUAL = 1;
  public static final int CMP_NOT_EQUAL = 2;
  public static final int CMP_GREATER = 3;
  public static final int CMP_LESS = 4;

  public static final int R_ADDRESS = 0;
  public static final int R_CMP = 1;
  public static final int R_VAL = 2;

  private ArrayList store = new ArrayList();
  private ArrayList rules = new ArrayList();

  private String prefixUrl = "";
  /**
   * Creates a new <code>AutoStore</code> instance.
   *
   */
  public AutoStore(String prefixUrl) {
    this.prefixUrl = prefixUrl;
  }

  // Currenly a slow execution...
  public String checkRules(int[] memory) {
    for (int i = 0, n = rules.size(); i < n; i++) {
      int[] rule = (int[]) rules.get(i);
      int cval = memory[rule[R_ADDRESS]];
      if (!matches(cval, rule[R_CMP], rule[R_VAL]))
	return null;
    }

    return saveState(memory);
  }

  private String saveState(int[] memory) {
    String urls = prefixUrl;

    for (int i = 0, n = store.size(); i < n; i += 2) {
      String name = (String) store.get(i);
      int[] arr = (int[]) store.get(i + 1);
      if (i > 0) urls += "&";
      urls += name + "=" + toHex(memory, arr[0], arr[1]);
    }

    System.out.println("URL: " + urls);

    try {
      URL url = getClass().getResource(urls);
      URLConnection urlc = url.openConnection();
      //     urlc.setDoOutput(true);
      //     urlc.setUseCaches(false);
      HttpURLConnection httpConnection = (HttpURLConnection) urlc;
      InputStream is = httpConnection.getInputStream();

      System.out.println("Read back:");
      int c;
      while ((c = is.read()) != -1) {
	System.out.print((char) c);
      }
      System.out.println("----------------");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private String toHex(int[] memory, int address, int len) {
    String s = "";
    for (int i = 0, n = len; i < n; i++) {
      if (memory[i + address] < 16) s += "0";
      s += Integer.toString(memory[i + address], 16);
    }
    return s;
  }

  private boolean matches(int v1, int cmp, int v2) {
    switch (cmp) {
    case CMP_EQUAL:
      return v1 == v2;
    case CMP_NOT_EQUAL:
      return v1 != v2;
    case CMP_LESS:
      return v1 < v2;
    case CMP_GREATER:
      return v1 > v2;
    }
    return false;
  }

  public void addRule(int address, int type, int value) {
    rules.add(new int[] {address, type, value});
  }

  // xxxx=14 [!=<>]
  public void addRule(String rule) {
    int adr = 0;
    int val = 0;
    String inx = "0123456789abcdef";
    rule = rule.trim().toLowerCase();
    for (int i = 0, n = 4; i < n; i++) {
      adr = adr << 4;
      int ix = inx.indexOf(rule.charAt(i));
      if (ix == -1) throw new IllegalArgumentException("Illegal syntax");
      adr += ix;
    }
    int realType = "#=!><".indexOf(rule.charAt(4));
    for (int i = 5, n = 7; i < n; i++) {
      val = val << 4;
      int ix = inx.indexOf(rule.charAt(i));
      if (ix == -1) throw new IllegalArgumentException("Illegal syntax");
      val += ix;
    }
    System.out.println(rule + " => " + adr + " " + realType + " " + val);
    addRule(adr, realType, val);
  }

  public void addStore(int address, int len, String name) {
    store.add(name);
    store.add(new int[] {address, len});
  }

}
