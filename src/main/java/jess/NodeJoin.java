package jess;

import java.io.Serializable;

/**
 * Node containing an arbitrary list of tests; used for TEST CE's and also
 * the base class for join nodes.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

abstract class NodeJoin extends Node implements Serializable {

    /**
     The tests this node performs
     */

    int m_nTests = 0;
    TestBase[] m_tests = new TestBase[2];

    /**
     * Constructor
     */
    NodeJoin() {
    }

    void addTest(int test, int slot_sidx, Value v, Rete engine)
            throws JessException {
        TestBase t = null;

        Funcall f = v.funcallValue(null);

        // if we have an accelerator, try to apply it
        if (ReteCompiler.getAccelerator() != null)
            t = ReteCompiler.getAccelerator().speedup(f, engine);

        // if no acceleration, use the standard Test1 class
        if (t == null)
            t = new Test1(test, "", slot_sidx, v);


        addTest(t);
    }

    void addTest(TestBase t) {
        if (m_nTests == m_tests.length) {
            TestBase[] temp = new TestBase[m_nTests * 2];
            System.arraycopy(m_tests, 0, temp, 0, m_nTests);
            m_tests = temp;
        }

        m_tests[m_nTests++] = t;
    }

    abstract void addTest(int test, int token_idx, int left_idx, int leftSub_idx,
                 int right_idx, int rightSub_idx)
            throws JessException;

    /**
     * For our purposes, two Node2's are equal if every test in one has
     * an equivalent test in the other, and if the test vectors are the
     * same size. This routine is used during network
     * compilation, not at runtime.  */

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (this.getClass() != o.getClass())
            return false;

        NodeJoin n = (NodeJoin) o;

        if (n.m_nTests != m_nTests)
            return false;

        outer_loop:
          for (int i = 0; i < m_nTests; i++) {
              TestBase t1 = m_tests[i];
              for (int j = 0; j < m_nTests; j++) {
                  if (t1.equals(n.m_tests[j]))
                      continue outer_loop;
              }
              return false;
          }
        return true;
    }

    abstract void callNodeLeft(int tag, Token token, Context context) throws JessException;

    /**
     Two-input nodes always make calls to the left input of other nodes.
     */

    void passAlong(int tag, Token t, Context context) throws JessException {
        Node[] sa = m_succ;
        for (int j = 0; j < m_nSucc; j++) {
            Node s = sa[j];
            s.callNodeLeft(tag, t, context);
        }
    }

    abstract public String toString();

    String getCompilationTraceToken() {
        return "2";
    }

    abstract public int getNodeType();

    void setOld() {
        // Stateless node; nothing to do
    }

    abstract void callNodeRight(int tag, Token token, Context context) throws JessException;

}




