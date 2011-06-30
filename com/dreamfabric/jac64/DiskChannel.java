package com.dreamfabric.jac64;
/**
 * Very simple implementation of a disk channel
 * (can only handle read on a good way).
 *
 *
 * Created: Tue Apr 18 22:19:57 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class DiskChannel {

  String filename;
  byte[] data;
  boolean open;
  int pos;
  int chID;

  /**
   * Creates a new <code>DiskChannel</code> instance.
   *
   */
  public DiskChannel(int chID) {
    this.chID = chID;
  }

  public void setFilename(String name) {
    filename = name;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }

  public int readChar() {
    if (pos >= data.length) return -1;
    return data[pos++] & 0xff;
  }

  public void open() {
    open = true;
    pos = 0;
  }

  public void close() {
    open = false;
  }

}
