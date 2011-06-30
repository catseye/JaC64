package com.dreamfabric.c64utils;
/**
 * Describe class TCPPacket here.
 *
 *
 * Created: Thu Apr 12 21:24:55 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class TCPPacket extends IPPacket {

  /**
   * Creates a new <code>TCPPacket</code> instance.
   *
   */
  public TCPPacket() {
  }

  public TCPPacket(IPPacket packet) {
    this.data = packet.data;
    this.header = packet.header;
  }

  public int getSourcePort() {
    return getData16(0);
  }

  public int getDestinationPort() {
    return getData16(2);
  }

  public long getSequenceNumber() {
    return getData32(4);
  }

  public long getAckNumber() {
    return getData32(8);
  }

  public int getTCPHeaderLengthBytes() {
    return 4 * ((getData8(12) >> 4) & 0x0f);
  }

  public int getFlags() {
    return getData8(13);
  }

  public int getWindow() {
    return getData16(14);
  }

  public int getChecksum() {
    return getData16(16);
  }

  public int getUrgentPointer() {
    return getData16(18);
  }

}
