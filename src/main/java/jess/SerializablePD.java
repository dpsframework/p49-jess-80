package jess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jess.ClassResearcher.Property;

/**
 * We use this in deftemplates so that we can serialize an engine containing
 * defclasses.
 */

class SerializablePD implements SerializableD {
    private final String m_class;
    private final String m_property;
    private transient Method m_get, m_set;
    // Arguments to pass to getter methods.
    private static final Object[] s_nullArgs = new Object[0];

    SerializablePD(String clazz, String propertyName) {
        m_class = clazz;
        m_property = propertyName;
    }

    private void reload(Rete engine) throws JessException {
        try {
            Class c = engine.findClass(m_class);
            ClassResearcher cr = engine.getClassResearcher();
            Property[] pd = cr.getBeanProperties(c.getName());
            for (int i=0; i<pd.length; i++)
                if (pd[i].getName().equals(m_property)) {
                    m_get = pd[i].getReadMethod();
                    m_set = pd[i].getWriteMethod();
                    return;
                }

        } catch (Exception e) {
            throw new JessException("SerializablePD.reload",
                                    "Can't recreate property", e);
        }
    }

    public String getName() { return m_property; }


    public Class getPropertyType(Rete engine) throws JessException {
        Method m = getReadMethod(engine);
        return m.getReturnType();
    }

    public Object getPropertyValue(Rete engine, Object o) throws JessException, IllegalAccessException, InvocationTargetException {
        try {
        	Method m = getReadMethod(engine);
        	return m.invoke(o, s_nullArgs);
        } catch (IllegalAccessException iae) {
        	System.out.println(m_property);
        	throw iae;
        }
    }

    public void setPropertyValue(Rete engine, Object om, Object value) throws JessException, IllegalAccessException, InvocationTargetException {
        Method m = getWriteMethod(engine);
        if (m == null)
            throw new JessException("SerializablePD.setPropertyValue", "Property is read-only", m_property);
        m.invoke(om, new Object[]{ value });
    }


    private Method getReadMethod(Rete engine) throws JessException {
        if (m_get == null)
            reload(engine);
        return m_get;
    }

    private Method getWriteMethod(Rete engine) throws JessException {
        if (m_set == null)
            reload(engine);
        return m_set;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SerializablePD))
            return false;

        SerializablePD pd = (SerializablePD) o;

        return m_class.equals(pd.m_class) &&
            m_property.equals(pd.m_property);
    }
}

