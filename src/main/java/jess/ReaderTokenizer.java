package jess;

import java.io.*;


/**
 * A simple Tokenizer implementation for the Jess language that takes
 * its input from a Reader.  Not serializable, as it contains a
 * reference to a java.io.Reader.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class ReaderTokenizer implements Tokenizer {
    private static final int EOF = -1;
    private final PushbackReader m_ios;
    private int m_line = 1;
    private boolean m_reportNewlines = false;
    private int m_streamIndex;
    private int m_tokenStart;
    private boolean m_returnComments = false;

    /**
     * Constructor - use large buffer for FileReader, no buffering
     * for everything else
     *
     * @param ios
     */
    public ReaderTokenizer(Reader ios, boolean parseComments) {
        m_returnComments = parseComments;
        if (ios instanceof PushbackReader)
            m_ios = (PushbackReader) ios;

        else if (ios instanceof FileReader)
            m_ios = new PushbackReader(new BufferedReader(ios, 512));
        else
            m_ios = new PushbackReader(ios);
    }

    public void reportNewlines(boolean b) {
        m_reportNewlines = b;
    }

    private final StringBuffer m_sb = new StringBuffer(5);

    public synchronized String readLine() throws JessException {
        int c;
        m_sb.setLength(0);
        while ((c = nextChar()) != '\n' && c != EOF)
            m_sb.append((char) c);

        if (m_reportNewlines && c == '\n')
            unread(c);

        if (c == EOF && m_sb.length() == 0)
            return null;

        ++m_line;
        return m_sb.toString();
    }

    public synchronized JessToken nextToken() throws JessException {
        m_sb.setLength(0);
        int c;

        // ******************************
        // Eat any leading whitespace

        m_tokenStart = m_streamIndex;
        whiteloop:
          do {
              c = nextChar();

              switch (c) {
                  case EOF:
                      {
                          return finishToken(EOF, m_sb);
                      }

                  case '\n':
                      // new line
                      ++m_line;
                      if (m_reportNewlines)
                          return finishToken('\n', m_sb);
                      // else keep going
                      break;

                  case ' ':
                  case '\t':
                  case '\r':
                      // keep going
                      break;

                  default:
                      // OK, no more whitespace
                      unread(c);
                      break whiteloop;
              }
          } while (c != EOF);
        m_tokenStart = m_streamIndex;
        c = nextChar();
        switch (c) {
            case EOF:
                return finishToken(EOF, m_sb);
            
                // comment
            case ';':
                String line = discardToEOL();
                if (m_returnComments) {
                    JessToken tok = new JessToken(m_tokenStart, m_streamIndex);
                    tok.m_ttype = JessToken.COMMENT_TOK;
                    tok.m_sval = ";" + line;
                    return tok;
                } else
                    return nextToken();

                // quoted string
            case '"':
                readString(m_sb, '"');
                return finishToken('"', m_sb);

                // Operator, comment, or regexp
            case '/':
                c = nextChar();
                if (Character.isWhitespace((char) c) || c == ')') {
                    unread(c);
                    return finishToken('/', m_sb);

                } else if (c == '*') { // multiline comment
                    do
                    {
                        readString(m_sb, '*');
                        c = nextChar();
                        if (c != '/') {
                            m_sb.append('*');
                            unread(c);
                        }
                    } while (c != EOF && c != '/');
                    m_sb.append('/');
                    return finishToken(JessToken.MULTILINE_COMMENT_TOK, m_sb);

                } else { // regexp
                    unread(c);
                    readString(m_sb, '/');
                    return finishToken(JessToken.REGEXP_TOK, m_sb);
                }

                // single-character tokens
            case '(':
            case ')':
            case '{':
            case '}':
            case '[':
            case ']':
            case '~':
                return finishToken(c, m_sb);

            case '&':
            case '|':
            case '=': {
                int d = nextChar();
                if (d == c) {
                    m_sb.append((char) c);
                    m_sb.append((char) c);
                    return finishToken(0, m_sb);
                } else {
                    unread(d);
                    return finishToken(c, m_sb);
                }

                // anything else
            }
            default:
                m_sb.append((char) c);
                break;
        }
        do {
            c = nextChar();
            if (Character.isWhitespace((char) c)) {
                unread(c);
                return finishToken(0, m_sb);
            }

            switch (c) {
                // end of file
                case EOF:
                    return finishToken(EOF, m_sb);

                    // separators
                case '(':
                case ')':
                case '}':
                case '{':
                case '&':
                case '~':
                case '|':
                case '<':
                case ';':
                case '"':
                    {
                        unread(c);
                        return finishToken(0, m_sb);
                    }

                    // character escape
                case '\\':
                    c = nextChar();
                    if (c == EOF)
                        return finishToken(EOF, m_sb);
                    else
                        m_sb.append((char) c);
                    break;

                    // ordinary chars
                default:
                    m_sb.append((char) c);
                    break;
            }
        } while (true);
        // NOT REACHED
    }

    /**
     * Returns the next character from the stream; CR, LF, or CRLF are
     * each returned as the single character '\n'.
     */
    private int nextChar() throws JessException {
        try {
            int c = m_ios.read();
            if (c == '\r') {
                if (m_ios.ready()) {
                    c = m_ios.read();
                    if (c != '\n')
                        m_ios.unread(c);
                    else
                        ++m_streamIndex;
                }
                c = '\n';
            }
            if (c != -1)
                ++m_streamIndex;
            return c;
        } catch (IOException ioe) {
            throw new JessException("ReaderTokenizer.nextChar",
                                    "Error on input stream",
                                    ioe);
        }
    }

    public void eatWhitespace() throws JessException {
        try {
            int c = 0;
            if (m_ios.ready()) {
                while (m_ios.ready() && (c = nextChar()) != -1) {
                    if (!Character.isWhitespace((char) c))
                        break;
                    else if (c == '\n')
                        ++m_line;
                }


                if (c != -1 && !Character.isWhitespace((char) c))
                    unread(c);
            }

        } catch (IOException ioe) {
            throw new JessException("ReaderTokenizer.eatWhitespace",
                                    "Error on input stream",
                                    ioe);
        }
    }

    private void unread(int c) throws JessException {
        try {
            m_ios.unread(c);
            --m_streamIndex;
        } catch (IOException e) {
            throw new JessException("ReaderTokenizer.unread", "Error unreading character", e);
        }
    }


    /**
     * The first argument is a hint about what kind of token this is.
     * '0' means an atom or number; -1 means EOF was hit; '"' means a
     * String; '\n' means return CRLF; anything else is a one-character token.
     */
    private JessToken finishToken(int c, StringBuffer sb) throws JessException {
        int tokenEnd = m_streamIndex;
        JessToken jt = new JessToken(m_tokenStart, tokenEnd);
        jt.m_lineno = m_line;

        switch (c) {
            // quoted string
            case '"':
                jt.m_ttype = JessToken.STRING_TOK;
                jt.m_sval = sb.toString();
                break;

                // Regular expression
            case JessToken.REGEXP_TOK:
                jt.m_ttype = JessToken.REGEXP_TOK;
                jt.m_sval = sb.toString();
                break;

                // Regular expression
            case JessToken.MULTILINE_COMMENT_TOK:
                jt.m_ttype = JessToken.MULTILINE_COMMENT_TOK;
                jt.m_sval = sb.toString();
                break;

                // single-character tokens:
            case '{':
                jt.m_ttype = '{';
                jt.m_sval = "{";
                break;

            case '}':
                jt.m_ttype = '}';
                jt.m_sval = "}";
                break;

            case '[':
                jt.m_ttype = '[';
                jt.m_sval = "[";
                break;

            case ']':
                jt.m_ttype = ']';
                jt.m_sval = "]";
                break;

            case '(':
                jt.m_ttype = '(';
                jt.m_sval = "(";
                break;

            case ')':
                jt.m_ttype = ')';
                jt.m_sval = ")";
                break;

            case '&':
                jt.m_ttype = '&';
                jt.m_sval = "&";
                break;

            case '~':
                jt.m_ttype = '~';
                jt.m_sval = "~";
                break;

            case '|':
                jt.m_ttype = c;
                jt.m_sval = "|";
                break;

            case '=':
                jt.m_ttype = '=';
                jt.m_sval = "=";
                break;

            case '/':
                jt.m_ttype = JessToken.SYMBOL_TOK;
                jt.m_sval = "/";
                break;

                // Return newline token
            case '\n':
                jt.m_ttype = '\n';
                jt.m_sval = "\n";
                break;

                // EOF encountered
            case EOF:
                if (sb.length() == 0) {
                    jt.m_ttype = JessToken.NONE_TOK;
                    jt.m_sval = "EOF";
                    jt.m_end = jt.m_start;
                    break;
                } else
                    return finishToken(0, sb);

                // everything else
            case 0:
                String sval = sb.toString();
                char ch = sval.charAt(0);

                // VARIABLES
                if (ch == '?') {
                    jt.m_ttype = JessToken.VARIABLE_TOK;
                    if (sval.length() > 1)
                        jt.m_sval = sval.substring(1);
                    else
                        jt.m_sval = RU.gensym(BLANK_PREFIX);
                    break;
                }

                // MULTIVARIABLES
                else if (ch == '$' && sval.length() > 1 &&
                        sval.charAt(1) == '?') {
                    jt.m_ttype = JessToken.MULTIVARIABLE_TOK;
                    if (sval.length() > 2)
                        jt.m_sval = sval.substring(2);
                    else
                        jt.m_sval = RU.gensym(BLANK_MULTI);
                    break;
                }

                // Atoms that look like parts of numbers
                else if (sval.length() == 1 && (ch == '-' || ch == '.' || ch == '+')) {
                    jt.m_ttype = JessToken.SYMBOL_TOK;
                    jt.m_sval = sval;
                    break;
                }

                if (isAnInteger(sval)) {
                    // INTEGERS
                    try {
                        int i = Integer.parseInt(sval, 10);
                        jt.m_ttype = JessToken.INTEGER_TOK;
                        jt.m_nval = i;
                        break;
                    } catch (NumberFormatException nfe) {
                        // FALL THROUGH
                    }
                }

                if (isALong(sval)) {
                    try {
                        String lval = sval;
                        if (lval.toUpperCase().endsWith("L"))
                            lval = lval.substring(0, lval.length() - 1);
                        long l = Long.parseLong(lval, 10);
                        jt.m_ttype = JessToken.LONG_TOK;
                        jt.m_lval = l;
                        break;
                    } catch (NumberFormatException e) {
                        // FALL THROUGH
                    }
                }

                // FLOATS
                if (couldBeADouble(sval)) {
                    try {
                        double d = Double.valueOf(sval).doubleValue();
                        jt.m_ttype = JessToken.FLOAT_TOK;
                        jt.m_nval = d;
                        break;
                    } catch (NumberFormatException nfe) {
                        // FALL THROUGH
                    }
                }

                jt.m_sval = sval;
                jt.m_ttype = JessToken.SYMBOL_TOK;
                break;

            default:
                throw new JessException("ReaderTokenizer.finishToken", "Impossible tag:", "" + (char) c);
        }
        return jt;
    }

    static boolean couldBeADouble(String sval) {
        char c = sval.charAt(0);
        if (!Character.isDigit(c) && c != '+' && c != '-' && c != '.')
            return false;
        return true;
    }

    // Saves the expense of throwing many exceptions
    public static boolean isAnInteger(String sval) {
        if (sval.length() > 11)
            return false;
        char c = sval.charAt(0);
        if (!Character.isDigit(c) && c != '+' && c != '-')
            return false;
        for (int i = 1; i < sval.length(); i++)
            if (!Character.isDigit(sval.charAt(i)))
                return false;

        return true;
    }

    public static boolean isALong(String sval) {
        int length = sval.length();
        if (length > 21)
            return false;
        char c = sval.charAt(0);
        if (!Character.isDigit(c) && c != '+' && c != '-')
            return false;
        for (int i = 1; i < length - 1; i++)
            if (!Character.isDigit(sval.charAt(i)))
                return false;
        char lastChar = sval.charAt(length - 1);
        return !(!Character.isDigit(lastChar) && lastChar != 'L' && lastChar != 'l');

        }

    public String discardToEOL() throws JessException {
        // ******************************
        // return all characters up to the next CR, LF, or CRLF
        StringBuffer line = new StringBuffer();
        int c;
        while (true) {
            c = nextChar();
            if (c == '\n' || c == EOF) {
                ++m_line;
                if (m_reportNewlines)
                    unread(c);
                break;
            } else {
                line.append((char) c);
            }
        }
        return line.toString();
    }

    private void readString(StringBuffer s, char endChar) throws JessException {
        int c;
        loop:
          do {
              c = nextChar();
              switch (c) {
                  case EOF:
                      break;
                  case '\\':
                      {
                          c = nextChar();
                          s.append((char) c);
                          break;
                      }

                      // this is no longer an error
                  case '\n':
                      {
                          ++m_line;
                          s.append((char) c);
                          break;
                      }

                  default:
                      if (c == endChar)
                          break loop;
                      else
                          s.append((char) c);
                      break;
              }
          } while (c != EOF);

    }

    public int getStreamPos() {
        return m_streamIndex;
    }

}

