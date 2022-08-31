package jess;

import java.util.HashMap;
import java.io.Serializable;

/**
 * A ValueFactory creates and possibly caches {@link jess.Value} instances, and instances
 * of its subclasses. Each Rete engine has its own ValueFactory. You can obtain a reference to a
 * Rete object's ValueFactory and use it to create your own instances. Using the factory allows for
 * large memory savings, especially when parsing a lot of redundant data. You can create a
 * custom subclass to implement your own strategies.
 *
 * (C) 2013 Sandia Corporation
 */
public class ValueFactory implements Serializable {

    private final HashMap m_symbols = new HashMap();

    /**
     * Create a new ValueFactory. Provided so that users might subclass this class;
     * there's no reason to create your own ValueFactory objects otherwise.
     * @see jess.Rete#getValueFactory()
     */

    public ValueFactory() {

        Value[] constants = {
                Funcall.TRUE, Funcall.FALSE, Funcall.NIL,
                Funcall.EOF, Funcall.T, Funcall.CRLF,
                Funcall.s_else, Funcall.s_elif,
                Funcall.s_then, Funcall.s_do
        };
        try {
            for (int i = 0; i < constants.length; i++) {
                Value constant = constants[i];
                get(constant.symbolValue(null), constant.type());
            }
        } catch (JessException cantHappen) {
            // Silently ignore
        }

    }

    /**
     * Create a STRING, SYMBOL, VARIABLE or MULTIVARIABLE value.
     * @param v the contents as a String
     * @param type STRING, SYMBOL, VARIABLE or MULTIVARIABLE
     * @return a new Value, or a previously cached one
     * @throws JessException if anything goes wrong
     */
    public Value get(String v, int type) throws JessException {
        switch (type) {
            case RU.VARIABLE:
            case RU.MULTIVARIABLE:
                return new Variable(v, type);

            case RU.SYMBOL: {
                synchronized(m_symbols) {
                    Value symbol = (Value) m_symbols.get(v);
                    if (symbol == null) {
                        symbol = new Value(v, RU.SYMBOL);
                        m_symbols.put(v, symbol);
                    }
                    return symbol;
                }
            }

            default:
                return new Value(v, type);

        }
    }

    /**
     * Return a numeric Value object of the given type. Allowed types are RU.LONG, RU.INTEGER, and RU.FLOAT.
     * @param v a numberic value
     * @param type one of RU.INTEGER, RU.FLOAT, or RU.LONG
     * @return a Value or LongValue containing the given value
     * @throws JessException
     */

    public Value get(double v, int type) throws JessException {
        if (type == RU.LONG)
            return get((long) v);
        return new Value(v, type);
    }

    /**
     * Return a Value appropriate to the type argument.
     * @param v an integer
     * @param type one of RU.INTEGER, RU.NONE
     * @return a Value of the given type with the given value
     * @throws JessException
     */
    public Value get(int v, int type) throws JessException {
        return new Value(v, type);
    }

    /**
     * Returns a Value of type {@link jess.RU#INTEGER}. You need to watch out for autoboxing;
     * you'll get different results from an int vs. an Integer.
     * @param v an int
     * @return a Value of type RU.INTEGER
     * @throws JessException if anything goes wrong
     */
    public Value get(int v) throws JessException {
        return new Value(v);
    }

    /**
     * Returns a {@link jess.LongValue} representing the argument.
     * @param v a long value
     * @return a LongValue object
     * @throws JessException if anything goes wrong
     */

    public Value get(long v) throws JessException {
        return new LongValue(v);
    }

    /**
     * Returns one of the contants {@link jess.Funcall#TRUE} or {@link jess.Funcall#FALSE}.
     * @param v a boolean value
     * @return Funcall.TRUE or Funcall.FALSE
     * @throws JessException if anything goes wrong
     */

    public Value get(boolean v) throws JessException {
        return v ? Funcall.TRUE : Funcall.FALSE;
    }

    /**
     * Return a new Value of type {@link jess.RU#JAVA_OBJECT}.
     * If the argument is null, returns the constant {@link jess.Funcall#NIL}.
     * @param v any Java object
     * @return a Value of type JAVA_OBJECT
     * @throws JessException if anything goes wrong
     */

    public Value get(Object v) throws JessException {
        return v == null ? Funcall.NIL : new Value(v);
    }
}
