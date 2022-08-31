package jess;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Pattern;
import java.io.Serializable;

/**
 * Regular expression functions for Jess<BR>
 * (C) 2006 E.J. Friedman-Hill and Sandia National Laboratories
 */

class RegexpFunctions extends IntrinsicPackageImpl {

    private static boolean regexpAvailable() {
        try {
            Class.forName("java.util.regex.Pattern");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void add(HashMap ht) {
        if (regexpAvailable())
            addFunction(new RegexpMatch(), ht);
        else
            addFunction(new RegexpMatchStub(), ht);
    }
}

class RegexpMatch implements Userfunction, Serializable {
    private static final Map s_patterns = Collections.synchronizedMap(new HashMap());

    public String getName() {
        return "regexp";
    }

    // TODO Group metasyntactic variables, etc
    public Value call(ValueVector vv, Context context) throws JessException {
        String expression = vv.get(1).stringValue(context);
        String trial = vv.get(2).stringValue(context);
        Pattern regex = getPattern(expression);
        boolean match = regex.matcher(trial).matches();
        return match ? Funcall.TRUE : Funcall.FALSE;
    }

    private Pattern getPattern(String expression) {
        synchronized(s_patterns) {
            Pattern p = (Pattern) s_patterns.get(expression);
            if (p == null) {
                p = Pattern.compile(expression);
                s_patterns.put(expression, p);
            }
            return p;
        }
    }
}

class RegexpMatchStub implements Userfunction, Serializable {
    public String getName() {
        return "regexp";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        throw new JessException("regexp", "Regular expressions not available in Java version",
                System.getProperty("java.specification.version"));
    }
}