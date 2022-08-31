package jess;

import java.io.Serializable;
import java.lang.reflect.*;

/**
 * Lets you implement any interface from Jess.
 * Here's an example of creating a Runnable object entirely from Jess and
 * running it in a new Thread:
 *
 * <pre>
 * (load-function Implement)
 *
 * ;; Function's arguments will be the name of the method called on proxy object
 * ;; (run, here ), followed by the individual arguments passed to called method (none here).
 * (deffunction my-runnable ($?rest)
 *   (printout t "Hello, World" crlf))
 *
 * ;; Make a Runnable whose run() method will call my-runnable
 * (bind ?runnable (implement Runnable my-runnable))
 *
 * ;; Use the runnable
 * ((new Thread ?runnable) start)
 * </pre>
 * (C) 2013 Sandia Corporation<br>
 */

class Implement implements Userfunction, Serializable {

    public String getName() {
        return "implement";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String iface = vv.get(1).stringValue(context);
        try {
            Class clazz = engine.findClass(iface);
            int functionIndex = 2;
            if (vv.size() == 4) {
                String using = vv.get(2).stringValue(context);
                if (!"using".equals(using))
                    throw new JessException("implement", "Expected 'using'", using);
                ++functionIndex;
            }

            Value v = vv.get(functionIndex).resolveValue(context);
            Userfunction uf;
            if (v.type() == RU.LAMBDA)
                uf = (Deffunction) v.javaObjectValue(context);
            else
                uf = engine.findUserfunction(v.toString());

            Object proxy =
                    Proxy.newProxyInstance(clazz.getClassLoader(),
                                           new Class[] {clazz},
                                           new IH(uf, engine.getGlobalContext().push()));
            return new Value(proxy);

        } catch (ClassNotFoundException e) {
            throw new JessException("implement", "Interface not found:", iface);
        } catch (IllegalArgumentException iae) {
            throw new JessException("implement", "Can't make proxy", iae);
        }
    }
}

class IH implements InvocationHandler {
    private Userfunction m_function;
    private Context m_context;

    IH(Userfunction theFunction, Context theContext) {
        m_function = theFunction;
        m_context = theContext;
    }

    public Object invoke(Object proxy,
                         Method method,
                         Object[] args)
            throws Throwable {

        ValueVector vv = new ValueVector();
        vv.add(new Value(m_function.getName(), RU.SYMBOL));
        vv.add(new Value(method.getName(), RU.SYMBOL));

        if (args != null) {
            for (int i=0; i<args.length; ++i) {
                Object obj = args[i];
                Class clazz = Object.class;
                if (obj != null)
                    clazz = obj.getClass();
                vv.add(RU.objectToValue(clazz, obj));
            }
        }
        Value result = m_function.call(vv, m_context);
        Class returnType = method.getReturnType();
        if (returnType == Void.TYPE)
            return null;
        else
            return RU.valueToObject(returnType, result, m_context);
    }
}

