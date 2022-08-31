package jess;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * Serialiable representation for a list of overloaded static methoda
 */

class SerializableMD implements Serializable {
    private final String m_class;
    private final String m_name;
    private transient List m_methods;

    SerializableMD(List methods) {
        Method m = (Method) methods.get(0);
        m_class = m.getDeclaringClass().getName();
        m_name = m.getName();
        m_methods = methods;
        for (Iterator it = methods.iterator(); it.hasNext();) {
            Method method = (Method) it.next();
            method.setAccessible(true);
        }
    }

    private void reload(Rete engine) throws JessException {
        try {
            m_methods = new ArrayList();
            Class c = engine.findClass(m_class);
            Method[] methods = c.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals(m_name) && Modifier.isStatic(method.getModifiers())) {
                    m_methods.add(method);
                    method.setAccessible(true);
                }
            }

        } catch (Exception e) {
            throw new JessException("SerializableMD.reload",
                                    "Can't recreate method", e);
        }
    }

    public String getName() {
        return m_name;
    }

    public Value invoke(List argList, Rete engine) throws JessException {
        if (m_methods == null)
            reload(engine);
        try {
            for (Iterator it = m_methods.iterator(); it.hasNext();) {
                Method method = (Method) it.next();
                Class[] argTypes = method.getParameterTypes();
                if (argList.size() != argTypes.length)
                    continue;
                Object[] args = argList.toArray(new Object[argList.size()]);
                for (int i = 0; i < args.length; i++) {
                    Value arg = (Value) args[i];
                    args[i] = RU.valueToObject(argTypes[i], arg, engine.getGlobalContext());
                }
                Object result = method.invoke(null, args);
                return RU.objectToValue(method.getReturnType(), result);
            }
            ValueVector vv = new ValueVector();
            vv.addAll(argList);
            throw new JessException(m_name, "No overloading I can call with these arguments", vv.toStringWithParens());
        } catch (IllegalAccessException e) {
            throw new JessException(m_name, "Access exception", e);
        } catch (InvocationTargetException e) {
            throw new JessException(m_name, "Called method threw an exception", e.getCause());
        }
    }


    public boolean equals(Object o) {
        if (!(o instanceof SerializableMD))
            return false;

        SerializableMD pd = (SerializableMD) o;

        return m_class.equals(pd.m_class) &&
                m_name.equals(pd.m_name);
    }
}

