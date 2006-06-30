/*  $Header$
 *  (c) Copyright 2006, Intuwave Ltd. All Rights Reserved.
 *
 *  Although Intuwave has tested this program and reviewed the documentation,
 *  Intuwave makes no warranty or representation, either expressed or implied,
 *  with respect to this software, its quality, performance, merchantability,
 *  or fitness for a particular purpose. As a result, this software is licensed
 *  "AS-IS", and you are assuming the entire risk as to its quality and
 *  performance.
 *
 *  You are granted license to use this code as a basis for your own
 *  application(s) under the terms of the separate license between you and
 *  Intuwave.
 */


import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

// for testing only
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Syntax analyser.  Lexing, parsing, code generation.
 */
final class Syntax
{
  /** End of File, must be -1 as that is what read() returns. */
  private static final int EOZ = -1;

  private static final int FIRST_RESERVED = 257;

  // WARNING: if you change the order of this enumeration,
  // grep "ORDER RESERVED"
  private static final int TK_AND       = FIRST_RESERVED + 0;
  private static final int TK_BREAK     = FIRST_RESERVED + 1;
  private static final int TK_DO        = FIRST_RESERVED + 2;
  private static final int TK_ELSE      = FIRST_RESERVED + 3;
  private static final int TK_ELSEIF    = FIRST_RESERVED + 4;
  private static final int TK_END       = FIRST_RESERVED + 5;
  private static final int TK_FALSE     = FIRST_RESERVED + 6;
  private static final int TK_FOR       = FIRST_RESERVED + 7;
  private static final int TK_FUNCTION  = FIRST_RESERVED + 8;
  private static final int TK_IF        = FIRST_RESERVED + 9;
  private static final int TK_IN        = FIRST_RESERVED + 10;
  private static final int TK_LOCAL     = FIRST_RESERVED + 11;
  private static final int TK_NIL       = FIRST_RESERVED + 12;
  private static final int TK_NOT       = FIRST_RESERVED + 13;
  private static final int TK_OR        = FIRST_RESERVED + 14;
  private static final int TK_REPEAT    = FIRST_RESERVED + 15;
  private static final int TK_RETURN    = FIRST_RESERVED + 16;
  private static final int TK_THEN      = FIRST_RESERVED + 17;
  private static final int TK_TRUE      = FIRST_RESERVED + 18;
  private static final int TK_UNTIL     = FIRST_RESERVED + 19;
  private static final int TK_WHILE     = FIRST_RESERVED + 20;
  private static final int TK_CONCAT    = FIRST_RESERVED + 21;
  private static final int TK_DOTS      = FIRST_RESERVED + 22;
  private static final int TK_EQ        = FIRST_RESERVED + 23;
  private static final int TK_GE        = FIRST_RESERVED + 24;
  private static final int TK_LE        = FIRST_RESERVED + 25;
  private static final int TK_NE        = FIRST_RESERVED + 26;
  private static final int TK_NUMBER    = FIRST_RESERVED + 27;
  private static final int TK_NAME      = FIRST_RESERVED + 28;
  private static final int TK_STRING    = FIRST_RESERVED + 29;
  private static final int TK_EOS       = FIRST_RESERVED + 30;

  private static final int NUM_RESERVED = TK_WHILE - FIRST_RESERVED + 1;

  /** Equivalent to luaX_tokens.  ORDER RESERVED */
  static String[] tokens = new String[]
  {
    "and", "break", "do", "else", "elseif",
    "end", "false", "for", "function", "if",
    "in", "local", "nil", "not", "or", "repeat",
    "return", "then", "true", "until", "while",
    "..", "...", "==", ">=", "<=", "~=",
    "<number>", "<name>", "<string>", "<eof>"
  };

  static Hashtable reserved = new Hashtable();
  static
  {
    for (int i=0; i < NUM_RESERVED; ++i)
    {
      reserved.put(tokens[i], new Integer(FIRST_RESERVED+i));
    }
  }

  // From struct LexState

  /** current character */
  int current;
  /** input line counter */
  int linenumber = 1;
  /** line of last token 'consumed' */
  int lastline = 1;
  /**
   * The token value.  For "punctuation" tokens this is the ASCII value
   * for the character for the token; for other tokens a member of the
   * enum (all of which are > 255).
   */
  int token;
  /** Semantic info for token; a number. */
  double tokenR;
  /** Semantic info for token; a string. */
  String tokenS;

  /** Lookahead token value. */
  int lookahead = TK_EOS;
  /** Semantic info for lookahead; a number. */
  double lookaheadR;
  /** Semantic info for lookahead; a string. */
  String lookaheadS;

  /** Semantic info for return value from {@link Syntax#llex}; a number. */
  double semR;
  /** As <code>semR</code>, for string. */
  String semS;

  /** FuncState for current (innermost) function being parsed. */
  FuncState fs;
  Lua L;

  /** input stream */
  private Reader z;

  /** Buffer for tokens. */
  StringBuffer buff = new StringBuffer();

  /** current source name */
  String source;

  /** locale decimal point. */
  private char decpoint = '.';

  private Syntax(Lua L, Reader z, String source) throws IOException
  {
    this.L = L;
    this.z = z;
    this.source = source;
    next();
  }

  int lastline()
  {
    return lastline;
  }


  // From <ctype.h>

  // Implementations of functions from <ctype.h> are only correct copies
  // to the extent that Lua requires them.

  private static boolean isalnum(int c)
  {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        (c >= '0' && c <='9');
  }

  private static boolean isalpha(int c)
  {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z');
  }

  /** True if and only if the char (when converted from the int) is a
   * control character.
   */
  private static boolean iscntrl(int c)
  {
    return (char)c < 0x20 || c == 0x7f;
  }

  private static boolean isdigit(int c)
  {
    return c >= '0' && c <= '9';
  }

  private static boolean isspace(int c)
  {
    return c == ' ' || c == '\t';
  }


  // From llex.c

  private boolean check_next(String set) throws IOException
  {
    if (set.indexOf(current) < 0)
    {
      return false;
    }
    save_and_next();
    return true;
  }

  private boolean currIsNewline()
  {
    return current == '\n' || current == '\r';
  }

  private void inclinenumber() throws IOException
  {
    int old = current;
    // assert currIsNewline();
    next();     // skip '\n' or '\r'
    if (currIsNewline() && current != old)
    {
      next();   // skip '\n\r' or '\r\n'
    }
    if (++linenumber < 0)       // overflow
    {
      xSyntaxerror("chunk has too many lines");
    }
  }

  FuncState linkfs(FuncState newfs)
  {
    FuncState oldfs = fs;
    fs = newfs;
    return oldfs;
  }


  private void lua_assert (boolean b, String routine)
  {
    if (!b)
    {
      System.out.println ("lua_assert failure in "+routine) ;
      xSyntaxerror ("lua_assert failure in "+routine) ;
    }
  }

  private int skip_sep () throws IOException
  {
    int count = 0;
    int s = current;
    lua_assert(s == '[' || s == ']', "skip_sep()");
    save_and_next();
    while (current == '=')
    {
      save_and_next();
      count++;
    }
    return (current == s) ? count : (-count) - 1;
  }

