package jess;

import jess.xml.XMLVisitor;

import java.io.*;
import java.util.*;

/**
 * A FactList is the collection that holds and processes all the Fact
 * object for a Rete engine. You could say that this is the "working
 * memory", although that would really include all the Rete network
 * join node memories, as well.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


class FactList implements Serializable {

    private final Map<Fact, Fact> m_table = Collections.synchronizedMap(new HashMap<Fact, Fact>());
    private final List<Fact> m_factsToAssert = Collections.synchronizedList(new ArrayList<Fact>());
    private final List<Fact> m_factsToRetract = Collections.synchronizedList(new ArrayList<Fact>());

    private int m_time = 0;
    private final LogicalSupport m_logicalSupport = new LogicalSupport(m_factsToRetract);

    private void processToken(int tag, Fact fact, Rete engine, Context context) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            assignTime(fact);
            Token t = Rete.getFactory().newToken(fact);
            engine.getCompiler().getRoot().callNodeRight(tag, t, context);
        }
    }

    void updateNodes(Rete engine, Set n) throws JessException {
        try {
            JessException exception = null;
            for (Iterator e = listFacts(); e.hasNext();) {                  
                Fact fact = (Fact) e.next();
                Token t = Rete.getFactory().newToken(fact);
                for (Iterator nodes = n.iterator(); nodes.hasNext();)
                    synchronized (engine.getWorkingMemoryLock()) {
                        Node node = (Node) nodes.next();
                        try {
                            node.callNodeRight(RU.UPDATE, t, engine.getGlobalContext().push());
                        } catch (JessException je) {
                            exception = je;
                        }
                    }
            }
            processPendingFacts(engine);
            if (exception != null)
                throw exception;
        } finally {
            engine.commitActivations();
        }
    }


    void clear(Rete engine) throws JessException {
        processToken(RU.CLEAR, Fact.getClearFact(), engine, engine.getGlobalContext());
        m_table.clear();
        m_time = 0;
        m_factsToAssert.clear();
        m_factsToRetract.clear();
        m_logicalSupport.clear();
        m_nextFactId = 0;
    }

    int getTime() {
        return m_time;
    }

    void assignTime(Fact fact) {
        fact.updateTime(++m_time);
    }

    int doPreAssertionProcessing(Fact f) {
        return m_table.containsKey(f) ? -1 : 0;
    }

    Fact findFactByFact(Fact f) {
        return m_table.get(f);
    }

    Fact findFactByID(int id) {
        synchronized (m_table) {
            for (Iterator<Fact> it = m_table.keySet().iterator(); it.hasNext();) {
                Fact f = it.next();
                if (f.getFactId() == id)
                    return f;
            }
            return null;
        }
    }

    void ppFacts(String name, Writer output, boolean inXML) throws IOException {
        String line = System.getProperty("line.separator");
        if (inXML) {
            output.write("<?xml version='1.0' encoding='US-ASCII'?>");
            output.write(line);
            output.write("<fact-list>");
            output.write(line);
        }

        for (Iterator<Fact> e = listFacts(); e.hasNext();) {
            Fact f = (Fact) e.next();
            if (name == null || f.getName().equals(name)) {
                if (inXML) {
                    output.write(new XMLVisitor(f).toString());
                } else {
                    output.write(f.toString());
                }
                output.write(line);
            }
        }

        if (inXML) {
            output.write("</fact-list>");
            output.write(line);            
        }
    }

    void ppFacts(Writer output, boolean inXML) throws IOException {
        ppFacts(null, output, inXML);
    }

    Iterator<Fact> listFacts() {
        synchronized (m_table) {
            return new SortedIterator(m_table);
        }
    }

    /**
     * Successively incremented ID for asserted facts.
     */
    private final Object m_idLock = new String("IDLOCK");
    private int m_nextFactId;

    private int consumeFactId() {
        synchronized (m_idLock) {
            return m_nextFactId++;
        }
    }

    int peekFactId() {
        synchronized(m_idLock) {
            return m_nextFactId;
        }
    }

    // Process any facts that were asserted by rule LHS processing --
    // i.e., for backwards chaining -- or should be retracted, for
    // instance, because logical support was removed.
    void processPendingFacts(Rete engine) throws JessException {
        Context context = engine.getGlobalContext();
        synchronized (m_factsToAssert) {
            while (m_factsToAssert.size() > 0) {
                Fact f = m_factsToAssert.get(0);
                m_factsToAssert.remove(0);
                _assert(f, engine, context);
            }
        }

        synchronized (m_factsToRetract) {
            while (m_factsToRetract.size() > 0) {
                Fact f = m_factsToRetract.get(0);
                m_factsToRetract.remove(0);
                if (f.isShadow()) {
                    Object ov = f.getSlotValue("OBJECT").javaObjectValue(null);
                    engine.undefinstanceNoRetract(ov);
                }
                _retract(f, engine, context);
            }
        }
    }

    Fact assertFact(Fact f, Rete engine, Context context)
            throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            Fact fact = _assert(f, engine, context);
            if (fact != null)
                processPendingFacts(engine);
            return fact;
        }
    }

    private Fact _assert(Fact f, Rete engine, Context context)
            throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            try {
                if (!f.isExpanded())
                    f.expandInPlace(context);

                // find any old copy
                boolean alreadyExisted = (engine.doPreAssertionProcessing(f) != 0);

                m_logicalSupport.factAsserted(context, f, alreadyExisted);

                if (alreadyExisted)
                    return null;

                f.setFactId(consumeFactId());

                engine.broadcastEvent(JessEvent.FACT, f, context);

                m_table.put(f, f);

                // Send it to the Rete network
                processToken(RU.ADD, f, engine, engine.getGlobalContext().push());

                return f;
            } finally {
                engine.commitActivations();
            }
        }
    }

    void removeFacts(String name, Rete engine) throws JessException {
        synchronized(engine.getWorkingMemoryLock()) {
            ArrayList<Fact> facts = new ArrayList<Fact>();
            name = engine.resolveName(name);
            for (Iterator<Fact> it = m_table.keySet().iterator(); it.hasNext();) {
                Fact fact = it.next();
                if (fact.getDeftemplate().getName().equals(name))
                    facts.add(fact);
            }
            for (Iterator<Fact> it = facts.iterator(); it.hasNext();) {
                Fact fact = it.next();
                engine.retract(fact);
            }
        }
    }

    /**
     * Retract a fact.
     *
     * @param f A Fact object. Doesn't need to be the actual object
     *          that appears on the fact-list; can just be a Fact that could
     *          compare equal to one.
     * @throws JessException If anything goes wrong.
     */

    Fact retract(Fact f, Rete engine) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            Fact ff;
            if ((ff = m_table.get(f)) != null) {
                _retract(ff, engine, engine.getGlobalContext());
                processPendingFacts(engine);
            }
            return ff;
        }
    }

    // f should be the actual fact
    private void _retract(Fact f, Rete engine, Context context) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            try {
                f = m_table.remove(f.getIcon());
                engine.broadcastEvent(JessEvent.FACT | JessEvent.REMOVED, f, context);
                if (f != null) {
                    m_logicalSupport.removeAllLogicalSupportFor(f);
                    processToken(RU.REMOVE, f, engine, engine.getGlobalContext().push());
                }
            } finally {
                engine.commitActivations();
            }
        }
    }


    Fact modify(Fact fact, String[] slotNames, Value[] slotValues, Context context, Rete engine)
            throws JessException {

        if (slotNames.length != slotValues.length)
            throw new JessException("modify",
                                    "Wrong number of values, expected " + slotNames.length + ", got",
                                    slotValues.length);
        synchronized (engine.getWorkingMemoryLock()) {
            try {
                switch (fact.getShadowMode()) {

                    case Fact.NO:
                        fact = modifyRegularFact(fact, slotNames, slotValues, engine, context);
                        break;
                    case Fact.DYNAMIC:
                        fact = modifyDefinstancedObject(fact, slotNames, slotValues, engine, context);
                        break;
                    case Fact.STATIC:
                        slotValues = (Value[]) slotValues.clone();
                        for (int i = 0; i < slotValues.length; i++) {
                            slotValues[i] = slotValues[i].resolveValue(context);
                        }
                        modifyDefinstancedObject(fact, slotNames, slotValues, engine, context);
                        fact = modifyRegularFact(fact, slotNames, slotValues, engine, context);
                        break;
                    default:
                        throw new JessException("modify",
                                                "Impossible shadow mode",
                                                fact.getShadowMode());
                }
            } finally {
                engine.commitActivations();
            }
        }

        return fact;
    }

    private Fact prepareToModifyRegularFact(Fact fact, String[] slotNames, Rete engine, Context context)
            throws JessException {

        synchronized (engine.getWorkingMemoryLock()) {
            Context newContext = context.push();
            if (fact.getDeftemplate().isSlotSpecific()) {
                newContext.setSlotSpecificModifiedFact(fact);
                newContext.setModifiedSlots(slotNames);
            }
            processToken(RU.MODIFY_REMOVE, fact, engine, newContext);
            fact = m_table.remove(fact);
            m_logicalSupport.removeAllLogicalSupportFor(fact);
            return fact;
        }
    }

    private Fact finishModifyRegularFact(Fact fact, Rete engine, String[] slotNames, Context context)
            throws JessException {

        synchronized (engine.getWorkingMemoryLock()) {
            engine.broadcastEvent(JessEvent.FACT | JessEvent.MODIFIED, fact, context);
            m_logicalSupport.factAsserted(context, fact, false);
            if (engine.doPreAssertionProcessing(fact) == 0) {
                m_table.put(fact, fact);
                Context newContext = context.push();
                if (fact.getDeftemplate().isSlotSpecific()) {
                    newContext.setSlotSpecificModifiedFact(fact);
                    newContext.setModifiedSlots(slotNames);
                }
                processToken(RU.MODIFY_ADD, fact, engine, newContext);
                processPendingFacts(engine);
                return fact;
            } else
                return Fact.getNullFact();
        }
    }

    Fact modifyRegularFact(Fact input, String[] slotNames, Value[] slotValues, Rete engine, Context context)
            throws JessException {

        synchronized (engine.getWorkingMemoryLock()) {
            Fact fact = m_table.get(input.getIcon());
            if (fact == null) {
                throw new JessException("modify", "Fact object not in working memory", input.toStringWithParens());
            }

            List<String> nameList = new ArrayList<String>();
            List<Value> valueList = new ArrayList<Value>();
            for (int i=0; i < slotValues.length; i++) {
                Value v = slotValues[i].resolveValue(context);
                if (!fact.getSlotValue(slotNames[i]).equals(v)) {
                    nameList.add(slotNames[i]);
                    valueList.add(v);
                }
            }
            if (nameList.size() == 0)
                return fact;
            slotNames = nameList.toArray(new String[nameList.size()]);
            fact = prepareToModifyRegularFact(fact, slotNames, engine, context);
            try {
                for (int i = 0; i < nameList.size(); i++) {
                    fact.setSlotValue(nameList.get(i), valueList.get(i));
                }
            } finally {
                fact = finishModifyRegularFact(fact, engine, slotNames, context);
            }
            return fact;
        }
    }

    private Fact modifyDefinstancedObject(Fact fact, String[] slotNames, Value[] slotValues, Rete engine,
                                          Context context)
            throws JessException {

        try {
            Deftemplate deft = fact.getDeftemplate();
            Object ov = fact.getSlotValue("OBJECT").javaObjectValue(context);
            for (int i = 0; i < slotNames.length; i++) {
                int index = deft.getSlotIndex(slotNames[i]);
                Value defaultValue = deft.getSlotDefault(index);
                SerializableD pd = (SerializableD) defaultValue.javaObjectValue(context);
                Class type = pd.getPropertyType(engine);
                pd.setPropertyValue(engine, ov, RU.valueToObject(type, slotValues[i], context));
            }

        } catch (Exception e) {
            throw new JessException("modify", "Error setting slot value", e);
        }
        return fact;
    }

    public List getSupportedFacts(Fact supporter) {
        return m_logicalSupport.getSupportedFacts(supporter);
    }

    public List getSupportingTokens(Fact fact) {
        return m_logicalSupport.getSupportingTokens(fact);
    }

    public void removeLogicalSupportFrom(Token token, Fact fact) {
        m_logicalSupport.removeLogicalSupportFrom(token, fact);
    }

    public void setPendingFact(Fact fact, boolean assrt) {
        if (assrt)
            m_factsToAssert.add(fact);

        else {
            fact = m_table.get(fact);
            if (fact != null)
                m_factsToRetract.add(fact);
        }
    }
}

class FactComparator implements Comparator<Fact>, Serializable {
    public int compare(Fact o1, Fact o2) {
        return o1.getFactId() - o2.getFactId();
    }
}

class SortedIterator implements Iterator<Fact>, Serializable {
    private Fact[] m_arr;
    private int m_index;

    SortedIterator(Map<Fact, Fact> map) {
        m_arr = map.keySet().toArray(new Fact[map.size()]);
        Arrays.sort(m_arr, new FactComparator());
    }

    public boolean hasNext() {
        return m_index < m_arr.length;
    }

    public Fact next() {
        return m_arr[m_index++];
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}



