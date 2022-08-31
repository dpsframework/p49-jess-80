package jess;

/** 
 * Test class type
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1TECT extends Node1 {
    private final String m_name;

    Node1TECT(String name) {
        // ###
        m_name = name;
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        if (processClearCommand(tag, t, context))
            return;

        try {
            boolean result;
            Fact fact = t.topFact();
            // If this fact is of this class, we're done.
            if ((result = m_name.equals(fact.getName())))
                ;
            else {
                Deftemplate dt = fact.getDeftemplate();
                dt = dt.getParent();
                while (dt != null) {
                    if ((result = m_name.equals(dt.getName())))
                        break;
                    dt = dt.getParent();
                }
            }

            // debugPrint(fact, result);

            if (result)
                passAlong(tag, t, context);

        } catch (JessException re) {
            re.addContext("rule LHS (TECT)", context);
            throw re;
        } catch (Exception e) {
            JessException re = new JessException("Node1TECT.call",
                    "Error during LHS execution",
                    e);
            re.addContext("rule LHS (TECT)", context);
            throw re;

        }
    }

    public String toString() {
        return "[Test that fact class is " + m_name + " or a subclass of type " + m_name + "]";
    }

    public boolean equals(Object o) {
        if (o instanceof Node1TECT) {
            Node1TECT n = (Node1TECT) o;
            return (m_name.equals(n.m_name));
        } else
            return false;
    }

    public String getName() {
        return m_name;
    }

}