  private void read_long_string (boolean is_string, int sep) throws IOException
  {
    int cont = 0;
    save_and_next();  /* skip 2nd `[' */
    if (currIsNewline())  /* string starts with a newline? */
      inclinenumber();  /* skip it */
    boolean looping = true ;
    while (looping)
    {
      switch (current)
      {
        case EOZ:
          xLexerror(is_string ? "unfinished long string" : "unfinished long comment",
                    TK_EOS);
          break;  /* to avoid warnings */
        case ']':
          if (skip_sep() == sep)
          {
            save_and_next();  /* skip 2nd `]' */
            looping = false ;
          }
          break;

        case '\n':
        case '\r':
          save('\n');
          inclinenumber();
          if (!is_string)
            buff.setLength(0) ; /* avoid wasting space */
          break;

        default:
          if (is_string) save_and_next();
          else next();
      }
    } 
    if (is_string)
    {
      String rawtoken = buff.toString();
      int trim_by = 2+sep ;
      semS = rawtoken.substring(trim_by, rawtoken.length()-trim_by) ;
    }
  }


  /** Lex a token and return it.  The semantic info for the token is
   * stored in <code>this.semR</code> or <code>this.semS</code> as
   * appropriate.
   */
  private int llex() throws IOException
  {
    buff.setLength(0);
    while (true)
    {
      switch (current)
      {
        case '\n':
        case '\r':
          inclinenumber();
          continue;
        case '-':
          next();
          if (current != '-')
            return '-';
          /* else is a comment */
          next();
          if (current == '[')
          {
            int sep = skip_sep();
            buff.setLength(0) ; // buff.zResetbuffer();  /* `skip_sep' may dirty the buffer */
            if (sep >= 0)
            {
              read_long_string(false, sep);  /* long comment */
              buff.setLength(0) ; //buff.zResetbuffer();
              continue;
            }
          }
          /* else short comment */
          while (!currIsNewline() && current != EOZ)
            next();
          continue;

        case '[':
          int sep = skip_sep();
          if (sep >= 0)
          {
            read_long_string(true, sep);
            return TK_STRING;
          }
          else if (sep == -1)
            return '[';
          else
            xLexerror("invalid long string delimiter", TK_STRING);

        case '=':
          next() ;
          if (current != '=')
          { return '=' ; }
          else
          {
            next() ;
            return TK_EQ ;
          }
        case '<':
          next() ;
          if (current != '=')
          { return '<' ; }
          else
          {
            next () ;
            return TK_LE ;
          }             
        case '>':
          next() ;
          if (current != '=')
          { return '>' ; }
          else
          {
            next () ;
            return TK_GE ;
          }             
        case '~':
          next();
          if (current != '=')
          { return '~'; }
          else
          {
            next();
            return TK_NE;
          }
        case '"':
        case '\'':
          read_string(current);
          return TK_STRING;
        case '.':
          save_and_next();
          if (check_next("."))
          { return check_next(".") ? TK_DOTS : TK_CONCAT ; }
          else if (!isdigit(current))
          { return '.'; }
          else
          {
            read_numeral();
            return TK_NUMBER;
          }
        case EOZ:
          return TK_EOS;
        default:
          if (isspace(current))
          {
            // assert !currIsNewline();
            next();
            continue;
          }
          else if (isdigit(current))
          {
            read_numeral();
            return TK_NUMBER;
          }
          else if (isalpha(current) || current == '_')
          {
            // identifier or reserved word
            do
            {
              save_and_next();
            } while (isalnum(current) || current == '_');
            String s = buff.toString();
            Object t = reserved.get(s);
            if (t == null)
            {
              semS = s;
              return TK_NAME;
            }
            else
            {
              return ((Integer)t).intValue();
            }
          }
          else
          {
            int c = current;
            next();
            return c; // single-char tokens
          }
      }
    }
  }

  private void next() throws IOException
  {
    current = z.read();
  }

  /** Reads number.  Writes to semR. */
  private void read_numeral() throws IOException
  {
    // assert isdigit(current);
    do
    {
      save_and_next();
    } while (isdigit(current) || current == '.');
    if (check_next("Ee"))       // 'E' ?
    {
      check_next("+-"); // optional exponent sign
    }
    while (isalnum(current) || current == '_')
    {
      save_and_next();
    }
    // :todo: consider doing PUC-Rio's decimal point tricks.
    String s = buff.toString();
    try
    {
      semR = Double.parseDouble(s);
      return;
    }
    catch (NumberFormatException e)
    {
      xLexerror("malformed number", TK_NUMBER);
    }
  }

  /** Reads string.  Writes to semS. */
  private void read_string(int del) throws IOException
  {
    save_and_next();
    while (current != del)
    {
      switch (current)
      {
        case EOZ:
          xLexerror("unfinished string", TK_EOS);
          continue;     // avoid compiler warning
        case '\n':
        case '\r':
          xLexerror("unfinished string", TK_STRING);
          continue;     // avoid compiler warning
        case '\\':
        {
          int c;
          next();       // do not save the '\'
          switch (current)
          {
            case 'a': c = 7; break;     // no '\a' in Java.
            case 'b': c = '\b'; break;
            case 'f': c = '\f'; break;
            case 'n': c = '\n'; break;
            case 'r': c = '\r'; break;
            case 't': c = '\t'; break;
            case 'v': c = 11; break;    // no '\v' in Java.
            case '\n': case '\r':
              save('\n');
              inclinenumber();
              continue;
            case EOZ:
              continue; // will raise an error next loop
            default:
              if (!isdigit(current))
              {
                save_and_next();        // handles \\, \", \', \?
              }
              else    // \xxx
              {
                int i = 0;
                c = 0;
                do
                {
                  c = 10*c + (current - '0');
                  next();
                } while (++i<3 && isdigit(current));
                // In unicode, there are no bounds on a 3-digit decimal.
                save(c);
              }
              continue;
          }
          save(c);
          next();
          continue;
        }
        default:
          save_and_next();
      }
    }
    save_and_next();    // skip delimiter
    String rawtoken = buff.toString() ;
    semS = rawtoken.substring(1, rawtoken.length()-1) ;
  }

  private void save()
  {
    buff.append((char)current);
  }

  private void save(int c)
  {
    buff.append((char)c);
  }

  private void save_and_next() throws IOException
  {
    save();
    next();
  }

  /** Getter for source. */
  String source()
  {
    return source;
  }

  private String txtToken(int token)
  {
    switch (token)
    {
      case TK_NAME:
      case TK_STRING:
      case TK_NUMBER:
        return buff.toString();
      default:
        return xToken2str(token);
    }
  }

  /** Equivalent to <code>luaX_lexerror</code>. */
  private void xLexerror(String msg, int token)
  {
    msg = source + ":" + linenumber + ": " + msg;
    if (token != 0)
    {
      msg = msg + " near '" + txtToken(token) + "'";
    }
    L.dThrow(Lua.ERRSYNTAX);
  }

  /** Equivalent to <code>luaX_next</code>. */
  private void xNext() throws IOException
  {
    lastline = linenumber;
    if (lookahead != TK_EOS)    // is there a look-ahead token?
    {
      token = lookahead;        // Use this one,
      tokenR = lookaheadR;
      tokenS = lookaheadS;
      lookahead = TK_EOS;       // and discharge it.
    }
     else
     {
      token = llex();
      tokenR = semR;
      tokenS = semS;
    }
  }

