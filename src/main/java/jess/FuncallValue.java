package jess;

import java.io.Serializable;

/**
 * A class to represent a Jess function call stored in a Value.
 * It is 'self-resolving' using Context.
 * Use this subclass of Value when you want to create a Value that
 * represents a function call (for example, when you are creating a
 * <tt>jess.Funcall</tt> containing nested function calls.)
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 *
 * @see Funcall
 */

public class FuncallValue extends Value implements Serializable {
    public FuncallValue(Funcall f) throws JessException {
        super(f, RU.FUNCALL);
    }

    public Value resolveValue(Context c) throws JessException {
        if (c == null)
            throw new JessException("FuncallValue.resolveValue",
                                    "Null context for",
                                    funcallValue(c).toStringWithParens());

        else
            return funcallValue(c).execute(c);
    }

    public final Object javaObjectValue(Context c) throws JessException {
        return resolveValue(c).javaObjectValue(c);
    }

    public final Fact factValue(Context c) throws JessException {
        return resolveValue(c).factValue(c);
    }

    public final ValueVector listValue(Context c) throws JessException {
        return resolveValue(c).listValue(c);
    }

    public final int intValue(Context c) throws JessException {
        return resolveValue(c).intValue(c);
    }

    public final double floatValue(Context c) throws JessException {
        return resolveValue(c).floatValue(c);
    }

    public final double numericValue(Context c) throws JessException {
        return resolveValue(c).numericValue(c);
    }

    public final String symbolValue(Context c) throws JessException {
        return resolveValue(c).symbolValue(c);
    }

    public final String variableValue(Context c) throws JessException {
        return resolveValue(c).variableValue(c);
    }

    public final String stringValue(Context c) throws JessException {
        return resolveValue(c).stringValue(c);
    }

    public Value copy() throws JessException {
        Funcall f = (Funcall) funcallValue(null).clone();
        for (int i = 0; i<f.size(); ++i) {
            if (f.get(i) instanceof FuncallValue) {
                f.set(((FuncallValue)f.get(i)).copy(), i);
            }
        }
        return new FuncallValue(f);
    }
}

