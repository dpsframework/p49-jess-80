package jess;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Functions that implement arithmetic operations
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

class ArithmeticFunctions extends IntrinsicPackageImpl {
    final static Value ONE = new Value(1);

    public void add(HashMap map) {
        addFunction(new Eq(), map);
        addFunction(new EqStar(), map);
        addFunction(new Equals(), map);
        addFunction(new NotEquals(), map);
        addFunction(new Gt(), map);
        addFunction(new Lt(), map);
        addFunction(new GtOrEq(), map);
        addFunction(new LtOrEq(), map);
        addFunction(new Neq(), map);
        addFunction(new Mod(), map);
        addFunction(new Plus(), map);
        addFunction(new Times(), map);
        addFunction(new Minus(), map);
        addFunction(new Divide(), map);
        addFunction(new PlusPlus(), map);
        addFunction(new MinusMinus(), map);
    }
}

class Eq implements Userfunction, Serializable {

    public String getName() {
        return "eq";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value first = vv.get(1).resolveValue(context);
        for (int i = 2; i < vv.size(); i++) {
            if (!vv.get(i).resolveValue(context).equals(first))
                return Funcall.FALSE;
        }
        return Funcall.TRUE;
    }
}

class EqStar implements Userfunction, Serializable {

    public String getName() {
        return "eq*";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value first = vv.get(1).resolveValue(context);
        for (int i = 2; i < vv.size(); i++) {
            if (!vv.get(i).resolveValue(context).equalsStar(first))
                return Funcall.FALSE;
        }
        return Funcall.TRUE;
    }
}

class Equals extends AbstractComparison {

    public String getName() {
        return "=";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
        try {
			return v1.compareTo(v2) == 0;
		} catch (Exception e) {
			// The "equals" function needs to be relatively tolerant;
			// without this catch the behavior is intransitive .
			return false;
		}
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            return false;

        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) == v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) == v2.intValue(null);

            default:
                return v1.numericValue(null) == v2.numericValue(null);
        }
    }
}

class NotEquals extends AbstractComparison {

    public String getName() {
        return "<>";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
        return v1.compareTo(v2) != 0;
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
	    return true;

        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) != v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) != v2.intValue(null);

            default:
                return v1.numericValue(null) != v2.numericValue(null);
        }
    }

    protected Value newV1(Value v1, Value v2) {
        // Don't swap new for old; we want to compare
        // the first argument with all the other ones,
        // unlike most of these operators.
        return v1;
    }
}

class Gt extends AbstractComparison {

    public String getName() {
        return ">";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
        return v1.compareTo(v2) > 0;
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException(">", "Not a number:", v2.toString());

        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) > v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) > v2.intValue(null);
            default:
                return v1.numericValue(null) > v2.numericValue(null);

        }
    }
}

class Lt extends AbstractComparison {

    public String getName() {
        return "<";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
            return v1.compareTo(v2) < 0;
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException("<", "Not a number:", v2.toString());

        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) < v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) < v2.intValue(null);

            default:
                return v1.numericValue(null) < v2.numericValue(null);
        }
    }
}

class GtOrEq extends AbstractComparison {

    public String getName() {
        return ">=";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
        return v1.compareTo(v2) >= 0;
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException(">=", "Not a number:", v2.toString());
        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) >= v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) >= v2.intValue(null);

            default:
                return v1.numericValue(null) >= v2.numericValue(null);
        }
    }
}

class LtOrEq extends AbstractComparison {

    public String getName() {
        return "<=";
    }

    protected boolean computeComparable(Comparable v1, Object v2) throws JessException {
        return v1.compareTo(v2) <= 0;
    }

    protected boolean computeNumeric(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException("<=", "Not a number:", v2.toString());
        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return v1.longValue(null) <= v2.longValue(null);

            case RU.INTEGER + RU.INTEGER:
                return v1.intValue(null) <= v2.intValue(null);

            default:
                return v1.numericValue(null) <= v2.numericValue(null);
        }
    }
}

class Neq implements Userfunction, Serializable {

    public String getName() {
        return "neq";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value first = vv.get(1).resolveValue(context);
        for (int i = 2; i < vv.size(); i++) {
            if (vv.get(i).resolveValue(context).equals(first))
                return Funcall.FALSE;
        }
        return Funcall.TRUE;
    }
}

