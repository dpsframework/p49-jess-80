package jess;

import java.io.Serializable;

/**
 *  A tiny class to hold an individual test for a 2-input node to perform
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Test2Simple implements TestBase, Serializable {

    /**
       What test to do (true is EQ, false is NEQ)
    */

    private boolean m_test;

    boolean getTest() { return m_test; }

    /**
       Which fact within a token (0,1,2...)
    */

    private int m_tokenIdx;
    int getTokenIndex() { return m_tokenIdx; }
    /**
       Which field (absolute index of slot start) from the left memory
    */

    private int m_leftIdx;
    int getLeftIndex() { return m_leftIdx; }

    /**
       Which field (absolute index of slot start) from the right memory
    */

    private int m_rightIdx;
    int getRightIndex() { return m_rightIdx; }

    Test2Simple(int test, int tokenIdx, int leftIdx, int rightIdx) {
        m_test = (test == EQ);
        m_tokenIdx = tokenIdx;
        m_rightIdx = rightIdx;
        m_leftIdx = leftIdx;
    }

    public boolean equals(Object tt)  {
        if (! (tt instanceof Test2Simple))
            return false;

        Test2Simple t = (Test2Simple) tt;
        return  (m_test == t.m_test &&
                 m_tokenIdx == t.m_tokenIdx &&
                 m_rightIdx == t.m_rightIdx &&
                 m_leftIdx == t.m_leftIdx);
    }

    public boolean doTest(Context c)
        throws JessException {
        Token lt = c.getToken();
        Fact rf = c.getFact();
        Value v1 = lt.fact(m_tokenIdx).get(m_leftIdx);
        Value v2 = rf.get(m_rightIdx);
        return m_test == v1.equals(v2);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Test2Simple: test=");
        sb.append(m_test ? "EQ" : "NEQ");
        sb.append(";tokenIdx=");
        sb.append(m_tokenIdx);
        sb.append(";leftIdx=");
        sb.append(m_leftIdx);
        sb.append(";rightIdx=");
        sb.append(m_rightIdx);
        sb.append("]");

        return sb.toString();
    }

}








