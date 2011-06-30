package com.dreamfabric.c64utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Describe class IPPacket here.
 *
 *
 * Created: Thu Apr 12 15:30:35 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class IPPacket {

  byte[] data;
  byte[] header;
  /**
   * Creates a new <code>IPPacket</code> instance.
   *
   */
  public IPPacket() {
  }
  // Should take a data array and split it into header and data!!!!
  public IPPacket(byte[] header, byte[] data) {
    this.header = header;
    this.data = data;
  }

  public void readIPPacket(InputStream fs) throws IOException {
    byte[] header = new byte[20];
    // Search for next 0x45 (typical IP header size!)
    int pos = 0;
    int c = 0;
    boolean startFound = false;
    while (pos < 20 && (c = fs.read()) != -1) {
      if (c == 0x45) {
	startFound = true;
      }
      if (startFound) {
	header[pos++] = (byte) c;
      }
    }
    this.header = header;
    int dataLen = getTotalLength() - 20;
    data = new byte[dataLen];
    for (int i = 0, n = dataLen; i < n && (c = fs.read()) != -1; i++) {
      data[i] = (byte) (c & 0xff);
    }
  }

  public int getVersion() {
    return (header[0] >> 4) & 0xf;
  }

  public int getHeaderLengthBytes() {
    return 4 * ((header[0]) & 0xf);
  }

  public int getServiceType() {
    return header[1] & 0xff;
  }

  // High byte first?
  public int getTotalLength() {
    return get16(2);
  }

  public int getID() {
    return get16(4);
  }

  public int getFlags() {
    return (header[6] >> 5)& 0x7;
  }

  public int getFragment0() {
    return (header[6] & 0x1f) << 8 + header[7];
  }

  public int getTTL() {
    return header[8] & 0xff;
  }

  public int getProtocol() {
    return header[9] & 0xff;
  }

  public int getChecksum() {
    return get16(10);
  }

  public long getSourceIP() {
    return get32(12);
  }

  public long getDestinationIP() {
    return get32(16);
  }

  int get16(int pos) {
    return ((header[pos] & 0xff) << 8) | (header[pos + 1] & 0xff);
  }

  long get32(int pos) {
    return (((long) get16(pos)) << 16L) + get16(pos + 2);
  }

  int getData8(int pos) {
    return data[pos] & 0xff;
  }

  int getData16(int pos) {
    return ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
  }

  long getData32(int pos) {
    return (((long) get16(pos)) << 16L) + get16(pos + 2);
  }

  public static String getIPStr(long adr) {
    return "" + ((adr >> 24) & 0xff) + "." + ((adr >> 16) & 0xff) + "." +
      ((adr >> 8) & 0xff) + "." + (adr & 0xff);
  }

  public static void main(String[] args) throws IOException {
    FileInputStream fs = new FileInputStream("test.txt");
    IPPacket p = new IPPacket();
    p.readIPPacket(fs);
    System.out.println("---- IP Data -----");
    System.out.println("Version: " + p.getVersion());
    System.out.println("HDL: " + p.getHeaderLengthBytes());
    System.out.println("TotLen: " + p.getTotalLength());
    System.out.println("Protocol: " + p.getProtocol());
    System.out.println("IP Adr dst: " + getIPStr(p.getDestinationIP()));
    System.out.println("IP Adr src: " + getIPStr(p.getSourceIP()));

    if (p.getProtocol() == 6) {
      TCPPacket tcp = new TCPPacket(p);
      System.out.println("---- TCP Data -----");
      System.out.println("Source Port: " + tcp.getSourcePort());
      System.out.println("Destination Port: " + tcp.getDestinationPort());
      System.out.println("SeqNo: " + tcp.getSequenceNumber());
      System.out.println("TCP Header length: " +
			 tcp.getTCPHeaderLengthBytes());

      int offset = tcp.getTCPHeaderLengthBytes();
      int len = tcp.getTotalLength() - tcp.getHeaderLengthBytes() -
	offset;
      System.out.println("Content: " + len);
      for (int i = 0, n = len; i < n; i++) {
	System.out.print("" + (char) tcp.data[offset + i]);
      }
    }

  }

}
