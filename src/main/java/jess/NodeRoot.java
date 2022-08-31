package jess;

import java.util.*;
/**
 * The root of the Rete network.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class NodeRoot extends Node1 {
    private final HashMap m_map = new HashMap();

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        passAlong(tag, t, context);
    }

    boolean isInUse(Deftemplate template) {
        return m_map.containsKey(template.getName());
    }


    public Iterator successors() {
        return m_map.values().iterator();
    }

    void passAlong(int tag, Token t, Context context) throws JessException {
        if (tag == RU.CLEAR) {
            for (Iterator it = m_map.values().iterator();it.hasNext();)
                ((Node) it.next()).callNodeRight(tag, t, context);
            return;
        }

        Fact fact = t.fact(0);
        Deftemplate template = fact.getDeftemplate();
        while (template != null) {
            String name = template.getName();
            Node node = (Node) m_map.get(name);
            if (node != null) {
                node.callNodeRight(tag, t, context);
            }
            template = template.getParent();
        }
    }

    Node mergeSuccessor(Node n, NodeSink r)
            throws JessException {
        if (n instanceof Node1TECT) {
            Node1TECT tect = (Node1TECT) n;
            Node old;
            if ((old = (Node) m_map.get(tect.getName())) != null) {
                r.addNode(old);
                return old;
            }
        }

        // No match found
        addSuccessor(n, r);
        return n;
    }


    Node addSuccessor(Node n, NodeSink r)
            throws JessException {
        if (n instanceof Node1TECT) {
            Node1TECT tect = (Node1TECT) n;
            String name = tect.getName();
            m_map.put(name, tect);
            r.addNode(n);
            return n;
        }
        return null;
    }

    void removeSuccessor(Node n) {
        if (n instanceof Node1TECT) {
            Node1TECT tect = (Node1TECT) n;
            String name = tect.getName();
            m_map.remove(name);
        }
    }


    public String toString() {
        return "The root of the Rete network";
    }
}
