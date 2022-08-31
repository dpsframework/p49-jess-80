package jess;

/**
 * The "duplicate" function
 * (C) 2006 Sandia Corporation and E.J Friedman-Hill
 */

class Duplicate extends NVPairOperation implements Userfunction {

    public String getName() {
        return "duplicate";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Fact f = duplicate(vv, context);

        if (f == null)
            return Funcall.FALSE;
        else
            return new FactIDValue(f);
    }

    static Fact duplicate(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        Fact fact = getFactArgument(vv, context, engine, "duplicate");

        if (fact.isShadow())
            throw new JessException("duplicate",
                    "Can't duplicate shadow fact",
                    fact.toString());

        Deftemplate dt = fact.getDeftemplate();

        synchronized (engine.getWorkingMemoryLock()) {

            fact = (Fact) fact.clone();

            for (int i = 2; i < vv.size(); i++) {

                // fetch the slot, value subexp, stored as a List
                ValueVector svp = vv.get(i).listValue(context);
                String slotName = getSlotName(svp, context);
                int idx = dt.getSlotIndex(slotName);
                if (idx == -1)
                    throw new JessException("duplicate",
                            "No such slot " + slotName + " in template",
                            dt.getName());
                int type = dt.getSlotType(idx);

                // Set the value in the fact
                fact.setSlotValue(slotName, getSlotValue(svp, context, type));
            }
        }
        // Add the new fact to the Rete network
        return engine.assertFact(fact, context);
    }
}

