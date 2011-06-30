package com.dreamfabric.jac64;
/**
 * Describe class DiskChangeListener here.
 *
 *
 * Created: Thu Aug 03 23:11:29 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public interface DiskListener {

  public void diskChanged();
  public void atnChanged(boolean low);

}
