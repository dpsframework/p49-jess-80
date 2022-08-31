
package jess;
import java.io.Serializable;

/** 
 *  A tiny class to hold an individual test for a 2-input node to perform
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */
class Test2Multi implements TestBase, Serializable {

  /**
    What test to do (Test2.EQ, Test2.NEQ, etc)
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
    Which subfield from the left memory
   */

  private int m_leftSubIdx;
  int getLeftSubIndex() { return m_leftSubIdx; }

  /**
    Which field (absolute index of slot start) from the right memory
   */

  private int m_rightIdx;
  int getRightIndex() { return m_rightIdx; }

  /**
    Which subfield from the right memory
   */

  int m_rightSubIdx;
  int getRightSubIndex() { return m_rightSubIdx; }

  /**
   * Constructors
   * @param test
   * @param tokenIdx
   * @param leftIdx
   * @param leftSubIdx
   * @param rightIdx
   * @param rightSubIdx
   */
  Test2Multi(int test, int tokenIdx, int leftIdx,
             int leftSubIdx, int rightIdx, int rightSubIdx)
  {
    m_test = (test == EQ);
    m_tokenIdx = tokenIdx;
    m_rightIdx = rightIdx;
    m_rightSubIdx = rightSubIdx;
    m_leftIdx = leftIdx;
    m_leftSubIdx = leftSubIdx;
  }

  /**
   * @param tt
   * @return
   */
  public boolean equals(Object tt)
  {
    if (! (tt instanceof Test2Multi))
      return false;

    Test2Multi t = (Test2Multi) tt;

    return  (m_test == t.m_test &&
             m_tokenIdx == t.m_tokenIdx &&
             m_rightIdx == t.m_rightIdx &&
             m_leftIdx == t.m_leftIdx &&
             m_rightSubIdx == t.m_rightSubIdx &&
             m_leftSubIdx == t.m_leftSubIdx);
  }

  public boolean doTest(Context c)
       throws JessException
  {
    Token lt = c.getToken();

    ValueVector lf = lt.fact(m_tokenIdx);

    if (lf == null) {
        return true;
    }

    ValueVector rf = c.getFact();

    Value v1;

    if (m_leftSubIdx != -1)  // i.e., first variable is in a multislot
      v1 = lf.get(m_leftIdx).listValue(null).get(m_leftSubIdx);
    else
      v1 = lf.get(m_leftIdx);

    boolean retval;

    if (m_rightSubIdx != -1)  // i.e., first variable is in a multislot
      retval = v1.equals(rf.get(m_rightIdx).listValue(null).get(m_rightSubIdx));
    else
      retval = v1.equals(rf.get(m_rightIdx));

    return (retval == m_test);
  }

  /**
   * @return
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Test2Multi: test=");
    sb.append(m_test? "EQ" : "NEQ");
    sb.append(";tokenIdx=");
    sb.append(m_tokenIdx);
    sb.append(";leftIdx=");
    sb.append(m_leftIdx);
    sb.append(";leftSubIdx=");
    sb.append(m_leftSubIdx);
    sb.append(";rightIdx=");
    sb.append(m_rightIdx);
    sb.append(";rightSubIdx=");
    sb.append(m_rightSubIdx);
    sb.append("]");

    return sb.toString();
  }

}


