package jess;

/**
 * The "modify" function
 * (C) 2006 Sandia Corporation and E. J. Friedman-Hill
 */

class Modify extends NVPairOperation implements Userfunction {

    public String getName() {
        return "modify";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        Rete engine = context.getEngine();

        Fact fact = getFactArgument(vv, context, engine, "modify");

        Deftemplate dt = fact.getDeftemplate();

        int size = vv.size()-2;
        String[] slotNames = new String[size];
        Value[] slotValues = new Value[size];
        for (int i = 2, j=0; i < vv.size(); i++, j++) {
            // fetch the slot, value subexp, stored as a List
            ValueVector svp = vv.get(i).listValue(context);

            String slotName = getSlotName(svp, context);
            int type = dt.getSlotType(slotName);
            slotNames[j] = slotName;
            slotValues[j] = getSlotValue(svp, context, type);
        }

        engine.modify(fact, slotNames, slotValues, context);

        return new FactIDValue(fact);
    }

}

