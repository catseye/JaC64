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
 * @author  Niclas Finne (niclas.finne@sics.se)
 * @version $Revision: 1.2 $, $Date: 2006/05/01 14:57:57 $
 */
public class DirEntry {

  public String name;
  public int trk;
  public int sec;
  public int size;
  public int type;

  public DirEntry(String name, int trk, int sec, int size, int type) {
    this.name = name;
    this.trk = trk;
    this.sec = sec;
    this.size = size;
    this.type = type;
  }

  public String getTypeString() {
    switch (type) {
    case 0x80:
      return " DEL ";
    case 0x81:
      return " SEQ ";
    case 0x82:
      return " PRG ";
    case 0x83:
      return " USR ";
    case 0x84:
      return " REL ";
    }
    return "---";
  }

  public String toString() {
    String typeStr = getTypeString();

    return name + " (" + typeStr + ") " + size;
  }
}
