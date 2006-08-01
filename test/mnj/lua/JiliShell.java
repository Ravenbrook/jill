// $Header$

package mnj.lua;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;


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
    public static final int EXIT_STATE  = 0x04 ;
    public static final int EXECUTE_STATE      = 0x06 ;
    public static final int PROMPT_STATE       = 0x07 ;

    private Display the_display = Display.getDisplay (this) ;
    private Form title_screen ;
    private TextBox the_output_screen ;
    private TextField the_input_box ;

    public JiliShell ()
    {
        Command exit_command = new Command ("EXIT", Command.EXIT,
            HIGHISH_PRIO | EXIT_STATE) ;
        Command exec_command = new Command ("Exec", Command.SCREEN,
            HIGH_PRIO | EXECUTE_STATE) ;
        Command back_command = new Command ("Again", Command.SCREEN,
            HIGH_PRIO | PROMPT_STATE) ;

        title_screen = new Form ("Jili Shell") ;

        the_output_screen = new TextBox ("output", "", 2048, 0) ;
        the_input_box  = new TextField ("input", "", 1024, 0) ;
        the_input_box.setInitialInputMode("MIDP_LOWERCASE_LATIN");

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
    {
        loseLua () ;
    }

    public synchronized void commandAction (Command c, Displayable d)
    {
        int prio = c.getPriority () ;
        int new_state = prio & STATE_MASK ;

        handle_new_state (new_state) ;
    }

    private void handle_new_state (int new_state)
    {
        switch (new_state)
        {
        case EXIT_STATE:
            destroyApp (true) ;
            notifyDestroyed () ;
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

    private String describe (Object o)
    {
        if (o == null)
        {
          return "null";
        }
        return o.getClass().getName()+": "+o.toString() ;
    }

    private Lua l = null ;

    private void getLua ()
    {
        l = new Lua () ;
        BaseLib.open (l) ;
        PackageLib.open(l);
        MathLib.open(l);
        OSLib.open(l);
        StringLib.open(l);
        TableLib.open(l);
    }

    private void loseLua ()
    {
        l = null ;
    }

    private void execute ()
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
            result = describe (obj);
        }
        else
        {
            result = "Error: "+Integer.toString(res) ;
            switch (res)
            {
            case Lua.ERRRUN:    result = result + " Runtime error" ; break ;
            case Lua.ERRSYNTAX: result = result + " Syntax error" ; break ;
            case Lua.ERRERR:    result = result + " Error error" ; break ;
            case Lua.ERRFILE:   result = result + " File error" ; break ;
            }
        }
        l.setTop (0) ;
        the_output_screen.setString (result) ;
    }
}
