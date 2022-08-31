package jess;

import java.util.HashMap;
import java.io.Serializable;

/**
 * Functions related to debugging.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */


class DebugFunctions extends IntrinsicPackageImpl {
    public void add(HashMap ht) {
        addFunction(new JessType(), ht);
    }
}

class JessType implements Userfunction, Serializable {
    public String getName() {
        return "jess-type";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return context.getEngine().getValueFactory().get(RU.getTypeName(vv.get(1).resolveValue(context).type()), RU.SYMBOL);
    }

}
