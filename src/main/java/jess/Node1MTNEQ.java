package jess;

/** 
 * Test multislot value and type for inequality.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1MTNEQ extends Node1 {

    private final int m_idx;
    private final int m_subidx;
    private final Value m_value;

    Node1MTNEQ(int idx, int subidx, Value val) throws JessException {
        m_idx = idx;
        m_subidx = subidx;
        m_value = cleanupBindings(val);
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        if (processClearCommand(tag, t, context))
            return;

        else if (tag == RU.REMOVE || tag == RU.MODIFY_REMOVE) {
            passAlong(tag, t, context);
            return;
        }

        try {
            boolean result = false;
            Fact fact = t.topFact();
            Value s;
            if ((s = fact.get(m_idx)).type() == RU.LIST) {
                ValueVector vv = s.listValue(null);
                if (vv.size() >= m_subidx) {

                    Value subslot = vv.get(m_subidx);
                    if (m_value.type() == RU.FUNCALL) {
                        context.setFact(fact);
                        context.setToken(t);

                        if (m_value.resolveValue(context).equals(Funcall.FALSE))
                            result = true;

                        // inform extensions that functions were called and result of calls
                        t = t.prepare(result);
                    } else if (!subslot.equals(m_value.resolveValue(context)))
                        result = true;
                }
            }

            if (result)
                passAlong(tag, t, context);

            //debugPrint(token, callType, fact, result);
        } catch (JessException re) {
            re.addContext("rule LHS (MTNEQ)", context);
            throw re;
        } catch (Exception e) {
            JessException re = new JessException("Node1MTNEQ.call",
                    "Error during LHS execution",
                    e);
            re.addContext("rule LHS (MTNEQ)", context);
            throw re;

        }
    }

    public String toString() {
        if (m_value.type()  == RU.FUNCALL)
            return"[Test that " + m_value + " is FALSE]";
        else
            return "[Test that subslot " + m_subidx + " of multislot " + m_idx +
                    " does not equal " + m_value + "]";
    }

    public boolean equals(Object o) {
        if (o instanceof Node1MTNEQ) {
            Node1MTNEQ n = (Node1MTNEQ) o;
            return (m_idx == n.m_idx && m_subidx == n.m_subidx && m_value.equals(n.m_value));
        } else
            return false;
    }
}

