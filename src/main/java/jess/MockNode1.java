package jess;

import java.util.ArrayList;

class MockNode1 extends Node1 {
    ArrayList m_inputs = new ArrayList();

    public MockNode1() {
        super();
    }

    void callNodeRight(int tag, Token token, Context context) throws JessException {
        m_inputs.add(token);
    }

    String getCompilationTraceToken() {
        return "M";
    }

    public int getNodeType() {
        return TYPE_NONE;
    }

    // This should do nothing for stateless nodes, and for stateful nodes
    // it should make them ignore (but pass along) further update tokens.
    void setOld() {
        // Stateless; nothing to do
    }
}
