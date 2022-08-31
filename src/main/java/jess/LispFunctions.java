package jess;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Some LISP compatibility functions for Jess.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

class LispFunctions extends IntrinsicPackageImpl {

    public void add(HashMap table) {
        addFunction(new Progn(), table);
        addFunction(new Apply(), table);
        addFunction(new MapFunction(), table);
        addFunction(new FilterFunction(), table);
        addFunction(new CreateMF("list"), table);
        addFunction(new AsList(), table);
    }
}

/**
 * Executes a list of function calls, returning the value of the last one
 */

class Progn implements Userfunction, Serializable {
    public String getName() {
        return "progn";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value v = Funcall.NIL;
        for (int i = 1; i < vv.size(); i++)
            v = vv.get(i).resolveValue(context);
        return v;
    }
}

/**
 * Executes a function on each item in a list, returns a list containing the results
 */

class MapFunction implements Userfunction, Serializable {
    public String getName() {
        return "map";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Userfunction function = vv.get(1).functionValue(context);
        ValueVector result = new ValueVector();

        if (vv.size() > 2) {

            ValueVector[] targets = new ValueVector[vv.size() - 2];
            for (int i = 2; i < vv.size(); ++i)
                targets[i - 2] = vv.get(i).listValue(context);

            // Build up a little "function call"
            ValueVector args = new ValueVector();
            args.add(context.getEngine().getValueFactory().get("lambda", RU.SYMBOL));
            for (int i = 0; i < targets.length; ++i)
                args.add(Funcall.NIL);

            for (int i = 0; i < targets[0].size(); ++i) {
                for (int j = 0; j < targets.length; j++) {
                    args.set(targets[j].get(i), j + 1);
                }
                result.add(function.call(args, context));
            }
        }
        return new Value(result, RU.LIST);
    }
}




/**
 * Executes a predicate function on each item in a list, returns a list containing the items that passed
 */

class FilterFunction implements Userfunction, Serializable {
    public String getName() {
        return "filter";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Userfunction function = vv.get(1).functionValue(context);
        ValueVector result = new ValueVector();
        ValueVector targets = vv.get(2).listValue(context);

        // Build up a little "function call"
        ValueVector args = new ValueVector();
        args.add(context.getEngine().getValueFactory().get("lambda", RU.SYMBOL));
        args.add(Funcall.NIL);

        for (int i = 0; i < targets.size(); ++i) {
            Value target = targets.get(i).resolveValue(context);
            args.set(target, 1);
            Value pass = function.call(args, context);
            if (!pass.equals(Funcall.FALSE))
                result.add(target);
        }
        return new Value(result, RU.LIST);
    }
}


/**
 * Calls the named function to the list of arguments
 */

class Apply implements Userfunction, Serializable {
    public String getName() {
        return "apply";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value functionObject = vv.get(1).resolveValue(context);
        Userfunction function = functionObject.functionValue(context);
        ValueVector funcall = new ValueVector();
        funcall.add(functionObject);
        int size = vv.size();
        for (int i = 2; i < size; i++)
            funcall.add(vv.get(i));
        return function.call(funcall, context);
    }
}










