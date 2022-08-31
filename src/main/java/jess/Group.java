package jess;

import java.io.Serializable;
import java.util.*;

/**
 * <p>A group of patterns on the LHS of a rule, like an "and", "or", "not", "accumulate", or
 * other special CE.</p>
 * (C) 2013 Sandia Corporation
 */

public class Group implements ConditionalElement, ConditionalElementX, Serializable, Visitable {
    private String m_name;
    private boolean m_explicit, m_logical;
    CEVector m_data;
    private boolean m_unary = false;

    public static final String
            AND = "and", UNIQUE = "unique", EXPLICIT = "explicit",
    NOT = "not", EXISTS = "exists", TEST = "test", OR = "or",
    LOGICAL = "logical", FORALL = "forall", ACCUMULATE = "accumulate";

    private final static Pattern s_initialFactPattern =
            new Pattern(Deftemplate.getInitialTemplate());
    private final static Pattern s_logicalInitialFactPattern;

    static {
        s_logicalInitialFactPattern =
                new Pattern(Deftemplate.getInitialTemplate());
        s_logicalInitialFactPattern.setLogical();
    }

    public Object clone() {
        try {
            Group g = (Group) super.clone();
            g.m_data = new CEVector();
            for (int i = 0; i < m_data.size(); ++i) {
                g.m_data.add((ConditionalElementX) getConditionalElementX(i).clone());
            }
            return g;
        } catch (CloneNotSupportedException cnse) {
            throw new IllegalArgumentException();
        }
    }

    public Group(String name) throws JessException {
        m_data = new CEVector();
        m_name = name;

        if (m_name.equals(EXPLICIT)) {
            m_explicit = true;
            m_unary = true;
        } else if (m_name.equals(LOGICAL)) {
            m_logical = true;
            m_unary = false;
        } else if (m_name.equals(NOT)) {
            m_unary = true;
        } else if (m_name.equals(ACCUMULATE)) {
            m_unary = true;
        } else if (m_name.equals(EXISTS)) {
            throw new JessException("Group::Group", "Invalid CE name", EXISTS);
        }
    }

    public String getName() {
        return m_name;
    }

    public int getPatternCount() {
        if (isNegatedName(m_name))
            return 1;
        else {
            int sum = 0;
            for (int i = 0; i < m_data.size(); ++i)
                sum += m_data.get(i).getPatternCount();
            return sum;
        }
    }

    public final ConditionalElementX add(ConditionalElement g) throws JessException {
        return add((ConditionalElementX) g);
    }

    /** 
     * One important thing to note here is that the actual argument should
     * be considered "dead" after calling this method. You can't add group
     * A to group B, then add children to A; you must fully build A, then
     * add it to B.
     */

