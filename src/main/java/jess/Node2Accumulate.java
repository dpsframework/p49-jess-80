package jess;

class Node2Accumulate extends Node2 {
    private final Value m_body;
    private final Value m_initializer;
    private final Value m_return;
    private boolean m_old = false;

    public Node2Accumulate(Accumulate accum, int hashKey, Rete engine) {
        super(hashKey, engine);
        m_body = accum.getBody();
        m_initializer = accum.getInitializer();
        m_return = accum.getReturn();
    }

    void callNodeLeft(int tag, Token token, Context context) throws JessException {
        Rete engine = context.getEngine();
        createTokenTreesIfNeeded(engine);
        switch(tag) {
        // ADD, MODIFY_ADD, UPDATE:
        //     Add token to left memory
        //     Run accumulation
        //     ADD/MODIFY_ADD/UPDATE results token

            case RU.UPDATE: {
                if (m_old)
                    break;
                // ELSE FALL THROUGH
            }
            case RU.ADD:
            case RU.MODIFY_ADD:
            {
                Context localContext = setupForAccumulation(context, token);
                accumulateResults(tag, token, localContext);
                sendAccumulateToken(tag, token, localContext);
                break;
            }

        // REMOVE, MODIFY_REMOVE
        //     Remove token from left memory
        //     REMOVE/MODIFY_REMOVE matching results token

            case RU.REMOVE:
            case RU.MODIFY_REMOVE: {
                getLeftMemory(engine).remove(token);
                sendNilResultsToken(tag, token, context);
                break;
            }

        // CLEAR
        // Invoke super action and just pass it along
            case RU.CLEAR:
            default: {
                super.callNodeLeft(tag, token, context);
                break;
            }
        }
    }

    private void sendNilResultsToken(int tag, Token token, Context context) throws JessException {
        Context localContext = context.push();
        m_initializer.resolveValue(localContext);
        sendAccumulateToken(tag, token, localContext);
    }

    // All the real work is done in tokenMatchesRight()
    void callNodeRight(int tag, Token token, Context context) throws JessException {
        if (tag == RU.UPDATE && m_old)
            return;	
        if (tag != RU.CLEAR) {
            Context localContext = setupForAccumulation(context, token);
            super.callNodeRight(tag, token, localContext);
        }
    }

    private void accumulateResults(int tag, Token token, Context localContext) throws JessException {
        super.callNodeLeft(tag, token, localContext);
    }

    private Context setupForAccumulation(Context context, Token token) throws JessException {
        Context localContext = context.push();
        localContext.setToken(token);
        m_initializer.resolveValue(localContext);
        return localContext;
    }

    private void sendAccumulateToken(int tag, Token token, Context context) throws JessException {
        Fact rightFact = new AccumulateFact();
        rightFact.updateTime(context.getEngine().getTime());
        rightFact.setSlotValue("value", m_return.resolveValue(context));
        rightFact.setFactId(-2);
        Token newToken = Rete.getFactory().newToken(token, rightFact);
        super.passAlong(tag, newToken, context);
    }

    public void tokenMatchesLeft(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        context.setFact(rightToken.fact(0));
        m_body.resolveValue(context);
    }

    // We found one match based on an assertion from the right.
    // Take the left token and do the full accumulation, which may send several tokens
    // down the pipe.
    public void tokenMatchesRight(int tag, Token leftToken, Token rightToken, Context context) throws JessException {
        switch (tag) {
            // ADD, UPDATE:
            //     REMOVE one matching results token.
            //     Accumulate, ADD/UPDATE one results token.
            case RU.ADD:
            case RU.UPDATE: {
                sendNilResultsToken(RU.REMOVE, leftToken, context);
                Context localContext = setupForAccumulation(context, leftToken);
                runTestsVaryRight(tag, leftToken, localContext, this);
                sendAccumulateToken(tag, leftToken, localContext);
                break;
            }


            // MODIFY_ADD:
            //     Accumulate, MODIFY_ADD one results tokens.
            case RU.MODIFY_ADD: {
                Context localContext = setupForAccumulation(context, leftToken);
                runTestsVaryRight(tag, leftToken, localContext, this);
                sendAccumulateToken(tag, leftToken, localContext);
                break;
            }

            // REMOVE:
            //     REMOVE matching results token
            //     Accumulate, ADD one results token
            case RU.REMOVE: {
                sendNilResultsToken(tag, leftToken, context);
                Context localContext = setupForAccumulation(context, leftToken);
                runTestsVaryRight(tag, leftToken, localContext, this);
                sendAccumulateToken(RU.ADD, leftToken, localContext);
                break;
            }

            // MODIFY_REMOVE:
            //     MODIFY_REMOVE matching results token
            case RU.MODIFY_REMOVE: {
                sendNilResultsToken(tag, leftToken, context);
                break;
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(256);
        sb.append("[Node2Accumulate");
        sb.append(";usecount = ");
        sb.append(getUseCount());
        sb.append("]");
        return sb.toString();
    }

    // Don't share accumulates; it makes updates impossible
    public boolean equals(Object o) {
        return this == o;
    }
    /*
    public boolean equals(Object o) {
        if (o == this)
            return true;
        else if (!(o instanceof Node2Accumulate))
            return false;

        Node2Accumulate other = (Node2Accumulate) o;
        if (!other.m_initializer.equals(m_initializer))
            return false;
        else if (!other.m_body.equals(m_body))
            return false;
        else if (!other.m_return.equals(m_return))
            return false;

        return true;
    } */


    void setOld() {
        m_old = true;
    }
}
