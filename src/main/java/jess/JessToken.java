package jess;

import java.io.Serializable;

/**
 * <p>A packet of info about a single token parsed from an input stream.</p>
 *
 * (C) 2013 Sandia Corporation<br>
 * @see Jesp
 * @see Tokenizer
 */

public final class JessToken implements Serializable {
    public static final int NONE_TOK = 0;
    public static final int MULTIVARIABLE_TOK = 1;
    public static final int VARIABLE_TOK = 2;
    public static final int STRING_TOK = 3;
    public static final int SYMBOL_TOK = 4;
    public static final int INTEGER_TOK = 5;
    public static final int FLOAT_TOK = 6;
    public static final int COMMENT_TOK = 7;
    public static final int REGEXP_TOK = 8;
    public static final int LONG_TOK = 9;
    public static final int MULTILINE_COMMENT_TOK = 11;

    public String m_sval;
    public double m_nval;
    public long m_lval;
    public int m_lineno;
    public int m_ttype;
    // TODO Don't use ugly public class and variables
    // Maybe there should be a public "DebugHelper"  class in this package?
    public int m_start, m_end;

    public JessToken(int start, int end) {
        m_start = start;
        m_end = end;
    }

    public JessToken(int start, int end, int type, String sval) {
        this(start, end);
        m_ttype = type;
        m_sval = sval;
    }

    /**
     * Return the value of this token. Converts a JessToken to a Value. Will not resolve variables.
     * @param context an execution context
     * @return a Value equivalent to the JessToken
     * @throws JessException if something goes wrong
     */

    public Value rawValueOf(Context context) throws JessException {
        final ValueFactory factory = context.getEngine().getValueFactory();
        switch(m_ttype) {
            case JessToken.LONG_TOK:
                return factory.get(m_lval);
            case JessToken.FLOAT_TOK:
                return factory.get(m_nval, RU.FLOAT);
            case JessToken.INTEGER_TOK:
                return factory.get(m_nval, RU.INTEGER);
            case JessToken.STRING_TOK:
                return factory.get(m_sval, RU.STRING);
            case JessToken.SYMBOL_TOK:
                return factory.get(m_sval, RU.SYMBOL);
            case JessToken.VARIABLE_TOK:
            case JessToken.MULTIVARIABLE_TOK:
                return factory.get("?" + m_sval, RU.STRING);

            case JessToken.NONE_TOK:
                if ("EOF".equals(m_sval))
                    return Funcall.EOF;
                // FALL THROUGH
                
            default:
            {
                return factory.get("" + (char)m_ttype, RU.STRING);
            }
        }
    }

    /**
     * Return the value of this token. Converts a JessToken to a Value. Will resolve variables if possible.
     * @param context an execution context
     * @return a Value equivalent to the JessToken
     * @throws JessException if something goes wrong
     */
    public Value valueOf(Context context) throws JessException {
        // Turn the token into a value.
        switch (m_ttype) {
            case JessToken.VARIABLE_TOK:
            case JessToken.MULTIVARIABLE_TOK:
                return context.getVariable(m_sval);
            default:
                return rawValueOf(context);
        }
    }

    public String toString() {
        if (m_ttype == JessToken.VARIABLE_TOK)
            return "?" + m_sval;
        else if (m_ttype == JessToken.MULTIVARIABLE_TOK)
            return "$?" + m_sval;
        else if (m_ttype == JessToken.STRING_TOK)
            return "\"" + m_sval + "\"";
        else if (m_sval != null)
            return m_sval;
        else if (m_ttype == JessToken.FLOAT_TOK)
            return "" + m_nval;
        else if (m_ttype == JessToken.INTEGER_TOK)
            return "" + (int) m_nval;
        else
            return "" + (char) m_ttype;
    }

    public boolean isComment() {
        return m_ttype == JessToken.COMMENT_TOK || m_ttype == JessToken.MULTILINE_COMMENT_TOK;
    }

    public boolean isEOF() {
        return m_ttype == JessToken.NONE_TOK && "EOF".equals(m_sval);
    }

    public boolean isVariable() {
        return m_ttype == JessToken.VARIABLE_TOK || m_ttype == JessToken.MULTIVARIABLE_TOK;
    }

    public boolean isLexeme() {
        return m_ttype == JessToken.SYMBOL_TOK || m_ttype == JessToken.STRING_TOK;
    }

    public boolean equals(Object o) {
        if (! (o instanceof JessToken))
            return false;
        JessToken other = (JessToken) o;
        return other.m_ttype == m_ttype &&
                other.m_start == m_start &&
                other.m_end == m_end &&
                other.m_nval == m_nval &&
                (other.m_sval == m_sval || (other.m_sval != null && other.m_sval.equals(m_sval)));
    }

    public int hashCode() {
        return (int) (m_ttype + m_start + m_end + m_nval) + (m_sval == null ? 0 : m_sval.hashCode());
    }
}




