package jess;

import java.util.Map;

/**
 * A test that always passes, but makes calls with calltype LEFT
 * instead of RIGHT.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1RTL extends Node1 implements LogicalNode {

    /**
     * Tokens that give logical support to facts
     */

    private int m_tokenSize;
    private MatchInfoSource m_matchInfoSource;
    private final String m_keyL;

    Node1RTL(Rete engine) {
        m_keyL = engine.getNextNodeKey() + "L";
    }

    void callNodeRight(int tag, Token t, Context context) throws JessException {
        recordLogicalMatch(tag, t, context);
        passAlong(tag, t, context);
        return;
    }

    private void recordLogicalMatch(int tag, Token t, Context context) {
        if (m_matchInfoSource != null) {
            NodeLogicalDependencyHandler handler = ensureHandlerAllocated(context.getEngine());
            handler.tokenMatched(tag, t, context);
        }
    }


    void passAlong(int tag, Token t, Context context) throws JessException {
        m_tokenSize = t.size();
        Node[] sa = m_succ;
        for (int j = 0; j < m_nSucc; j++) {
            Node s = sa[j];
            // System.out.println(this + " " +  t);
            s.callNodeLeft(tag, t, context);
        }
    }

    public boolean equals(Object o) {
        return (o instanceof Node1RTL);
    }

    public String toString() {
        return "[Left input adapter]";
    }

    public void dependsOn(Fact f, Token t, Rete engine) {
        if (m_matchInfoSource != null) {
            NodeLogicalDependencyHandler handler = ensureHandlerAllocated(engine);
            handler.dependsOn(f, t);
        }
    }

    public int getTokenSize() {
        return m_tokenSize;
    }

    // For testing only
    public Map getLogicalDependencies(Rete engine) {
        return getLogicalDepends(engine).getMap();
    }

    private NodeLogicalDependencyHandler getLogicalDepends(Rete engine) {
        return (NodeLogicalDependencyHandler) engine.getKeyedStorage(m_keyL);
    }

    public void setMatchInfoSource(MatchInfoSource source) {
        m_matchInfoSource = source;
    }

    NodeLogicalDependencyHandler ensureHandlerAllocated(Rete engine) {
        NodeLogicalDependencyHandler handler = getLogicalDepends(engine);
        if (handler == null) {
            handler = new NodeLogicalDependencyHandler(getTokenSize());
            engine.putKeyedStorage(m_keyL, handler);
        }
        handler.setMatchInfoSource(m_matchInfoSource);
        return handler;
    }


    public int getNodeType() {
        return TYPE_ADAPTER;
    }
}

