package jess;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

/**
 * A Defrule is a specific action meant to be taken when certain
 * conditions are met in working memory. Although you can construct
 * Defrules from the Java API, they are almost always parsed from
 * "defrule" constructs in the Jess language. Building a Defrule from
 * Java is very complicated.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class Defrule extends HasLHS implements Serializable, MatchInfoSource {
    private Funcall[] m_actions;
    private int m_nActions;
    private Value m_salienceVal;
    private boolean m_autoFocus = false;
    private boolean m_noLoop;
    private Set m_testedSlots = new HashSet();
    private final String m_actKey;
    private final String m_salKey;
    private final String m_sscKey;

    public Defrule(String name, String docstring, Rete engine) throws JessException {
        super(name, docstring, engine);
        // ###
        m_salienceVal = new Value(0, RU.INTEGER);
        long key = engine.getNextNodeKey();
        m_actKey = key + "A";
        m_salKey = key + "S";
        m_sscKey = key + "C";
        // addJessListener(new PrintingListener());
    }

    /**
     * Fetch the current salience value for this rule. This method won't re-evaluate the
     * salience unless it's never been evaluated before; the last value previously computed will be returned.
     *
     * @return the salience of this defrule
     */

    public final int getSalience(Rete engine) throws JessException {
        int[] holder = ((int[]) engine.getKeyedStorage(m_salKey));
        if (holder == null) {
            holder = new int[1];
            holder[0] = computeSalience(engine);
            engine.putKeyedStorage(m_salKey, holder);
        }
        return holder[0];
    }

    int[] getSalienceHolder(Rete engine) {
        int[] holder = ((int[]) engine.getKeyedStorage(m_salKey));
        if (holder == null) {
            holder = new int[1];
            engine.putKeyedStorage(m_salKey, holder);
        }
        return holder;
    }

    /**
     * Fetch the raw salience for this rule as a jess.Value. For rules with dynamic salience, the
     * Value returned will be a jess.FuncallValue.
     *
     * @return the salience of this defrule
     */

    public Value getSalienceValue() {
        return m_salienceVal;
    }

    /**
     * Set the salience of this rule. The value van be a fixed integer, or it can be a function call. The default salience is 0.
     *
     * @param v      the salience expression
     * @param engine a Rete object to provide an execution context
     * @throws JessException if anything goes wrong
     */
    public void setSalience(Value v, Rete engine) throws JessException {
        synchronized (engine.getActivationSemaphore()) {            
            try {
                m_salienceVal = v;
                evalSalience(engine);
            } finally {
                if (getNext() != null)
                    ((Defrule) getNext()).setSalience(v, engine);
            }
        }
    }

    /**
     * Evaluate the salience of this rule. If the salience was set to a
     * Funcall value during parsing, then this function may return
     * different values over time. If the salience is constant, this is
     * equivalent to getSalience. The computed value will be the next value returned by
     * getSalience().
     *
     * @param engine The Rete engine the rule belongs to
     * @return The evaluated salience
     * @throws JessException If something goes wrong
     */
    public int evalSalience(Rete engine) throws JessException {
        int[] holder = getSalienceHolder(engine);
        holder[0] = computeSalience(engine);
        return holder[0];
    }

    private int computeSalience(Rete engine) throws JessException {
        Context gc = engine.getGlobalContext().push();
        try {
            return m_salienceVal.intValue(gc);
        } finally {
            gc.pop();
        }
    }

    /**
     * Indicate whether this rule's salience is different from the default value.
     *
     * @return true if this rule's salience is not the default.
     */
    public boolean hasNonDefaultSalience() {
        try {
            return m_salienceVal.type() != RU.INTEGER || m_salienceVal.intValue(null) != 0;
        } catch (JessException e) {
            return true;
        }
    }

    /**
     * Indicate whether this rule will automatically focus its module when it is activated.
     *
     * @return the value of this property
     */
    public boolean getAutoFocus() {
        return m_autoFocus;
    }

    /**
     * Tell this rule whether to automatically focus the module it appears in when the rule is activated.
     * The default value is false.
     *
     * @param autoFocus the desired value of this property.
     */
    public void setAutoFocus(boolean autoFocus) {
        m_autoFocus = autoFocus;
        if (getNext() != null)
            ((Defrule) getNext()).setAutoFocus(autoFocus);
    }

    /**
     * Add a conditional element (a pattern or group of patterns) to this rule. Normally this will only
     * be called by the Jess parser or the XML parser, but it is theoretically possible for users to build their
     * own rules this way.
     *
     * @param ce     the conditional element to add
     * @param engine the rule engine that provides the execution context
     * @throws JessException if anything goes wrong
     */

    public void addCE(ConditionalElementX ce, Rete engine) throws JessException {
        if (ce.getLogical() && getGroupSize() > 0)
            if (!getLHSComponent(getGroupSize() - 1).getLogical())
                throw new JessException("Defrule.addCE",
                                        "Logical CEs can't follow non-logical CEs",
                                        m_name);

        super.addCE(ce, engine);

        // Make a note of slots that should matter for per-slot activation
        ce.recordTestedSlots(m_testedSlots);

    }

    boolean isSlotTested(Deftemplate template, int slotIndex) {
        do {
            TestedSlot ts = new TestedSlot(template, slotIndex);
            if (m_testedSlots.contains(ts))
                return true;
            template = template.getParent();
        } while (template != null);
        return false;
    }


    private boolean isSlotTested(Context context) {
        Fact fact = context.getSlotSpecificModifiedFact();
        String[] slotNames = context.getModifiedSlots();

        // TODO Inheritance
        Deftemplate template = fact.getDeftemplate();
        for (int i = 0; i < slotNames.length; i++) {
            String slotName = slotNames[i];
            int slotIndex = fact.getDeftemplate().getSlotIndex(slotName);
            if (isSlotTested(template, slotIndex)) {
                return true;
            } 
        }
        return false;
    }

    // TODO Must handle inheritance
    private boolean isSlotTested(Deftemplate template, Context context) {
        Fact fact = context.getSlotSpecificModifiedFact();
        String[] slotNames = context.getModifiedSlots();
        for (int i = 0; i < slotNames.length; i++) {
            String slotName = slotNames[i];
            int slotIndex = fact.getDeftemplate().getSlotIndex(slotName);
            if (isSlotTested(template, slotIndex))
                return true;
        }
        return false;
    }

    private void doAddCall(int tag, Token token, Context context, Rete engine) throws JessException {
        if (shouldAddCall(tag, token, context)) {
            Activation a = new Activation(engine, token, this);
            engine.addActivation(a);
            getActivations(engine).put(token, a);

        } else if (isModifyAddOnSlotSpecificUnmatched(tag, context)) {
            // See comment at end of file
            List cache = getSlotSpecificCache(engine);
            if (cache.size() > 0) {
                for (Iterator it = cache.iterator(); it.hasNext();) {
                    Activation a = (Activation) it.next();
                    if (token.fastDataEquals(a.getToken())) {
                        it.remove();
                        a.replaceToken(token);
                        getActivations(engine).put(token, a);
                        break;
                    }
                }
            }
        }
    }

    private boolean isModifyAddOnSlotSpecificUnmatched(int tag, Context context) {
        return tag == RU.MODIFY_ADD && context.getSlotSpecificModifiedFact() != null;
    }

    private Map getActivations(Rete engine) {
        Map activations = (Map) engine.getKeyedStorage(m_actKey);
        if (activations == null) {
            activations = new HashMap();
            engine.putKeyedStorage(m_actKey, activations);
        }
        return activations;
    }

    private void removeCall(int tag, Token token, Context context) throws JessException {
        Rete engine = context.getEngine();
        if (shouldRemoveCall(tag, token, context)) {
            Activation a = (Activation) getActivations(engine).remove(token);
            if (a != null) {                
                engine.removeActivation(a);
            }

        } else if (isModifyRemoveOnSlotSpecificUnmatched(tag, context)) {
            // See comment at end of file
            Activation a = (Activation) getActivations(engine).remove(token);
            if (a != null) {
                getSlotSpecificCache(engine).add(a);
            }
        }
    }

    private boolean isModifyRemoveOnSlotSpecificUnmatched(int tag, Context context) {
        return tag == RU.MODIFY_REMOVE && context.getSlotSpecificModifiedFact() != null;
    }

    private List getSlotSpecificCache(Rete engine) {
        List cache = (List) engine.getKeyedStorage(m_sscKey);
        if (cache == null) {
            cache = new ArrayList();
            engine.putKeyedStorage(m_sscKey, cache);
        }
        return cache;
    }

    private boolean shouldRemoveCall(int tag, Token token, Context context) {
        if (tag == RU.MODIFY_REMOVE)
            return context.getSlotSpecificModifiedFact() == null || isSlotTested(context);

        else
            return true;
    }

    private boolean shouldAddCall(int tag, Token token, Context context) {
        if (m_noLoop) {
            Rete engine = context.getEngine();
            if (engine.getRunThread() == Thread.currentThread()) {
                Activation a = engine.getThisActivation();
                if (a != null && a.getRule() == this) {
                    return false;
                }
            }
        }

        if (tag == RU.MODIFY_ADD) {
            return context.getSlotSpecificModifiedFact() == null || isSlotTested(context);
        } else
            return true;
    }

    private void possiblyDoAddCall(Token token, Context context, Rete engine)
            throws JessException {
        // We're not new, so updates don't affect us
        if (!m_new)
            return;

        // We've already got this one
        if (getActivations(engine).get(token) != null)
            return;

        // Add a new activation
        doAddCall(RU.UPDATE, token, context, engine);
    }

    /**
     * An implementation detail, public only because Java requires methods that implement an
     * interface to be public.
     */
    public void callNodeLeft(int tag, Token token, Context context)
            throws JessException {
        broadcastEvent(tag, JessEvent.RETE_TOKEN_LEFT, token, context);

        Rete engine = context.getEngine();
        switch (tag) {

            case RU.ADD:
            case RU.MODIFY_ADD:
                doAddCall(tag, token, context, engine);
                break;

            case RU.REMOVE:
            case RU.MODIFY_REMOVE:
                removeCall(tag, token, context);
                break;


            case RU.UPDATE:
                possiblyDoAddCall(token, context, engine);
                break;

            case RU.CLEAR:
                getActivations(engine).clear();
                break;
        }
    }

    void ready(Token fact_input, Context c) throws JessException {
        super.ready(fact_input, c);

        // If our assertions will have logical support, put the needed
        // info into the context. We peel off the post-logical part of
        // the token that activated us and save only the "logical"
        // part. The assertFact() function will save information about
        // this token and any asserted facts into the Node2. The
        // Node2, in turn, will send messages to the Rete object when
        // that token is removed.

        if (m_logicalNode != null) {

            Token logicalToken = fact_input;
            int count = fact_input.size();

            int logicalSize = m_logicalNode.getTokenSize();
            while (count > logicalSize) {
                logicalToken = logicalToken.getParent();
                --count;
            }

            c.setLogicalSupportNode(m_logicalNode);
            c.setToken(logicalToken);
        }

        return;
    }


    /**
     * Do the RHS of this rule. For each action (ValueVector form of a
     * Funcall), do two things: 1) Call ExpandAction to do variable
     * substitution and subexpression expansion 2) call
     * Funcall.Execute on it.
     * <p/>
     * Fact_input is the Vector of ValueVector facts we were fired with.
     */
    void fire(Token factInput, Rete engine, Context context) throws JessException {
        getActivations(engine).remove(factInput);
        Context c = context.push();
        c.clearReturnValue();

        // Pull needed values out of facts into bindings table
        synchronized (engine.getWorkingMemoryLock()) {
            ready(factInput, c);
        }

        try {
            // OK, now run the rule. For every action...
            for (int i = 0; i < m_nActions; i++) {
                m_actions[i].execute(c);

                if (c.returning()) {
                    c.clearReturnValue();
                    c.pop();
                    engine.popFocus(getModule());
                    return;
                }
            }
        } catch (BreakException bk) {
            // Fall through
        } catch (JessException re) {
            re.addContext("defrule " + getDisplayName(), c);
            throw re;
        } finally {
            c.pop();
        }

    }

    void debugPrint(Token facts, int seq, PrintWriter ps) {
        ps.print("FIRE ");
        ps.print(seq);
        ps.print(" ");
        ps.print(getDisplayName());
        for (int i = 0; i < facts.size(); i++) {
            Fact f = facts.fact(i);
            if (f.getFactId() != -1)
                ps.print(" f-" + f.getFactId());
            if (i < facts.size() - 1)
                ps.print(",");
        }
        ps.println();
        ps.flush();
    }

    /**
     * Fetch the number of actions on this rule's RHS
     *
     * @return The number of actions
     */
    public int getNActions() {
        return m_nActions;
    }

    /**
     * Fetch the idx-th RHS action of this rule.
     *
     * @param idx The zero-based index of the action to fetch
     * @return the action as a function call
     */
    public Funcall getAction(int idx) {
        return m_actions[idx];
    }


    /**
     * Add an action to this defrule
     *
     * @param fc the action as a function call
     */
    public void addAction(Funcall fc) {
        if (m_actions == null || m_nActions == m_actions.length) {
            Funcall[] temp = m_actions;
            m_actions = new Funcall[m_nActions + 5];
            if (temp != null)
                System.arraycopy(temp, 0, m_actions, 0, m_nActions);
        }
        m_actions[m_nActions++] = fc;

        if (getNext() != null)
            ((Defrule) getNext()).addAction(fc);
    }

    /**
     * Defrule participates in the Visitor pattern. Will call accept(this) on the Visitor.
     *
     * @param jv a Visitor
     * @return whatever jv.accept(this) returns
     */
    public Object accept(Visitor jv) {
        return jv.visitDefrule(this);
    }

    private LogicalNode m_logicalNode = null;

    void setLogicalInformation(LogicalNode node) {
        m_logicalNode = node;
        node.setMatchInfoSource(this);
    }

    /**
     * Returns a brief description of this rule
     *
     * @return the String "Defrule rule-name"
     */
    public String toString() {
        return "Defrule " + getName();
    }

    // For testing
    LogicalNode getLogicalNode() {
        return m_logicalNode;
    }

    /**
     * Specify whether this rule can activate itself through actions it takes on its RHS. If this property is
     * set to true, then modifying a fact on the RHS of this rule can't reactivate the same rule, even if that
     * fact is matched by this rule. The default value is false.
     *
     * @param b the desired value of the property.
     */
    public void setNoLoop(boolean b) {
        m_noLoop = b;
        if (getNext() != null)
            ((Defrule) getNext()).setNoLoop(b);
    }

    /**
     * Indicate whether this rule can activate itself through actions it takes on its RHS. If this property is
     * true, then modifying a fact on the RHS of this rule can't reactivate the same rule, even if that
     * fact is matched by this rule.
     *
     * @return the current value of the property.
     */

    public boolean isNoLoop() {
        return m_noLoop;
    }

    void createChain(ConditionalElementX ce, Rete engine) throws JessException {
        doSetLHS(ce.getConditionalElementX(0), engine);
        Defrule rule = this;
        String name = getName();
        for (int i = 1; i < ce.getGroupSize(); ++i) {
            ConditionalElementX newPatterns = ce.getConditionalElementX(i);
            String fullName = name + "&" + i;
            Defrule next = new Defrule(fullName, getDocstring(), engine);
            next.doSetLHS(newPatterns, engine);
            rule.setNext(next);
            rule = next;
        }
    }

    /**
     * Return the type of this construct
     *
     * @return the String "defrule"
     */
    public final String getConstructType() {
        return "defrule";
    }

    /**
     * Implementation detail. Public only because Java requires methods that implement an interface to
     * be public.
     */
    public boolean isRelevantChange(int factIndex, Token token, Context context) {
        Fact slotSpecificModifiedFact = context.getSlotSpecificModifiedFact();
        if (slotSpecificModifiedFact == null)
            return true;
        return isSlotTested(slotSpecificModifiedFact.getDeftemplate(), context);
    }

    /**
     * Test probe only, do not use
     */
    public Set getTestedSlotInfo() {
        return m_testedSlots;        
    }

    /*
     * If unmatched slot of a fact is being modified, we KNOW that we have the activation already
     * (since modifying an unmatched slot can't affect the activation, and if we're seeing the event, then we must
     * have matched in the past.) Furthermore if we see a MODIFY_REMOVE for an unmatched slot, we can be
     * sure we're getting the MODIFY_ADD later, for the same reason. Thirdly, an unmatched slot will never be
     * split into multiple subfacts -- the only way to get split is to be matched! Although there might be multiple
     * tokens due to splitting on some other slot, the pre/post modify tokens will differ only in the simple value of a slot.
     *
     * The problem is that Token.equals() uses all the fact data (and it has to, to distinguish between multifield matches.)
     * So if we change an unmatched slot and don't patch up the activation list, we're not going to be able to find it if
     * it's subsequently removed by a later modify or retract.
     *
     * Solution: if slot-specific applies, and we see MODIFY_REMOVE for an unmatched slot, then remove the activations
     * from storage, and keep them in a collection in another storage location (no leak, since we know for sure we're going to
     * get a MODIFY_ADD.) When we see the MODIFY_ADD, remove each activation from the collection, update the slot contents
     * for each fact, then put it back into storage.
     *
     * Since a single fact may be part of multiple activations on the same rule, there may be more than one activation in
     * the cache; we have to look at each of them until we find the right one.
     *
     * Optimization: do this only if Fact.getIcon() != Fact. I think we can get away with this since Token.hashCode()
     * doesn't depend on fact data.
     */


}



