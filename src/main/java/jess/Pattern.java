package jess;
        

import java.io.Serializable;
import java.util.*;

/**
 * <p>A Pattern represents a single conditional element on a rule LHS.
 * A Pattern consists mainly of a two-dimensional array of Test1 structures.
 * Each Test1 contains information about a specific characteristic of a slot.
 * </P>
 * <p>Most users will not use this class directly, but you could use it to
 * build rules directly in the Java language.
 * </p>
 * (C) 2013 Sandia Corporation<br>
 */

public class Pattern implements ConditionalElement, ConditionalElementX, Serializable, Visitable {

    /**
     * The deftemplate corresponding to this pattern
     */

    private Deftemplate m_deft;

    public Object clone() {
        try {
            Pattern p = (Pattern) super.clone();
            if (m_slotLengths != null)
                p.m_slotLengths = (int[]) m_slotLengths.clone();
            p.m_allTests = new ArrayList<Test1>();
            for (Iterator<Test1> it = m_allTests.iterator(); it.hasNext();) {
                p.m_allTests.add((Test1) ((Test1) it.next()).clone());
            }
            Collections.sort(p.m_allTests, new TestComparator());
            return p;

        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /* The number of sub-fields in each multislot */
    private int m_slotLengths[];
    
    /* Am I in a (not () ) ? */
    private boolean m_negated;

    /* Do I provide logical support? */
    private boolean m_logical;

    /* Only match explicitly, no backwards chaining */
    private boolean m_explicit;

    /* Class of fact matched by this pattern */
    private String m_name;

    /* Bound to a variable if non-null */
    private String m_boundName;

    private ArrayList<Test1> m_allTests;

    public Pattern(String name, Rete engine)
            throws JessException {

        this(engine.createDeftemplate(name));
    }

    public Pattern(Deftemplate deft) {
        m_name = deft.getName();
        m_deft = deft;

        int nvalues = m_deft.getNSlots();
        m_allTests = new ArrayList<Test1>();
        m_slotLengths = new int[nvalues];
        for (int i = 0; i < nvalues; i++)
            m_slotLengths[i] = -1;
    }

    /* Creates a new Pattern which shares some data, but with a new name.
       Used by backchaining stuff. */

    Pattern(Pattern p, String name) throws JessException {
        m_name = name;
        m_deft = p.m_deft;

        // We need to copy the tests and replace any blank variables with
        // new blanks (with new names.)
        // TODO What if a blank appears more than once? We need to use a rename map here
        m_allTests = (ArrayList<Test1>) p.m_allTests.clone();
        for (int i=0; i<m_allTests.size(); ++i) {
            Test1 test = (Test1) m_allTests.get(i);
            Value v = test.m_slotValue;
            if (v instanceof Variable && v.variableValue(null).startsWith(Tokenizer.BLANK_PREFIX)) {
                test = new Test1(test, new Variable(RU.gensym(Tokenizer.BLANK_PREFIX), v.type()));
                m_allTests.set(i, test);
            }
        }
        Collections.sort(m_allTests, new TestComparator());
        m_slotLengths = p.m_slotLengths;
    }

    /**
     * Set the length of a multislot within a pattern
     */
    public void setSlotLength(String slotname, int length)
            throws JessException {
        int index = m_deft.getSlotIndex(slotname);
        if (index == -1)
            throw new JessException("Pattern.setSlotLength",
                    "No such slot " + slotname + " in template",
                    m_deft.getName());

        m_slotLengths[index] = length;
    }

    /**
     * Add a test to this pattern
     */
    public void addTest(Test1 aTest) throws JessException {

        String slotName = aTest.m_slotName;

        // try to find this slotName in the deftemplate
        if (!RU.NO_SLOT.equals(slotName)) {
            int idx = m_deft.getSlotIndex(slotName);
            if (idx == -1) {
                throw new JessException("Pattern.addTest",
                        "No such slot " + slotName + " in template ",
                        m_deft.getName());
            }
        }
        m_allTests.add(aTest);
        Collections.sort(m_allTests, new TestComparator());
    }

    // Used by "replaceOrConjunctionsWithOrFuncalls"
    void replaceTests(String slotName, Test1[] theTests) {
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName))
                it.remove();
        }
        for (int i = 0; i < theTests.length; i++) {
            m_allTests.add(theTests[i]);
        }
        Collections.sort(m_allTests, new TestComparator());
    }

    /* Add the name of every variable that is "directly positively
       matched" in this pattern to the Map as a key/value pair. If a
       name is already there, that's OK. */

    public void addDirectlyMatchedVariables(Set set) throws JessException {
        for (Iterator<Test1> it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
                Value val = test.m_slotValue;
                if (val instanceof Variable && test.m_test == Test1.EQ) {
                    String name = val.variableValue(null);
                    set.add(name);
                }
        }
        if (getBoundName() != null)
            set.add(getBoundName());
    }

     public int renameUnmentionedVariables(Set set, Map substitutes, final int seqNum, final String groupPrefix) throws JessException {
         String preFix = new StringBuffer("_").append(groupPrefix).append(seqNum).append("_").toString();
         for (Iterator it = m_allTests.iterator(); it.hasNext();) {
             Test1 test = (Test1) it.next();
             Value val = test.m_slotValue;
             if (val instanceof Variable) {
                 String name = val.variableValue(null);
                 if (!Defglobal.isADefglobalName(name) && !set.contains(name) && !name.startsWith(preFix)) {
                     String sub;
                     if (substitutes.get(name) == null) {
                         sub = preFix + name;
                         substitutes.put(name, sub);
                     } else
                         sub = (String) substitutes.get(name);
                     test.m_slotValue = new Variable(sub, val.type());
                 }
             }
         }
         substituteVariableNamesInFuncalls(substitutes);
         return seqNum;
    }



    // TODO This needs to be smarter. If the test is just a binding
    // TODO and it's unused elsewhere, then we shouldn't consider it
    // TODO tested here
    
    public void recordTestedSlots(Set testedSlots) throws JessException {
        for (int slot = 0; slot < getNSlots(); ++slot) {
            if (anyTestsForSlot(slot))
                testedSlots.add(new TestedSlot(m_deft, slot));
        }
    }

    private boolean anyTestsForSlot(int slot) throws JessException {
        String slotName = m_deft.getSlotName(slot);
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName))
                return true;
        }
        return false;
    }

    private void substituteVariableNamesInFuncalls(Map substitutes) throws JessException {
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            Value val = test.m_slotValue;
            if (val.type() == RU.FUNCALL) {
                substFuncall(val.funcallValue(null), substitutes);
            }
        }
    }

    private void substFuncall(Funcall f, Map substitutes) throws JessException {
        for (int i = 1; i < f.size(); i++) {
            Value current = f.get(i);
            if (current instanceof Variable) {
                String s = (String) substitutes.get(current.variableValue(null));
                if (s != null)
                    f.set(new Variable(s, current.type()), i);
            } else if (current instanceof FuncallValue)
                substFuncall(current.funcallValue(null), substitutes);
        }
    }

    public void findVariableDefinitions(final int startIndex, Map bindingsSoFar, Map newBindings)
            throws JessException {

        Collections.sort(m_allTests, new TestComparator());
       
        if (m_boundName != null) {
            
            if (bindingsSoFar.get(m_boundName) == null) {
                BindingValue binding = new BindingValue(m_boundName, this, startIndex, RU.PATTERN, -1, RU.FACT);
                newBindings.put(m_boundName, binding);
            }
        }

        Deftemplate dt = getDeftemplate();
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            Value val = test.m_slotValue;
            boolean eq = (test.m_test == Test1.EQ);
            if (val instanceof Variable) {
                String name = val.variableValue(null);
                if (bindingsSoFar.get(name) == null && newBindings.get(name) == null) {
                    // Test for defglobal
                    if (Defglobal.isADefglobalName(name))
                        continue;

                    // If the test is positive, this is the
                    // definition of this variable, whether it's
                    // first or not, and whether it's defined in a
                    // "not" or not.

                    if (eq) {
                        int slotIndex = m_deft.getSlotIndex(test.m_slotName);
                        int type = dt.getSlotDataType(slotIndex);
                        newBindings.put(name,
                                new BindingValue(name,
                                        this,
                                        startIndex,
                                        slotIndex,
                                        test.m_subIdx,
                                        type));
                    }

                }
            }
        }
    }

    /**
     * Is this pattern a (not()) CE pattern, possibly nested?
     */
    public boolean getNegated() {
        return m_negated;
    }

    public void setNegated() {
        m_negated = true;
    }

    public void setLogical() {
        m_logical = true;
    }

    public boolean getLogical() {
        return m_logical;
    }

    public void setExplicit() {
        m_explicit = true;
    }

    public boolean getExplicit() {
        return m_explicit;
    }

    public boolean getBackwardChaining() {
        return m_deft.getBackwardChaining();
    }

    public String getName() {
        if (m_name.equals("MAIN::test"))
            return Group.TEST;
        else
            return m_name;
    }

    public void setBoundName(String s) throws JessException {
        if ((m_negated || m_name.equals("test")) && s != null)
            throw new JessException("Pattern.setBoundName",
                    "Can't bind negated pattern to variable",
                    s);
        m_boundName = s;
    }

    public String getBoundName() {
        if (isBackwardChainingTrigger() && m_boundName == null)
            m_boundName = RU.gensym("__factidx");
        return m_boundName;
    }

    public int getNSlots() {
        return m_deft.getNSlots();
    }

    public int getNTests(int slot) throws JessException {
        int count = 0;
        String slotName = m_deft.getSlotName(slot);
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName))
                ++count;
        }
        return count;
    }

    public int getNTests() {
        return m_allTests.size();
    }


    public int getSlotLength(int slot) {
        return m_slotLengths[slot];
    }

    public int getNMultifieldsInSlot(int slot) throws JessException {
        int count = 0;
        String slotName = m_deft.getSlotName(slot);
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName) && test.m_slotValue.type() == RU.MULTIVARIABLE)
                ++count;
        }
        return count;
    }

    public boolean isMultifieldSubslot(int slot, int subslot) throws JessException {
        String slotName = m_deft.getSlotName(slot);
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName)) {
                if (test.m_subIdx == subslot && test.m_slotValue.type() == RU.MULTIVARIABLE)
                    return true;
            }
        }
        return false;
    }

    boolean[] getMultifieldFlags(int slot) throws JessException {
        boolean[] indexes = new boolean[getSlotLength(slot)];

        for (int i = 0; i < getSlotLength(slot); i++)
            if (isMultifieldSubslot(slot, i))
                indexes[i] = true;

        return indexes;
    }

    public Iterator getTests() {
        return m_allTests.iterator();
    }

    public Iterator getTests(int slot) throws JessException {
        final String name = m_deft.getSlotName(slot);
        ArrayList list = new ArrayList();
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (name.equals(test.m_slotName))
                list.add(test);
        }
        return list.iterator();
    }

    public Iterator getTests(String slotName) throws JessException {
        ArrayList list = new ArrayList();
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (slotName.equals(test.m_slotName))
                list.add(test);
        }
        return list.iterator();
    }

    public Deftemplate getDeftemplate() {
        return m_deft;
    }

    public void addToGroup(Group g) throws JessException {
        g.m_data.add((Pattern) clone());
    }

    public ConditionalElementX canonicalize() {
        return this;
    }

    public Object accept(Visitor jv) {
        return jv.visitPattern(this);
    }

    public int getGroupSize() {
        return 1;
    }

    public boolean isGroup() {
        return false;
    }

    public ConditionalElement getConditionalElement(int i) {
        return getConditionalElementX(i);
    }

    public int getPatternCount() {
        return 1;
    }


    public ConditionalElementX getConditionalElementX(int i) {
        if (i > 0)
            throw new IllegalArgumentException();
        else
            return this;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Pattern))
            return false;

        Pattern p = (Pattern) o;
        if (!getName().equals(p.getName()))
            return false;

        if (m_negated != p.m_negated)
            return false;

        for (int i = 0; i < m_slotLengths.length; ++i) {
            if (m_slotLengths[i] != p.m_slotLengths[i])
                return false;
        }

        if (m_allTests.size() != p.m_allTests.size())
            return false;

        Collections.sort(m_allTests, new TestComparator());
        Collections.sort(p.m_allTests, new TestComparator());
        return m_allTests.equals(p.m_allTests);

    }

    public boolean isBackwardChainingTrigger() {
        return !m_negated && m_deft.isBackwardChainingTrigger();
    }

    public String getBackchainingTemplateName() {
        return m_deft.getBackchainingTemplateName();
    }

    public String getNameWithoutBackchainingPrefix() {
        return m_deft.getNameWithoutBackchainingPrefix();
    }

    public String toString() {
        return "(" + getName() + ")";
    }

    public void transformOrConjunctionsIntoOrFuncalls(int startIndex, Map bindings, Rete engine) throws JessException {
        Deftemplate dt = getDeftemplate();

        for (int i = 0; i < getNSlots(); i++) {
            Test1[] testArr = getTestsAsArray(i);
            int nTests = testArr.length;

            if (nTests == 0)
                continue;

            // Rearrange the tests
            ArrayList tests = new ArrayList();
            String slotName = dt.getSlotName(i);
            int currentSubIndex = testArr[0].m_subIdx;
            int doneIdx = 0;

            // This is a loop over sub-indexes in the test array. doneIdx
            // will be incremented inside the loop, and some constructs
            // will break out to this label.
            subIdxLoop:
            while (doneIdx < nTests) {
                // Find out if there are any ORs on this subslot
                boolean hasOrs = false;
                for (int j = doneIdx; j < nTests; j++) {
                    Test1 aTest = testArr[j];
                    if (aTest.m_subIdx != currentSubIndex)
                        break;
                    else if (aTest.m_conjunction == RU.OR) {
                        hasOrs = true;
                        break;
                    }
                }

                // If no ORs on this subslot, just copy tests into ArrayList
                if (!hasOrs) {
                    Test1 aTest;
                    for (int j = doneIdx; j < nTests; j++) {
                        aTest = testArr[j];
                        if (aTest.m_subIdx != currentSubIndex) {
                            currentSubIndex = aTest.m_subIdx;
                            continue subIdxLoop;
                        } else {
                            tests.add(aTest);
                            ++doneIdx;
                        }
                    }
                    continue;
                }


                // First find a variable to represent this (sub)slot; we
                // may have to create one
                Value var;
                Test1 firstTest = testArr[doneIdx];
                Value testValue = firstTest.m_slotValue;

                if (isAVariableDefinition(testValue, i, bindings)) {
                    var = testValue;
                    ++doneIdx;
                } else {
                    String name = RU.gensym(Tokenizer.BLANK_PREFIX);
                    var = new Variable(name, RU.VARIABLE);
                    bindings.put(name,
                            new BindingValue(name,
                                    this,
                                    startIndex,
                                    i, currentSubIndex,
                                    dt.getSlotDataType(i)));
                }

                tests.add(new Test1(TestBase.EQ, slotName, currentSubIndex, var));

                // We're going to build up this function call
                Funcall or = new Funcall("or", engine);

                // Count how many tests until an OR, so we can omit the
                // AND if not needed
                while (true) {
                    int andCount = 1;
                    for (int j = doneIdx + 1; j < nTests; j++) {
                        Test1 aTest = testArr[j];
                        if (aTest.m_conjunction == RU.OR ||
                                aTest.m_subIdx != currentSubIndex)
                            break;
                        else
                            ++andCount;
                    }

                    if (andCount == 1) {
                        or.add(testToFuncall(testArr[doneIdx],
                                var, engine));
                    } else {
                        Funcall and = new Funcall("and", engine);
                        for (int j = doneIdx; j < doneIdx + andCount; j++)
                            and.add(testToFuncall(testArr[j],
                                    var, engine));
                        or.add(new FuncallValue(and));
                    }

                    doneIdx += andCount;

                    if (doneIdx == nTests)
                        break;
                    else if (testArr[doneIdx].m_subIdx != currentSubIndex)
                        break;
                }
                tests.add(new Test1(TestBase.EQ, slotName, currentSubIndex, new FuncallValue(or)));

                if (doneIdx < nTests &&
                        testArr[doneIdx].m_subIdx != currentSubIndex)
                    currentSubIndex = testArr[doneIdx].m_subIdx;
            }

            Test1[] testArray = new Test1[tests.size()];
            for (int j = 0; j < testArray.length; j++)
                testArray[j] = (Test1) tests.get(j);
            replaceTests(slotName, testArray);
        }
    }

    private  Test1[] getTestsAsArray(int i) throws JessException {
        ArrayList list = new ArrayList();
        for (Iterator it = getTests(i); it.hasNext();) {
            list.add(it.next());
        }
        return (Test1[]) list.toArray(new Test1[list.size()]);
    }

    private boolean isAVariableDefinition(Value testValue,
                                          int slot,
                                          Map bindings)
            throws JessException {

        if (testValue.type() != RU.VARIABLE)
            return false;

        String name = testValue.variableValue(null);
        if (Defglobal.isADefglobalName(name))
            return false;

        BindingValue bv = (BindingValue) bindings.get(name);

        return (bv.getCE() == this && bv.getSlotIndex() == slot);
    }

    /**
     *  Given a test, create an implied function call that does it
     */
    private Value testToFuncall(Test1 t, Value var, Rete engine)
            throws JessException {
        Value v = t.m_slotValue;
        switch (t.m_slotValue.type()) {
            case RU.FUNCALL:
                {
                    if (t.m_test == TestBase.NEQ)
                        return new FuncallValue(new Funcall("not", engine).arg(v));
                    else
                        return v;
                }
            default:
               {
                   return new FuncallValue(new Funcall(t.m_test == TestBase.EQ ? "eq" : "neq", engine).arg(v).arg(var));
               }
        }
    }
    
    Variable findAnyExistingVariable(int idx, int subidx) throws JessException {
        String slotName = m_deft.getSlotName(idx);
        for (Iterator it = m_allTests.iterator(); it.hasNext();) {
            Test1 test = (Test1) it.next();
            if (test.getTest() == Test1.EQ && slotName.equals(test.m_slotName)) {
                Value var = test.getValue();
                if (var.type() == RU.VARIABLE && test.m_subIdx == subidx) {
                    return (Variable) var;
                }
            }
        }
        return null;
    }

    Variable getVariable(int idx, int subidx) throws JessException {
        Variable v = findAnyExistingVariable(idx, subidx);
        if (v == null) {
            String name = RU.gensym("__synth");
            v = new Variable(name, RU.VARIABLE);
            Test1 test = new Test1(Test1.EQ, m_deft.getSlotName(idx), subidx, v);
            addTest(test);
        }
        return v;
    }

    // TODO Should sort NO_SLOT to the ***end*** of the tests for this pattern!
    class TestComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Test1 t1 = (Test1) o1;
            Test1 t2 = (Test1) o2;
            int i1 = m_deft.getSlotIndex(t1.m_slotName);
            int i2 = m_deft.getSlotIndex(t2.m_slotName);

            if (i1 == -1 && i2 != -1)
                return 1;
            else if (i2 == -1 && i1 != -1)
                return -1;

            int comp =  i1 - i2;
            if (comp != 0) {
                return comp;
            } else {
                return t1.m_subIdx - t2.m_subIdx;
            }
        }
    }
}


