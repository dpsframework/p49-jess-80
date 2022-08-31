package jess;

import java.beans.*;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * Manages the list of Java objects that are subject to pattern
 * matching in a Rete engine.
 * (C) 2013 Sandia Corporation
 */

class DefinstanceList implements Serializable, PropertyChangeListener {

    // Keys are objects to match, values are the facts that represent them.
    private Map<Object, Fact> m_definstances = new IdentityHashMap<Object, Fact>(101);
    private transient Rete m_engine;
    public static final String JAVA_OBJECT = "$JAVA-OBJECT$ ";


    DefinstanceList(Rete engine) {
        // ###
        setEngine(engine);
    }

    void setEngine(Rete engine) {
        m_engine = engine;
    }

    void clear(Rete engine) {
        synchronized (engine.getWorkingMemoryLock()) {
            for (Iterator<Object> it = m_definstances.keySet().iterator(); it.hasNext();)
                removePropertyChangeListener(it.next());

            m_definstances.clear();
        }
    }

    void reset(Rete engine) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            Context globalContext = engine.getGlobalContext();
            for (Iterator<Object> e = m_definstances.keySet().iterator(); e.hasNext();) {
                reassertShadowFact(engine, e.next(), globalContext);
            }
        }
    }

    Value definstance(Rete engine, String jessTypename, Object object, boolean dynamic, Context context)
            throws JessException {
        try {
            synchronized (engine.getWorkingMemoryLock()) {
                String javaTypename = engine.jessNameToJavaName(jessTypename);

                if (javaTypename == null)
                    throw new JessException("definstance",
                            "Unknown object class",
                            jessTypename);

                Fact existing = m_definstances.get(object);
                if (existing != null)
                    return new FactIDValue(existing);

                if (!engine.findClass(javaTypename).isAssignableFrom(object.getClass()))
                    throw new JessException("definstance", "Object is not instance of", javaTypename);

                Method apcl = null;
                if (dynamic) {
                    // Add ourselves to the object as a PropertyChangeListener

                    Class pcl = engine.findClass("java.beans.PropertyChangeListener");
                    apcl = object.getClass().getMethod("addPropertyChangeListener", new Class[]{pcl});
                }

                int shadowMode = dynamic ? Fact.DYNAMIC : Fact.STATIC;
                Fact fact = createNewShadowFact(object, jessTypename, context, shadowMode);
                fact = engine.assertFact(fact, context);

                if (dynamic && fact != null) {
                    apcl.invoke(object, new Object[]{this});
                }

                return new FactIDValue(fact);
            }
        } catch (InvocationTargetException ite) {
            throw new JessException("DefinstanceList.definstance",
                    "Cannot add PropertyChangeListener",
                    ite.getTargetException());

        } catch (NoSuchMethodException nsm) {
            throw new JessException("DefinstanceList.definstance",
                    "Obj doesn't accept " +
                            "PropertyChangeListeners",
                    nsm);
        } catch (ClassNotFoundException cnfe) {
            throw new JessException("DefinstanceList.definstance",
                    "Class not found", cnfe);
        } catch (IllegalAccessException iae) {
            throw new JessException("DefinstanceList.definstance",
                    "Class or method is not accessible",
                    iae);
        }
    }

    void undefinstanceNoRetract(Rete engine, Object o)  {
        synchronized (engine.getWorkingMemoryLock()) {
            removePropertyChangeListener(o);
            m_definstances.remove(o);
        }
    }

    Fact undefinstance(Rete engine, Object o) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            Fact f = m_definstances.get(o);
            undefinstanceNoRetract(engine, o);
            if (f != null)
                f = engine.retractNoUndefinstance(f);
            return f;
        }
    }

    private void removePropertyChangeListener(Object o) {
        try {
            Method apcl =
                    o.getClass().getMethod("removePropertyChangeListener",
                            new Class[]{
                                    PropertyChangeListener.class
                            });
            apcl.invoke(o, new Object[]{this});

        } catch (Exception e) { /* whatever */
        }
    }

    Set<Object> listDefinstances(Rete engine) {
        synchronized (engine.getWorkingMemoryLock()) {
            // We build a new collection in case the client does something
            // destructive with this iterator (like call undefinstance)
            return new IdentityHashMap<Object, Fact>(m_definstances).keySet();
        }
    }

    Set<Object> listDefinstances(Rete engine, Filter filter) {
        synchronized (engine.getWorkingMemoryLock()) {
            Set<Object> set = new HashSet<Object>();
            for (Iterator<Object> it = m_definstances.keySet().iterator(); it.hasNext();) {
                Object obj = it.next();
                if (filter.accept(obj))
                    set.add(obj);
            }
            return set;
        }
    }

    boolean containsObject(Object o) {
        return m_definstances.containsKey(o);
    }

    Fact getShadowFactForObject(Rete engine, Object o) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            Fact fact = m_definstances.get(o);
            if (fact == null) {
                throw new JessException("DefinstanceList.getShadowFactForObject",
                        "Object not a definstance: ", o.toString());
            }
            return fact;
        }
    }

    // For each bean property that appears to have changed in the object,
    // modify the shadow fact.

    private Fact updateMultipleSlots(Object o, Context context)
            throws JessException {

        Rete engine = context.getEngine();

        synchronized (engine.getWorkingMemoryLock()) {
            Fact fact = getShadowFactForObject(engine, o);

            m_definstances.put(o, fact);

            Deftemplate deft = fact.getDeftemplate();

            ArrayList<String> names = new ArrayList<String>();
            ArrayList<Value> values = new ArrayList<Value>();

            try {
                for (int i = 0; i < deft.getNSlots(); i++) {
                    String name = deft.getSlotName(i);

                    if (name.equals("OBJECT"))
                        continue;

                    Value newV = getSlotValueFromObject(deft, i, engine, o).resolveValue(context);
                    Value oldV = fact.getSlotValue(name);

                    if (oldV != null && oldV.equals(newV))
                        continue;

                    names.add(name);
                    values.add(newV);
                }
                engine.modifyRegularFact(fact, names.toArray(new String[names.size()]),
                        values.toArray(new Value[values.size()]), engine, context);

            } finally {
                engine.commitActivations();
            }
            return fact;
        }
    }

    // Modify the single slot that's changed.
    private Fact updateSingleSlot(Object o,
                                  String slotName,
                                  Object newValue,
                                  Context context)
            throws JessException {

        Rete engine = context.getEngine();
        try {
            synchronized (engine.getWorkingMemoryLock()) {
                Fact fact = getShadowFactForObject(engine, o);
                Deftemplate deft = fact.getDeftemplate();
                int index = deft.getSlotIndex(slotName);
                if (index == -1)
                    throw new JessException("DeftemplateList.updateSingleSlot",
                            "No such slot " + slotName + " in template",
                            deft.getName());

                Value newV;
                if (newValue == null)
                    newV = getSlotValueFromObject(deft, index, engine, o);
                else
                    newV = objectPropertyToSlotValue(deft, index, engine, newValue);

                try {
                    if (!fact.getSlotValue(slotName).equals(newV))
                        engine.modifyRegularFact(fact,
                                new String[]{slotName},
                                new Value[]{newV},
                                engine, context);
                } finally {
                    engine.commitActivations();
                }

                return fact;
            }

        } catch (IllegalArgumentException iae) {
            throw new JessException("DefinstanceList.updateShadowFact",
                    "Invalid argument", iae);
        }
    }

    private Value objectPropertyToSlotValue(Deftemplate deft, int index, Rete engine, Object newValue)
            throws JessException {
        SerializableD pd = (SerializableD)
                deft.getSlotDefault(index).javaObjectValue(engine.getGlobalContext());
        Class rt = pd.getPropertyType(engine);
        return RU.objectToValue(rt, newValue);
    }

    private void reassertShadowFact(Rete engine, Object o, Context context)
            throws JessException {
        Fact fact = getShadowFactForObject(engine, o);
        setAllSlotValuesFromObject(context, fact, o);
        m_engine.assertFact(fact, context);
    }

    private Fact createNewShadowFact(Object o,
                                     String clazz,
                                     Context context,
                                     int shadowMode)
            throws JessException {

        synchronized (m_engine.getWorkingMemoryLock()) {
            Fact fact = new Fact(clazz, m_engine);
            fact.setShadowMode(shadowMode);
            setAllSlotValuesFromObject(context, fact, o);
            fact.setExpanded();
            return fact;
        }
    }

    private void setAllSlotValuesFromObject(Context context, Fact fact, Object o)
            throws JessException {
        synchronized (m_engine.getWorkingMemoryLock()) {
            Rete engine = context.getEngine();

            fact.setSlotValue("OBJECT", new Value(o));
            m_definstances.put(o, fact);

            Deftemplate deft = fact.getDeftemplate();

            for (int i = 0; i < deft.getNSlots(); i++) {
                String name = deft.getSlotName(i);

                if (name.equals("OBJECT"))
                    continue;

                Value newV = getSlotValueFromObject(deft, i, engine, o);

                fact.setSlotValue(name, newV);
            }
        }
    }

    private Value getSlotValueFromObject(Deftemplate deft, int slotIndex, Rete engine, Object o)
            throws JessException {

        try {
            SerializableD pd = (SerializableD)
                    deft.getSlotDefault(slotIndex).javaObjectValue(engine.getGlobalContext());

            Object newValue = pd.getPropertyValue(engine, o);
            return RU.objectToValue(pd.getPropertyType(engine), newValue);

        } catch (InvocationTargetException ite) {
            throw new JessException("DefinstanceList.updateMultipleSlots",
                    "Called method threw an exception",
                    ite.getTargetException());

        } catch (IllegalAccessException iae) {
            throw new JessException("DefinstanceList.updateMultipleSlots",
                    "Method is not accessible",
                    iae);
        } catch (IllegalArgumentException iae) {
            throw new JessException("DefinstanceList.updateMultipleSlots",
                    "Invalid argument", iae);
        }

    }

    Value updateObject(Object object, Context context) throws JessException {
        Fact fact = updateMultipleSlots(object, context);
        return new FactIDValue(fact);
    }

    Value updateObject(Object object, String slotName, Context context) throws JessException {
        Fact fact = updateSingleSlot(object, slotName, null, context);
        return new FactIDValue(fact);
    }

    public void propertyChange(PropertyChangeEvent pce) {
        Object o = pce.getSource();

        try {
            synchronized (m_engine.getWorkingMemoryLock()) {
                if (m_definstances.get(o) == null)
                    return;

                String changedName = pce.getPropertyName();
                Object newValue = pce.getNewValue();
                Context context = m_engine.getGlobalContext();

                if (changedName == null)
                    updateMultipleSlots(o, context);
                else
                    updateSingleSlot(o, changedName, newValue, context);
            }

        } catch (JessException re) {
            System.out.println("Error while processing asynchronous property change notification\n" + re);
            re.printStackTrace();
            if (re.getCause() != null)
                re.getCause().printStackTrace();
        }
    }


}

