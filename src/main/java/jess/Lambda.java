package jess;

import java.io.Serializable;

/**
 * Define anonymous functions in Jess<P>
 * (C) 2013 Sandia Corporation<BR>
 */
class Lambda implements Userfunction, Serializable {

    public String getName() {
        return "lambda";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Deffunction function = new Deffunction(RU.gensym("lambda"), "Generated function");
        ValueVector args = vv.get(1).listValue(context);
        for (int i=0; i<args.size(); ++i) {
            Value v = args.get(i);
            if (! (v instanceof Variable))
                throw new JessException("lamba", "Bad value in argument list", v.toStringWithParens());
            function.addArgument(v.variableValue(context), v.type());
        }
        for (int i=2; i<vv.size(); ++i)
            function.addValue(vv.get(i));
        return new Value(function);
    }
}
