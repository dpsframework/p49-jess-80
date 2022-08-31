package jess;

import java.io.Serializable;

/**
 * Methods  for  operating  on  name-value  pairs, the  way  that  the
 * "modify" and "duplicate" functions do.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


abstract class NVPairOperation implements Serializable {
    protected static Fact getFactArgument(ValueVector vv, Context context, Rete engine, String routine) throws JessException {
        Value factValue = vv.get(1).resolveValue(context);
        Fact fact;
        int id;
        if (factValue.type() == RU.INTEGER) {
            id = factValue.intValue(context);
            fact = engine.findFactByID(id);
        } else if (factValue.type() == RU.FACT) {
            fact = (Fact) factValue.javaObjectValue(context);
            id = fact.getFactId();
        } else if (factValue.type() == RU.JAVA_OBJECT) {
            fact = factValue.factValue(context);
            id = fact.getFactId();
        } else {
            throw Value.typeError(factValue,
                    vv.get(0).symbolValue(context),
                    "a fact", factValue.type());
        }

        if (fact == null || fact.getFactId() == -1)
            throw new JessException(routine, "no such fact", id);
        return fact;
    }

    protected static String getSlotName(ValueVector svp, Context c)
            throws JessException {

        return svp.get(0).stringValue(c);
    }

    protected static Value getSlotValue(ValueVector svp, Context c, int type)
            throws JessException {

        if (type == RU.SLOT) {
            Value v = svp.get(1).resolveValue(c);
            while (v.type() == RU.LIST)
                v = v.listValue(c).get(0).resolveValue(c);

            return v;

        } else { // MULTISLOT
            ValueVector vv = new ValueVector();
            for (int i = 1; i < svp.size(); i++) {
                Value listItem = svp.get(i).resolveValue(c);
                if (listItem.type() == RU.LIST) {
                    ValueVector sublist = listItem.listValue(c);
                    for (int j = 0; j < sublist.size(); j++)
                        vv.add(sublist.get(j).resolveValue(c));
                } else
                    vv.add(listItem);
            }
            return new Value(vv, RU.LIST);
        }
    }
}
