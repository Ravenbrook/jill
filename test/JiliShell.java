import java.io.*;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;


public class JiliShell extends MIDlet implements CommandListener
{
    private static final long ALERT_TIMEOUT =   4000L ; // 4 seconds
    private static final long LONGER_TIMEOUT = 12000L ; // 12 seconds

    public static final int STATE_MASK = 0x00FF ;
    public static final int STATE_SHIFT = 8 ;

    private static final int HIGH_PRIO          = 0x1 << STATE_SHIFT ;
    private static final int HIGHISH_PRIO       = 0x2 << STATE_SHIFT ;
    private static final int NORM_PRIO          = 0x3 << STATE_SHIFT ;
    private static final int LOW_PRIO           = 0x4 << STATE_SHIFT ;
    private static final int LOWLOW_PRIO        = 0x5 << STATE_SHIFT ;

    public static final int INVALID_STATE      = 0x00 ;
    public static final int LEAVE_GAME_STATE   = 0x01 ;
    public static final int FAILED_STATE       = 0x02 ;
    public static final int SUCCESS_STATE      = 0x03 ;
    public static final int REALLY_EXIT_STATE  = 0x04 ;
    public static final int ENTER_STATE        = 0x05 ;
    public static final int EXECUTE_STATE      = 0x06 ;
    public static final int PROMPT_STATE       = 0x07 ;

    public static final String version = "0.0" ;

    private int state = INVALID_STATE ;

    protected Form title_screen ;
    protected Display the_display ;
    protected Form exit_confirm_screen ;
    protected Command exit_command ;
    protected Command exec_command ;
    protected Command back_command ;
    protected TextBox the_output_screen ;
    protected TextField the_input_box ;
    protected Command ack_succ_command ;
    protected Command ack_fail_command ;

    protected String appName = "" ;

    public JiliShell ()
    {
        appName = "Jili Test MIDlet v"+version ;
        the_display = Display.getDisplay (this) ;
        exit_command = new Command ("EXIT", Command.EXIT, HIGHISH_PRIO | LEAVE_GAME_STATE) ;
        exec_command = new Command ("Exec", Command.SCREEN, HIGH_PRIO | EXECUTE_STATE) ;
        back_command = new Command ("Again", Command.SCREEN, HIGH_PRIO | PROMPT_STATE) ;

        ack_fail_command = new Command ("DISMISS", Command.OK, LOW_PRIO | FAILED_STATE) ;
        ack_succ_command = new Command ("DISMISS", Command.OK, LOW_PRIO | SUCCESS_STATE) ;

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

        exit_confirm_screen = new Form ("Really quit?") ;
        exit_confirm_screen.append (new StringItem (null, "Do you really wish to quit?")) ;
        exit_confirm_screen.setCommandListener (this) ;
        exit_confirm_screen.addCommand (new Command ("EXIT", Command.OK, LOW_PRIO | SUCCESS_STATE)) ;
        exit_confirm_screen.addCommand (new Command ("NO", Command.CANCEL, LOW_PRIO | FAILED_STATE)) ;
    }

    protected void startApp ()
    {
        /*
        try
        {
            // super.startApp () ;
        }
        catch (MIDletStateChangeException e)
        {
            return ;
        }
        */
        getLua () ;
        the_display.setCurrent (title_screen) ;
    }

    protected void pauseApp ()
    {
    }

    public void destroyApp (boolean unconditional) throws MIDletStateChangeException 
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
        int priority = prio & ~STATE_MASK ;

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

        case LEAVE_GAME_STATE:
            push_states (REALLY_EXIT_STATE, ENTER_STATE) ; // was options
            auto_dismiss (exit_confirm_screen, ack_succ_command, LONGER_TIMEOUT) ;
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

        case SUCCESS_STATE:
            handle_popped_state (true) ;
            break ;

        case FAILED_STATE:
            handle_popped_state (false) ;
            break ;

        case EXECUTE_STATE:
            execute() ;
            the_display.setCurrent (the_output_screen) ;
            break ;

        case PROMPT_STATE:
            the_display.setCurrent (title_screen) ;
            break ;

            /*
        case SHOW_DIALOG_STATE:
            dialog_screen.setString (getDialog ()) ;
            push_states (old_state, old_state) ;
            the_display.setCurrent (dialog_screen) ;
            break ;
            */

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

    // Command state stack
    private long prev_works_state = -1L ;
    private long prev_fails_state = -1L ;
    
    private void push_state (int state)
    {
        push_states (state, state) ;
    }
    private void push_states (int success, int fail)
    {
        prev_works_state <<= 16 ;
        prev_works_state |= success & 0xFFFF ;
        prev_fails_state <<= 16 ;
        prev_fails_state |= fail & 0xFFFF ;
    }

    private void handle_popped_state (boolean success)
    {
        int new_state = pop_state (success) ;
        handle_new_state (new_state) ;
    }

    private int pop_state (boolean success)
    {
        cancel_dismisser () ;
        int state = (int) (success ? prev_works_state : prev_fails_state) & 0xFFFF ;
        prev_works_state >>= 16 ;
        prev_fails_state >>= 16 ;
        return state ;
    }
    class dismisser_thread extends Thread
    {
        private long time ;
        private CommandListener listener ;
        private Command ok_command ;
        private Command cancel_command ;
        private Displayable displayable ;
        private boolean cancelled = false ;

        public dismisser_thread (long time, CommandListener listener, 
                                 Command ok_command, Command cancel_command, Displayable displayable)
        {
            this.time           = time ;
            this.listener       = listener ;
            this.ok_command     = ok_command ;
            this.cancel_command = cancel_command ;
            this.displayable    = displayable ;
            if (listener == null || ok_command == null || displayable == null)
                throw new RuntimeException ("dismisser_thread problem") ;
        }

        // cancel is called if the user responds to the screen.
        public synchronized void cancel () 
        {
            cancelled = true ;
        }

        public void run ()
        {
            JiliShell.sleep (time) ;
            synchronized (this)
            {
                if (!cancelled)
                    listener.commandAction (ok_command, displayable) ;
            }
        }
    }

    private static void sleep (long ms)
    {
        try { Thread.sleep (ms); } 
        catch (InterruptedException e) {}
    }


    private static dismisser_thread dismisser = null ;

    private synchronized void cancel_dismisser ()
    {
        if (dismisser != null)
            dismisser.cancel () ;
    }

    private synchronized void auto_dismiss (Displayable disp, Command comm)
    {
        auto_dismiss (disp, comm, ALERT_TIMEOUT) ;
    }

    private synchronized void auto_dismiss (Displayable disp, Command comm, long timeout)
    {
        if (disp == null || comm == null)
            throw new RuntimeException ("auto_dismiss problem") ;
        cancel_dismisser () ;
        the_display.setCurrent (disp) ;
        dismisser = new dismisser_thread (timeout, JiliShell.this, comm, null, disp) ;
        dismisser.start () ;
    }

    private synchronized void auto_dismiss (Displayable disp, Command ok_comm, Command cancel_comm, long timeout)
    {
        if (disp == null || ok_comm == null)
            throw new RuntimeException ("auto_dismiss problem") ;
        cancel_dismisser () ;
        the_display.setCurrent (disp) ;
        dismisser = new dismisser_thread (timeout, JiliShell.this, ok_comm, cancel_comm, disp) ;
        dismisser.start () ;
    }


}
