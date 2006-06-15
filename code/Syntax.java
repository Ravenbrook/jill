// $Header$

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

/**
 * Syntax analyser.  Lexing, parsing, code generation.
 */
final class Syntax {
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
  static String[] tokens = new String[] {
    "and", "break", "do", "else", "elseif",
    "end", "false", "for", "function", "if",
    "in", "local", "nil", "not", "or", "repeat",
    "return", "then", "true", "until", "while",
    "..", "...", "==", ">=", "<=", "~=",
    "<number>", "<name>", "<string>", "<eof>"
  };

  static Hashtable reserved = new Hashtable();
  static {
    for (int i=0; i < NUM_RESERVED; ++i) {
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
  private String source;
  /** locale decimal point. */
  private char decpoint = '.';

  private Syntax(Lua L, Reader z, String source) throws IOException {
    this.L = L;
    this.z = z;
    this.source = source;
    next();
  }

  int lastline() {
    return lastline;
  }


  // From <ctype.h>

  // Implementations of functions from <ctype.h> are only correct copies
  // to the extent that Lua requires them.

  private static boolean isalnum(int c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        (c >= '0' && c <='9');
  }

  private static boolean isalpha(int c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z');
  }

  /** True if and only if the char (when converted from the int) is a
   * control character.
   */
  private static boolean iscntrl(int c) {
    return (char)c < 0x20 || c == 0x7f;
  }

  private static boolean isdigit(int c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isspace(int c) {
    return c == ' ' || c == '\t';
  }


  // From llex.c

  private boolean check_next(String set) throws IOException {
    if (set.indexOf(current) < 0) {
      return false;
    }
    save_and_next();
    return true;
  }

  private boolean currIsNewline() {
    return current == '\n' || current == '\r';
  }

  private void inclinenumber() throws IOException {
    int old = current;
    // assert currIsNewline();
    next();     // skip '\n' or '\r'
    if (currIsNewline() && current != old) {
      next();   // skip '\n\r' or '\r\n'
    }
    if (++linenumber < 0) {     // overflow
      xSyntaxerror("chunk has too many lines");
    }
  }

  /** Links new FuncState into this, and returns the old FuncState.
   * Not really from llex.c. */
  FuncState linkfs(FuncState newfs) {
    FuncState oldfs = fs;
    fs = newfs;
    return oldfs;
  }

  /** Lex a token and return it.  The semantic info for the token is
   * stored in <code>this.semR</code> or <code>this.semS</code> as
   * appropriate.
   */
  private int llex() throws IOException {
    buff.setLength(0);
    while (true) {
      switch (current) {
        case '\n':
        case '\r':
          inclinenumber();
          continue;
        case '"':
        case '\'':
          read_string(current);
          return TK_STRING;
        case EOZ:
          return TK_EOS;
        default:
          if (isspace(current)) {
            // assert !currIsNewline();
            next();
            continue;
          } else if (isdigit(current)) {
            read_numeral();
            return TK_NUMBER;
          } else if (isalpha(current) || current == '_') {
            // identifier or reserved word
            do {
              save_and_next();
            } while (isalnum(current) || current == '_');
            String s = buff.toString();
            Object t = reserved.get(s);
            if (t == null) {
              semS = s;
              return TK_NAME;
            } else {
              return ((Integer)t).intValue();
            }
          } else {
            int c = current;
            next();
            return c; // single-char tokens
          }
        //:todo: more cases
      }
    }
  }

  private void next() throws IOException {
    current = z.read();
  }

  /** Reads number.  Writes to semR. */
  private void read_numeral() throws IOException {
    // assert isdigit(current);
    do {
      save_and_next();
    } while (isdigit(current) || current == '.');
    if (check_next("Ee")) {     // 'E' ?
      check_next("+-"); // optional exponent sign
    }
    while (isalnum(current) || current == '_') {
      save_and_next();
    }
    // :todo: consider doing PUC-Rio's decimal point tricks.
    String s = buff.toString();
    try {
      semR = Double.parseDouble(s);
      return;
    } catch (NumberFormatException e) {
      xLexerror("malformed number", TK_NUMBER);
    }
  }

  /** Reads string.  Writes to semS. */
  private void read_string(int del) throws IOException {
    save_and_next();
    while (current != del) {
      switch (current) {
        case EOZ:
          xLexerror("unfinished string", TK_EOS);
          continue;     // avoid compiler warning
        case '\n':
        case '\r':
          xLexerror("unfinished string", TK_STRING);
          continue;     // avoid compiler warning
        case '\\': {
          int c;
          next();       // do not save the '\'
          switch (current) {
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
              if (!isdigit(current)) {
                save_and_next();        // handles \\, \", \', \?
              } else {  // \xxx
                int i = 0;
                c = 0;
                do {
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
    // consider optimising this StringBuffer.delete by not doing a save
    // at the beginning and end of the string literal.
    buff.deleteCharAt(0);
    buff.deleteCharAt(buff.length()-1);
    semS = buff.toString();
  }

  private void save() {
    buff.append((char)current);
  }

  private void save(int c) {
    buff.append((char)c);
  }

  private void save_and_next() throws IOException {
    save();
    next();
  }

  /** Getter for source. */
  String source() {
    return source;
  }

  private String txtToken(int token) {
    switch (token) {
      case TK_NAME:
      case TK_STRING:
      case TK_NUMBER:
        return buff.toString();
      default:
        return xToken2str(token);
    }
  }

  /** Equivalent to <code>luaX_lexerror</code>. */
  private void xLexerror(String msg, int token) {
    msg = source + ":" + linenumber + ": " + msg;
    if (token != 0) {
      msg = msg + " near '" + txtToken(token) + "'";
    }
    L.dThrow(Lua.ERRSYNTAX);
  }

  /** Equivalent to <code>luaX_next</code>. */
  private void xNext() throws IOException {
    lastline = linenumber;
    if (lookahead != TK_EOS) {  // is there a look-ahead token?
      token = lookahead;        // Use this one,
      tokenR = lookaheadR;
      tokenS = lookaheadS;
      lookahead = TK_EOS;       // and discharge it.
    }  else {
      token = llex();
      tokenR = semR;
      tokenS = semS;
    }
  }

  /** Equivalent to <code>luaX_syntaxerror</code>. */
  void xSyntaxerror(String msg) {
    xLexerror(msg, token);
  }

  private String xToken2str(int token) {
    if (token < FIRST_RESERVED) {
      // assert token == (char)token;
      if (iscntrl(token)) {
        return "char(" + token + ")";
      }
      return (new Character((char)token)).toString();
    }
    return tokens[token-FIRST_RESERVED];
  }

  // From lparser.c

  private static boolean block_follow (int token) {
    switch (token) {
      case TK_ELSE: case TK_ELSEIF: case TK_END:
      case TK_UNTIL: case TK_EOS:
        return true;
      default:
        return false;
    }
  }

  private void check(int c) {
    if (token != c) {
      error_expected(c);
    }
  }

  /**
   * @param what   the token that is intended to end the match.
   * @param who    the token that begins the match.
   * @param where  the line number of <var>what</var>.
   */
  private void check_match(int what, int who, int where)
      throws IOException {
    if (!testnext(what)) {
      if (where == linenumber) {
        error_expected(what);
      } else {
        xSyntaxerror("'" + xToken2str(what) + "' expected (to close '" +
            xToken2str(who) + "' at line " + where + ")");
      }
    }
  }

  private void close_func() {
    removevars(0);
    fs.kRet(0, 0);
    fs.close();
    fs = fs.prev;
  }

  private void codestring(Expdesc e, String s) {
    e.init(Expdesc.VK, fs.kStringK(s));
  }

  private void enterlevel() {
    // :todo: implement me;
  }

  private void error_expected(int token) {
    xSyntaxerror("'" + xToken2str(token) + "' expected");
  }

  private void leavelevel() {
    // :todo: implement me;
  }

  /** Equivalent to luaY_parser. */
  static Proto parser(Lua L, Reader in, String name)
      throws IOException {
    Syntax lexstate = new Syntax(L, in, name);
    FuncState funcstate = new FuncState(lexstate);
    funcstate.f.setVararg();
    lexstate.xNext();
    lexstate.chunk();
    lexstate.check(TK_EOS);
    lexstate.close_func();
    // assert funcstate.prev == NULL;
    // assert funcstate.f.nups() == 0;
    // assert fs == NULL;
    return funcstate.f;
  }

  private void removevars(int tolevel) {
    // :todo: consider making a method in FuncState.
    while (fs.nactvar > tolevel) {
      fs.getlocvar(--fs.nactvar).setEndpc(fs.pc);
    }
  }

  private void singlevar(Expdesc var) throws IOException {
    String varname = str_checkname();
    if (singlevaraux(fs, varname, var, true) == Expdesc.VGLOBAL) {
      var.setInfo(fs.kStringK(varname));
    }
  }

  private static int singlevaraux(FuncState fs,
      String n,
      Expdesc var,
      boolean base) {
    if (fs == null) {   // no more levels?
      var.init(Expdesc.VGLOBAL, Lua.NO_REG);    // default is global variable
      return Expdesc.VGLOBAL;
    } else {
      int v = fs.searchvar(n);
      if (v >= 0) {
        var.init(Expdesc.VLOCAL, v);
        if (!base) {
          fs.markupval(v);      // local will be used as an upval
        }
        return Expdesc.VLOCAL;
      } else {  // not found at current level; try upper one
        if (singlevaraux(fs.prev, n, var, false) == Expdesc.VGLOBAL) {
          return Expdesc.VGLOBAL;
        }
        var.upval(fs.indexupval(n, var));       // else was LOCAL or UPVAL
        return Expdesc.VUPVAL;
      }
    }
  }

  private String str_checkname() throws IOException {
    check(TK_NAME);
    String s = tokenS;
    xNext();
    return s;
  }

  private boolean testnext(int c) throws IOException {
    if (token == c) {
      xNext();
      return true;
    }
    return false;
  }


  // GRAMMAR RULES

  private void chunk() throws IOException {
    // chunk -> { stat [';'] }
    boolean islast = false;
    enterlevel();
    while (!islast && !block_follow(token)) {
      islast = statement();
      testnext(';');
      // assert :todo: fill in assert
      fs.freereg = fs.nactvar;
    }
    leavelevel();
  }

  private void constructor (Expdesc t) {
    // constructor -> ??
    // :todo: implement me
  }

  private void expr(Expdesc v) throws IOException {
    subexpr(v, 0);
  }

  /** @return number of expressions in expression list. */
  private int explist1(Expdesc v) throws IOException {
    // explist1 -> expr { ',' expr }
    int n = 1;  // at least one expression
    expr(v);
    while (testnext(',')) {
      fs.kExp2nextreg(v);
      expr(v);
      ++n;
    }
    return n;
  }

  private void exprstat() throws IOException {
    // stat -> func | assignment
    Expdesc e = new Expdesc();
    primaryexp(e);
    if (e.kind() == Expdesc.VCALL) {    // stat -> func
      fs.setargc(e, 1); // call statement uses no results
    } else {    // stat -> assignment
      // :todo: implement me
      xSyntaxerror("unimplemented assignment");
    }
  }

  private void funcargs(Expdesc f) throws IOException {
    Expdesc args = new Expdesc();
    int line = linenumber;
    switch (token) {
      case '(':         // funcargs -> '(' [ explist1 ] ')'
        if (line != lastline) {
          xSyntaxerror("ambiguous syntax (function call x new statement)");
        }
        xNext();
        if (token == ')') {     // arg list is empty?
          args.setKind(Expdesc.VVOID);
        } else {
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
    if (args.hasmultret()) {
      nparams = Lua.MULTRET;     // open call
    } else {
      if (args.kind() != Expdesc.VVOID) {
        fs.kExp2nextreg(args);  // close last argument
      }
      nparams = fs.freereg - (base+1);
    }
    f.init(Expdesc.VCALL, fs.kCodeABC(Lua.OP_CALL, base, nparams+1, 2));
    fs.kFixline(line);
    fs.freereg = base+1;        // call removes functions and arguments
                // and leaves (unless changed) one result.
  }

  private void prefixexp(Expdesc v) throws IOException {
    // prefixexp -> NAME | '(' expr ')'
    switch (token) {
      case '(': {
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

  private void primaryexp(Expdesc v) throws IOException {
    // primaryexp ->
    //    prefixexp { '.' NAME | '[' exp ']' | ':' NAME funcargs | funcargs }
    // :todo: implement me
    prefixexp(v);
    while (true) {
      switch (token) {
        // :todo: missing cases
        case '(': case TK_STRING: case '{':     // funcargs
          fs.kExp2nextreg(v);
          funcargs(v);
          break;
        default:
          return;
      }
    }
  }

  private void retstat() throws IOException {
    // stat -> RETURN explist
    xNext();    // skip RETURN
    int first = 0, nret;    // registers with returned values
    if (block_follow(token) || token == ';') {
      first = nret = 0; // return no values
    } else {
      Expdesc e = new Expdesc();
      nret = explist1(e);
      // :todo: if hasmultret
      {
        if (nret == 1) {        // only one single value?
          first = fs.kExp2anyreg(e);
        } else {
          // :todo: returning more than one value
        }
      }
    }
    fs.kRet(first, nret);
  }

  private void simpleexp (Expdesc v) throws IOException {
    // simpleexp -> NUMBER | STRING | NIL | true | false | ... |
    //              constructor | FUNCTION body | primaryexp
    switch (token) {
      case TK_NUMBER:
        v.init(Expdesc.VKNUM, 0);
        v.setNval(tokenR);
        break;
      // :todo: more cases
      default:
        primaryexp(v);
        return;
    }
    xNext();
  }

  private boolean statement() throws IOException {
    int line = linenumber;
    switch (token) {
      // :todo: missing cases
      case TK_RETURN:
        retstat();
        return true;
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
  private static int getbinopr(int op) {
    switch (op) {
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

  private static int getunopr(int op) {
    switch (op) {
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
   * <code>priority[op][1]</code>.
   */
  private static final int[][] priority = new int[][] {
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
  private int subexpr(Expdesc v, int limit) throws IOException {
    enterlevel();
    int uop = getunopr(token);
    if (uop != OPR_NOUNOPR) {
      xNext();
      subexpr(v, UNARY_PRIORITY);
      fs.kPrefix(uop, v);
    } else {
      simpleexp(v);
    }
    // expand while operators have priorities higher than 'limit'
    int op = getbinopr(token);
    while (op != OPR_NOBINOPR && priority[op][0] > limit) {
      Expdesc v2 = new Expdesc();
      xNext();
      fs.kInfix(op, v);
      // read sub-expression with higher priority
      int nextop = subexpr(v2, priority[op][1]);
      fs.kPosfix(op, v, v2);
      op = nextop;
    }
    leavelevel();
    return op;
  }
}

