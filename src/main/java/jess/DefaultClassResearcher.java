package jess;

import java.beans.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * (C) 2013 Sandia Corporation
 */
public class DefaultClassResearcher implements ClassResearcher {
    private final Rete m_engine;

    public DefaultClassResearcher(Rete engine) {
        m_engine = engine;
    }

    public String resolveClassName(String clazz) throws ClassNotFoundException {
        Class theClass = m_engine.findClass(clazz);
        return theClass.getName();
    }

    public Property[] getBeanProperties(String clazz) throws ClassNotFoundException, JessException {
        Class c = m_engine.findClass(clazz);
        try {
            PropertyDescriptor[] props = getPropertyDescriptors(c);
            ArrayList properties = new ArrayList();
            for (int i = 0; i < props.length; i++) {
                PropertyDescriptor pd = props[i];
                Method m = props[i].getReadMethod();
                if (m == null)
                    continue;
                String name = pd.getName();
                Class type = pd.getPropertyType();
                boolean isArray = type.isArray();
                if (isArray)
                    type = type.getComponentType();
                String jessType = getSlotType(type);

                properties.add(new Property(name, jessType, isArray, pd.getReadMethod(), pd.getWriteMethod()));
            }
            return (Property[]) properties.toArray(new Property[properties.size()]);

        } catch (IntrospectionException e) {
            throw new JessException("ReflectionClassResearcher.getBeanProperties", "Introspector threw an exception", e);
        }

    }

    public Property[] getPublicInstanceFields(String clazz) throws ClassNotFoundException, JessException {
        Class c = m_engine.findClass(clazz);
        Field[] fields = c.getFields();
        ArrayList properties = new ArrayList();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (Modifier.isStatic(f.getModifiers()))
                continue;

            String name = f.getName();
            Class type = f.getType();
            boolean isArray = type.isArray();
            if (isArray)
                type = type.getComponentType();
            String jessType = getSlotType(type);

            properties.add(new Property(name, jessType, isArray, null, null));
        }
        return (Property[]) properties.toArray(new Property[properties.size()]);
    }

    static String getSlotType(Class type) {
        if (type == int.class || type == Integer.class ||
                type == short.class || type == Short.class ||
                type == byte.class || type == Byte.class)
            return "INTEGER";
        else if (type == long.class || type == Long.class)
            return "LONG";
        else if (type == boolean.class || type == Boolean.class)
            return "SYMBOL";
        else if (type == float.class || type == Float.class ||
                type == double.class || type == Double.class)
            return "FLOAT";
        else if (type == String.class)
            return "STRING";
        else
            return "ANY";
    }
    
    private static final Map s_descriptors = Collections.synchronizedMap(new HashMap());

    private static PropertyDescriptor[] getPropertyDescriptors(Class c)
            throws JessException, IntrospectionException {

        PropertyDescriptor[] pds;
        if ((pds = (PropertyDescriptor[]) s_descriptors.get(c)) != null)
            return pds;

        BeanInfo bi = Introspector.getBeanInfo(c);
        if (bi.getBeanDescriptor().getBeanClass() != c)
            throw new JessException("ReflectFunctions.getPropertyDescriptors",
                                    "Introspector returned bogus BeanInfo object for class ",
                                    bi.getBeanDescriptor().getBeanClass().getName());

        pds = bi.getPropertyDescriptors();
        s_descriptors.put(c, pds);
        return pds;
    }
}