    public final ConditionalElementX add(ConditionalElementX newChild) throws JessException {
        verifyAdditionIsAllowed(newChild);

        if (newChild.getName().equals(TEST)) {
            if (m_name.equals(NOT)) {
                m_name = AND;
                m_unary = false;
                FuncallValue fv = (FuncallValue) ((Test1) (((Pattern) newChild).getTests().next())).m_slotValue;
                Funcall f = fv.funcallValue(null).negate();
                Pattern p = new Pattern(Deftemplate.getInitialTemplate());
                p.addTest(new Test1(Test1.EQ, RU.NO_SLOT, new FuncallValue(f)));
                return add(p);

            } else if (m_data.size() == 0 || m_name.equals(OR)) {
                newChild = wrapTestInInitialFactTemplate(newChild);
            } else {
                ConditionalElementX ce = m_data.get(m_data.size()-1);
                if (ce.getName().equals(ACCUMULATE) || ce.getName().equals(NOT)) {
                    newChild = wrapTestInInitialFactTemplate(newChild);

		} else if (ce.getName().equals(OR)) {
		    for (int i=0; i<ce.getGroupSize(); ++i) {
			ConditionalElement elem = ce.getConditionalElement(i);
			if (elem instanceof Group) {
			    ((Group) elem).add(newChild);
			} else {
			    Pattern p = (Pattern) elem;
			    p.addTest(new Test1(Test1.EQ, RU.NO_SLOT, ((Test1) (((Pattern) newChild).getTests().next())).m_slotValue));
			}
		    }
		    return this;
                } else {
                    // TODO This probably still isn't always right
                    Pattern p = findLastPattern();
                    p.addTest(new Test1(Test1.EQ, RU.NO_SLOT, ((Test1) (((Pattern) newChild).getTests().next())).m_slotValue));
                    return this;
                }
            }
        }

        // Remove single-branch ORs if added to anything.
        if (isOrWithSingleBranch(newChild))
            newChild = newChild.getConditionalElementX(0);

        // Remove single-branch ANDs if added to anything,
        // unless they are being added to an OR CE
        if (isSingleBranchAnd(newChild) && !getName().equals(OR))
            newChild = newChild.getConditionalElementX(0);

        // Flatten nested ORs immediately.
        if (m_name.equals(OR) && newChild.getName().equals(OR)) {
            Group grp = (Group) newChild;
            for (int i = 0; i < grp.m_data.size(); i++)
                add(grp.m_data.get(i));
        }

        // Flatten nested ANDs immediately.
        else if (m_name.equals(AND) && newChild.getName().equals(AND)) {
            Group grp = (Group) newChild;
            for (int i = 0; i < grp.m_data.size(); i++)
                add(grp.m_data.get(i));
        }

        // Rearrange an (OR) inside of a (NOT)
        // According to (not (or A B)) => (and (not A) (not B))
        else if (m_name.equals(NOT) && newChild.getName().equals(OR)) {
            m_name = AND;
            m_unary = false;
            Group grp = (Group) newChild;
            for (int i = 0; i < grp.m_data.size(); i++) {
                Group not = new Group(NOT);
                not.add(grp.m_data.get(i));
                add(not);
            }
        }

        // If we're a NOT, and the new addition has an ORs in it,
        // they should be canonicalized.
        else if (m_name.equals(NOT) && newChild.getName().equals(AND) && hasEmbeddedORs((Group) newChild)) {
            newChild = newChild.canonicalize();
            add(newChild);
        }

        // Otherwise keep the tree-like structure
        else
            m_data.add(newChild);


        if (m_explicit)
            setExplicit();

        if (m_logical)
            setLogical();

        if (getNegated())
            setNegated();

        return this;
    }

    private ConditionalElementX wrapTestInInitialFactTemplate(ConditionalElementX newChild) throws JessException {
        Pattern p = new Pattern(Deftemplate.getInitialTemplate());
        p.addTest(new Test1(Test1.EQ, RU.NO_SLOT, ((Test1) (((Pattern) newChild).getTests().next())).m_slotValue));
        newChild = p;
        return newChild;
    }

    private Pattern findLastPattern() {
        PatternIterator it = new PatternIterator(this);
        Pattern p = null;
        while (it.hasNext()) {
            p = (Pattern) it.next();
        }
        return p;
    }

    private boolean isSingleBranchAnd(ConditionalElementX newChild) {
        return newChild.getName().equals(AND) && newChild.getGroupSize() == 1;
    }

    private boolean isOrWithSingleBranch(ConditionalElementX newChild) {
        return newChild.getName().equals(OR) && newChild.getGroupSize() == 1;
    }

    private boolean hasEmbeddedORs(Group g) {
        if (g.getName().equals(OR))
            return true;
        for (int i = 0; i < g.getGroupSize(); ++i) {
            ConditionalElementX ce = g.getConditionalElementX(i);
            if (ce instanceof Group)
                if (hasEmbeddedORs((Group) ce))
                    return true;
        }
        return false;
    }

    private void verifyAdditionIsAllowed(ConditionalElementX g) throws JessException {
        if (m_data.size() > 0 && m_unary)
            throw new JessException("Group.add",
                    "CE is a unary modifier", m_name);

        else if (m_name.equals(NOT) && g.getName().equals(LOGICAL))
            throw new JessException("Group.add",
                    "CE can't be used in not:", "logical");

    }

    public boolean getBackwardChaining() {
        return false;
    }

    public void setExplicit() throws JessException {
        for (int i = 0; i < m_data.size(); i++)
            m_data.get(i).setExplicit();
        m_explicit = true;
    }

    public void setLogical() throws JessException {
        for (int i = 0; i < m_data.size(); i++)
            m_data.get(i).setLogical();
        m_logical = true;
    }

    public boolean getLogical() {
        return m_logical;
    }

    public boolean getNegated() {
        return m_name.equals(NOT);
    }

    public void setNegated() throws JessException {
        for (int i = 0; i < m_data.size(); i++)
            m_data.get(i).setNegated();
    }

    public void setBoundName(String name) throws JessException {
        if (m_name.equals(NOT) ||
                m_name.equals(TEST) ||
                (m_data.size() > 1 && !m_name.equals(OR)))
            throw new JessException("Group.setBoundName",
                    "This CE can't be bound to a variable",
                    m_name);

        for (int i = 0; i < m_data.size(); i++)
            m_data.get(i).setBoundName(name);
    }

