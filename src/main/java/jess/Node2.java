package jess;

import java.io.*;
import java.util.Map;
import java.util.Iterator;

/**
 * A non-negated, two-input node of the Rete network.
 * Each tests in this node tests that a slot from a fact from the left input
 * and one from the right input have the same value and type.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node2 extends NodeJoin implements LogicalNode, Serializable, TokenTask {

    private int m_tokenSize;

    /**
     * The key to use when creating token trees
     */

    protected final int m_hashkey;

    /**
     * Only non-null when I should backwards-chain
     */
    Pattern m_pattern;

    /**
     * Only non-null when I should backwards-chain
     */

    private HasLHS m_defrule;

    /**
     * True if we can do short-cut testing
     */
    private boolean m_blessed = false;

    private final String m_rightKey;
    private final String m_leftKey;
    protected final String m_bcKey;
    private final String m_logicKey;
    private MatchInfoSource m_matchInfoSource;

    /**
     * Constructor
     *
     * @param hashkey Hashkey to use for creating TokenTrees
     */

    Node2(int hashkey, Rete engine) {
        m_hashkey = hashkey;
        long key = engine.getNextNodeKey();
        m_leftKey = key + "L";
        m_rightKey = key + "R";
        m_bcKey = key + "BC";
        m_logicKey = key + "LG";
        // addJessListener(new PrintingListener());
    }

    /**
     * Add a test to this node.
     *
     * @param test          EQ or NEQ
     * @param tokenIndex    Which fact in the token
     * @param leftIndex     Which slot in the left fact
     * @param leftSubIndex  Which subslot in the left slot
     * @param rightIndex    Which slot in the right fact
     * @param rightSubIndex Which subslot in the right slot
     */
    void addTest(int test, int tokenIndex, int leftIndex, int leftSubIndex,
                 int rightIndex, int rightSubIndex) {

        if (leftSubIndex == -1 && rightSubIndex == -1)
            addTest(new Test2Simple(test, tokenIndex, leftIndex, rightIndex));
        else
            addTest(new Test2Multi(test, tokenIndex, leftIndex,
                    leftSubIndex, rightIndex,
                    rightSubIndex));
    }


    /**
     * Do the business of this node.
     * The 2-input nodes, on receiving a token, have to do several things,
     * and their actions change based on whether it's an ADD or REMOVE,
     * and whether it's the right or left input!
     * <PRE>
     * <p/>
     * For ADDs, left input:
     * 1) Look for this token in the left memory. If it's there, do nothing;
     * If it's not, add it to the left memory.
     * <p/>
     * 2) Perform all this node's tests on this token and each of the right-
     * memory tokens. For any right token for which they succeed:
     * <p/>
     * 3) a) append the right token to a copy of this token. b) do a
     * CallNode on each of the successors using this new token.
     * <p/>
     * For ADDs, right input:
     * <p/>
     * 1) Look for this token in the right memory. If it's there, do nothing;
     * If it's not, add it to the right memory.
     * <p/>
     * 2) Perform all this node's tests on this token and each of the left-
     * memory tokens. For any left token for which they succeed:
     * <p/>
     * 3) a) append this  token to a copy of the left token. b) do a
     * CallNode on each of the successors using this new token.
     * <p/>
     * For REMOVEs, left input:
     * <p/>
     * 1) Look for this token in the left memory. If it's there, remove it;
     * else do nothing.
     * <p/>
     * 2) Perform all this node's tests on this token and each of the right-
     * memory tokens. For any right token for which they succeed:
     * <p/>
     * 3) a) append the right token to a copy of this token. b) do a
     * CallNode on each of the successors using this new token.
     * <p/>
     * For REMOVEs, right input:
     * <p/>
     * 1) Look for this token in the right memory. If it's there, remove it;
     * else do nothing.
     * <p/>
     * 2) Perform all this node's tests on this token and each of the left-
     * memory tokens. For any left token for which they succeed:
     * <p/>
     * 3) a) append this token to a copy of the left token. b) do a
     * CallNode on each of the successors using this new token.
     * <p/>
     * </PRE>
     */

    void callNodeLeft(int tag, Token token, Context context) throws JessException {
        try {
            Rete engine = context.getEngine();
            createTokenTreesIfNeeded(engine);
            clearBackchainMatches(engine);
            broadcastEvent(tag, JessEvent.RETE_TOKEN_LEFT, token, context);
            switch (tag) {

                case RU.MODIFY_ADD:
                case RU.ADD:
                    // Temporary patch to catch uncovered case
                    try {
                        m_tokenSize = token.size();
                        getLeftMemory(engine).add(token, false);
                    } catch (NullPointerException npe) {
                        throw new JessException("Node2.callNode",
                                "Negated conjunction with unbound variables", npe);
                    }
                    runTestsVaryRight(tag, token, context, this);
                    askForBackChain(token, context);

                    break;

                case RU.UPDATE:

                    // Temporary patch to catch uncovered case
                    try {
                        m_tokenSize = token.size();
                        Token storedToken = Rete.getFactory().newToken(token);
                        if (getLeftMemory(engine).add(storedToken, true)) {
                            runTestsVaryRight(RU.UPDATE, storedToken, context, this);
                            askForBackChain(token, context);
                        }
                    } catch (NullPointerException npe) {
                        throw new JessException("Node2.callNode",
                                "Negated conjunction with unbound variables", npe);
                    }

                    break;

                case RU.REMOVE:
                case RU.MODIFY_REMOVE: {
                    Token stored = getLeftMemory(engine).remove(token);
                    if (stored != null) {
                        runTestsVaryRight(tag, stored, context, this);
                    }
                    break;

                } case RU.CLEAR: {
                    // This is a special case. If we get a 'clear', we flush
                    // our memories, then notify all our successors and
                    // return.
                    clearTokenTrees(engine);
                    clearLogicalDepends(engine);

                    passAlong(tag, token, context);
                    break;

                } default:
                    throw new JessException("Node2.callNode", "Bad tag", tag);
            } // switch tag

        } catch (JessException je) {
            je.addContext("rule LHS (Node2)", context);
            throw je;
        }
    }

    private void clearLogicalDepends(Rete engine) {
        NodeLogicalDependencyHandler handler = getLogicalDepends(engine);
        if (handler != null)
            handler.clear();
    }

    private void clearBackchainMatches(Rete engine) {
        if (m_pattern != null)
            getBackchainMatchCounter(engine)[0] = 0;
    }

    private int[] getBackchainMatchCounter(Rete engine) {
        return ((int[]) engine.getKeyedStorage(m_bcKey));
    }

    void callNodeRight(int tag, Token token, Context context) throws JessException {
        try {
            Rete engine = context.getEngine();
            createTokenTreesIfNeeded(engine);
            broadcastEvent(tag, JessEvent.RETE_TOKEN_RIGHT, token, context);
            switch (tag) {

                case RU.UPDATE:
                case RU.ADD:
                case RU.MODIFY_ADD:
                    getRightMemory(engine).add(token, tag == RU.UPDATE);
                    runTestsVaryLeft(tag, token, context, this);
                    break;

                case RU.REMOVE:
                case RU.MODIFY_REMOVE: {
                    Token stored = getRightMemory(engine).remove(token);
                    if (stored != null) {
                        runTestsVaryLeft(tag, token, context, this);
                    }
                    break;

                } case RU.CLEAR:
                    break;

                default:
                    throw new JessException("Node2.callNode",
                            "Bad tag in token",
                            tag);
            } // switch tag


        } catch (JessException je) {
            je.addContext("rule LHS (Node2)", context);
            throw je;
        }
    }

    protected void createTokenTreesIfNeeded(Rete engine) {
        if (getLeftMemory(engine) == null)
            createTokenTrees(engine);
    }

    /**
     * Node2.callNode can call this to produce debug info.
     *
     * @param token
     * @param callType
     */

    void debugPrint(int tag, Token token, int callType) {
        System.out.println("TEST " + toString() + "(" + hashCode() +
                ");calltype=" + callType +
                ";tag=" + tag + ";class=" +
                token.fact(0).getName());
    }

    /**
     * Run all the tests on a given (left) token and every token in the
     * right memory. For the true ones, assemble a composite token and
     * pass it along to the successors.
     */

    void runTestsVaryRight(int tag, Token leftToken, Context context, TokenTask task)
            throws JessException {
        if (m_blessed) {
            Rete engine = context.getEngine();
            Value key = getLeftMemory(engine).extractKey(leftToken);
            TokenList tokens = getRightMemory(engine).getTestableTokens(key);
            doRunTestsVaryRight(tag, leftToken, tokens, context, task);
        } else
            doRunTestsVaryRight(tag, leftToken, context, task);
    }


    void doRunTestsVaryRight(int tag, Token leftToken, Context context, TokenTask task) throws JessException {
        Rete engine = context.getEngine();
        for (int j = 0; j < getRightMemory(engine).getHash(); j++)
            doRunTestsVaryRight(tag, leftToken, getRightMemory(engine).getTokenList(j), context, task);
    }


    void doRunTestsVaryRight(int tag, Token leftToken, TokenList tokens, Context context, TokenTask task)
            throws JessException {

        if (tokens != null) {
            int size = tokens.size();

            for (int i = 0; i < size; i++) {
                // Must be inside loop due to passAlong() call
                context.setToken(leftToken);
                Token rt = tokens.get(i);
                if (runTests(m_nTests, context, rt)) {
                    task.tokenMatchesLeft(tag, leftToken, rt, context);
                }
            }
        }
    }

    public void tokenMatchesLeft(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        if (tag != RU.REMOVE && tag != RU.MODIFY_REMOVE)
            incrementBackchainMatches(context.getEngine());

        if (m_nTests != 0)
            leftToken = leftToken.prepare(true);
        Token newToken = Rete.getFactory().newToken(leftToken, rightToken);
        recordLogicalMatch(tag, newToken, context);
        passAlong(tag, newToken, context);
    }

    protected void recordLogicalMatch(int tag, Token newToken, Context context) {
        if (m_matchInfoSource != null)
            ensureHandlerAllocated(context.getEngine()).tokenMatched(tag, newToken, context);
    }

    void incrementBackchainMatches(Rete engine) {
        if (m_pattern != null)
            ++getBackchainMatchCounter(engine)[0];
    }

    public void tokenMatchesRight(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        tokenMatchesLeft(tag, leftToken, rightToken, context);
    }

    private void runTestsVaryLeft(int tag, Token token, Context context, TokenTask task)
            throws JessException {

        if (m_blessed) {
            Rete engine = context.getEngine();
            Value key = getRightMemory(engine).extractKey(token);
            TokenList tokens = getLeftMemory(engine).getTestableTokens(key);
            doRunTestsVaryLeft(tag, token, tokens, context, task);
        } else {
            doRunTestsVaryLeft(tag, token, context, this);
        }
    }


    void doRunTestsVaryLeft(int tag, Token rightToken, Context context, TokenTask task) throws JessException {
        Rete engine = context.getEngine();
        int count = getLeftMemory(engine).getHash();
        for (int j = 0; j < count; j++) {
            doRunTestsVaryLeft(tag, rightToken, getLeftMemory(engine).getTokenList(j), context, task);
        }
    }

    void doRunTestsVaryLeft(int tag, Token rightToken, TokenList tokens, Context context, TokenTask task)
            throws JessException {

        if (tokens != null) {
            int size = tokens.size();
            if (size > 0) {
                int ntests = m_nTests;
                for (int i = 0; i < size; i++) {
                    Token lt = tokens.get(i);
                    context.setToken(lt);

                    if (runTests(ntests, context, rightToken)) {
                        task.tokenMatchesRight(tag, lt, rightToken, context);
                    }
                }
            }
        }
    }

    boolean runTests(int ntests, Context context, Token rightToken)
            throws JessException {

        TestBase[] theTests = m_tests;
        context.setFact(rightToken.topFact());
        for (int i = 0; i < ntests; i++) {
            if (!theTests[i].doTest(context))
                return false;
        }

        return true;
    }

    /**
     * Beginning of backward chaining. This is very slow; we need to do more
     * of the work at compile time.
     */
    void setBackchainInfo(Pattern pattern, HasLHS rule) {
        m_pattern = pattern;
        m_defrule = rule;
    }

    /**
     * The point of this method is to ask for a fact that would join with this token. We look
     * for either literal values, and put them into the fact directly, or pure unifications, and
     * get the values out of the token to put into our fact.
     */

    private void askForBackChain(Token token, Context context) throws JessException {
        // In theory, we could allow m_matches != 0 and use this to
        // retract need-x facts.  I can't quite figure out how to do this,
        // though.

        if (m_pattern == null)
            return;

        Rete engine = context.getEngine();
        int matchCount = getBackchainMatchCounter(engine)[0];

        if (matchCount != 0)
            return;

        Fact fact = new Fact(m_pattern.getBackchainingTemplateName(),
                context.getEngine());

        // For each slot in the pattern...
        for (int i = 0; i < m_pattern.getNSlots(); i++) {
            int type = m_pattern.getDeftemplate().getSlotType(i);

            // This is the slot value, which we're looking to set to
            // something useful...
            Value val = Funcall.NIL;

            ValueVector slot = null;
            if (type == RU.MULTISLOT)
                slot = new ValueVector();

            // Look at every test

            for (Iterator it = m_pattern.getTests(i); it.hasNext();) {
                Test1 test = (Test1) it.next();

                // only consider EQ tests, not NEQ
                if (test.m_test != TestBase.EQ)
                    continue;


                // If this is a variable, and we can pull a value out
                // of the token, we're golden; but if this is the
                // first occurrence, forget it.
                else if (test.m_slotValue instanceof Variable) {
                    BindingValue b = (BindingValue) m_defrule.getBindings().
                            get(test.m_slotValue.variableValue(null));

                    // Handle defglobals here
                    if (b == null)
                        val = test.m_slotValue;

                    else if (b.getFactNumber() < token.size()) {
                        val = token.fact(b.getFactNumber()).get(b.getSlotIndex());

                        if (b.getSubIndex() != -1)
                            val = val.listValue(null).get(b.getSubIndex());
                    }

                    if (type == RU.SLOT)
                        break;
                }

                // Otherwise, it's a plain value, and this is what we want!
                else {
                    val = test.m_slotValue;

                    if (type == RU.SLOT)
                        break;
                }

                // Add something to this multislot.
                if (type == RU.MULTISLOT) {
                    if (slot.size() < (test.m_subIdx + 1))
                        slot.setLength(test.m_subIdx + 1);
                    slot.set(val, test.m_subIdx);
                    val = Funcall.NIL;
                }
            }

            if (type == RU.MULTISLOT) {
                for (int ii = 0; ii < slot.size(); ii++)
                    if (slot.get(ii) == null)
                        slot.set(Funcall.NIL, ii);

                val = new Value(slot, RU.LIST);
            }

            fact.set(val, i);
        }

        // The engine will assert or retract this after the current LHS cycle.
        context.getEngine().setPendingFact(fact, matchCount == 0);
    }


    /**
     * Describe myself
     *
     * @return A string showing all the tests, etc, in this node.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(256);
        sb.append("[Node2 ntests=");
        sb.append(m_nTests);
        sb.append(" ");
        for (int i = 0; i < m_nTests; i++) {
            sb.append(m_tests[i].toString());
            sb.append(" ");
        }
        sb.append(";usecount = ");
        sb.append(getUseCount());
        if (m_blessed)
            sb.append(";blessed");
        sb.append("]");
        return sb.toString();
    }


    /**
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    protected final void initTokenTrees(Rete engine) {
        if (getLeftMemory(engine) == null) {
            createTokenTrees(engine);
        } else {
            clearTokenTrees(engine);
        }
    }

    private void clearTokenTrees(Rete engine) {
        getLeftMemory(engine).clear();
        getRightMemory(engine).clear();
        clearBackchainMatches(engine);
    }

    protected void createTokenTrees(Rete engine) {
        MemoryInfo info = new MemoryInfo(m_tests, m_nTests);
        m_blessed = info.blessed;
        int tokenIndex = info.leftSlot == -1 ? 0 : info.tokenIndex;

        setLeftMemory(new TokenTree(m_hashkey, info.leftSlot == -1, tokenIndex,
                info.leftSlot, info.leftSubSlot), engine);
        setRightMemory(new TokenTree(m_hashkey, info.rightSlot == -1, 0,
                info.rightSlot, info.rightSubSlot), engine);
        if (m_pattern != null)
            engine.putKeyedStorage(m_bcKey, new int[1]);
    }

    /*
     * Textural description of memory contents
     */

    StringBuffer displayMemory(Rete engine) {
        createTokenTreesIfNeeded(engine);
        StringBuffer buffer = new StringBuffer("*** Left Memory:\n");
        getLeftMemory(engine).dumpMemory(buffer);
        buffer.append("*** RightMemory:\n");
        getRightMemory(engine).dumpMemory(buffer);
        return buffer;
    }

    public int getTokenSize() {
        return m_tokenSize + 1;
    }

    public void dependsOn(Fact fact, Token token, Rete engine) {
        NodeLogicalDependencyHandler handler = ensureHandlerAllocated(engine);
        handler.dependsOn(fact, token);
    }

    // For testing only
    public Map getLogicalDependencies(Rete engine) {
        return getLogicalDepends(engine).getMap();
    }

    private NodeLogicalDependencyHandler getLogicalDepends(Rete engine) {
        return (NodeLogicalDependencyHandler) engine.getKeyedStorage(m_logicKey);
    }

    public void setMatchInfoSource(MatchInfoSource source) {
        m_matchInfoSource = source;
    }

    private NodeLogicalDependencyHandler ensureHandlerAllocated(Rete engine) {
        NodeLogicalDependencyHandler handler = getLogicalDepends(engine);
        if (handler == null) {
            handler = new NodeLogicalDependencyHandler(getTokenSize());
            engine.putKeyedStorage(m_logicKey, handler);
        }
        handler.setMatchInfoSource(m_matchInfoSource);
        return handler;
    }

    public String getIndexingInfo(Rete engine) throws JessException {
        if (m_blessed) {
            return "Left memory indexed by " +
                    getLeftMemory(engine).getIndexingInfo() + "\n" +
                    "Right memory indexed by " +
                    getRightMemory(engine).getIndexingInfo();
        } else {
            return "Unindexed.";
        }
    }

    public int getNodeType() {
        return TYPE_NODE2;
    }

    protected TokenTree getLeftMemory(Rete engine) {
        return (TokenTree) engine.getKeyedStorage(m_leftKey);
    }

    protected void setLeftMemory(TokenTree left, Rete engine) {
        engine.putKeyedStorage(m_leftKey, left);
    }

    protected TokenTree getRightMemory(Rete engine) {
        return (TokenTree) engine.getKeyedStorage(m_rightKey);
    }

    protected void setRightMemory(TokenTree right, Rete engine) {
        engine.putKeyedStorage(m_rightKey, right);
    }

    void removeLogicalSupportFrom(Token token, Context context) {
        ensureHandlerAllocated(context.getEngine()).removeLogicalSupportFrom(token,  context);
    }

    void setOld() {
        // Nothing to do. Updates properly handled in main code.
    }
}




