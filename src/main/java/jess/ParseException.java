package jess;

/**
 * An error during parsing. These are used extensively in the JessDE editor.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class ParseException extends JessException {
    private String m_filename;
    private JessToken m_errorToken;
    private String[] m_alternatives;
    private Named m_construct;

    /** A syntax error has been detected in the input. */
    public static final int SYNTAX_ERROR = 1000;

    /** A semantic error has been detected in the input. */
    public static final int SEMANTIC_ERROR = 1001;

    /** A semantic error has been detected in the input. */
    public static final int INVALID_DECLARAND = 1002;

    /** An implied ordered deftemplate has been created. */
    public static final int WARNING_IMPLIED_DEFTEMPLATE = 1002;

    /** An unknown funtion name was seen. */
    public static final int WARNING_UNDEFINED_FUNCTION= 1003;

    /** An undefined defglobal was referenced. */
    public static final int WARNING_UNDEFINED_DEFGLOBAL= 1004;

    /** An undefined defquery is invoked. */
    public static final int WARNING_UNDEFINED_DEFQUERY= 1005;

    /** An invalid slot name was seen in a pattern. */
    public static final int WARNING_NO_SUCH_SLOT= 1006;

    /** A rule or query is being redefined, perhaps inadvertently */
    public static final int WARNING_REDEFINITION = 1007;

    /** First advice */
    public static final int ADVICE = 2000;

    /** This could be a nil list, but it also could be a function call. */
    public static final int ADVICE_COULD_BE_FUNCTION= 2001;


    ParseException(String s1, String s2, String filename, JessToken errorToken) {
        super(s1, s2, "at token '" + String.valueOf(errorToken)+"'");
        m_filename = filename;
        m_errorToken = errorToken;
    }

    ParseException(String s1, String s2, String filename, JessToken errorToken, Named construct) {
        super(s1, s2, "at token '" + String.valueOf(errorToken)+"'", filename);
        m_errorToken = errorToken;
        m_construct = construct;
    }

    ParseException(String s1, String s2, String filename, String[] alternatives, JessToken errorToken) {
        super(s1, s2, "at token '" + String.valueOf(errorToken)+"'", filename);
        m_errorToken = errorToken;
        m_alternatives = alternatives;
    }

    ParseException(String s1, String s2, String filename, String[] alternatives, JessToken errorToken, Named construct) {
        super(s1, s2, "at token '" + String.valueOf(errorToken)+"'", filename);
        m_errorToken = errorToken;
        m_alternatives = alternatives;
        m_construct = construct;
    }

    /**
     * Returns the token at which the error was detected
     * @return the token
     */
    public JessToken getErrorToken() {
        return m_errorToken;
    }

    /**
     * Returns an array of valid tokens that would have been accepted in place of the error token.
     * @return an array of strings
     * @see #getErrorToken
     */
    public String[] getAlternatives() {
        return m_alternatives;
    }

    /**
     * If the parser is parsing a construct when the error occurred, then the partially-built
     * construct will be returned; otherwise, this method returns null.
     * @return a partially built construct, or  null
     */
    public Named getConstruct() {
        return m_construct;
    }

    public boolean isAdvice() {
        return getErrorCode() >= ADVICE;
    }

    /**
     * Returns the name of the file in which the error was detected,
     * if known; otherwise returns null.
     *
     * @return a filename, or null
     */
    public String getFilename() {
        return m_filename;
    }
}
