package jess;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * Discover the public static members of a class, the turn them into Userfunctions that
 * either reflect a static property or call a static method.
 * (C) 2013 Sandia Corporation<BR>
 */
class StaticMemberImporter {
    private Class m_clazz;

    public StaticMemberImporter(Class clazz) {
        m_clazz = clazz;
    }

    public Userfunction createFieldFunction(Field field) throws JessException {
        return new FieldFunction(field);
    }

    public Userfunction createMethodFunctions(List methods) {
        return new MethodFunction(methods);
    }

    public void addAllStaticFields(Rete engine) throws JessException {
        Field[] fields = m_clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (isPublicStatic(field)) {
                Userfunction function = createFieldFunction(field);
                engine.addUserfunction(function);
            }
        }
    }

    public void addAllStaticMethods(Rete engine) {
        Method[] methods = m_clazz.getMethods();
        Multimap map = new Multimap();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (isPublicStatic(method)) {
                map.put(method.getName(), method);
            }
        }

        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            Userfunction function = createMethodFunctions((List) map.get(it.next()));
            engine.addUserfunction(function);
        }
    }

    private boolean isPublicStatic(Member method) {
        return Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers());
    }
}

class FieldFunction implements Userfunction, Serializable {
    private String m_name;
    private SerializableVD m_field;

    public FieldFunction(Field field)  {
        m_name = ClassSource.classNameOnly(field.getDeclaringClass().getName()) + "." + field.getName();
        m_field = new SerializableVD(field.getDeclaringClass().getName(), field.getName());
    }

    public String getName() {
        return m_name;
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        try {
            final Rete engine = context.getEngine();
            return RU.objectToValue(m_field.getPropertyType(engine), m_field.getPropertyValue(engine, null));
        } catch (IllegalAccessException e) {
            throw new JessException(m_name, "Access exception", e);
        }
    }
}

class MethodFunction implements Userfunction, Serializable {
    private String m_name;
    private SerializableMD m_method;

    public MethodFunction(List methods) {
        Method method = (Method) methods.get(0);
        m_name = ClassSource.classNameOnly(method.getDeclaringClass().getName()) + "." + method.getName();
        m_method = new SerializableMD(methods);
    }

    public String getName() {
        return m_name;
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ArrayList argList = new ArrayList();
        Rete engine = context.getEngine();
        for (int i=1; i<vv.size(); ++i)
            argList.add(vv.get(i).resolveValue(context));

        return m_method.invoke(argList, engine);
    }
}
