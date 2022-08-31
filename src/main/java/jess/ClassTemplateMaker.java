package jess;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The code that knows how to make a Deftemplate out of a class. Getting actual information about the class is
 * delegated to a ClassResearcher.
 *
 * (C) 2007 Sandia National Laboratories
 */

class ClassTemplateMaker {
    private final Rete m_engine;
    private String m_className;
    private final ClassResearcher m_classResearcher;

    ClassTemplateMaker(Rete engine, String clazz) throws ClassNotFoundException, JessException {
        m_classResearcher  = engine.getClassResearcher();
        m_engine = engine;
        m_className = m_classResearcher.resolveClassName(clazz);
    }


    public String getClassName() {
        return m_className;
    }

    public Deftemplate createDeftemplate(String jessName, String parent, boolean includeMemberVariables)
            throws JessException {

        try {
            Deftemplate dt;
            if (parent != null) {
                Deftemplate parentTemplate = m_engine.findDeftemplate(m_engine.resolveName(parent));
                if (parentTemplate == null)
                    throw new JessException("defclass",
                            "No such parent template: ",
                            parent);
                dt = new Deftemplate(jessName, DefinstanceList.JAVA_OBJECT + m_className, parentTemplate, m_engine);
            } else
                dt = new Deftemplate(jessName, DefinstanceList.JAVA_OBJECT + m_className, m_engine);


            addBeanProperties(dt);
            if (includeMemberVariables)
                addPublicVariableSlots(dt);
            addActiveInstanceSlot(dt);
            return dt;
        } catch (ClassNotFoundException e) {
            throw new JessException("defclass", "Class not found:", e);
        }
    }


    private void addPublicVariableSlots(Deftemplate dt) throws JessException, ClassNotFoundException {
        ClassResearcher.Property[] fields = m_classResearcher.getPublicInstanceFields(m_className);
        Arrays.sort(fields, new Comparator<ClassResearcher.Property>() {
            public int compare(ClassResearcher.Property f1, ClassResearcher.Property f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            String type = fields[i].getType();
            Value defaultValue = new Value(new SerializableVD(m_className, name));
            if (fields[i].isArray())
                dt.addMultiSlot(name, defaultValue, type);
            else
                dt.addSlot(name, defaultValue, type);
        }
    }

    private void addActiveInstanceSlot(Deftemplate dt) throws JessException {
        dt.addSlot("OBJECT", Funcall.NIL, "OBJECT");
    }




    private void addBeanProperties(Deftemplate dt) throws JessException,  ClassNotFoundException {
        ClassResearcher.Property[] props = m_classResearcher.getBeanProperties(m_className);

        // Sort them first
        Arrays.sort(props, new Comparator<ClassResearcher.Property>() {
            public int compare(ClassResearcher.Property p1, ClassResearcher.Property p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });

        for (int i = 0; i < props.length; i++) {
            String name = props[i].getName();
            Value defaultValue = new Value(new SerializablePD(m_className, name));
            if (props[i].isArray())
                dt.addMultiSlot(name, defaultValue, props[i].getType());
            else
                dt.addSlot(name, defaultValue, props[i].getType());
        }
    }

}
