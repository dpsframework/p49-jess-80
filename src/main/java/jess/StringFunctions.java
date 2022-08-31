package jess;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Implements String handling functions.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class StringFunctions extends IntrinsicPackageImpl {

    public void add(HashMap ht) {
        addFunction(new StrCat(), ht);
        addFunction(new StrCompare(), ht);
        addFunction(new StrIndex(), ht);
        addFunction(new SubString(), ht);
        addFunction(new StrSimple(StrSimple.LENGTH), ht);
        addFunction(new StrSimple(StrSimple.UPCASE), ht);
        addFunction(new StrSimple(StrSimple.LOWCASE), ht);
    }
}

class StrCat implements Userfunction, Serializable {
    public String getName() {
        return "str-cat";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        Value v = vv.get(1).resolveValue(context);
        if (vv.size() == 2 && v.type() == RU.STRING)
            return v;

        StringBuffer buf = new StringBuffer();

        for (int i=1; i<vv.size(); ++i) {

            if (i > 1)
                v = vv.get(i).resolveValue(context);

            if (v.type() == RU.STRING)
                buf.append(v.stringValue(context));
            else if (v.type() == RU.JAVA_OBJECT)
                buf.append(v.javaObjectValue(context).toString());
            else
                buf.append(v.toString());
        }


        return new Value(buf.toString(), RU.STRING);
    }
}

class StrCompare implements Userfunction, Serializable {
    public String getName() {
        return "str-compare";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(vv.get(1).stringValue(context).compareTo(vv.get(2).stringValue(context)), RU.INTEGER);
    }
}

class StrIndex implements Userfunction, Serializable {
    public String getName() {
        return "str-index";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        int rv = vv.get(2).stringValue(context).indexOf(vv.get(1).stringValue(context));
        return rv == -1 ? Funcall.FALSE : new Value(rv + 1, RU.INTEGER);
    }
}

class SubString implements Userfunction, Serializable {
    public String getName() {
        return "sub-string";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        int begin = (int) vv.get(1).numericValue(context) - 1;
        int end = (int) vv.get(2).numericValue(context);
        String s = vv.get(3).stringValue(context);

        if (begin < 0 || begin > s.length() - 1 ||
                end > s.length() || end <= 0)
            throw new JessException("sub-string",
                    "Indices must be between 1 and " +
                    s.length(), "");
        return new Value(s.substring(begin, end), RU.STRING);
    }
}

class StrSimple implements Userfunction, Serializable {
    public static final int LENGTH = 0, UPCASE = 1, LOWCASE = 2;
    public static final String m_names[] = {"str-length", "upcase", "lowcase"};

    private int m_name;

    StrSimple(int n) {
        m_name = n;
    }

    public String getName() {
        return m_names[m_name];
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value val = vv.get(1).resolveValue(context);
        int type = val.type() == RU.SYMBOL ? RU.SYMBOL : RU.STRING;
        String stringValue = val.stringValue(context);
        switch (m_name) {
            case UPCASE:
                return new Value(stringValue.toUpperCase(), type);
            case LOWCASE:
                return new Value(stringValue.toLowerCase(), type);
            case LENGTH:
                return new Value(stringValue.length(), RU.INTEGER);
            default:
                return Funcall.NIL;
        }
    }
}

