package jess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Defquery is a way of requesting specific information in working
 * memory from procedural code. These are usually constructed by the
 * parser when it sees a "defquery" construct.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public class Defquery extends HasLHS implements Serializable {
    private final ArrayList<Variable> m_queryVariables = new ArrayList<Variable>();

    /**
     * String prepended to query names to form backwards chaining goals
     */

    public static final String QUERY_TRIGGER = "__query-trigger-";
    private String m_key;

    public Defquery(String name, String docstring, Rete engine) throws JessException {
        super(name, docstring, engine);
        // ###
        m_key = engine.getNextNodeKey() + "Q";
    }

    private int m_maxBackgroundRules = 0;

    /**
     * Return the maximum number of rules that will fire during this query.
     * Queries call Rete.run() when they are executed to allow backward
     * chaining to occur. No more than this number of rules will be
     * allowed to fire.
     * @return As described
     */
    public int getMaxBackgroundRules() {
        return m_maxBackgroundRules;
    }

    /**
     * Set the maximum number of rules that will fire during this query.
     * @see #getMaxBackgroundRules
     * @param maxBackgroundRules  The new value for this property
     */
    public void setMaxBackgroundRules(int maxBackgroundRules) {
        m_maxBackgroundRules = maxBackgroundRules;
        if (getNext() != null)
            ((Defquery) getNext()).setMaxBackgroundRules(maxBackgroundRules);
    }

    /**
     * Recieve satisfied queries
     */
    public synchronized void callNodeLeft(int tag, Token token, Context context)
            throws JessException {
        broadcastEvent(tag, JessEvent.RETE_TOKEN_LEFT, context, context);
        Rete engine = context.getEngine();
        switch (tag) {
            case RU.ADD:
            case RU.MODIFY_ADD:
                addResult(engine, token);
                break;

            case RU.UPDATE:
                if (m_new)
                    addResult(engine, token);
                break;


            case RU.REMOVE:
            case RU.MODIFY_REMOVE:
                removeResult(engine, token);
                break;

            case RU.CLEAR:
                clearResults(engine);
                break;
        }
    }

    private void removeResult(Rete engine, Token token) {
        getTheResults(engine).remove(new QueryResultRow(this, token));
    }

    private void addResult(Rete engine, Token token) {
        getTheResults(engine).add(new QueryResultRow(this, token));
    }

    /*
     * Get any query results
     */

    synchronized Iterator<QueryResultRow> getResults(Rete engine) {
        ArrayList<QueryResultRow> v = new ArrayList<QueryResultRow>();
        Defquery dq = this;
        while (dq != null) {
            List<QueryResultRow> results = dq.getTheResults(engine);
            synchronized (results) {
                int size = results.size();
                for (int i = 0; i < size; i++)
                    v.add(results.get(i));
            }
            dq = (Defquery) dq.getNext();
        }
        return v.iterator();
    }

    private List<QueryResultRow> getTheResults(Rete engine) {
        List<QueryResultRow> list = (List<QueryResultRow>) engine.getKeyedStorage(m_key);
        if (list == null) {
            list = new ArrayList<QueryResultRow>();
            engine.putKeyedStorage(m_key, list);
        }
        return list;        
    }

    synchronized void clearResults(Rete engine) {
        getTheResults(engine).clear();
        Defquery next = (Defquery) getNext();
        if (next != null)
            next.clearResults(engine);
    }

    synchronized int countResults(Rete engine) {
        int n = getTheResults(engine).size();
        Defquery next = (Defquery) getNext();
        if (next != null)
            n += next.countResults(engine);
        return n;
    }

    public String getQueryTriggerName() {
        String name = getDisplayName();
        int colons = name.indexOf("::");
        return RU.scopeName(getModule(), QUERY_TRIGGER + name.substring(colons + 2));
    }

    /**
     * Tell this rule to set the LHS up for faster execution
     * @exception JessException
     */
    void freeze(Rete engine) throws JessException {
        if (m_frozen)
            return;

        super.freeze(engine);
        // Build and install query pattern here
        Pattern p = new Pattern(getQueryTriggerName(), engine);
        int i = 0;
        for (Iterator<Variable> e = m_queryVariables.iterator(); e.hasNext(); i++)
            p.addTest(new Test1(TestBase.EQ, RU.DEFAULT_SLOT_NAME, i, e.next()));

        insertCEAtStart(p, engine);
    }

    public void addQueryVariable(Variable v) {
        m_queryVariables.add(v);
        String name = v.variableValue(null);
        getBindings().put(name, new BindingValue(name));
        if (getNext() != null)
            ((Defquery) getNext()).addQueryVariable(v);
    }

    public int getNVariables() {
        return m_queryVariables.size();
    }

    public Variable getQueryVariable(int i) {
        return m_queryVariables.get(i);
    }

    public void addCE(ConditionalElementX ce, Rete engine) throws JessException {
        if (ce.getLogical())
            throw new JessException("Defquery.addCE",
                "Can't use logical CE in defquery", "");
        super.addCE(ce, engine);
    }

    public String toString() {
        return "Defquery " + getName();
    }

    public Object accept(Visitor jv) {
        return jv.visitDefquery(this);
    }

    void createChain(ConditionalElementX ce, Rete engine) throws JessException {
        doSetLHS(ce.getConditionalElementX(0), engine);
        Defquery query = this;
        String name = getName();
        for (int i = 1; i < ce.getGroupSize(); ++i) {
            ConditionalElementX newPatterns = ce.getConditionalElementX(i);
            Defquery next = new Defquery(name + "&" + i, getDocstring(), engine);
            copyQueryVariables(query, next);
            next.doSetLHS(newPatterns, engine);
            query.setNext(next);
            query = next;
        }
    }

    private void copyQueryVariables(Defquery q1, Defquery q2) {
        int count = q1.getNVariables();
        for (int i=0; i<count; ++i) {
            q2.addQueryVariable(q1.getQueryVariable(i));
        }
    }

    public final String getConstructType() {
        return "defquery";
    }
}



