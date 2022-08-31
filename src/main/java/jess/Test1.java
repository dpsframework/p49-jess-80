package jess;

import java.io.Serializable;

/** 
 * Holds a single test in a Pattern on the LHS of a Rule.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class Test1 implements TestBase, Serializable, Visitable, Cloneable {

    /**
       What test to do (Test1.EQ, Test1.NEQ, etc)
    */

    int m_test;


    /**
     * The name slot within the fact
     */

    String m_slotName;

    /**
       Which subslot within a multislot (0,1,2...)
    */

    int m_subIdx;

    /**
       The datum to test against
    */

    Value m_slotValue;

    /**
       AND or OR
    */
    int m_conjunction = RU.AND;

    /**
     * Create a single test.
     * @param test TestBase.EQ or TestBase.NEQ
     * @param sub_idx The subfield of a multislot
     * @param slot_value An object test against
     * @param conjunction RU.AND or RU.OR
     */
    public Test1(int test, String slot, int sub_idx, Value slot_value, int conjunction) {
        this(test, slot, sub_idx, slot_value);
        m_conjunction = conjunction;
    }
    /**
     * Create a single test.
     * @param test TestBase.EQ or TestBase.NEQ
     * @param slot_value An object test against
     * @param conjunction RU.AND or RU.OR
     */
    public Test1(int test, String slot, Value slot_value, int conjunction) {
        this(test, slot, -1, slot_value, conjunction);
    }

    public Test1(int test, String slot, int sub_idx, Value slot_value) {
        m_test = test;
        m_slotName = slot;
        m_subIdx = sub_idx;
        m_slotValue = slot_value;
    }

    public Test1(int test, String slot, Value slot_value) {
        this(test, slot, -1, slot_value);
    }

    Test1(Test1 t, Value slot_value) {
        m_test = t.m_test;
        m_slotName = t.m_slotName;
        m_subIdx = t.m_subIdx;
        m_conjunction = t.m_conjunction;
        m_slotValue = slot_value;
    }

    public Object clone() {
        try {
            Test1 t = (Test1) super.clone();
            if (t.m_slotValue instanceof FuncallValue) {
                t.m_slotValue = ((FuncallValue) m_slotValue).copy();
            }
            return t;
        } catch (CloneNotSupportedException cnse) {
            return null;
        } catch (JessException je) {
            return null;
        }
    }

    public int getTest() { return m_test; }
    public Value getValue() { return m_slotValue; }

    public int getMultiSlotIndex() { return m_subIdx; }
    public void setMultiSlotIndex(int subIndex) {
        m_subIdx = subIndex;
    }

    public int getConjunction() { return m_conjunction; }

    public boolean doTest(Context context) throws JessException {
        boolean retval;

        retval = m_slotValue.resolveValue(context).equals(Funcall.FALSE);

        switch (m_test) {
        case EQ:
            if (retval)
                return false;
            break;

        case NEQ:
            if (!retval)
                return false;
            break;

        }
        return true;
    }



    public boolean equals(Object o) {
        if (! (o instanceof Test1))
            return false;

        Test1 t = (Test1) o;
        if (m_test != t.m_test)
            return false;

        else if (m_subIdx != t.m_subIdx)
            return false;

        else if (m_conjunction != t.m_conjunction)
            return false;

        else return m_slotValue.equals(t.m_slotValue);
    }

    public Object accept(Visitor jv) {
        return jv.visitTest1(this);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Test1: test=");
        // TODO Add slot
        sb.append(m_test == NEQ ? "NEQ" : "EQ");
        sb.append(";sub_idx=");
        sb.append(m_subIdx);
        sb.append(";slot_value=");
        sb.append(m_slotValue);
        sb.append(";conjunction=");
        sb.append(m_conjunction);
        sb.append("]");

        return sb.toString();
    }

    public void setSlotName(String slotName) {
        m_slotName = slotName;
    }

    public String getSlotName() {
        return m_slotName;
    }

    boolean isRegularSlotTest() {
        return !RU.NO_SLOT.equals(m_slotName);
    }
}



