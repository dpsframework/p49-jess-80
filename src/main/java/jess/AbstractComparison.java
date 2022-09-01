package jess;

import java.io.Serializable;

/**
 * (C) 2007 Sandia National Laboratories<BR>
 * $Id: AbstractComparison.java,v 1.3 2008-05-02 19:54:48 ejfried Exp $
 */
abstract class AbstractComparison implements Userfunction, Serializable {
    public Value call(ValueVector vv, Context context) throws JessException {
        Value v1 = vv.get(1).resolveValue(context);
        int index = 1;
        if (v1.isNumeric(context)) {
            for (int i=index+1; i<vv.size(); ++i) {
                Value v2 = vv.get(++index).resolveValue(context);
                if (!computeNumeric(v1, v2))
                    return Funcall.FALSE;
                v1 = newV1(v1, v2);
            }
        } else if (v1.type() == RU.JAVA_OBJECT || v1.isLexeme(null)) {
            for (int i=index+1; i<vv.size(); ++i) {
                if (!(v1.javaObjectValue(null) instanceof Comparable))
                    throw new JessException(vv.get(0).symbolValue(context), "Not a Comparable:", v1.toString());
                Comparable<?> c = (Comparable<?>) v1.javaObjectValue(null);
                Value v2 = vv.get(++index).resolveValue(context);
                try {
                    if (!computeComparable(c, v2.javaObjectValue(null)))
                        return Funcall.FALSE;
                } catch (RuntimeException ex) {
                    throw new JessException(vv.get(0).symbolValue(context), "compareTo threw an exception", ex);
                }

                v1 = newV1(v1, v2);
            }
        } else
            throw new JessException(vv.get(0).symbolValue(context), "Not a number or Comparable:", v1.toString());

        return Funcall.TRUE;
    }

    protected Value newV1(Value v1, Value v2) {
        return v2;
    }

    protected abstract boolean computeComparable(Comparable<?> v1, Object v2) throws JessException;

    protected abstract boolean computeNumeric(Value v1, Value v2) throws JessException;
}
