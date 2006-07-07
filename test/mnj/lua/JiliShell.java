// $Header$

package mnj.lua;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;


public class JiliShell extends MIDlet implements CommandListener
{
    public static final int STATE_MASK = 0x00FF ;
    public static final int STATE_SHIFT = 8 ;

    private static final int HIGH_PRIO          = 0x1 << STATE_SHIFT ;
    private static final int HIGHISH_PRIO       = 0x2 << STATE_SHIFT ;
    private static final int NORM_PRIO          = 0x3 << STATE_SHIFT ;
    private static final int LOW_PRIO           = 0x4 << STATE_SHIFT ;
    private static final int LOWLOW_PRIO        = 0x5 << STATE_SHIFT ;

    public static final int INVALID_STATE      = 0x00 ;
    public static final int REALLY_EXIT_STATE  = 0x04 ;
    public static final int ENTER_STATE        = 0x05 ;
    public static final int EXECUTE_STATE      = 0x06 ;
    public static final int PROMPT_STATE       = 0x07 ;

    public static final String version = "0.0" ;

    private int state = INVALID_STATE ;

    protected Form title_screen ;
    protected Display the_display ;
    protected Command exit_command ;
    protected Command exec_command ;
    protected Command back_command ;
    protected TextBox the_output_screen ;
    protected TextField the_input_box ;

    protected String appName = "" ;

    public JiliShell ()
    {
        appName = "Jili Test MIDlet v"+version ;
        the_display = Display.getDisplay (this) ;
        exit_command = new Command ("EXIT", Command.EXIT,
          HIGHISH_PRIO | REALLY_EXIT_STATE) ;
        exec_command = new Command ("Exec", Command.SCREEN, HIGH_PRIO | EXECUTE_STATE) ;
        back_command = new Command ("Again", Command.SCREEN, HIGH_PRIO | PROMPT_STATE) ;

        title_screen = new Form ("Jili Shell") ;
        title_screen.append (new StringItem (null, appName)) ;

        the_output_screen = new TextBox ("--output--", "", 2048, 0) ;
        the_input_box  = new TextField ("input", "", 1024, 0) ;

        title_screen.append (the_input_box) ;

        title_screen.setCommandListener (this) ;
        title_screen.addCommand (exit_command) ;
        title_screen.addCommand (exec_command) ;

        the_output_screen.setCommandListener (this) ;
        the_output_screen.addCommand (exit_command) ;
        the_output_screen.addCommand (back_command) ;
    }

    protected void startApp ()
    {
        getLua () ;
        the_display.setCurrent (title_screen) ;
    }

    protected void pauseApp ()
    {
    }

    public void destroyApp (boolean unconditional)
        throws MIDletStateChangeException 
    {
        if (unconditional)
            loseLua () ;
        else
            throw new MIDletStateChangeException() ;
    }

    public synchronized void commandAction (Command c, Displayable d)
    {
        int new_state = INVALID_STATE ;

        int prio = c.getPriority () ;
        new_state = prio & STATE_MASK ;

        handle_new_state (new_state) ;
    }

    public void handle_new_state (int new_state)
    {
        int old_state = state ;
        state = new_state ;
        switch (new_state)
        {
        case ENTER_STATE:
            the_display.setCurrent (title_screen) ;
            break ;

        case REALLY_EXIT_STATE:
            try
            {
                destroyApp (true) ;
                notifyDestroyed () ;
            }
            catch (MIDletStateChangeException e)
            {}
            break ;

        case EXECUTE_STATE:
            execute() ;
            the_display.setCurrent (the_output_screen) ;
            break ;

        case PROMPT_STATE:
            the_display.setCurrent (title_screen) ;
            break ;
        }

    }

    String describe (Object o)
    {
        return o.getClass().getName()+": "+o.toString() ;
    }

    Lua l = null ;

    void getLua ()
    {
        l = new Lua () ;
        BaseLib.open (l) ;
    }

    void loseLua ()
    {
        l = null ;
    }

    void execute ()
    {
        String result = "** no Lua! **" ;
        if (l == null)
        {
            the_output_screen.setString (result) ;
            return ;
        }
        String input = the_input_box.getString() ;
        l.setTop(0) ;
        int res = l.doString (input) ;
        if (res == 0)
        {
            Object obj = l.value(1) ;
            result = obj == null ? "NULL" :
                describe (obj);
        }
        else
        {
            result = "Error: "+Integer.toString(res) ;
            switch (res)
            {
            case Lua.ERRRUN:    result = result + " Runtime error" ; break ;
            case Lua.ERRSYNTAX: result = result + " Syntax error" ; break ;
            case Lua.ERRMEM:    result = result + " Memory error" ; break ;
            case Lua.ERRERR:    result = result + " Error error" ; break ;
            case Lua.ERRFILE:   result = result + " File error" ; break ;
            }
        }
        l.setTop (0) ;
        the_output_screen.setString (result) ;
    }
}
