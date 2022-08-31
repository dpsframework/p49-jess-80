package jess;

import java.io.Serializable;

/**
 * Single-input nodes of the pattern network
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

abstract class Node1 extends Node implements Serializable {
    /**
     * Do the business of this node.
     * The input token of a Node1 should only be single-fact tokens.
     *
     * RU.CLEAR means flush two-input ndoe memories; we just pass these along.
     * All one-input nodes must call this and just immediately return *false*
     * if it returns true!
     */

    boolean processClearCommand(int tag, Token t, Context context) throws JessException {
        broadcastEvent(tag, JessEvent.RETE_TOKEN_RIGHT, t, context);
        if (tag == RU.CLEAR) {
            passAlong(tag, t, context);
            return true;
        } else
            return false;
    }

    // Nodes that will store values should call this to clean them up.
    Value cleanupBindings(Value v) throws JessException {
        if (v.type() == RU.BINDING) {
            BindingValue bv = new BindingValue((BindingValue) v);
            bv.resetFactNumber();
            return bv;
        } else if (v.type() == RU.FUNCALL) {
            Funcall vv = (Funcall) v.funcallValue(null).clone();
            for (int i = 0; i < vv.size(); i++)
                vv.set(cleanupBindings(vv.get(i)), i);
            return new FuncallValue(vv);
        } else
            return v;
    }

    void passAlong(int tag, Token t, Context context) throws JessException {
        Node[] sa = m_succ;
        for (int j = 0; j < m_nSucc; j++) {
            Node s = sa[j];
            s.callNodeRight(tag, t, context);
        }
    }

    /**
     * callNode can call this to print debug info
     */
    void debugPrint(Token t, boolean result) {

        System.out.println(this + " " + t.topFact() + " => " + result);
    }

    String getCompilationTraceToken() {
        return "1";
    }


    public int getNodeType() {
        return TYPE_NODE1;
    }

    void setOld() {
        // Nothing
    }

    void callNodeLeft(int tag, Token token, Context context) throws JessException {
        throw new JessException("callNodeLeft", "One-input nodes have no left input", getClass().getName());
    }
}




