package jess;

import java.io.Serializable;

/**
 * Specialized two-input nodes for negated pattern
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class NodeNot2 extends Node2 implements Serializable {
    private final int m_size;
    private boolean m_old;

    NodeNot2(int hashkey, int size, Rete engine)  {
        super(hashkey, engine);
        m_size = size;
    }

    void callNodeLeft(int tag, Token token, Context context) throws JessException {
        // UPDATE tokens will already be copied by superclass method
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
            Rete engine = context.getEngine();
            Token newToken = Rete.getFactory().newToken(leftToken, Fact.getNullFact());
            newToken.updateTime(engine);
            incrementBackchainMatches(engine);
            passAlong(tag, newToken, context);
            recordLogicalMatch(tag, newToken, context);
        }
    }

    void doRunTestsVaryRight(int tag, Token leftToken, Context context, TokenTask task)
            throws JessException {

        TokenList tokens = getRightMemory(context.getEngine()).findListForToken(leftToken, false);
        doRunTestsVaryRight(tag, leftToken, tokens, context, task);
    }

    public void tokenMatchesLeft(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        if (tag == RU.UPDATE && m_old)
            return;
        leftToken.m_negcnt++;
    }

    public void tokenMatchesRight(int tag, Token leftToken, Token rightToken, Context context) throws JessException {

        if (tag == RU.ADD || tag == RU.UPDATE || tag == RU.MODIFY_ADD) {
            // retract any activation due to the left token
            Token newToken = Rete.getFactory().
                    newToken(leftToken, Fact.getNullFact());
            newToken.updateTime(context.getEngine());
            passAlong(RU.REMOVE, newToken, context);
            recordLogicalMatch(tag, newToken, context);
            if (tag != RU.UPDATE || !m_old)
                leftToken.m_negcnt++;

        } else if (--leftToken.m_negcnt == 0) { // tag == REMOVE
            // pass along the revitalized left token
            Token newToken = Rete.getFactory().
                    newToken(leftToken, Fact.getNullFact());
            newToken.updateTime(context.getEngine());
            passAlong(tag == RU.MODIFY_REMOVE ? RU.MODIFY_ADD : RU.ADD, newToken, context);
            recordLogicalMatch(tag, newToken, context);
        }
    }

    /**
     * Run all the tests on a given (right) token and every relevant
     * token in the left memory. For the true ones, increment (or
     * decrement) the appropriate negation counts. Any left token
     * which transitions to zero gets passed along.
     *
     * "Relevant" here means that the sort code of the left token is
     * the same as the sort code of the "prefix" of the right
     * token. The "prefix" is the part that precedes the "not" pattern
     * on the rule LHS.
     */

    void doRunTestsVaryLeft(int tag, Token rightToken, Context context, TokenTask task) throws JessException {

        Token parent = subsetRightToken(rightToken);
        TokenList tokens = getLeftMemory(context.getEngine()).findListForToken(parent, false);
        doRunTestsVaryLeft(tag, rightToken, tokens, context, task);
    }

    void doRunTestsVaryLeft(int tag, Token rightToken, TokenList tokens, Context context, TokenTask task)
            throws JessException {

        if (tokens != null) {
            int size = tokens.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    Token leftToken = tokens.get(i);
                    context.setToken(leftToken);
                    // Optimization, but it doesn't check for
                    // corrupted negcnt errors. Haven't seen one of
                    // those in a long time, though.
                    if ((tag == RU.REMOVE || tag == RU.MODIFY_REMOVE) && leftToken.m_negcnt == 0) {
                        continue;
                    }

                    // Test count doesn't matter here, see overload
                    if (runTests(0, context, rightToken))
                        task.tokenMatchesRight(tag, leftToken, rightToken, context);
                }
            }
        }
    }

    private Token subsetRightToken(Token rightToken) {
        Token parent = rightToken;
        while (parent.size() > m_size)
            parent = parent.getParent();
        return parent;
    }

    /**
     * This override is the whole purpose for this class. It returns
     * true if left and right tokens share a prefix.
     */

    boolean runTests(int ntests, Context context, Token rightToken)
            throws JessException {

        Token lt = context.getToken();
        rightToken = subsetRightToken(rightToken);
        return rightToken == lt || rightToken.dataEquals(lt);
    }

    protected void createTokenTrees(Rete engine) {
        boolean useSortCode = true;
        setLeftMemory(new TokenTree(m_hashkey, useSortCode, 0, 0, 0), engine);
        setRightMemory(new TokenTree(m_hashkey, useSortCode, m_size, 0, 0), engine);
        // EJFH Is this condition right?
        if (m_pattern != null)
            engine.putKeyedStorage(m_bcKey, new int[1]);
    }


    public boolean equals(Object o) {
        return this == o;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(256);
        buffer.append("[NodeNot2");
        buffer.append(";usecount = ");
        buffer.append(getUseCount());
        buffer.append("]");

        return buffer.toString();
    }


    void addTest(int test, int tokenIndex, int leftIndex, int leftSubIndex,
                 int rightIndex, int rightSubIndex) {
        addTest(null);
    }

    void addTest(TestBase t) {
        throw new RuntimeException("ABORT: Can't add tests to NodeNot2");
    }


    public int getNodeType() {
        return TYPE_NODENOT2;
    }

    void setOld() {
        m_old = true;
    }

}

