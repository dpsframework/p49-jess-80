package jess;

import java.lang.reflect.*;

class SerializableVD implements SerializableD {
    private final String m_fieldName;
    private final String m_className;
    private transient Field m_field;

    SerializableVD(String clazz, String field)  {
        m_fieldName = field;
        m_className = clazz;
    }

    public String getName() {
        return m_fieldName;
    }

    public Class getPropertyType(Rete engine) throws JessException {
        if (m_field == null)
            reload(engine);
        return m_field.getType();
    }

    private void reload(Rete engine) throws JessException{
        try {
            Class c = engine.findClass(m_className);
            m_field = c.getField(m_fieldName);

        } catch (Exception e) {
            throw new JessException("SerializableVD.reload",
                                    "Can't recreate property", e);
        }
    }

    public Object getPropertyValue(Rete engine, Object o) throws JessException, IllegalAccessException {
        if (m_field == null)
            reload(engine);
        return m_field.get(o);
    }

    public void setPropertyValue(Rete engine, Object om, Object value) throws JessException, IllegalAccessException, InvocationTargetException {
        if (m_field == null)
            reload(engine);
        m_field.set(om, value);
    }
}
