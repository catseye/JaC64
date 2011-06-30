/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.c64utils;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Describe class C64Script here.
 *
 *
 * Created: Thu Sep 14 21:47:57 2006
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public class C64Script {

  int pos;

  /**
   * Creates a new <code>C64Script</code> instance.
   *
   */
  public C64Script() {
  }

  public void test() {
    System.out.println("Test was called!!!");
  }

  public void test2(String arg1) {
    System.out.println("Test2 was called with arg:" + arg1);
  }

  public void enterText(String arg1) {
    System.out.println("enterText was called with arg:" + arg1);
  }

  private String getString(String line, char endChar) {
    char c;
    StringBuffer sb = new StringBuffer();
    int max = line.length();
    while (pos < max) {
      c = line.charAt(pos++);
      if (c == '\\') {
	sb.append(line.charAt(pos++));
      } else if (c == endChar) {
	return sb.toString();
      } else {
	sb.append(c);
      }
    }
    throw new IllegalArgumentException("Illegal string syntax at: " + pos);
  }

  public void interpretCall(String line, Object callable) {
    // Pick the call method
    System.out.println("Parsing: " + line);
    String fnName = "";
    pos = 0;
    int max = line.length();
    line.trim();
    char c;
    while (pos < max && (c = line.charAt(pos++)) != '(');
    fnName = line.substring(0, pos - 1);

    System.out.println("function name: " + fnName);

    ArrayList args = new ArrayList();
    String value = "";
    while (pos < max) {
      c = line.charAt(pos++);
      switch(c) {
      case ',':
	if (value != "") {
	  args.add(value);
	  value = "";
	} else {
	  throw new IllegalArgumentException("unexpected ',' at " + pos);
	}
	break;
      case '\'':
      case '"':
	value = getString(line, c);
      case ')':
	if (value != "") {
	  args.add(value);
	  value = "";
	}

	// Finished!!! - call method and ignore the rest (for now)
	Method[] methods = callable.getClass().getMethods();
	for (int i = 0, n = methods.length; i < n; i++) {
	  if (fnName.equals(methods[i].getName())) {
	    Method method = methods[i];
	    System.out.println("Method found: " + method);
	    Class[] pTypes = method.getParameterTypes();
	    if (args.size() == pTypes.length) {
	      System.out.println("Correct param number, calling method!");
	      try {
		method.invoke(callable, args.toArray());
	      } catch (Exception e) {
		e.printStackTrace();
	      }
	    }
	  }
	}
      default:
	value += c;
      }
    }
  }

  public static void main(String[] args) {
    C64Script s = new C64Script();
    s.interpretCall(args[0], s);
  }
}
