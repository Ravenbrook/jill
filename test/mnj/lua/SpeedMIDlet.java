/**
 * SpeedMIDlet.java
 * Runs a bunch of lua scripts designed to measure speed.
 */

package mnj.lua;

import java.io.Reader;
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class SpeedMIDlet extends MIDlet 
{
    public SpeedMIDlet()
    {
        String report = Speed.report();
        
        Form form1 = new Form("Speed Test");
        form1.append( new StringItem(report, null ) );
        Display.getDisplay(this).setCurrent(form1);
    }

    public void startApp()
    {
        
    }

    public void pauseApp()
    {
    }
    
    public void destroyApp(boolean unconditional)
    {
    }
}
