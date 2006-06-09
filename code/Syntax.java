// $Header$

import java.io.IOException;
import java.io.Reader;

/**
 * Syntax analyser.  Lexing, parsing, code generation.
 */
final class Syntax {

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
  char lookahead;
  /** Semantic info for lookahead; a number. */
  double lookaheadR;
  /** Semantic info for lookahead; a string. */
  String lookaheadS;

  /** FuncState for current (innermost) function being parsed. */
  FuncState ds;
  Lua L;
  /** input stream */
  private Reader z;
  /** Buffer for tokens. */
  StringBuffer buff;
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

  static LuaFunction parser(Lua L, Reader in, String name)
      throws IOException {
    Syntax ls = new Syntax(L, in, name);
    // :todo: implement me
    return null;
  }

  private void next() throws IOException {
    current = z.read();
  }
}

