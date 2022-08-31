package jess;

/**
 * A test that always passes, and makes calls with calltype RIGHT.
 * Used to build nested NOTs
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1LTR extends Node {
    void callNodeLeft(int tag, Token t, Context context) throws JessException {
        broadcastEvent(tag, JessEvent.RETE_TOKEN_LEFT, t, context);
        passAlong(tag, t, context);
    }

    void passAlong(int tag, Token t, Context context) throws JessException {
        Node[] sa = m_succ;
        for (int j = 0; j < m_nSucc; j++) {
            Node s = sa[j];
            // System.out.println(this + " " +  t);
            s.callNodeRight(tag, t, context);
        }
    }

    public boolean equals(Object o) {
        return (o instanceof Node1LTR);
    }

    public String getCompilationTraceToken() {
        return "a";
    }

    public String toString() {
        return "[Right input adapter]";
    }


    public int getNodeType() {
        return TYPE_ADAPTER;
    }

    void setOld() {
        // Nothing to do, because this node is stateless
    }

    void callNodeRight(int tag, Token token, Context context) throws JessException {
        throw new JessException("callNodeRight", "Special node has no right input", getClass().getName());
    }
}