class Mod implements Userfunction, Serializable {

    public String getName() {
        return "mod";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value v1 = vv.get(1).resolveValue(context);
        Value v2 = vv.get(2).resolveValue(context);
        if (v1.type() == RU.LONG || v2.type() == RU.LONG) {
            return new LongValue(v1.longValue(null) % v2.longValue(null));
        } else {
            return new Value(v1.intValue(null) % v2.intValue(null), RU.INTEGER);
        }
    }
}

class Plus implements Userfunction, Serializable {

    public String getName() {
        return "+";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value result = vv.get(1).resolveValue(context);
        if (!result.isNumeric(context))
            throw new JessException("+", "Not a number:", result.toString());
        for (int i = 2; i < vv.size(); ++i) {
            result = compute(result, vv.get(i).resolveValue(context));
        }
        return result;
    }

    static Value compute(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException("+", "Not a number:", v2.toString());
        switch (v1.type() + v2.type()) {
            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return new LongValue(v1.longValue(null) + v2.longValue(null));

            case RU.INTEGER + RU.INTEGER:
                return new Value(v1.intValue(null) + v2.intValue(null), RU.INTEGER);

            default:
                return new Value(v1.numericValue(null) + v2.numericValue(null), RU.FLOAT);
        }
    }
}

class Times implements Userfunction, Serializable {

    public String getName() {
        return "*";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value result = vv.get(1).resolveValue(context);
        if (!result.isNumeric(context))
            throw new JessException("*", "Not a number:", result.toString());
        for (int i = 2; i < vv.size(); ++i) {
            result = compute(result, vv.get(i).resolveValue(context));
        }
        return result;
    }

    private Value compute(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException("*", "Not a number:", v2.toString());
        switch (v1.type() + v2.type()) {

            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return new LongValue(v1.longValue(null) * v2.longValue(null));

            case RU.INTEGER + RU.INTEGER:
                return new Value(v1.intValue(null) * v2.intValue(null), RU.INTEGER);

            default:
                return new Value(v1.numericValue(null) * v2.numericValue(null), RU.FLOAT);
        }
    }
}

class Minus implements Userfunction, Serializable {
    public String getName() {
        return "-";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value result = vv.get(1).resolveValue(context);
        if (!result.isNumeric(context))
            throw new JessException("-", "Not a number:", result.toString());
        for (int i = 2; i < vv.size(); ++i) {
            result = compute(result, vv.get(i).resolveValue(context));
        }
        return result;
    }

    static Value compute(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException("-", "Not a number:", v2.toString());
        switch (v1.type() + v2.type()) {

            case RU.LONG + RU.INTEGER:
            case RU.LONG + RU.LONG:
                return new LongValue(v1.longValue(null) - v2.longValue(null));

            case RU.INTEGER + RU.INTEGER:
                return new Value(v1.intValue(null) - v2.intValue(null), RU.INTEGER);

            default:
                return new Value(v1.numericValue(null) - v2.numericValue(null), RU.FLOAT);
        }
    }
}

class Divide implements Userfunction, Serializable {

    public String getName() {
        return "/";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        double quotient = vv.get(1).numericValue(context);
        int size = vv.size();
        for (int i = 2; i < size; i++) {
            quotient /= vv.get(i).numericValue(context);
        }
        return new Value(quotient, RU.FLOAT);
    }
}

class PlusPlus implements Userfunction, Serializable {

    public String getName() {
        return "++";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value var = vv.get(1);
        if (var.type() != RU.VARIABLE)
            throw new JessException("++", "Not a variable", var.toString());
        Value result = Plus.compute(var.resolveValue(context), ArithmeticFunctions.ONE);
        context.setVariable(var.variableValue(context), result);
        return result;
    }
}

class MinusMinus implements Userfunction, Serializable {

    public String getName() {
        return "--";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value var = vv.get(1);
        if (var.type() != RU.VARIABLE)
            throw new JessException("--", "Not a variable", var.toString());
        Value result = Minus.compute(var.resolveValue(context), ArithmeticFunctions.ONE);
        context.setVariable(var.variableValue(context), result);
        return result;
    }
}

