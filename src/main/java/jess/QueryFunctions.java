package jess;

import java.io.Serializable;
import java.util.HashMap;

class QueryFunctions extends IntrinsicPackageImpl {
    public void add(HashMap ht) {
        addFunction(new RunQuery(), ht);
        addFunction(new CountQueryResults(), ht);
        addFunction(new RunQueryStar(), ht);
    }
}

class RunQueryStar extends QueryFunction {

    public String getName() {
        return "run-query*";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String queryName = vv.get(1).symbolValue(context);
        ValueVector params = getParamVector(vv, context);
        QueryResult result = engine.runQueryStar(queryName, params, context);
        return new Value(result);
    }
}

class RunQuery extends QueryFunction {
    public String getName() {
        return "run-query";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String queryName = vv.get(1).symbolValue(context);
        ValueVector params = getParamVector(vv, context);
        return new Value(engine.strippedQueryIterator(queryName, params, context));
    }
}

class CountQueryResults extends QueryFunction {

    public String getName() {
        return "count-query-results";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String queryName = vv.get(1).symbolValue(context);
        ValueVector params = getParamVector(vv, context);
        return new Value(engine.countQueryResults(queryName, params, context), RU.INTEGER);
    }
}

abstract class QueryFunction implements Userfunction, Serializable {
    ValueVector getParamVector(ValueVector vv, Context context) throws JessException {
        ValueVector params = new ValueVector();

        for (int i = 2; i < vv.size(); i++)
            params.add(vv.get(i).resolveValue(context));
        return params;
    }
}