  /** Equivalent to <code>luaX_syntaxerror</code>. */
  void xSyntaxerror(String msg)
  {
    xLexerror(msg, token);
  }

  private String xToken2str(int token)
  {
    if (token < FIRST_RESERVED)
    {
      // assert token == (char)token;
      if (iscntrl(token))
      {
        return "char(" + token + ")";
      }
      return (new Character((char)token)).toString();
    }
    return tokens[token-FIRST_RESERVED];
  }

  // From lparser.c

  private static boolean block_follow(int token)
  {
    switch (token)
    {
      case TK_ELSE: case TK_ELSEIF: case TK_END:
      case TK_UNTIL: case TK_EOS:
        return true;
      default:
        return false;
    }
  }

  private void check(int c)
  {
    if (token != c)
    {
      error_expected(c);
    }
  }

  /**
   * @param what   the token that is intended to end the match.
   * @param who    the token that begins the match.
   * @param where  the line number of <var>what</var>.
   */
  private void check_match(int what, int who, int where)
      throws IOException
  {
    if (!testnext(what))
    {
      if (where == linenumber)
      {
        error_expected(what);
      }
      else
      {
        xSyntaxerror("'" + xToken2str(what) + "' expected (to close '" +
            xToken2str(who) + "' at line " + where + ")");
      }
    }
  }

  private void close_func()
  {
    removevars(0);
    fs.kRet(0, 0);  // final return;
    fs.close();
    System.out.println ("debug:") ;
    debug_closures (fs) ;
    System.out.println ("end debug:") ;
    lua_assert (fs != fs.prev, "close_func()") ; // :todo: check this is a valid assertion to make
    fs = fs.prev;
  }

    void debug_closures (FuncState fs)
    {
        debug_fs (fs) ;
        if (fs.prev != null)
            debug_closures (fs.prev) ;
    }

    void debug_fs (FuncState fs)
    {
        debug_proto (fs.f) ;
    }

    void debug_proto (Proto p)
    {
        System.out.println ("    Proto: "+p.source) ;
        System.out.println ("      vararg = "+p.is_vararg) ;
        System.out.println ("      sizecode = "+p.sizecode) ;
        System.out.println ("      sizek = "+p.sizek) ;
        System.out.println ("      sizep = "+p.sizep) ;
        System.out.println ("      sizeupvalues = "+p.sizeupvalues) ;
        System.out.println ("      linedefined = "+p.linedefined) ;
        System.out.println ("      lastlinedefined = "+p.lastlinedefined) ;
        System.out.println ("      CODE:") ;
        for (int i = 0 ; i < p.sizecode ; i++)
            System.out.println ("        "+i+": "+Integer.toHexString(p.code[i])) ;

    }            

  private void codestring(Expdesc e, String s)
  {
    e.init(Expdesc.VK, fs.kStringK(s));
  }

  private void checkname(Expdesc e) throws IOException
  {
    codestring(e, str_checkname());
  }

  private void enterlevel()
  {
      L.nCcalls ++ ;
  }

  private void error_expected(int token)
  {
    xSyntaxerror("'" + xToken2str(token) + "' expected");
  }

  private void leavelevel()
  {
      L.nCcalls -- ;
  }

  static Proto test_parser (File infile)
  {
      String name = infile.getName() ;
      InputStream in = null ;
      Reader reader = null ;
      try
      {
          in = new FileInputStream (infile) ;
          Lua L = new Lua () ;
          BaseLib.open (L) ;
          reader = new InputStreamReader (in, "UTF-8") ;
          Proto result = parser (L, reader, name) ;

          LuaInternal.debug_compiler (L, result, false) ;
          return result ;
      }
      catch (Exception e)
      {
          System.out.println ("test_parser Excp: "+e.getClass().getName()+": "+e.getMessage()) ;
          e.printStackTrace();
          return null ;
      }
      finally
      {
          if (reader != null)
              try { reader.close () ; } catch (IOException io) {}
      }
  }

  /** Equivalent to luaY_parser. */
  static Proto parser(Lua L, Reader in, String name)
      throws IOException
  {
    Syntax ls = new Syntax(L, in, name);
    FuncState fs = new FuncState(ls);
    fs.f.is_vararg = true;
    ls.xNext();
    System.out.println ("parser A") ;
    ls.chunk();
    System.out.println ("parser B") ;
    ls.check(TK_EOS);
    System.out.println ("parser C") ;
    ls.close_func();
    ls.lua_assert (fs.prev == null,"parser() 2") ;
    ls.lua_assert (fs.f.nups == 0, "parser() 3") ;
    ls.lua_assert (ls.fs == null,  "parser() 4") ;
    return fs.f;
  }

  private void removevars(int tolevel)
  {
    // :todo: consider making a method in FuncState.
    while (fs.nactvar > tolevel)
    {
      fs.getlocvar(--fs.nactvar).endpc = fs.pc;
    }
  }

  private void singlevar(Expdesc var) throws IOException
  {
    String varname = str_checkname();
    if (singlevaraux(fs, varname, var, true) == Expdesc.VGLOBAL)
    {
      var.setInfo(fs.kStringK(varname));
    }
  }

  private int singlevaraux(FuncState fs, String n, Expdesc var, boolean base)
  {
    if (fs == null)     // no more levels?
    {
      var.init(Expdesc.VGLOBAL, Lua.NO_REG);    // default is global variable
      return Expdesc.VGLOBAL;
    }
    else
    {
      int v = fs.searchvar(n);
      if (v >= 0)
      {
        var.init(Expdesc.VLOCAL, v);
        if (!base)
        {
          markupval(v);      // local will be used as an upval
        }
        return Expdesc.VLOCAL;
      }
      else    // not found at current level; try upper one
      {
        if (singlevaraux(fs.prev, n, var, false) == Expdesc.VGLOBAL)
        {
          return Expdesc.VGLOBAL;
        }
        var.upval(indexupvalue(n, var));       // else was LOCAL or UPVAL
        return Expdesc.VUPVAL;
      }
    }
  }

  private String str_checkname() throws IOException
  {
    check(TK_NAME);
    String s = tokenS;
    xNext();
    return s;
  }

  private boolean testnext(int c) throws IOException
  {
    if (token == c)
    {
      xNext();
      return true;
    }
    return false;
  }


  // GRAMMAR RULES

  private void chunk() throws IOException
  {
    // chunk -> { stat [';'] }
    boolean islast = false;
System.out.println ("chunk 1") ;
    enterlevel();
System.out.println ("chunk 2") ;
    while (!islast && !block_follow(token))
    {
System.out.println ("chunk loop 1") ;
      islast = statement();
System.out.println ("chunk loop 2") ;
      testnext(';');
System.out.println ("chunk loop 3") ;
      lua_assert (fs.f.maxstacksize >= fs.freereg &&
                  fs.freereg >= fs.nactvar, 
                  "chunk()");
      fs.freereg = fs.nactvar;
System.out.println ("chunk loop 4") ;
    }
System.out.println ("chunk 3") ;
    leavelevel();
System.out.println ("chunk 4") ;
  }

