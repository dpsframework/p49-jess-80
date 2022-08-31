package jess;

import java.io.Serializable;

/**
 * The "provide" Userfunction is part of the "require/provide" system
 * for noting dependencies between Jess language source files.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Provide implements Userfunction, Serializable {
    public String getName() {
        return "provide";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value feature = vv.get(1).resolveValue(context);
        String symbol = feature.symbolValue(context);
        context.getEngine().defineFeature(symbol);
        return feature;
    }
}
