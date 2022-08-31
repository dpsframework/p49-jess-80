package jess;

import java.io.Serializable;

/**
 * A class to represent a Jess variable. It is 'self-resolving' using Context.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class Variable extends Value implements Serializable {
    /**
     * Create a Variable.
     *
     * @param name the name of the variable
     * @param type RU.VARIABLE or RU.MULTIVARIABLE
     * @throws JessException if the type is invalid
     */

    public Variable(String name, int type) throws JessException {
        super(name, type);
        if (name.indexOf("?") != -1 || name.indexOf("$") != -1)
            throw new JessException("Variable.Variable",
                    "Variable name cannot contain '?' or '$'",
                    name);

    }

    /**
     * Will resolve the variable (return the value it represents.)
     *
     * @param c an evaluation context. Cannot be null!
     * @return the value of this variable
     * @throws JessException if the variable is undefined.
     */

    public Value resolveValue(Context c) throws JessException {
        if (c == null)
            throw new JessException("Variable.resolveValue",
                    "Null context for",
                    variableValue(c));
        else
            return c.getVariable(variableValue(c));
    }

    /**
     * Resolves the variable, then returns the value as an Java object.
     */

    public final Object javaObjectValue(Context c) throws JessException {
        return resolveValue(c).javaObjectValue(c);
    }

    /**
     * Resolves the variable, then returns the value as a Fact
     */

    public final Fact factValue(Context c) throws JessException {
        return resolveValue(c).factValue(c);
    }

    /**
     * Resolves the variable, then returns the value as a list
     */

    public final ValueVector listValue(Context c) throws JessException {
        return resolveValue(c).listValue(c);
    }

    /**
     * Resolves the variable, then returns the value as an int
     */

    public final int intValue(Context c) throws JessException {
        return resolveValue(c).intValue(c);
    }

    /**
     * Resolves the variable, then returns the value as a float
     */

    public final double floatValue(Context c) throws JessException {
        return resolveValue(c).floatValue(c);
    }

    /**
     * Resolves the variable, then returns the value as a float
     */

    public final double numericValue(Context c) throws JessException {
        return resolveValue(c).numericValue(c);
    }

    /**
     * Resolves the variable, then returns the value as a symbol
     */

    public final String symbolValue(Context c) throws JessException {
        return resolveValue(c).symbolValue(c);
    }

    /**
     * Returns the name of this variable
     */

    public final String variableValue(Context c) {
        try {
            return super.stringValue(c);
        } catch (JessException ignored) {
            return null;
        }
    }

    /**
     * Resolves the variable, then returns the value as a string
     */

    public final String stringValue(Context c) throws JessException {
        return resolveValue(c).stringValue(c);
    }

    public boolean isVariable() {        
        return true;
    }
}

