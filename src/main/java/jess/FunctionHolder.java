package jess;

import java.io.Serializable;

/**
 * Allows functions to be called indirectly, so that we can 'advice' them.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */


final class FunctionHolder implements Serializable {
    private static Userfunction UNDEFINED = new UndefinedFunction();
    private Userfunction m_uf;

    FunctionHolder(Userfunction uf) {
        setFunction(uf);
    }

    final void setFunction(Userfunction uf) {
        if (uf == null) {
            m_uf = UNDEFINED;
        } else
            m_uf = uf;
    }

    final Userfunction getFunction() {
        return m_uf;
    }

    final Value call(Funcall vv, Context c) throws JessException {
        Userfunction uf = m_uf;
        if (c.getInAdvice())
            uf = stripAdvice();
        return uf.call(vv, c);
    }

    // If inside an "advice", we had better not call any other advice functions!
    Userfunction stripAdvice() {
        Userfunction uf = m_uf;

        while (uf instanceof Advice)
            uf = ((Advice) uf).getFunction();

        return uf;
    }

    private static class UndefinedFunction implements Userfunction, Serializable {
        public String getName() {
            return "undefined";
        }

        public Value call(ValueVector vv, Context c) throws JessException {
            if (vv instanceof Funcall) {
                Funcall funcall = (Funcall) vv;
                funcall.reset();
                return funcall.execute(c);
            } else {
                String name = vv.get(0).stringValue(c);
                throw new JessException(name, "undefined function", name);
            }
        }
    }
}
