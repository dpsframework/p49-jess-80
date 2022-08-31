package jess;

import java.io.Serializable;
import java.util.*;

/**
 * Parent class of Defrules and Defqueries.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public abstract class HasLHS extends Node
        implements Serializable, Visitable, Modular, NodeSink {
    String m_module;
    String m_name;
    String m_displayName;
    String m_docstring = "";
    private ArrayList m_nodes = new ArrayList();
    private Map m_bindings = new HashMap();
    private Group m_CEs;
    private int m_nodeIndexHash = 0;
    private StringBuffer m_compilationTrace;
    boolean m_new = true;
    boolean m_frozen = false;    
    private HasLHS m_next = null;

    HasLHS(String name, String docstring, Rete engine) throws JessException {

        int colons = name.indexOf("::");
        if (colons != -1) {
            m_module = name.substring(0, colons);
            engine.verifyModule(m_module);
            m_name = name;
        } else {
            m_module = engine.getCurrentModule();
            m_name = engine.resolveName(name);
        }

        int amp = m_name.indexOf('&');
        m_displayName = (amp == -1) ? m_name : m_name.substring(0, amp);

        m_docstring = docstring;
        m_CEs = new Group(Group.AND);
    }

    /**
     * Fetch the number of  elenments on the LHS of this construct.
     * @return The number of CEs
     */
    public int getGroupSize() {
        return m_CEs.getGroupSize();
    }

    /**
     * Return the idx-th Conditional element on this construct's LHS.
     * @param idx The zero-based index of the desired CE
     * @return the CE
     */
    ConditionalElementX getLHSComponent(int idx) {
        return m_CEs.getConditionalElementX(idx);
    }

    /**
     * Consider this ConditionalElement to be READ ONLY!
     */

    public ConditionalElement getConditionalElements() {
        return getLHSComponents();
    }

    ConditionalElementX getLHSComponents() {
        return m_CEs;
    }

    /**
     * Return a string (useful for debugging) describing all the Rete network
     * nodes connected to this construct.
     * @return A textual description of all the nodes used by this construct
     */

    public String listNodes() {
        StringBuffer sb = new StringBuffer(100);
        for (int i = 0; i < m_nodes.size(); i++) {
            sb.append(m_nodes.get(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Some rules that use the "or" conditional element can be compiled into multiple
     * subrules. In those situations, those subrules are chained together and can be
     * accessed, in order, using this method.
     * @return the next subrule or subquery in the chain.
     */
    public HasLHS getNext() {
        return m_next;
    }

    void setNext(HasLHS next) {
        m_next = next;
    }

    // TODO Can we do "cullUnusedBindings()" here, instead?
    void freeze(Rete engine) throws JessException {
        m_frozen = true;
    }

    public int getPatternCount() {
        return getLHSComponents().getPatternCount();
    }

    void insertCEAtStart(ConditionalElementX ce, Rete engine)
            throws JessException {
        Group CEs = m_CEs;
        m_bindings = new HashMap();
        m_CEs = new Group(Group.AND);

        addCE(ce, engine);
        for (int i = 0; i < CEs.getGroupSize(); i++) {
            addCE(CEs.getConditionalElementX(i), engine);
        }
    }

    public void setLHS(Group ce, Rete engine)
            throws JessException {

        ce = (Group) ce.canonicalize();
        createChain(ce, engine);
    }

    abstract void createChain(ConditionalElementX ce, Rete engine)
            throws JessException;

    void doSetLHS(ConditionalElementX ce, Rete engine) throws JessException {
        Group flat = new Group(Group.AND);
        ce.addToGroup(flat);

        for (int i=0; i<flat.getGroupSize(); i++) {
            ConditionalElementX cex = (ConditionalElementX) flat.getConditionalElement(i);
            addCE(cex, engine);
        }
    }

    /**
     * Add a conditional element to this construct
     */

    protected void addCE(ConditionalElementX ce, Rete engine) throws JessException {
        int startIndex = getPatternCount();
        ce = (ConditionalElementX) ce.clone();

        // EJFH This represents an implementation limit, although I'm unsure a limit on *what*
        m_seqNum += 10;
        if (ce.getName().equals(Group.NOT) || ce.getName().equals(Group.ACCUMULATE)) {
            m_CEs.renameVariables(ce, getSequenceNumber(), "");
        }

        storeBoundName(ce);
        m_CEs.add(ce);

        // Look for unresolved references, and resolve them if possible. Although
        // little-used now, this may become very important!

        resolveUnresolvedReferences(ce, m_bindings, engine);

        //Look for variables, and create bindings for the ones defined
        //in this CE.
        ce.findVariableDefinitions(startIndex, m_bindings, m_bindings);

        // Now look for variables that were never given a proper definition --
        // those that were only negated.
        findUndefinedVariables(ce);

        // Now handle '|' conjunctions by transforming tests into (or) Funcalls
        ce.transformOrConjunctionsIntoOrFuncalls(startIndex, m_bindings, engine);

        transformEqCallsIntoDirectMatching(ce);
        transformEqCallsIntoLiteralMatching(ce);

        addNOTToSupressUnneededBackwardChaining(ce, engine);
    }

    // Calls to eq/neq that just compare one slot's contents to another can
    // be replaced with a more efficient direct match.

    private void transformEqCallsIntoDirectMatching(ConditionalElementX ce) throws JessException {
        for (PatternIterator it = new PatternIterator(ce); it.hasNext();) {
            Pattern pattern = (Pattern) it.next();
            for (Iterator testIt = pattern.getTests(); testIt.hasNext();) {
                Test1 test = (Test1) testIt.next();
                if (RU.NO_SLOT.equals(test.m_slotName))
                    continue;
                if (test.m_slotValue instanceof FuncallValue) {
                    Funcall funcall = test.m_slotValue.funcallValue(null);
                    if (funcall.getName().equals("eq") || funcall.getName().equals("neq")) {
                        if (argumentsAreTwoVariables(funcall)) {
                            int slotIndex = pattern.getDeftemplate().getSlotIndex(test.m_slotName);
                            if (oneVariableIsDefinedOnThisSlot(funcall, pattern, slotIndex)) {
                                Value localVariable = funcall.get(1);
                                Value foreignVariable = funcall.get(2);
                                test.m_slotValue = foreignVariable;
                                int theTest = test.m_test == Test1.EQ ?
                                        funcall.getName().equals("eq") ? Test1.EQ : Test1.NEQ :
                                        funcall.getName().equals("eq") ? Test1.NEQ : Test1.EQ;
                                test.m_test = theTest;
                            }
                        }
                    }
                }
            }
        }
    }

    // Calls to eq/neq that just compare one slot's contents to a literal can
    // be replaced with a more efficient direct match.

    private void transformEqCallsIntoLiteralMatching(ConditionalElementX ce) throws JessException {
        for (PatternIterator it = new PatternIterator(ce); it.hasNext();) {
            Pattern pattern = (Pattern) it.next();
            for (Iterator testIt = pattern.getTests(); testIt.hasNext();) {
                Test1 test = (Test1) testIt.next();
                if (RU.NO_SLOT.equals(test.m_slotName))
                    continue;
                if (test.m_slotValue instanceof FuncallValue) {
                    Funcall funcall = test.m_slotValue.funcallValue(null);
                    if (funcall.getName().equals("eq") || funcall.getName().equals("neq")) {
                        int slotIndex = pattern.getDeftemplate().getSlotIndex(test.m_slotName);
                        if (argumentsAreAVariableAndALiteral(funcall, pattern, slotIndex)) {
                            Value var = funcall.get(1);
                            Value literal = funcall.get(2);
                            test.m_slotValue = literal;
                            int theTest = test.m_test == Test1.EQ ?
                                    funcall.getName().equals("eq") ? Test1.EQ : Test1.NEQ :
                                    funcall.getName().equals("eq") ? Test1.NEQ : Test1.EQ;
                            test.m_test = theTest;
                        }
                    }
                }
            }
        }
    }

    private boolean argumentsAreAVariableAndALiteral(Funcall funcall, Pattern pattern, int slotIndex) throws JessException {
        if (funcall.get(1).type() == RU.VARIABLE ^ funcall.get(2).type() == RU.VARIABLE) {
            int varIndex = funcall.get(1).type() == RU.VARIABLE ? 1 : 2;
            Value var = funcall.get(varIndex);
            if (variableDefinedOnThisSlot(var, pattern, slotIndex)) {
                int argIndex = varIndex == 1 ? 2 : 1;
                Value arg = funcall.get(argIndex);
                if (arg.isLiteral()) {
                    if (varIndex == 2) {
                        funcall.set(var, 1);
                        funcall.set(arg, 2);
                    }
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Return true if the two arguments to this funcall are two variables, one of which is defined in this pattern.
     * If the function will return true, also reorder the two arguments so the local one is first. if necessary.
     */
    private boolean oneVariableIsDefinedOnThisSlot(Funcall funcall, Pattern pattern, int slotIndex) throws JessException {
        Value v1 = funcall.get(1);
        Value v2 = funcall.get(2);
        boolean v1IsLocal = variableDefinedOnThisSlot(v1, pattern, slotIndex);
        boolean v2IsLocal = variableDefinedOnThisSlot(v2, pattern, slotIndex);
        if (v1IsLocal || v2IsLocal) {
            if (!v1IsLocal) {
                funcall.set(v2, 1);
                funcall.set(v1, 2);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean variableDefinedOnThisSlot(Value value, Pattern pattern, int slotIndex) throws JessException {
        String name = value.variableValue(null);
        BindingValue binding = (BindingValue) m_bindings.get(name);
        if (binding != null) {
            return binding.getCE() == pattern && binding.getSlotIndex() == slotIndex;
        } else {
            return false;
        }        
    }

    private boolean argumentsAreTwoVariables(Funcall funcall) throws JessException {
        return funcall.get(1).type() == RU.VARIABLE && funcall.get(2).type() == RU.VARIABLE;
    }


    private void resolveUnresolvedReferences(ConditionalElementX ce, Map bindings, Rete engine) throws JessException {
        for (PatternIterator it = new PatternIterator(ce); it.hasNext();) {
            Pattern pattern = (Pattern) it.next();
            for (Iterator testIt = pattern.getTests(); testIt.hasNext();) {
                Test1 test = (Test1) testIt.next();
                Value val = test.m_slotValue;
                if (val instanceof InfixSlotParser.UnresolvedSlotReference) {
                    InfixSlotParser.UnresolvedSlotReference unres = (InfixSlotParser.UnresolvedSlotReference) val;
                    Value replacement = resolveValue(unres, engine);
                    test.m_slotValue = replacement;
                } else if (val instanceof FuncallValue) {
                    resolveUnresolvedReferencesInFuncalls((FuncallValue) val, bindings, engine);
                }
            }
        }
    }

    private Value resolveValue(InfixSlotParser.UnresolvedSlotReference unres, Rete engine) throws JessException {
        String patternBindingName = unres.getPatternBinding();
        String slotName = unres.getSlotName();
        BindingValue patternBinding = (BindingValue) m_bindings.get(patternBindingName);
        if (patternBinding != null && patternBinding.getSlotIndex() == RU.PATTERN) {
            ConditionalElementX ce = patternBinding.getCE();
            if (ce instanceof Pattern) {
                Pattern p = (Pattern) ce;
                int index = p.getDeftemplate().getSlotIndex(slotName);
                Variable newVar = p.findAnyExistingVariable(index, -1);
                if (newVar == null) {
                    newVar = new Variable(RU.gensym("__hl"), RU.VARIABLE);
                    p.addTest(new Test1(Test1.EQ, slotName, -1, newVar, RU.AND));
                    BindingValue replacement = new BindingValue(newVar.variableValue(null), p, patternBinding.getFactNumber(), index, -1, RU.NONE);
                    m_bindings.put(newVar.variableValue(null), replacement);
                }
                return newVar;
            }
        }
        Funcall f = new Funcall("fact-slot-value", engine);
        f.arg(new Variable(patternBindingName, RU.VARIABLE));
        f.arg(slotName);
        return new FuncallValue(f);
    }


    private void resolveUnresolvedReferencesInFuncalls(FuncallValue val, Map bindings, Rete engine) throws JessException {
        Funcall funcall = val.funcallValue(null);
        for (int i=0; i<funcall.size(); ++i) {
            Value arg = funcall.get(i);
            if (arg instanceof InfixSlotParser.UnresolvedSlotReference) {
                InfixSlotParser.UnresolvedSlotReference unres = (InfixSlotParser.UnresolvedSlotReference) arg;
                Value resolved = resolveValue(unres, engine);
                funcall.set(resolved, i);
            } else if (arg instanceof FuncallValue) {
                resolveUnresolvedReferencesInFuncalls((FuncallValue) arg, bindings, engine);
            }
        }
    }

    private boolean containsReferencesToOtherPatterns(Value v, Pattern current) throws JessException {
        if (v.type() == RU.BINDING) {
            return ((BindingValue) v).getCE() != current;
        } else if (v.type() == RU.FUNCALL) {
            Funcall f = v.funcallValue(null);
            return containsReferencesToOtherPatterns(f, current);
        } else {
            return false;
        }
    }

    private boolean containsReferencesToOtherPatterns(Funcall f, Pattern current) throws JessException {
        for (int i = 1; i < f.size(); i++) {
            Value arg = f.get(i);
            if (arg.type() == RU.BINDING && ((BindingValue) arg).getCE() != current)
                return true;

            else if (containsReferencesToOtherPatterns(arg, current))
                return true;
        }
        return false;
    }

    private void addNOTToSupressUnneededBackwardChaining(ConditionalElementX ce,
                                                         Rete engine)
            throws JessException {
        if (ce.isBackwardChainingTrigger()) {
            Pattern pattern = (Pattern) ce;
            Pattern notPattern =
                    new Pattern(pattern,
                                pattern.getNameWithoutBackchainingPrefix());

            Group not = new Group(Group.NOT);
            not.add(notPattern);
            addCE(not, engine);
        }
    }

    private void storeBoundName(ConditionalElementX ce) throws JessException {
        String varname = ce.getBoundName();
        if (varname != null) {
            if (m_bindings.get(varname) != null) {
                for (PatternIterator it = new PatternIterator(m_CEs); it.hasNext();)
                    if (varname.equals(((Pattern) it.next()).getBoundName()))
                        throw new JessException("HasLHS::storeBoundName", "Duplicate pattern binding for variable", varname);
            } else {
                if (ce.getName().equals(Group.ACCUMULATE))  {
                    addBinding(varname, ce, getGroupSize(), -1, RU.ACCUM_RESULT);
                } else {
                    while (! (ce instanceof Pattern))
                        ce = ce.getConditionalElementX(0);
                    addBinding(varname, ce, getGroupSize(), -1, RU.PATTERN);
                }
            }
        }
    }

    private void findUndefinedVariables(ConditionalElementX ce)
            throws JessException {
        for (PatternIterator it = new PatternIterator(ce); it.hasNext();) {
            Pattern pattern = (Pattern) it.next();
            for (Iterator testIt = pattern.getTests(); testIt.hasNext();) {
                Test1 test = (Test1) testIt.next();
                Value val = test.m_slotValue;
                if (val instanceof Variable) {
                    String name = val.variableValue(null);
                    if (m_bindings.get(name) == null) {
                        // Ignore defglobals here
                        if (Defglobal.isADefglobalName(name))
                            continue;

                        throw new JessException("HasLHS.addPattern",
                                "First use of variable negated:",
                                RU.removePrefix(name));
                    }

                } else if (val instanceof FuncallValue) {
                    searchFuncallForUndefinedVariables((FuncallValue) val, (Map) ((HashMap) m_bindings).clone());
                }

            }                         
        }
    }

    // CAUTION: Destructively modifies "bindings"
    private void searchFuncallForUndefinedVariables(FuncallValue val, Map bindings) throws JessException {
        Funcall funcall = val.funcallValue(null);
        for (int i=0; i<funcall.size(); ++i) {
            if (funcall.get(i) instanceof Variable) {
                String name = funcall.get(i).variableValue(null);
                if (isADefinition(funcall, i))
                    bindings.put(name, name);
                else if (bindings.get(name) == null && !Defglobal.isADefglobalName(name)) {
                    if (name.indexOf(".") > -1) {
                        throw new JessException("HasLHS.addPattern",
                                                "Dotted variables can't be used in this context:",
                                                name);
                    }
                    else {
                        throw new JessException("HasLHS.addPattern",
                                                "Variable used before definition:",
                                                name);
                    }
                }
            } else if (funcall.get(i) instanceof FuncallValue) {
                searchFuncallForUndefinedVariables((FuncallValue) funcall.get(i), bindings);
            }
        }
    }

    private boolean isADefinition(Funcall funcall, int i) throws JessException {
        String name = funcall.get(0).stringValue(null);
        if (i == 1) {
            if (name.equals("bind") || name.equals("foreach"))
                return true;
        }
        return false;
    }

    private void addBinding(String name, ConditionalElementX patt,
                            int factidx,
                            int subidx, int type){
        m_bindings.put(name,
                       new BindingValue(name, patt, factidx, type, subidx, RU.FACT));
    }

    Map getBindings() {
        return m_bindings;
    }

    /**
     * Return an iterator listing all the variable names used on this rule's left hand side.
     * Both variables used in defining the rule as well as synthetic variables added by Jess will be included.
     * Note that while compiling a rule, Jess may rename some of the variables that appear. Synthetic variable
     * names always start with an underscore.
     *  
     * @return an Iterator over variable names
     */

    public Iterator getBindingNames() {
        return m_bindings.keySet().iterator();
    }

    public Iterator getNodes() {
        return m_nodes.iterator();
    }
    public int getNodeCount() {
        return m_nodes.size();
    }

    /**
     * The rule compiler calls this after adding a rule to the Rete network.
     */
    void cullUnusedBindings() {
        for (Iterator it = getBindings().entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            BindingValue b = (BindingValue) entry.getValue();

            if (b.getSlotIndex() == RU.LOCAL)
                it.remove();

            // Branch bindings, not active here
            else if (name.startsWith("_0"))
                it.remove();

            // Blank bindings, not active here
            else if (name.startsWith(Tokenizer.BLANK_PREFIX))
                it.remove();            
        }
    }

    void ready(Token fact_input, Context c) throws JessException {

        Fact fact;
        // set up the variable table
        for (Iterator e = getBindings().values().iterator(); e.hasNext();) {
            BindingValue b = (BindingValue) e.next();

            // You can get these from "not" patterns
            if (b.getFactNumber() > fact_input.size()) {
                e.remove();
                continue;
            }

            // Variables need info from a fact
            Value val;
            fact = fact_input.fact(b.getFactNumber());

            // if this is a "not" CE, skip it
            if (fact.getDeftemplate().equals(Deftemplate.getNullTemplate())) {
                e.remove();
                continue;
            }

            if (b.getSlotIndex() == RU.PATTERN) {
                val = new FactIDValue(fact.getIcon());
            } else {
                // Variables defined in an "accumulate" aren't available, except for the accumulated value
                if (fact.getDeftemplate().equals(Deftemplate.getAccumTemplate())) {
                    if (b.getSlotIndex() != 0 || b.getSubIndex() != -1) {
                        e.remove();
                        continue;
                    }
                }

                if (b.getSubIndex() == -1) {
                    val = fact.get(b.getSlotIndex());
                } else {
                    ValueVector vv = fact.get(b.getSlotIndex()).listValue(c);
                    val = vv.get(b.getSubIndex());
                }

            }
            c.setVariable(b.getName(), val);
        }
    }

    public void addNode(Node n) throws JessException {
        if (n == null)
            throw new JessException("HasLHS.addNode",
                              "Compiler fault",
                              "null Node added");
        else {
            for (int i = 0; i < m_nodes.size(); ++i)
                if (n == m_nodes.get(i))
                    return;

            appendCompilationTrace(n);
            n.incrementUseCount();
            m_nodes.add(n);
        }
    }

    /**
     * Completely remove this construct from the Rete network, including
     * removing any internal nodes that are only used by this construct.
     * @param root The roots of the Rete network where this construct lives.
     */

    void remove(Node root) {
        Iterator e = m_nodes.iterator();
        while (e.hasNext()) {
            Node s = (Node) e.next();
            if (s.decrementUseCount() <= 0) {
                // If it's a child of the root node, remove it
                root.removeSuccessor(s);
                Iterator e2 = m_nodes.iterator();
                while (e2.hasNext()) {
                    Node n = (Node) e2.next();
                    // If it's a child of any other node in the rule, remove it
                    n.removeSuccessor(s);
                }
            }
        }
        m_nodes.clear();
        if (getNext() != null)
            getNext().remove(root);
    }

    private void appendCompilationTrace(Node n) {
        if (m_compilationTrace == null)
            m_compilationTrace = new StringBuffer(m_name + ": ");

        if (n.getUseCount() == 0)
            m_compilationTrace.append("+");
        else
            m_compilationTrace.append("=");

        m_compilationTrace.append(n.getCompilationTraceToken());
    }

    String getCompilationTraceToken() {
        return "t";
    }

    StringBuffer getCompilationTrace() {
        return m_compilationTrace;
    }

    /**
     * Set the node-index-hash of this construct. The node-index-hash
     * value effects the indexing efficiency of the join nodes for this
     * construct. Larger values will make constructs with many partial
     * matches faster (to a point). Must be set before construct is
     * added to engine (so is typically set during parsing via the
     * equivalent Jess command.
     @param h The node index hash value
     */

    public void setNodeIndexHash(int h) {
        m_nodeIndexHash = h;
        if (getNext() != null)
            getNext().setNodeIndexHash(h);
    }

    /**
     * Get the node-index-hash setting of this construct.
     * @return The node-index-hash of this construct
     */
    public int getNodeIndexHash() {
        return m_nodeIndexHash;
    }

    /**
     * Fetch the name of this construct
     * @return The name of this construct
     */
    public final String getName() {
        return m_name;
    }

    /**
     * Fetch the display name of this construct
     * @return The display name of this construct
     */
    public String getDisplayName() {
        return m_displayName;
    }

    /**
     * Get the documentation string for this construct.
     * @return The docstring for this construct
     */
    public final String getDocstring() {
        return m_docstring;
    }

    // Compiler calls this after we've had initial update
    void setOld() {
        m_new = false;
        for (Iterator it = m_nodes.iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            if (node != this)
                node.setOld();
        }
    }

    public abstract Object accept(Visitor jv);

    public String getModule() {
        return m_module;
    }

    private int m_seqNum;

    int getSequenceNumber() {
        return m_seqNum;
    }

    public int getNodeType() {
        return TYPE_TERMINAL;
    }

    void callNodeRight(int tag, Token token, Context context) throws JessException {
        throw new JessException("callNodeRight", "Special node has no right input", getClass().getName());
    }

}



