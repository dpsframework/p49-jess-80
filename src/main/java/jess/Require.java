package jess;

import java.io.Serializable;

class Require implements Userfunction, Serializable {

    public String getName() {
        return "require";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value featureValue = vv.get(1).resolveValue(context);
        String feature = featureValue.stringValue(context);
        Rete engine = context.getEngine();
        if (engine.isFeatureDefined(feature))
            return featureValue;

        String filename;
        if (vv.size() == 2)
            filename = feature + ".clp";
        else
            filename = vv.get(2).stringValue(context);
        try {
            Batch.batch(filename, engine, context);
        } finally {
            if (!engine.isFeatureDefined(feature))
                throw new JessException("require", "Feature not found:", feature);
        }
        return featureValue;
    }
}
