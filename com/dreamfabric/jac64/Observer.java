package com.dreamfabric.jac64;
/**
 * Describe class Observable here.
 *
 *
 * Created: Sun Oct 15 22:54:00 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public interface Observer {

  /**
   * Creates a new <code>Observable</code> instance.
   *
   */
  public void update(Object src, Object data);
}
