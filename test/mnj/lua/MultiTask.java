// $Header$

package mnj.lua;

/**
 * <p>
 * This test/example shows how it is possible to use Jili's coroutine
 * features to effectively run multiple co-operative tasks together
 * (multi-tasking).
 * </p>
 * <p>
 * A number of instances of the script MultiTask.lua are excuted.  Each
 * script is executed in its own Lua thread (coroutine) that all share
 * the same main Lua thread.  The environment in which these scripts are
 * executed includes two Lua functions (implemented in Java in this
 * file): mkport and read.  The main Lua state, the threads, the
 * scheduler state, are all created and initialised in {@link
 * #init}.
 * </p>
 * <p>
 * The Lua function mkport creates an I/O port from a string.
 * In this test the port so
 * created will (eventually) return (via the read function) the contents
 * of the string passed to mkport.
 * </p>
 * <p>
 * The Lua function read(x) reads some amount of character data from
 * the port x.  If no data is available then read will suspend the Lua
 * thread, arranging that a subsequent resume will retry the read.  In
 * this example, read will pretend that no data is available for some of
 * its invocations.
 * </p>
 * <p>
 * The script, MultiTask.lua, receives 2 arguments: the script
 * identifier (used when printing out messages), and the argument to the
 * port call (which will become the character data read back).
 * </p>
 * <h2>Things to note</h2>
 * <p>
 * The <code>MultiTask.lua</code> script knows nothing about the
 * implementation of <code>read</code> nor about being a coroutine.  It
 * executes oblivious to the fact that it is being suspended and being
 * run concurrently with other scripts behind the scenes.
 * </p>
 * <p>
 * The read function consists of two parts.  A primitive
 * read function implemented in Java, {@link MultiLib#read}; and a "wrapper"
 * that retries the primitive read function when necessary.  This
 * wrapper is actually implemented in Lua and is a closure returned by a
 * "make resumable" function.  The "make resumable" function is loaded
 * from a Java string and immediately executed, see {@link MultiLib#open},
 * but in reality its loaded form would be stored somewhere convenient
 * and invoked several times, once for each function that needed to
 * become resumable like read.  Loading it from a string each time
 * requires that the Lua code be compiled each time and is extremely
 * inefficient.
 * </p>
 * <p>
 * The wrapper for our read function (which is actually the function
 * value stored in the Lua global "read") communicates with the thread
 * scheduler via a magic distinguished value.  This magic value is
 * shared between the scheduler, which uses it when it resumes a Lua
 * thread, and the wrapper function.  They way I've arranged things here
 * the magic value is actually an upvalue of the wrapper function and is
 * passed into the "make resumable" function as an argument.  The magic
 * value is stored in the Lua registry.  Note that no Lua code, apart
 * from the "make resumable" function, has
 * direct access to the magic value.
 * </p>
 * <p>
 * The primitive read function, {@link MultiLib#read}, knows nothing about
 * the scheduler directly, nor about being resumed.  All it does is invoke
 * <code>return L.yield(0)</code> when it wishes to block.
 * In reality the read
 * code would be waiting for some sort of event and would arrange that
 * the scheduler would ideally only resume it when the event occurred.
 * In addition, read could communicate data to the scheduler that
 * invoked <code>Lua.resume</code> by pushing some arguments onto
 * the Lua stack and
 * invoking <code>return L.yield(n)</code> where <code>n > 0</code>;
 * the arguments pushed in this
 * way become the result values of the call to <code>Lua.resume</code>.
 * An exception to be thrown could be passed in this way, for example.
 * </p>
 * <p>
 * Jili also supports having the VM be suspended by throwing an
 * Exception (eg TaskManagerException), see {@link Lua#yield}.
 * </p>
 * <p>
 * This example can be run in JSE by invoking this class:
 * </p>
 * <pre>
 * java -classpath test-compiled:compiled mnj.lua.MultiTask
 * </pre>
 * <p>
 * or perhaps more simply "ant multitask".  The code itself should also
 * run in JME, but there's no MIDlet provided, and of course, who knows
 * where the printed output will go.
 * </p>
 */
public final class MultiTask
{
  /** Main Lua state. */
  Lua main;
  /** Lua state for each thread: */
  Lua[] thread;
  /** Thread state: one of INITIAL/RUNNING/COMPLETE or error state.
   */
  int[] state;
  private static final int INITIAL = 0;
  private static final int RUNNING = -1;
  private static final int COMPLETE = -2;
  /**
   * Each thread's eventual result.
   */
  Object[] result;

  /** port data for each thread. */
  private static final String[] data =
    {
      "the quick brown fox jumps over the lazy dog",
      "Once upon a midnight dreary, while I pondered, weak and weary,",
      "When I do count the clock that tells the time,",
    };

  public static void main(String[] arg)
  {
    MultiTask trial = new MultiTask();
    trial.init();
    trial.run();
  }

