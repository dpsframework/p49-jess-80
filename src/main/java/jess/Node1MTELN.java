package jess;

/** 
 * Test multislot length. Used if there are no multifields matching a slot,
 * but instead a finite number of tests which restricts the length of the multislot
 * to a specific value.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1MTELN extends Node1 {
    private final int m_idx;
    private final int m_len;

    Node1MTELN(int idx, int len) {
        m_idx = idx;
        m_len = len;
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        try {
            if (!processClearCommand(tag, t, context)) {
                boolean result = false;
                Fact fact = t.topFact();
                Value s;
                if ((s = fact.get(m_idx)).type() == RU.LIST) {
                    ValueVector vv = s.listValue(null);
                    if (vv.size() == m_len)
                        result = true;
                }

                // debugPrint(fact, result);

                if (result)
                    passAlong(tag, t, context);
            }

        } catch (JessException re) {
            re.addContext("rule LHS (MTELN)", context);
            throw re;
        } catch (Exception e) {
            JessException re = new JessException("Node1MTELN.call",
                    "Error during LHS execution",
                    e);
            re.addContext("rule LHS (MTELN)", context);
            throw re;
        }
    }

    public String toString() {
        return "[Test that the multislot at index " + m_idx + " is " + m_len + " items long]";
    }

    public boolean equals(Object o) {
        if (o instanceof Node1MTELN) {
            Node1MTELN n = (Node1MTELN) o;
            return (m_idx == n.m_idx && m_len == n.m_len);
        } else
            return false;
    }
}




