package jess;

import java.io.Serializable;

/**
 * <p>A LongValue represents a Java long in Jess. You can do arithmetic
 * and perform various other operations on LongValues.
 * Use this subclass of Value when you want to create a Value that
 * represents a Java long.</p>
 * (C) 2013 Sandia Corporation<br>
 */

public class LongValue extends Value implements Serializable {
    private long m_long;

    /**
     * Create a LongValue
     * @param l The long value
     * @exception JessException If the type is invalid
     */

    public LongValue(long l) throws JessException {
        super((double) l, RU.LONG);
        m_long = l;
    }

    public final long longValue(Context c) {
        return m_long;
    }

    public final double numericValue(Context c) {
        return (double) m_long;
    }

    public final int intValue(Context c) {
        return (int) m_long;
    }


    public Object javaObjectValue(Context c) throws JessException {
        return new Long(m_long);
    }

    public final String stringValue(Context c) {
        return toString();
    }

    public final String toString() {
        return String.valueOf(m_long);
    }


    /** @noinspection CovariantEquals*/
    public final boolean equals(Value v) {
        if (v.type() != RU.LONG)
            return false;
        else
            return m_long == ((LongValue) v).m_long;
    }

    public final boolean equals(Object v) {
        if (v instanceof Value)
            return equals((Value)v);
        else
            return false;
    }


    public final boolean equalsStar(Value v) {
        if (v.type() == RU.LONG)
            return m_long == ((LongValue) v).m_long;
        else
            return super.equalsStar(v);
    }

    public int hashCode() {
        return (int) (m_long ^ (m_long >>> 32));
    }

    public boolean isNumeric(Context c) throws JessException {
        return true;
    }

    public boolean isLexeme(Context c) throws JessException {
        return false;
    }

    public boolean isLiteral() {
        return true;
    }
}