  private void constructor(Expdesc t) throws IOException
  {
    // constructor -> ??
    int line = linenumber;
    int pc = fs.kCodeABC(Lua.OP_NEWTABLE, 0, 0, 0);
    ConsControl cc = new ConsControl(t) ;
    init_exp(t, Expdesc.VRELOCABLE, pc);
    init_exp(cc.v, Expdesc.VVOID, 0);  /* no value (yet) */
    fs.kExp2nextreg(t);  /* fix it at stack top (for gc) */
    checknext('{');
    do
    {
      lua_assert(cc.v.k == Expdesc.VVOID || cc.tostore > 0, "constructor()");
      if (token == '}')
        break;
      closelistfield(cc);
      switch(token)
      {
        case TK_NAME:  /* may be listfields or recfields */
          xLookahead();
          if (lookahead != '=')  /* expression? */
            listfield(cc);
          else
            recfield(cc);
          break;

        case '[':  /* constructor_item -> recfield */
        recfield(cc);
        break;

        default:  /* constructor_part -> listfield */
          listfield(cc);
          break;
      }
    } while (testnext(',') || testnext(';'));
    check_match('}', '{', line);
    lastlistfield(cc);
    int [] code = fs.f.code ;
    code[pc] = Lua.SETARG_B(code[pc], oInt2fb(cc.na)); /* set initial array size */
    code[pc] = Lua.SETARG_C(code[pc], oInt2fb(cc.nh)); /* set initial table size */
  }

  private int oInt2fb (int x)
  {
    int e = 0;  /* exponent */
    while (x < 0 || x >= 16)
    {
      x = (x+1) >>> 1;
      e++;
    }
    return (x < 8) ? x : (((e+1) << 3) | (x - 8));
  }

  private void recfield (ConsControl cc) throws IOException
  {
    /* recfield -> (NAME | `['exp1`]') = exp1 */
    int reg = fs.freereg;
    Expdesc key = new Expdesc() ;
    Expdesc val = new Expdesc() ;
    if (token == TK_NAME)
    {
      // yChecklimit(fs, cc.nh, MAX_INT, "items in a constructor");
      checkname(key);
    }
    else  /* token == '[' */
      yindex(key);
    cc.nh++;
    checknext('=');
    fs.kExp2RK(key);
    expr(val);
    fs.kCodeABC(Lua.OP_SETTABLE, cc.t.info, fs.kExp2RK(key), fs.kExp2RK(val));
    fs.freereg = reg;  /* free registers */
  }

  private void lastlistfield (ConsControl cc)
  {
    if (cc.tostore == 0)
      return;
    if (hasmultret(cc.v.k))
    {
      fs.kSetmultret(cc.v);
      fs.kSetlist(cc.t.info, cc.na, Lua.MULTRET);
      cc.na--;  /* do not count last expression (unknown number of elements) */
    }
    else
    {
      if (cc.v.k != Expdesc.VVOID)
        fs.kExp2nextreg(cc.v);
      fs.kSetlist(cc.t.info, cc.na, cc.tostore);
    }
  }

  // from lopcodes.h
  static final int LFIELDS_PER_FLUSH = 50 ;

  private void closelistfield (ConsControl cc)
  {
    if (cc.v.k == Expdesc.VVOID)
      return;  /* there is no list item */
    fs.kExp2nextreg(cc.v);
    cc.v.k = Expdesc.VVOID;
    if (cc.tostore == LFIELDS_PER_FLUSH)
    {
      fs.kSetlist(cc.t.info, cc.na, cc.tostore);  /* flush */
      cc.tostore = 0;  /* no more items pending */
    }
  }

  private void expr(Expdesc v) throws IOException
  {
    subexpr(v, 0);
  }

  /** @return number of expressions in expression list. */
  private int explist1(Expdesc v) throws IOException
  {
    // explist1 -> expr { ',' expr }
    int n = 1;  // at least one expression
    expr(v);
    while (testnext(','))
    {
      fs.kExp2nextreg(v);
      expr(v);
      ++n;
    }
    return n;
  }

  private void exprstat() throws IOException
  {
    // stat -> func | assignment
    LHS_assign v = new LHS_assign () ;
    primaryexp(v.v);
    if (v.v.k == Expdesc.VCALL)      // stat -> func
    {
      fs.setargc(v.v, 1); // call statement uses no results
    }
    else      // stat -> assignment
    {
      v.prev = null;
      assignment(v, 1);
    }
  }

/*
** check whether, in an assignment to a local variable, the local variable
** is needed in a previous assignment (to a table). If so, save original
** local value in a safe place and use this safe copy in the previous
** assignment.
*/
  private void check_conflict (LHS_assign lh, Expdesc v)
  {
    int extra = fs.freereg;  /* eventual position to save local variable */
    boolean conflict = false ;
    for (; lh != null; lh = lh.prev)
    {
      if (lh.v.k == Expdesc.VINDEXED)
      {
        if (lh.v.info == v.info)    /* conflict? */
        {
          conflict = true;
          lh.v.info = extra;  /* previous assignment will use safe copy */
        }
        if (lh.v.aux == v.info)    /* conflict? */
        {
          conflict = true;
          lh.v.aux = extra;  /* previous assignment will use safe copy */
        }
      }
    }
    if (conflict)
    {
      fs.kCodeABC(Lua.OP_MOVE, fs.freereg, v.info, 0);  /* make copy */
      fs.kReserveregs(1);
    }
  }

  private void assignment (LHS_assign lh, int nvars) throws IOException
  {
    Expdesc e = new Expdesc () ;
    int kind = lh.v.k ;
    if (!(Expdesc.VLOCAL <= kind && kind <= Expdesc.VINDEXED))
      xSyntaxerror ("syntax error");
    if (testnext(','))    /* assignment -> `,' primaryexp assignment */
    {
      LHS_assign nv = new LHS_assign (lh) ;
      primaryexp(nv.v);
      if (nv.v.k == Expdesc.VLOCAL)
        check_conflict(lh, nv.v);
      assignment(nv, nvars+1);
    }
    else    /* assignment -> `=' explist1 */
    {
      int nexps;
      checknext('=');
      nexps = explist1(e);
      if (nexps != nvars)
      {
        adjust_assign(nvars, nexps, e);
        if (nexps > nvars)
          fs.freereg -= nexps - nvars;  /* remove extra values */
      }
      else
      {
        fs.kSetoneret(e);  /* close last expression */
        fs.kStorevar(lh.v, e);
        return;  /* avoid default */
      }
    }
    init_exp(e, Expdesc.VNONRELOC, fs.freereg-1);  /* default assignment */
    fs.kStorevar(lh.v, e);
  }


