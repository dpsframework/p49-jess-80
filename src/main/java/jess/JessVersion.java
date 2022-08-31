package jess;

import java.io.Serializable;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: JessVersion.java,v 1.23 2008-11-11 15:34:06 ejfried Exp $
 */

public class JessVersion implements Userfunction, Serializable {
    static final int NUMBER = 0, STRING = 1;
    static final String[] s_names = {"jess-version-number",
                                     "jess-version-string"};
    private int m_name;


    /**
     * A string describing this version of Jess, of the form
     *<p>
     * <tt>Jess Version 8.0 9/12/2013</tt>
     *</p>
     * This value is <i>not</i> a compile-time constant, so classes that
     * refer to this variable will see the real value.
     */
    public static final String VERSION_STRING;
    /**
     * The version number of Jess. Not a compile-time constant.
     */
    public static final double VERSION_NUMBER;
    /**
     * The copyright information for this version of Jess. Not a compile-time constant.
     */
    public static final String COPYRIGHT_STRING;

    static {
	VERSION_STRING = "Jess Version 8.0b1 31/08/2022";
	VERSION_NUMBER = 8.0;
        COPYRIGHT_STRING = "Copyright (C) 2013 Sandia Corporation";
    }

    JessVersion(int name) {
        m_name = name;
    }

    public String getName() {
        return s_names[m_name];
    }
                                                       
    public Value call(ValueVector vv, Context context)
            throws JessException {
        switch (m_name) {
            case NUMBER:
                return new Value(VERSION_NUMBER, RU.FLOAT);
            default:
                return new Value(VERSION_STRING, RU.STRING);
        }
    }
}

