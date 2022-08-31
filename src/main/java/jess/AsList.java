package jess;

import java.io.Serializable;

/**
 * (C) 2007 Sandia National Laboratories<BR>
 */
class AsList implements Userfunction, Serializable {
    public String getName() {
        return "as-list";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Object array = vv.get(1).javaObjectValue(context);
        if (!array.getClass().isArray())
            throw new JessException("as-list", "Argument is not an array:", array.toString());
        return RU.objectToValue(array.getClass(), array);
    }
}