  private void funcargs(Expdesc f) throws IOException
  {
    Expdesc args = new Expdesc();
    int line = linenumber;
    switch (token)
    {
      case '(':         // funcargs -> '(' [ explist1 ] ')'
        if (line != lastline)
        {
          xSyntaxerror("ambiguous syntax (function call x new statement)");
        }
        xNext();
        if (token == ')')       // arg list is empty?
        {
          args.setKind(Expdesc.VVOID);
        }
        else
        {
          explist1(args);
          fs.kSetmultret(args);
        }
        check_match(')', '(', line);
        break;

      case '{':         // funcargs -> constructor
        constructor(args);
        break;

      case TK_STRING:   // funcargs -> STRING
        codestring(args, tokenS);
        xNext();        // must use tokenS before 'next'
        break;

      default:
        xSyntaxerror("function arguments expected");
        return;
    }
    // assert (f.kind() == VNONRELOC);
    int nparams;
    int base = f.info();        // base register for call
    if (args.hasmultret())
    {
      nparams = Lua.MULTRET;     // open call
    }
    else
    {
      if (args.kind() != Expdesc.VVOID)
      {
        fs.kExp2nextreg(args);  // close last argument
      }
      nparams = fs.freereg - (base+1);
    }
    f.init(Expdesc.VCALL, fs.kCodeABC(Lua.OP_CALL, base, nparams+1, 2));
    fs.kFixline(line);
    fs.freereg = base+1;        // call removes functions and arguments
                // and leaves (unless changed) one result.
  }

  private void prefixexp(Expdesc v) throws IOException
  {
    // prefixexp -> NAME | '(' expr ')'
    switch (token)
    {
      case '(':
      {
        int line = linenumber;
        xNext();
        expr(v);
        check_match(')', '(', line);
        fs.kDischargevars(v);
        return;
      }
      case TK_NAME:
        singlevar(v);
        return;
      default:
        xSyntaxerror("unexpected symbol");
        return;
    }
  }

  private void primaryexp(Expdesc v) throws IOException
  {
    // primaryexp ->
    //    prefixexp { '.' NAME | '[' exp ']' | ':' NAME funcargs | funcargs }
    prefixexp(v);
    while (true)
    {
      switch (token)
      {
        case '.':  /* field */
          field(v);
          break;

        case '[':  /* `[' exp1 `]' */
          {
            Expdesc key = new Expdesc();
            fs.kExp2anyreg(v);
            yindex(key);
            fs.kIndexed(v, key);
          }
          break;

        case ':':  /* `:' NAME funcargs */
          {
            Expdesc key = new Expdesc() ;
            xNext();
            checkname(key);
            fs.kSelf(v, key);
            funcargs(v);
          }
          break;

        case '(':
        case TK_STRING:
        case '{':     // funcargs
          fs.kExp2nextreg(v);
          funcargs(v);
          break;

        default:
          return;
      }
    }
  }

  private void retstat() throws IOException
  {
    // stat -> RETURN explist
    xNext();    // skip RETURN
    int first = 0, nret;    // registers with returned values
    if (block_follow(token) || token == ';')
    {
      first = nret = 0; // return no values
    }
    else
    {
      Expdesc e = new Expdesc();
      nret = explist1(e);
      if (hasmultret(e.k))
      {
        fs.kSetmultret(e);
        if (e.k == Expdesc.VCALL && nret == 1)    /* tail call? */
        {
          fs.setcode(e, Lua.SET_OPCODE(fs.getcode(e), Lua.OP_TAILCALL));
          lua_assert(Lua.ARGA(fs.getcode(e)) == fs.nactvar, "retstat()");
        }
        first = fs.nactvar;
        nret = Lua.MULTRET;  /* return all values */
      }
      else
      {
        if (nret == 1)          // only one single value?
        {
          first = fs.kExp2anyreg(e);
        }
        else
        {
          fs.kExp2nextreg(e);  /* values must go to the `stack' */
          first = fs.nactvar;  /* return all `active' values */
          lua_assert(nret == fs.freereg - first, "retstat() 2");
        }
      }
    }
    fs.kRet(first, nret);
  }

  private void simpleexp(Expdesc v) throws IOException
  {
    // simpleexp -> NUMBER | STRING | NIL | true | false | ... |
    //              constructor | FUNCTION body | primaryexp
    switch (token)
    {
      case TK_NUMBER:
        init_exp(v, Expdesc.VKNUM, 0);
        v.nval = tokenR;
        break;

      case TK_STRING:
        codestring(v, tokenS);
        break;

      case TK_NIL:
        init_exp(v, Expdesc.VNIL, 0);
        break;

      case TK_TRUE:
        init_exp(v, Expdesc.VTRUE, 0);
        break;
  
      case TK_FALSE:
        init_exp(v, Expdesc.VFALSE, 0);
        break;

      case TK_DOTS:  /* vararg */
        if (!fs.f.is_vararg)
          xSyntaxerror("cannot use \"...\" outside a vararg function");
        // fs.f.is_vararg &= ~VARARG_NEEDSARG;  /* don't need 'arg' */
        init_exp(v, Expdesc.VVARARG, fs.kCodeABC(Lua.OP_VARARG, 0, 1, 0));
        break;

      case '{':   /* constructor */
        constructor(v);
        return;

      case TK_FUNCTION:
        xNext();
        body(v, false, linenumber);
        return;

      default:
        primaryexp(v);
        return;
    }
    xNext();
  }

  private boolean statement() throws IOException
  {
 System.out.println ("statement "+token) ;
    int line = linenumber;
    switch (token)
    {
      case TK_IF:   // stat -> ifstat
        ifstat(line);
        return false;

      case TK_WHILE:  // stat -> whilestat
        whilestat(line);
        return false;

      case TK_DO:       // stat -> DO block END
        xNext();         // skip DO
        block();
        check_match(TK_END, TK_DO, line);
        return false;

      case TK_FOR:      // stat -> forstat
        forstat(line);
        return false;

      case TK_REPEAT:   // stat -> repeatstat
        repeatstat(line);
        return false;

      case TK_FUNCTION:
        funcstat(line); // stat -> funcstat
        return false;

      case TK_LOCAL:    // stat -> localstat
        xNext();         // skip LOCAL
        if (testnext(TK_FUNCTION))  // local function?
          localfunc();
        else
          localstat();
        return false;

      case TK_RETURN:
        retstat();
        return true;  // must be last statement

      case TK_BREAK:  // stat -> breakstat
        xNext();       // skip BREAK
        breakstat();
        return true;  // must be last statement

      default:
        exprstat();
        return false;
    }
  }

  // grep "ORDER OPR" if you change these enums.
  // default access so that FuncState can access them.
  static final int OPR_ADD = 0;
  static final int OPR_SUB = 1;
  static final int OPR_MUL = 2;
  static final int OPR_DIV = 3;
  static final int OPR_MOD = 4;
  static final int OPR_POW = 5;
  static final int OPR_CONCAT = 6;
  static final int OPR_NE = 7;
  static final int OPR_EQ = 8;
  static final int OPR_LT = 9;
  static final int OPR_LE = 10;
  static final int OPR_GT = 11;
  static final int OPR_GE = 12;
  static final int OPR_AND = 13;
  static final int OPR_OR = 14;
  static final int OPR_NOBINOPR = 15;

  static final int OPR_MINUS = 0;
  static final int OPR_NOT = 1;
  static final int OPR_LEN = 2;
  static final int OPR_NOUNOPR = 3;

