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
    public Lua l;
    
    public long timeit(String name)
    {
        if(l == null)
        {
            l = new Lua () ;
            BaseLib.open (l) ;
            StringLib.open(l);
            TableLib.open(l);
            OSLib.open(l);
            MathLib.open(l);
            PackageLib.open(l);   
        }
        
        l.setTop(0) ;
        l.loadFile(name);
        long t0 = System.currentTimeMillis();
        int res = l.pcall(0, 0, null);
        long t1 = System.currentTimeMillis();
        return t1-t0;
    }
    
    public SpeedMIDlet()
    {
        long t = timeit("speed/fannkuch.lua");
        
        Form form1 = new Form("Speed Test");
        form1.append( new StringItem("Time: " + t, null ) );
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
