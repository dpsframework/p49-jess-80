package jess;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Mathematical functions for Jess. The original versions of many of these
 * were contributed by user Win Carus.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class MathFunctions extends IntrinsicPackageImpl {

    public void add(HashMap table) {
        // abs added by Win Carus (9.19.97)
        addFunction(new Abs(), table);
        // div added by Win Carus (9.19.97)
        addFunction(new Div(), table);
        // float added by Win Carus (9.19.97)
        addFunction(new JessFloat(), table);
        // integer added by Win Carus (9.19.97)
        addFunction(new JessInteger(), table);
        // max added by Win Carus (9.19.97)
        addFunction(new Max(), table);
        // min added by Win Carus (9.19.97)
        addFunction(new Min(), table);
        // ** added by Win Carus (9.19.97)
        addFunction(new Expt(), table);
        // exp added by Win Carus (9.19.97)
        addFunction(new Exp(), table);
        // log added by Win Carus (9.19.97)
        addFunction(new Log(), table);
        // log10 added by Win Carus (9.19.97)
        addFunction(new Log10(), table);
        addFunction(new Constant("pi", Math.PI), table);
        addFunction(new Constant("e", Math.E), table);
        // round added by Win Carus (9.19.97)
        addFunction(new Round(), table);
        // sqrt added by Win Carus (9.19.97)
        addFunction(new Sqrt(), table);
        // random added by Win Carus (9.19.97)
        addFunction(new JessRandom(), table);
    }
}

class Abs implements Userfunction, Serializable {
    public String getName() {
        return "abs";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value v = vv.get(1).resolveValue(context);
        switch (v.type()) {
            case RU.INTEGER:
                return new Value(Math.abs(v.intValue(context)), RU.INTEGER);
            case RU.LONG:
                return new LongValue(Math.abs(v.longValue(context)));
            default:
                return new Value(Math.abs(v.numericValue(context)), RU.FLOAT);
        }
    }
}

class Div implements Userfunction, Serializable {
    public String getName() {
        return "div";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        int first = vv.get(1).intValue(context);
        int second = vv.get(2).intValue(context);
        int result = first/second;

        for (int i=3; i<vv.size(); ++i)
            result /= vv.get(i).intValue(context);
        return new Value(result, RU.INTEGER);
    }
}

class JessFloat implements Userfunction, Serializable {
    public String getName() {
        return "float";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value val = vv.get(1).resolveValue(context);
        if (val.type() == RU.FLOAT)
            return val;
        else
            return new Value(val.numericValue(context), RU.FLOAT);
    }
}

class JessInteger implements Userfunction, Serializable {
    public String getName() {
        return "integer";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value val = vv.get(1).resolveValue(context);
        if (val.type() == RU.INTEGER)
            return val;
        else
            return new Value((int) val.numericValue(context), RU.INTEGER);
    }
}


class Max implements Userfunction, Serializable {
    public String getName() {
        return "max";
    }


    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value result = vv.get(1).resolveValue(context);
        if (!result.isNumeric(context))
            throw new JessException(getName(), "Not a number:", result.toString());
        for (int i = 2; i < vv.size(); ++i) {
            result = compute(result, vv.get(i).resolveValue(context));
        }
        return result;
    }

    private Value compute(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException(getName(), "Not a number:", v2.toString());
        switch (v1.type()) {
            case RU.INTEGER:
            case RU.LONG: {
                long l1 = v1.longValue(null);
                long l2 = v2.longValue(null);
                return l1 > l2? v1 : v2;
            }

            default: {
                double d1 = v1.floatValue(null);
                double d2 = v2.floatValue(null);
                return d1 > d2 ? v1 : v2;
            }
        }
    }
}

class Min implements Userfunction, Serializable {
    public String getName() {
        return "min";
    }


    public Value call(ValueVector vv, Context context)
            throws JessException {
        Value result = vv.get(1).resolveValue(context);
        if (!result.isNumeric(context))
            throw new JessException(getName(), "Not a number:", result.toString());
        for (int i = 2; i < vv.size(); ++i) {
            result = compute(result, vv.get(i).resolveValue(context));
        }
        return result;
    }

    private Value compute(Value v1, Value v2) throws JessException {
        if (!v2.isNumeric(null))
            throw new JessException(getName(), "Not a number:", v2.toString());
        switch (v1.type()) {
            case RU.INTEGER:
            case RU.LONG: {
                long l1 = v1.longValue(null);
                long l2 = v2.longValue(null);
                return l1 < l2? v1 : v2;
            }

            default: {
                double d1 = v1.floatValue(null);
                double d2 = v2.floatValue(null);
                return d1 < d2 ? v1 : v2;
            }
        }
    }
}

class Expt implements Userfunction, Serializable {
    public String getName() {
        return "**";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(Math.pow(vv.get(1).numericValue(context), vv.get(2).numericValue(context)), RU.FLOAT);
    }
}

class Exp implements Userfunction, Serializable {
    public String getName() {
        return "exp";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(Math.pow(Math.E, vv.get(1).numericValue(context)), RU.FLOAT);
    }
}

class Log implements Userfunction, Serializable {
    public String getName() {
        return "log";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(Math.log(vv.get(1).numericValue(context)), RU.FLOAT);
    }
}

class Log10 implements Userfunction, Serializable {

    private static final double log10 = Math.log(10.0);

    public String getName() {
        return "log10";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value((Math.log(vv.get(1).numericValue(context)) / log10), RU.FLOAT);
    }
}

class Constant implements Userfunction, Serializable {
    private Value m_val;
    private final String m_name;

    Constant(String name, double value) {
        m_name = name;
        try {
            m_val = new Value(value, RU.FLOAT);
        } catch (JessException re) {
        }
    }

    public String getName() {
        return m_name;
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return m_val;
    }
}

class Round implements Userfunction, Serializable {
    public String getName() {
        return "round";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(Math.round(vv.get(1).numericValue(context)), RU.INTEGER);
    }
}

class Sqrt implements Userfunction, Serializable {
    public String getName() {
        return "sqrt";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(Math.sqrt(vv.get(1).numericValue(context)), RU.FLOAT);
    }
}

class JessRandom implements Userfunction, Serializable {
    public String getName() {
        return "random";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value((int) (Math.random() * 65536), RU.INTEGER);
    }
}