  /** Converts token into binary operator.  */
  private static int getbinopr(int op)
  {
    switch (op)
    {
      case '+': return OPR_ADD;
      case '-': return OPR_SUB;
      case '*': return OPR_MUL;
      case '/': return OPR_DIV;
      case '%': return OPR_MOD;
      case '^': return OPR_POW;
      case TK_CONCAT: return OPR_CONCAT;
      case TK_NE: return OPR_NE;
      case TK_EQ: return OPR_EQ;
      case '<': return OPR_LT;
      case TK_LE: return OPR_LE;
      case '>': return OPR_GT;
      case TK_GE: return OPR_GE;
      case TK_AND: return OPR_AND;
      case TK_OR: return OPR_OR;
      default: return OPR_NOBINOPR;
    }
  }

  private static int getunopr(int op)
  {
    switch (op)
    {
      case TK_NOT: return OPR_NOT;
      case '-': return OPR_MINUS;
      case '#': return OPR_LEN;
      default: return OPR_NOUNOPR;
    }
  }


  // ORDER OPR
  /**
   * Priority table.  left-priority of an operator is
   * <code>priority[op][0]</code>, its right priority is
   * <code>priority[op][1]</code>.  Please do not modify this table.
   */
  private static final int[][] PRIORITY = new int[][]
  {
    {6, 6}, {6, 6}, {7, 7}, {7, 7}, {7, 7},     // + - * / %
    {10, 9}, {5, 4},                // power and concat (right associative)
    {3, 3}, {3, 3},                 // equality and inequality
    {3, 3}, {3, 3}, {3, 3}, {3, 3}, // order
    {2, 2}, {1, 1}                  // logical (and/or)
  };

  /** Priority for unary operators. */
  private static final int UNARY_PRIORITY = 8;

  /**
   * Operator precedence parser.
   * <code>subexpr -> (simpleexp) | unop subexpr) { binop subexpr }</code>
   * where <var>binop</var> is any binary operator with a priority
   * higher than <var>limit</var>.
   */
  private int subexpr(Expdesc v, int limit) throws IOException
  {
    enterlevel();
    int uop = getunopr(token);
    if (uop != OPR_NOUNOPR)
    {
      xNext();
      subexpr(v, UNARY_PRIORITY);
      fs.kPrefix(uop, v);
    }
    else
    {
      simpleexp(v);
    }
    // expand while operators have priorities higher than 'limit'
    int op = getbinopr(token);
    while (op != OPR_NOBINOPR && PRIORITY[op][0] > limit)
    {
      Expdesc v2 = new Expdesc();
      xNext();
      fs.kInfix(op, v);
      // read sub-expression with higher priority
      int nextop = subexpr(v2, PRIORITY[op][1]);
      fs.kPosfix(op, v, v2);
      op = nextop;
    }
    leavelevel();
    return op;
  }

  private void enterblock (FuncState fs, BlockCnt bl, boolean isbreakable)
  {
    bl.breaklist = FuncState.NO_JUMP ;
    bl.isbreakable = isbreakable ;
    bl.nactvar = fs.nactvar ;
    bl.upval = false ;
    bl.previous = fs.bl;
    fs.bl = bl;
    lua_assert(fs.freereg == fs.nactvar, "enterblock()");
  }

  private void leaveblock (FuncState fs)
  {
    BlockCnt bl = fs.bl;
    fs.bl = bl.previous;
    removevars(bl.nactvar);
    if (bl.upval)
      fs.kCodeABC(Lua.OP_CLOSE, bl.nactvar, 0, 0);
    lua_assert(!bl.isbreakable || !bl.upval, "leaveblock()");  /* loops have no body */
    lua_assert(bl.nactvar == fs.nactvar, "leaveblock() 2");
    fs.freereg = fs.nactvar;  /* free registers */
    fs.kPatchtohere(bl.breaklist);
  }


/*
** {======================================================================
** Rules for Statements
** =======================================================================
*/


  private void block () throws IOException
  {
    /* block -> chunk */
    BlockCnt bl = new BlockCnt () ;
    enterblock(fs, bl, false);
    chunk();
    lua_assert(bl.breaklist == FuncState.NO_JUMP, "block()");
    leaveblock(fs);
  }

  private void breakstat ()
  {
    BlockCnt bl = fs.bl;
    boolean upval = false;
    while (bl != null && !bl.isbreakable)
    {
      upval |= bl.upval;
      bl = bl.previous;
    }
    if (bl == null)
      xSyntaxerror("no loop to break");
    if (upval)
      fs.kCodeABC(Lua.OP_CLOSE, bl.nactvar, 0, 0);
    bl.breaklist = fs.kConcat(bl.breaklist, fs.kJump());
  }
    
  private void funcstat (int line) throws IOException
  {
    /* funcstat -> FUNCTION funcname body */
    Expdesc b = new Expdesc () ;
    Expdesc v = new Expdesc () ;
    xNext();  /* skip FUNCTION */
    boolean needself = funcname(v);
    body(b, needself, line);
    fs.kStorevar(v, b);
    fs.kFixline(line);  /* definition `happens' in the first line */
  }

  private void checknext (int c) throws IOException
  {
    check(c);
    xNext();
  }

  private void parlist () throws IOException
  {
    /* parlist -> [ param { `,' param } ] */
    Proto f = fs.f;
    int nparams = 0;
    f.is_vararg = false;
    if (token != ')')    /* is `parlist' not empty? */
    {
      do
      {
        switch (token)
        {
          case TK_NAME:    /* param -> NAME */
          {
            new_localvar(str_checkname(), nparams++);
            break;
          }
          case TK_DOTS:    /* param -> `...' */
          {
            xNext();
            f.is_vararg = true;
            break;
          }
          default: xSyntaxerror("<name> or '...' expected");
        }
      } while ((!f.is_vararg) && testnext(','));
    }
    adjustlocalvars(nparams);
    f.numparams = fs.nactvar ; /* VARARG_HASARG not now used */
    fs.kReserveregs(fs.nactvar);  /* reserve register for parameters */
  }


  private LocVar getlocvar(int i)
  {
    FuncState fstate = fs ;
    return fstate.f.locvars [fstate.actvar[i]] ;
  }

  private void adjustlocalvars (int nvars)
  {
    fs.nactvar += nvars;
    for (; nvars != 0; nvars--)
    {
      getlocvar(fs.nactvar - nvars).startpc = fs.pc;
    }
  }

  private void new_localvarliteral(String v, int n)
  {
    new_localvar(v, n) ;
  }

  private void errorlimit (int limit, String what)
  {
    String msg = fs.f.linedefined == 0 ?
      "main function has more than "+limit+" "+what :
      "function at line "+fs.f.linedefined+" has more than "+limit+" "+what ;
    xLexerror(msg, 0);
  }


  private void yChecklimit(int v,int l, String m)
  {
    if (v > l)
      errorlimit(l,m);
  }

  private void new_localvar (String name, int n)
  {
    yChecklimit(fs.nactvar+n+1, Lua.MAXVARS, "local variables");
    fs.actvar[fs.nactvar+n] = (short)registerlocalvar(name);
  }

  /** I think this is a C thing not Lua */
  static final int SHRT_MAX = (1<<15)-1 ;

  private int registerlocalvar (String varname)
  {
    Proto f = fs.f;
    f.ensureLocvars (L, fs.nlocvars, SHRT_MAX) ;
    f.locvars[fs.nlocvars].varname = varname;
    return fs.nlocvars++;
  }


