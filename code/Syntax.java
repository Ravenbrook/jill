// $Header$

import java.io.IOException;
import java.io.Reader;

/**
 * Syntax analyser.  Lexing, parsing, code generation.
 */
final class Syntax {
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
  String[] tokens = new String[] {
    "and", "break", "do", "else", "elseif",
    "end", "false", "for", "function", "if",
    "in", "local", "nil", "not", "or", "repeat",
    "return", "then", "true", "until", "while",
    "..", "...", "==", ">=", "<=", "~=",
    "<number>", "<name>", "<string>", "<eof>"
  };


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
  char token;
  /** Semantic info for token; a number. */
  double tokenR;
  /** Semantic info for token; a string. */
  String tokenS;

  /** Lookahead token value. */
  char lookahead = TK_EOS;
  /** Semantic info for lookahead; a number. */
  double lookaheadR;
  /** Semantic info for lookahead; a string. */
  String lookaheadS;

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

  // From llex.c

  /** Links new FuncState into this, and returns the old FuncState. */
  FuncState linkfs(FuncState newfs) {
    FuncState oldfs = fs;
    fs = newfs;
    return oldfs;
  }

  /** True if and only if the char (when converted from the int) is a
   * control character.
   */
  private boolean iscntrl(int c) {
    return (char)c < 0x20 || c == 0x7f;
  }

  private void next() throws IOException {
    current = z.read();
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
  private void xNext() {
    // :todo: implement me
  }

  /** Equivalent to <code>luaX_syntaxerror</code>. */
  private void xSyntaxerror(String msg) {
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

  void check(int c) {
    if (token != c) {
      error_expected(c);
    }
  }

  void chunk() { }

  void close_func() {
    fs.close();
    fs = fs.prev;
  }

  void error_expected(int token) {
    xSyntaxerror("'" + xToken2str(token) + "' expected");
  }

  /** Equivalent to luaY_parser. */
  static Proto parser(Lua L, Reader in, String name)
      throws IOException {
    Syntax lexstate = new Syntax(L, in, name);
    FuncState funcstate = new FuncState(lexstate);
    lexstate.xNext();
    lexstate.chunk();
    lexstate.check(TK_EOS);
    lexstate.close_func();
    // assert funcstate.prev == NULL;
    // assert funcstate.f.nups() == 0;
    // assert fs == NULL;
    return funcstate.f;
  }
}

