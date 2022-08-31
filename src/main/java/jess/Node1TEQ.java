package jess;

/** 
 * Test slot value.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */
class Node1TEQ extends Node1 {
    final Value m_value;
    final int m_idx;

    Node1TEQ(int idx, Value val) throws JessException {
        m_value = cleanupBindings(val);
        m_idx = idx;
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        if (processClearCommand(tag, t, context))
            return;

        else if (tag == RU.REMOVE || tag == RU.MODIFY_REMOVE) {
            passAlong(tag, t, context);
            return;
        }

        boolean result = false;
        Fact fact = t.topFact();
        try {
            if (m_value.type() == RU.FUNCALL) {
                context.setFact(fact);
                context.setToken(t);

                if (!m_value.resolveValue(context).equals(Funcall.FALSE))
                    result = true;

                // inform extensions that functions were called and result of calls
                t = t.prepare(result);
            } else if (fact.get(m_idx).equals(m_value.resolveValue(context))) {
                result = true;
            }

            if (result)
                passAlong(tag, t, context);

            //debugPrint(token, callType, fact, result);
        } catch (JessException re) {
            re.addContext("rule LHS (TEQ)", context);
            throw re;
        } catch (Exception e) {
            JessException re = new JessException("Node1TEQ.call",
                    "Error during LHS execution",
                    e);
            re.addContext("rule LHS (TEQ)", context);
            throw re;

        }
    }

    public String toString() {
        if (m_value.type() == RU.FUNCALL)
            return"[Test that " + m_value + " is not FALSE]";
        else
            return "[Test that slot at index " + m_idx + " equals " + m_value + "]";
    }

    public boolean equals(Object o) {
        if (o instanceof Node1TEQ) {
            Node1TEQ n = (Node1TEQ) o;
            return (m_idx == n.m_idx && m_value.equals(n.m_value));
        } else
            return false;
    }

}