  private void body (Expdesc e, boolean needself, int line) throws IOException
  {
    /* body ->  `(' parlist `)' chunk END */
    FuncState new_fs = new FuncState (this);
    open_func(new_fs);
    new_fs.f.linedefined = line;
    checknext('(');
System.out.println ("body 1") ;
    if (needself)
    {
      new_localvarliteral("self", 0);
      adjustlocalvars(1);
    }
    parlist();
    checknext(')');
    chunk();
    new_fs.f.lastlinedefined = linenumber;
    check_match(TK_END, TK_FUNCTION, line);
    close_func();
System.out.println ("body 5") ;
    pushclosure(new_fs, e);
  }

  static int UPVAL_K (int upvaldesc)    { return (upvaldesc >> 8) & 0xFF ; }
  static int UPVAL_INFO (int upvaldesc) { return upvaldesc & 0xFF ; }
  static int UPVAL_ENCODE (int k, int info) { return ((k & 0xFF) << 8) | (info & 0xFF) ; }


  private void pushclosure (FuncState func, Expdesc v)
  {
System.out.println ("pushclosure 1") ;
    Proto f = fs.f;
    int oldsize = f.sizep;
    f.ensureProtos (L, fs.np) ;
System.out.println ("pushclosure 2") ;
    Proto ff = func.f ;
    f.p[fs.np++] = ff;
    init_exp(v, Expdesc.VRELOCABLE, fs.kCodeABx(Lua.OP_CLOSURE, 0, fs.np-1));
System.out.println ("pushclosure 3 "+ff.nups) ;
    for (int i=0; i < ff.nups; i++)
    {
      int upvalue = func.upvalues[i] ;
      int o = (UPVAL_K(upvalue) == Expdesc.VLOCAL) ? Lua.OP_MOVE : Lua.OP_GETUPVAL;
System.out.println ("pushclosure loop "+o) ;
      fs.kCodeABC(o, 0, UPVAL_INFO(upvalue), 0);
    }
System.out.println ("pushclosure 4") ;
  }

  private boolean funcname (Expdesc v) throws IOException
  {
    /* funcname -> NAME {field} [`:' NAME] */
    boolean needself = false;
    singlevar(v);
    while (token == '.')
      field(v);
    if (token == ':')
    {
      needself = true;
      field(v);
    }
    return needself;
  }

  private void field (Expdesc v) throws IOException
  {
    /* field -> ['.' | ':'] NAME */
    Expdesc key = new Expdesc () ;
    fs.kExp2anyreg(v);
    xNext();  /* skip the dot or colon */
    checkname(key);
    fs.kIndexed(v, key);
  }

  private void repeatstat (int line) throws IOException
  {
    /* repeatstat -> REPEAT block UNTIL cond */
    int condexit;
    int repeat_init = fs.kGetlabel();
    BlockCnt bl1 = new BlockCnt ();
    BlockCnt bl2 = new BlockCnt ();
    enterblock(fs, bl1, true);  /* loop block */
    enterblock(fs, bl2, false);  /* scope block */
    xNext();  /* skip REPEAT */
    chunk();
    check_match(TK_UNTIL, TK_REPEAT, line);
    condexit = cond();  /* read condition (inside scope block) */
    if (!bl2.upval)    /* no upvalues? */
    {
      leaveblock(fs);  /* finish scope */
      fs.kPatchlist(condexit, repeat_init);  /* close the loop */
    }
    else    /* complete semantics when there are upvalues */
    {
      breakstat();  /* if condition then break */
      fs.kPatchtohere(condexit);  /* else... */
      leaveblock(fs);  /* finish scope... */
      fs.kPatchlist(fs.kJump(), repeat_init);  /* and repeat */
    }
    leaveblock(fs);  /* finish loop */
  }

  private int cond () throws IOException
  {
    /* cond -> exp */
    Expdesc v = new Expdesc () ;
    expr(v);  /* read condition */
    if (v.k == Expdesc.VNIL)
      v.k = Expdesc.VFALSE;  /* `falses' are all equal here */
    fs.kGoiftrue(v);
    return v.f;
  }

  private void init_exp (Expdesc e, int k, int i)
  {
    e.f = e.t = FuncState.NO_JUMP;
    e.k = k;
    e.info = i;
  }

  private void open_func (FuncState fs)
  {
    Proto f = new Proto (source, 2);  /* registers 0/1 are always valid */
    fs.f = f;
//    fs.prev = this.fs;  /* linked list of funcstates */
    fs.ls = this;
    fs.L = L;
//    this.fs = fs;
    fs.pc = 0;
    fs.lasttarget = -1;
    fs.jpc = FuncState.NO_JUMP;
    fs.freereg = 0;
    fs.nk = 0;
    fs.np = 0;
    fs.nlocvars = 0;
    fs.nactvar = 0;
    fs.bl = null;
    fs.h = new Hashtable () ;
  }

  private void localstat () throws IOException
  {
    /* stat -> LOCAL NAME {`,' NAME} [`=' explist1] */
    int nvars = 0;
    int nexps;
    Expdesc e = new Expdesc();
    do
    {
      new_localvar(str_checkname(), nvars++);
    } while (testnext(','));
    if (testnext('='))
    {
      nexps = explist1(e);
    }
    else
    {
      e.k = Expdesc.VVOID;
      nexps = 0;
    }
    adjust_assign(nvars, nexps, e);
    adjustlocalvars(nvars);
  }

  private void forstat (int line) throws IOException
  {
    /* forstat -> FOR (fornum | forlist) END */
    BlockCnt bl = new BlockCnt () ;
    enterblock(fs, bl, true);  /* scope for loop and control variables */
    xNext();  /* skip `for' */
    String varname = str_checkname();  /* first variable name */
    switch (token)
    {
      case '=':
        fornum(varname, line);
        break;
      case ',':
      case TK_IN:
        forlist(varname);
        break;
      default:
        xSyntaxerror("\"=\" or \"in\" expected");
    }
    check_match(TK_END, TK_FOR, line);
    leaveblock(fs);  /* loop scope (`break' jumps to this point) */
  }

  private void fornum (String varname, int line) throws IOException
  {
    /* fornum -> NAME = exp1,exp1[,exp1] forbody */
    int base = fs.freereg;
    new_localvarliteral("(for index)", 0);
    new_localvarliteral("(for limit)", 1);
    new_localvarliteral("(for step)", 2);
    new_localvar(varname, 3);
    checknext('=');
    exp1();  /* initial value */
    checknext(',');
    exp1();  /* limit */
    if (testnext(','))
      exp1();  /* optional step */
    else    /* default step = 1 */
    {
      fs.kCodeABx(Lua.OP_LOADK, fs.freereg, fs.kNumberK(1));
      fs.kReserveregs(1);
    }
    forbody(base, line, 1, true);
  }

  private int exp1 () throws IOException
  {
    Expdesc e = new Expdesc ();
    expr(e);
    int k = e.k;
    fs.kExp2nextreg(e);
    return k;
  }


