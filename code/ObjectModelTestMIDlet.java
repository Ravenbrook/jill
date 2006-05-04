// $Header$

import j2meunit.midletui.TestRunner;

/**
 * J2MEUnit TestRunner MIDlet.  Completely untested.
 */
public class ObjectModelTestMIDlet extends TestRunner {
  public ObjectModelTestMIDlet() { }

  public void startApp() {
    start(new String[] { "ObjectModelTest" });
  }
}

