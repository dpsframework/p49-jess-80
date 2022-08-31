package jess;

/** 
 * Test multislot value and type.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1MTEQ extends Node1 {
    final int m_idx, m_subidx;
    final Value m_value;
    private final String m_tag;

    Node1MTEQ(int idx, int subidx, Value val) throws JessException {
        m_idx = idx;
        m_subidx = subidx;
        m_value = cleanupBindings(val);
        m_tag = "_" + m_idx + "_" + m_subidx;
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        try {
            if (processClearCommand(tag, t, context))
                return;

            else if (tag == RU.REMOVE || tag == RU.MODIFY_REMOVE) {
                passAlong(tag, t, context);
                return;
            }

            boolean result = false;

            Fact fact = t.topFact();

            // TODO Should know in advance what will be needed!
            String name = fact.getFactId() + m_tag;
            Value subslot;
            if (context.isVariableDefined(name))
                subslot = context.getVariable(name);
            else {
                ValueVector vv = fact.get(m_idx).listValue(null);
                if (vv.size() >= m_subidx) {
                    subslot = vv.get(m_subidx);
                } else {
                    return;
                }
            }

            if (m_value.type() == RU.FUNCALL) {
                context.setFact(fact);
                context.setToken(t);

                // TODO The bindings in this function call may fetch the wrong values.
                if (!m_value.resolveValue(context).equals(Funcall.FALSE))
                    result = true;

                // inform extensions that functions were called
                // and result of calls
                t = t.prepare(result);
            } else if (subslot.equals(m_value.resolveValue(context)))
                result = true;


            if (result)
                passAlong(tag, t, context);

        } catch (JessException re) {
            re.addContext("rule LHS (MTEQ)", context);
            throw re;
        } catch (Exception e) {
            JessException re = new JessException("Node1MTEQ.call",
                    "Error during LHS execution",
                    e);
            re.addContext("rule LHS", context);
            throw re;

        }

    }

    public String toString() {
        if (m_value.type()  == RU.FUNCALL)
            return"[Test that " + m_value + " is not FALSE]";
        else
            return "[Test that the multislot entry at index " + m_idx + ", subindex " + m_subidx +
                    " equals " + m_value + "]";
    }


    public boolean equals(Object o) {
        if (o instanceof Node1MTEQ) {
            Node1MTEQ n = (Node1MTEQ) o;
            return (m_idx == n.m_idx && m_subidx == n.m_subidx && m_value.equals(n.m_value));
        } else
            return false;
    }

}

