// $Header$

import j2meunit.midletui.TestRunner;

/**
 * J2MEUnit TestRunner MIDlet.
 */
public class METestMIDlet extends TestRunner
{
  public METestMIDlet() { }

  public void startApp()
  {
    start(new String[] { "METest" });
  }
}