  private void init()
  {
    main = new Lua();
    // A magic value is stashed in the registry; it's used to
    // communicate between the thread scheduler, tick() in this code,
    // and resumable IO functions (see MultiLib.open and its use of the
    // "make resumable" higher order function).
    main.setField(main.getRegistry(), "AGAIN", main.newTable());
    BaseLib.open(main);
    StringLib.open(main);       // string.rep is required.
    MultiLib.open(main);        // declared in this file.
    // n is the number of threads to run.
    int n = data.length;
    thread = new Lua[n];
    state = new int[n];
    result = new Object[n];
    for (int i=0; i<n; ++i)
    {
      Lua s = main.newThread();
      thread[i] = s;
      int status = s.loadFile("MultiTask.lua");
      if (status != 0)
      {
        throw new IllegalArgumentException((String)s.value(-1));
      }
      s.pushNumber(i);
      s.pushString(data[i]);
      state[i] = INITIAL;
    }
  }

  static Object again(Lua L)
  {
    return L.getField(L.getRegistry(), "AGAIN");
  }

  /**
   * Runs all thread until they reach an error state or complete.
   */
  private void run()
  {
    while (tickAny())
    {
    }

    for (int i=0; i<thread.length; ++i)
    {
      if (state[i] == COMPLETE)
      {
        System.out.println("Thread: " + i + " completed.  Result: " +
            result[i]);
      }
      else
      {
        System.out.println("Thread: " + i + " error.  Status: " +
            state[i]);
      }
    }
  }

  /**
   * Cause some thread to make progress.
   * @return true if and only if some thread ran.
   */
  private boolean tickAny()
  {
    boolean any = false;

    // To avoid starvation each thread gets one chance.
    for (int i=0; i < thread.length; ++i)
    {
      if (state[i] == INITIAL || state[i] == RUNNING)
      {
        tick(i);
        any = true;
      }
    }
    return any;
  }

  /** 
   * @param i  thread index.
   */
  private void tick(int i)
  {
    Lua L = thread[i];
    int status = 99;

    switch (state[i])
    {
      case INITIAL:
        // Assumes script and args are already on stack
        status = L.resume(L.getTop()-1);
        state[i] = RUNNING;
        break;

      case RUNNING:
        L.push(again(L));
        status = L.resume(1);
        break;

      case COMPLETE:
      default:
        return;
    }
    
    switch (status)
    {
      case 0:
        state[i] = COMPLETE;
        result[i] = L.value(-1);
        return;

      case Lua.YIELD:
        return;

      default:
        // An error occurred.
        System.out.println(L.value(-1));
        state[i] = status;
        return;
    }
  }
}

final class MultiLib extends LuaJavaCallback
{
  int which;

  private static final int READ = 1;
  private static final int MKPORT = 2;

  public int luaFunction(Lua L)
  {
    switch (which)
    {
      case READ:
        return read(L);
      case MKPORT:
        return mkport(L);
    }
    return 0;
  }

  MultiLib(int which)
  {
    this.which = which;
  }

  static void open(Lua L)
  {
    L.setGlobal("mkport", new MultiLib(MKPORT));

    // Slightly magic "make resumable" function.
    // Receives a function, f, as an argument and returns a function,
    // Rf,  that is a resumable version of f.  Calling the Rf
    // function has the same effect as calling f.
    // If f returns a magic value (specified as the
    // second argument to this function) then Rf arranges to call f
    // again (instead of returning the magic value).
    int status = L.loadString(
      "local f, magic = ...\n" +
      "return function(...)\n" +
      "  local function capture(...)\n" +
      "      local n = select('#', ...)\n" +
      "      local t = {...}\n" +
      "      return function() return unpack(t, 1, n) end\n" +
      "    end\n" +
      "  while true do\n" +
      "    local result = capture(f(...))\n" +
      "    if result() ~= magic then\n" +
      "      return result()\n" +
      "    end\n" +
      "  end\n" +
      "end\n" +
      "", null);
    if (status != 0)
    {
      throw new IllegalArgumentException((String)L.value(-1));
    }
    // Now we apply the "make resumable" function to our function read.
    // Note that "make resumable" can be applied to any function.
    L.push(new MultiLib(READ));
    L.push(MultiTask.again(L));
    L.call(2, 1);
    L.setGlobal("read", L.value(-1));
    L.setTop(0);
  }

  private static int read(Lua L)
  {
    L.checkType(1, Lua.TTABLE);
    Object t = L.value(1);
    String s = (String)L.getField(t, "s");
    int i = L.toInteger(L.getField(t, "i"));
    boolean didYield = L.toBoolean(L.getField(t, "yield"));

    if (i == s.length())
    {
      // EOF signalled by empty string.
      L.pushString("");
      return 1;
    }
    char c = s.charAt(i);

    // Simulate the effect of data not being available by yielding
    // every now and then.
    if (!didYield && ((c ^ i) & 1) == 0)
    {
      L.setField(t, "yield", L.valueOfBoolean(true));
      return L.yield(0);
    }
    L.setField(t, "yield", L.valueOfBoolean(false));
    L.push(s.substring(i, i+1));
    ++i;
    L.setField(t, "i", L.valueOfNumber(i));
    return 1;
  }

  private static int mkport(Lua L)
  {
    L.checkString(1);
    // port object is a table with the following fields:
    // .s string, the data that read should return.
    // .i int, the position of the read cursor.
    // .yield boolean, whether previous call to read yielded.
    LuaTable t = L.createTable(0, 3);
    L.setField(t, "s", L.value(1));
    L.setField(t, "i", L.valueOfNumber(0));
    L.setField(t, "yield", L.valueOfBoolean(false));
    L.push(t);
    return 1;
  }
}