    public String getBoundName() {
        if (m_data.size() > 0)
            return m_data.get(0).getBoundName();
        else
            return null;
    }

    static boolean isGroupName(String s) {
        return
                isNegatedName(s) ||
                s.equals(AND) ||
                s.equals(OR) ||
                s.equals(LOGICAL) ||
                s.equals(EXPLICIT) ||
                s.equals(ACCUMULATE);
    }

    public static boolean isNegatedName(String s) {
        return s.equals(NOT);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("(");
        sb.append(m_name);
        for (int i = 0; i < m_data.size(); i++) {
            sb.append('\n');
            sb.append(' ');
            sb.append(m_data.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    private int countNumberOfBranches(ConditionalElementX[] lhss) throws JessException {

        for (int i = 0; i < lhss.length; i++)
            lhss[i] = m_data.get(i).canonicalize();

        int product;
        if (m_name.equals(OR)) {
            product = 0;
            for (int i = 0; i < lhss.length; i++) {
                product += lhss[i].getGroupSize();
            }
        } else {
            product = 1;
            for (int i = 0; i < lhss.length; i++) {
                product *= lhss[i].getGroupSize();
            }
        }
        return product;
    }


    public ConditionalElementX canonicalize() throws JessException {

        ConditionalElementX[] lhss = new ConditionalElementX[m_data.size()];
        int product = countNumberOfBranches(lhss);

        if (product == 1) {
            Group g = new Group(OR);
            g.add(this);
            return g;
        }

        // Each element in rv is a new LHS being built.
        Group[] branches = new Group[product];

        for (int i = 0; i < product; i++)
            branches[i] = new Group(AND);

        if (m_name.equals(OR)) {
            // If we're an OR, then we're just the OR of all the children.
            // Each child in lhss is just a list of one or more OR-options
            int index = 0;
            for (int i = 0; i < lhss.length; i++)
                for (int j = 0; j < lhss[i].getGroupSize(); j++)
                    branches[index++].add(lhss[i].getConditionalElementX(j));
        } else {
            // Otherwise, it's a little more complex. We need to
            // distribute our OR children. "Repeat" is the number of
            // consecutive LHSs in the list a given sub-OR element
            // will be added to. It will get smaller and smaller and
            // end up as 1 for the last OR.

            int copies = product;
            for (int i = 0; i < lhss.length; i++) {
                // If there's only one option in a branch, add it to every LHS.
                if (lhss[i].getGroupSize() == 1) {
                    for (int j = 0; j < product; j++)
                        branches[j].add(lhss[i].getConditionalElementX(0));
                }

                // If there are multiple options, we need to spread them
                // out.  We'll make "copies" copies of each option,
                // repeating the overall pattern "repeat" times. Each time
                // we encounter another OR in lhss, we divide "copies" by
                // the number of branches. If lhss looks like
                //
                //  A C D
                //  B   E
                //
                // Then the first and third entries are ORs, and we
                // want rv to look like
                //
                //  A A B B  copies == 2, repeat == 1
                //  C C C C
                //  D E D E  copies == 1, repeat == 2

                else {
                    copies /= lhss[i].getGroupSize();
                    int repeat = product / (copies * lhss[i].getGroupSize());
                    int index = 0;
                    for (int j = 0; j < repeat; j++)
                        for (int k = 0; k < lhss[i].getGroupSize(); k++)
                            for (int m = 0; m < copies; m++)
                                branches[index++].add(lhss[i].getConditionalElementX(k));
                }
            }
        }

        Group or = new Group(OR);
        for (int i = 0; i < product; ++i) {

            if (branches[i].getGroupSize() == 1 &&
                    !branches[i].getConditionalElementX(0).getName().equals(NOT) &&
                    !branches[i].getConditionalElementX(0).getName().equals(TEST))
                or.add(branches[i].getConditionalElementX(0));
            else
                or.add(branches[i]);
        }

        if (m_name.equals(NOT)) {
            Group not = new Group(NOT);
            not.add(or);
            return not.canonicalize();
        }

        return or;

    }

    void insertInitialFacts() {
        ConditionalElementX g0 = getConditionalElementX(0);
        if (needsPrecedingInitialFact(g0)) {

            m_data.addAtStart(g0 == null || !g0.getLogical() ?
                    (Pattern) s_initialFactPattern.clone() :
                    (Pattern) s_logicalInitialFactPattern.clone());
        }
    }

    private boolean needsPrecedingInitialFact(ConditionalElementX g0) {
        if (g0 == null)
            return true;

        else {
            final String name = g0.getName();
            return name.equals(NOT) ||
                    name.equals(TEST) ||
                    name.equals(ACCUMULATE) ||
                    g0.getBackwardChaining() ||
                    (name.equals(LOGICAL) && needsPrecedingInitialFact(g0.getConditionalElementX(0)));
        }
    }

    // This is called on a CE early in a LHS, to rename variables found
    // in a CE being added to the LHS.
    void renameVariables(ConditionalElementX g, int sequenceNumber, String groupPrefix) throws JessException {
        Set set = new HashSet();
        HashMap subs = new HashMap();
        addDirectlyMatchedVariables(set);
        g.renameUnmentionedVariables(set, subs, sequenceNumber, groupPrefix);
    }

    public void addToGroup(Group g) throws JessException {
        insertInitialFacts();

        for (int i = 0; i < m_data.size(); i++) {
            ConditionalElementX ce = m_data.get(i);
            g.m_data.add((ConditionalElementX) ce.clone());
        }
    }

    public int getGroupSize() {
        return m_data.size();
    }

    public boolean isGroup() {
        return true;
    }

    public ConditionalElement getConditionalElement(int i) {
        return getConditionalElementX(i);
    }

    public ConditionalElementX getConditionalElementX(int i) {
        return m_data.get(i);
    }

    public void addDirectlyMatchedVariables(Set set) throws JessException {
        for (int i = 0; i < m_data.size(); i++)
            m_data.get(i).addDirectlyMatchedVariables(set);
    }

    public int renameUnmentionedVariables(Set set, Map subs, int sequenceNumber, String groupPrefix)
            throws JessException {
        if (isNegatedName(m_name)) {
            subs = new HashMap(subs);
            ++sequenceNumber;
            groupPrefix = groupPrefix + "0";
        }
        for (int i = 0; i < m_data.size(); i++) {
            sequenceNumber = m_data.get(i).renameUnmentionedVariables(set, subs, sequenceNumber, groupPrefix);
        }
        return sequenceNumber;
    }

    public void recordTestedSlots(Set testedSlots) throws JessException {
        for (int i=0; i<m_data.size(); ++i) {
            ConditionalElementX element = m_data.get(i);
            element.recordTestedSlots(testedSlots);
        }
    }

    public boolean isBackwardChainingTrigger() {
        return false;
    }

    public Object accept(Visitor v) {
        return v.visitGroup(this);
    }

    /*
     * The argument could be a nested group -- a (NOT (AND)). In that
     * case, the indexes used for pattern binding should start with
     * the normal CE index and increment from there, but without affecting
     * the "size" of this LHS.These indexes will be used off on a "branch"
     * of the Rete network.
     */

    public void findVariableDefinitions(int startIndex, Map bindingsSoFar, Map newBindings)
            throws JessException {
        for (int i=0; i<m_data.size(); ++i) {
            ConditionalElementX element = m_data.get(i);
            element.findVariableDefinitions(startIndex, bindingsSoFar, newBindings);
            startIndex += element.getPatternCount();
        }
    }

    /*
     * See comment for findVariableDefinitions() -- the same thing is going on here.
     */                                                                            
    public void transformOrConjunctionsIntoOrFuncalls(int startIndex, Map bindings, Rete engine) throws JessException {
        for (int i=0; i<m_data.size(); ++i) {
            ConditionalElementX element = m_data.get(i);
            element.transformOrConjunctionsIntoOrFuncalls(startIndex, bindings, engine);
            startIndex += element.getPatternCount();
        }
    }

    static class CEVector implements Serializable {
        private ConditionalElementX[] m_data = new ConditionalElementX[1];
        private int m_nData;

        void addAtStart(ConditionalElementX g) {
            if (m_data.length == m_nData) {
                ConditionalElementX[] temp = new ConditionalElementX[m_nData * 2];
                System.arraycopy(m_data, 0, temp, 0, m_nData);
                m_data = temp;
            }
            System.arraycopy(m_data, 0, m_data, 1, m_nData);
            m_data[0] = g;
            ++m_nData;
        }

        void add(ConditionalElementX g) {

            if (m_data.length == m_nData) {
                ConditionalElementX[] temp = new ConditionalElementX[m_nData * 2];
                System.arraycopy(m_data, 0, temp, 0, m_nData);
                m_data = temp;
            }
            m_data[m_nData++] = g;
        }

        ConditionalElementX get(int i) {
            return m_data[i];
        }


        int size() {
            return m_nData;
        }
    }
}








