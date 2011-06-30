package com.dreamfabric.jac64;

import java.io.InputStream;
import java.net.URL;

/**
 * Describe class SELoader here.
 *
 *
 * Created: Mon Oct 16 21:17:54 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */

public class SELoader extends Loader {

  String codebase = null;

  public SELoader() {
  }

  public SELoader(String codebase) {
    this.codebase = codebase;
  }

  public InputStream getResourceStream(String resource) {
    try {
      URL url = getClass().getResource(resource);
      System.out.println("URL: " + url);
      System.out.println("Read ROM " + resource);
      if (url == null) url = new URL(codebase + resource);
      return url.openConnection().getInputStream();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
