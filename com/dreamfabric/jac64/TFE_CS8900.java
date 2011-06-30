package com.dreamfabric.jac64;
/**
 * Describe class TFE_CS8900 here.
 *
 *
 * Created: Fri Apr 13 16:08:58 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0  public int performRead(int address, long cycles) {
 */
public class TFE_CS8900 {

  // -------------------------------------------------------------------
  // TFE related - Ethernet emulation!
  // -------------------------------------------------------------------
  // Ehternet headers...
// struct uip_eth_addr {
//   u8_t addr[6];
// };
// struct uip_eth_hdr {
//   struct uip_eth_addr dest;
//   struct uip_eth_addr src;
//   u16_t type;
// };


  public static final int RXTXREG = 0x00;
  public static final int TXCMD = 0x04;
  public static final int TXLEN = 0x06;
  public static final int PPDATA = 0x0c;
  public static final int PACKET_PP = 0x0a;

  private int offset;
  /**
   * Creates a new <code>TFE_CS8900</code> instance.
   *
   */

  public TFE_CS8900(int offset) {
    this.offset = offset;
  }

  public int performRead(int address, long cycles) {
    System.out.println("TFE_CS8900: read " + Integer.toString(address, 16));
    switch (address) {
    case PPDATA:
      break;
    case PPDATA + 1:
      break;
    case PACKET_PP:
      break;
    case PACKET_PP + 1:
      break;
    case TXCMD:
      break;
    case TXLEN:
      break;
    case RXTXREG:
      break;
    case RXTXREG + 1:
      break;
    }
    return 0;
  }

  public void performWrite(int address, int data, long cycles) {
    System.out.println("TFE_CS8900: write " + Integer.toString(address, 16)
		       + " = " + data);
    address -= offset;
    switch (address) {
    case PPDATA:
      break;
    case PPDATA + 1:
      break;
    case PACKET_PP:
      break;
    case PACKET_PP + 1:
      break;
    case TXCMD:
      break;
    case TXLEN:
      break;
    case RXTXREG:
      break;
    case RXTXREG + 1:
      break;
    }
  }
}
