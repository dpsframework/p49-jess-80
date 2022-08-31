package jess;

import java.io.Serializable;

/** 
 * Specialized two-input nodes for negated patterns
 * <P>
 * NOTE: CLIPS behaves in a surprising way which I'm following here.
 * Given this defrule:
 * <PRE>
 * (defrule test-1
 *  (bar)
 *  (not (foo))
 *  =>
 *  (printout t "not foo"))
 * </PRE>
 * CLIPS behaves this way:
 * <PRE>
 * (watch activations)
 * (assert (bar))
 * ==> Activation 0 test-1
 * (assert (foo))
 * <== Activation 0 test-1
 * (retract (foo))
 * ==> Activation 0 test-1
 * </PRE>
 * This is not surprising yet. Here's the funky part
 * <PRE>
 * (run)
 * "not foo"
 * (assert (foo))
 * (retract (foo))
 * ==> Activation 0 test-1
 * </PRE>
 * The rule fires,  and all that's required to fire it again is for the
 * "not-ness" to be removed and replaced; the (bar) fact does not need to
 * be replaced. This obviously falls out of the implementation; it makes things
 * easy!
 *
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


class NodeNot2Single extends Node2 implements Serializable {
    private boolean m_old = false;

    NodeNot2Single(int hashkey, Rete engine) {
        super(hashkey, engine);
    }

    void callNodeLeft(int tag, Token token, Context context) throws JessException {
        // UPDATE tokens will always be copied by superclass method
        if (tag == RU.ADD || tag == RU.MODIFY_ADD)
            token = Rete.getFactory().newToken(token);

        super.callNodeLeft(tag, token, context);
    }

    /**
     * Run all the tests on a given (left) token and every token in the
     * right memory. Every time a right token *passes* the tests, increment
     * the left token's negation count; at the end, if the
     * left token has a zero count, pass it through.
     *
     * The 'nullToken' contains a fact used as a placeholder for the 'not' CE.
     */
    void runTestsVaryRight(int tag, Token leftToken, Context context, TokenTask task) throws JessException {
        if (tag != RU.REMOVE && tag != RU.MODIFY_REMOVE)
            super.runTestsVaryRight(tag, leftToken, context, task);

        if (leftToken.m_negcnt == 0) {
            Token newToken = Rete.getFactory().newToken(leftToken, Fact.getNullFact());
            Rete engine = context.getEngine();
            newToken.updateTime(engine);
            incrementBackchainMatches(engine);
            passAlong(tag, newToken, context);
            recordLogicalMatch(tag, newToken, context);
        }
    }

    public void tokenMatchesLeft(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        if (tag == RU.UPDATE && m_old)
            return;
        leftToken.m_negcnt++;
    }

    public void tokenMatchesRight(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
            if (tag == RU.ADD || tag == RU.UPDATE || tag == RU.MODIFY_ADD) {
                // retract any activation due to the left token
                Token newToken = Rete.getFactory().newToken(leftToken, Fact.getNullFact());
                newToken.updateTime(context.getEngine());
                passAlong(RU.REMOVE, newToken, context);
                recordLogicalMatch(tag, newToken, context);
                if (tag != RU.UPDATE || !m_old)
                    leftToken.m_negcnt++;
            } else if (--leftToken.m_negcnt == 0) { // REMOVE tags
                // pass along the revitalized left token
                Token newToken = Rete.getFactory().newToken(leftToken, Fact.getNullFact());
                newToken.updateTime(context.getEngine());
                passAlong(tag == RU.MODIFY_REMOVE ? RU.MODIFY_ADD : RU.ADD, newToken, context);
                recordLogicalMatch(tag, newToken, context);
            } else if (leftToken.m_negcnt < 0)
                throw new JessException("NodeNot2.tokenMatchesRight",
                        "Corrupted Negcnt (< 0)",
                        "");

    }

    public boolean equals(Object o) {
        return this == o;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(256);
        buffer.append("[NodeNot2Single ntests=");
        buffer.append(m_nTests);
        buffer.append(" ");
        for (int i = 0; i < m_nTests; i++) {
            buffer.append(m_tests[i].toString());
            buffer.append(" ");
        }
        buffer.append(";usecount = ");
        buffer.append(getUseCount());
        buffer.append("]");
        return buffer.toString();
    }

    public int getNodeType() {
        return TYPE_NODENOT2;
    }


    void setOld() {
        m_old = true;
    }
}