  private void forlist (String indexname) throws IOException
  {
    /* forlist -> NAME {,NAME} IN explist1 forbody */
    Expdesc e = new Expdesc () ;
    int nvars = 0;
    int base = fs.freereg;
    /* create control variables */
    new_localvarliteral("(for generator)", nvars++);
    new_localvarliteral("(for state)", nvars++);
    new_localvarliteral("(for control)", nvars++);
    /* create declared variables */
    new_localvar(indexname, nvars++);
    while (testnext(','))
      new_localvar(str_checkname(), nvars++);
    checknext(TK_IN);
    int line = linenumber;
    adjust_assign(3, explist1(e), e);
    fs.kCheckstack(3);  /* extra space to call generator */
    forbody(base, line, nvars - 3, false);
  }

  private void forbody (int base, int line, int nvars, boolean isnum) throws IOException
  {
    /* forbody -> DO block */
    BlockCnt bl = new BlockCnt () ;
    adjustlocalvars(3);  /* control variables */
    checknext(TK_DO);
    int prep = isnum ? fs.kCodeAsBx(Lua.OP_FORPREP, base, FuncState.NO_JUMP) : fs.kJump();
    enterblock(fs, bl, false);  /* scope for declared variables */
    adjustlocalvars(nvars);
    fs.kReserveregs(nvars);
    block();
    leaveblock(fs);  /* end of scope for declared variables */
    fs.kPatchtohere(prep);
    int endfor = isnum ? 
        fs.kCodeAsBx(Lua.OP_FORLOOP, base, FuncState.NO_JUMP) :
        fs.kCodeABC(Lua.OP_TFORLOOP, base, 0, nvars);
    fs.kFixline(line);  /* pretend that `OP_FOR' starts the loop */
    fs.kPatchlist((isnum ? endfor : fs.kJump()), prep + 1);
  }

  private void ifstat (int line) throws IOException
  {
    /* ifstat -> IF cond THEN block {ELSEIF cond THEN block} [ELSE block] END */
    int escapelist = FuncState.NO_JUMP;
    int flist = test_then_block();  /* IF cond THEN block */
    while (token == TK_ELSEIF)
    {
      fs.kConcat(escapelist, fs.kJump());
      fs.kPatchtohere(flist);
      flist = test_then_block();  /* ELSEIF cond THEN block */
    }
    if (token == TK_ELSE)
    {
      fs.kConcat(escapelist, fs.kJump());
      fs.kPatchtohere(flist);
      xNext();  /* skip ELSE (after patch, for correct line info) */
      block();  /* `else' part */
    }
    else
      fs.kConcat(escapelist, flist);
    fs.kPatchtohere(escapelist);
    check_match(TK_END, TK_IF, line);
  }

  private int test_then_block () throws IOException
  {
    /* test_then_block -> [IF | ELSEIF] cond THEN block */
    xNext();  /* skip IF or ELSEIF */
    int condexit = cond();
    checknext(TK_THEN);
    block();  /* `then' part */
    return condexit;
  }

  private void whilestat (int line) throws IOException
  {
    /* whilestat -> WHILE cond DO block END */
    BlockCnt bl = new BlockCnt () ;
    xNext();  /* skip WHILE */
    int whileinit = fs.kGetlabel();
    int condexit = cond();
    enterblock(fs, bl, true);
    checknext(TK_DO);
    block();
    fs.kPatchlist(fs.kJump(), whileinit);
    check_match(TK_END, TK_WHILE, line);
    leaveblock(fs);
    fs.kPatchtohere(condexit);  /* false conditions finish the loop */
  }

  private boolean hasmultret(int k)
  {
    return k == Expdesc.VCALL || k == Expdesc.VVARARG ;
  }

  private void adjust_assign (int nvars, int nexps, Expdesc e)
  {
    int extra = nvars - nexps;
    if (hasmultret(e.k))
    {
      extra++;  /* includes call itself */
      if (extra < 0)
        extra = 0;
      fs.kSetreturns(e, extra);  /* last exp. provides the difference */
      if (extra > 1)
        fs.kReserveregs(extra-1);
    }
    else
    {
      if (e.k != Expdesc.VVOID)
        fs.kExp2nextreg(e);  /* close last expression */
      if (extra > 0)
      {
        int reg = fs.freereg;
        fs.kReserveregs(extra);
        fs.kNil(reg, extra);
      }
    }
  }

  private void localfunc () throws IOException
  {
    Expdesc v = new Expdesc ();
    Expdesc b = new Expdesc ();
    new_localvar(str_checkname(), 0);
    init_exp(v, Expdesc.VLOCAL, fs.freereg);
    fs.kReserveregs(1);
    adjustlocalvars(1);
    body(b, false, linenumber);
    fs.kStorevar(v, b);
    /* debug information will only see the variable after this point! */
    fs.getlocvar(fs.nactvar - 1).startpc = fs.pc;
  }

  private void yindex (Expdesc v) throws IOException
  {
    /* index -> '[' expr ']' */
    xNext();  /* skip the '[' */
    expr(v);
    fs.kExp2val(v);
    checknext(']');
  }

  void xLookahead () throws IOException
  {
    lua_assert(lookahead == TK_EOS, "xLookahead()");
    lookahead = llex();
    lookaheadR = semR ;
    lookaheadS = semS ;
  }

  private void listfield (ConsControl cc) throws IOException
  {
    expr(cc.v);
    yChecklimit(cc.na, Lua.MAXARG_Bx, "items in a constructor");
    cc.na++;
    cc.tostore++;
  }

  private void markupval (int level)
  {
    BlockCnt bl = fs.bl;
    while (bl != null && bl.nactvar > level) 
      bl = bl.previous;
    if (bl != null)
      bl.upval = true;
  }

  private int indexupvalue (String name, Expdesc v)
  {
    int i;
    Proto f = fs.f;
    int oldsize = f.sizeupvalues;
    for (i=0; i<f.nups; i++)
    {
      if (UPVAL_K(fs.upvalues[i]) == v.k &&
          UPVAL_INFO(fs.upvalues[i]) == v.info)
      {
        if (!(name.equals (f.upvalues[i])))
            System.out.println ("indexupvalue check failed for name='"+name+"' and index="+i+"  found '"+f.upvalues[i]+"'") ;
        lua_assert(name.equals (f.upvalues[i]), "indexupvalue()");
        return i;
      }
    }
    /* new one */
    yChecklimit(f.nups + 1, Lua.MAXUPVALUES, "upvalues");
    f.ensureUpvals (L, f.nups) ;
    f.upvalues[f.nups] = name;
    lua_assert(v.k == Expdesc.VLOCAL || v.k == Expdesc.VUPVAL, "indexupvalue() 2");
    fs.upvalues[f.nups] = UPVAL_ENCODE(v.k, v.info) ;
    return f.nups++;
  }
}

final class LHS_assign
{
  LHS_assign prev ;
  Expdesc v = new Expdesc () ;

  LHS_assign () {}
  LHS_assign (LHS_assign prev) { this.prev = prev ; }
}

final class ConsControl
  {
  Expdesc v = new Expdesc() ;  /* last list item read */
  Expdesc t;  /* table descriptor */
  int nh;  /* total number of `record' elements */
  int na;  /* total number of array elements */
  int tostore;  /* number of array elements pending to be stored */

  ConsControl (Expdesc t)
  {
    this.t = t ;
  }
}
