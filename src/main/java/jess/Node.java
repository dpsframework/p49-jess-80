package jess;

import java.io.*;
import java.util.*;

/**
 * <p>Parent class of all nodes of the Rete network. It's unlikely that you'll use
 * this class unless you're building tools.
 * </P>
 * (C) 2013 Sandia Corporation<br>
 */

public abstract class Node implements Serializable {

    public static final int TYPE_NONE = 0;
    public static final int TYPE_NODE1 = 1;
    public static final int TYPE_NODE2 = 2;
    public static final int TYPE_NODENOT2 = 3;
    public static final int TYPE_TERMINAL = 5;
    public static final int TYPE_ADAPTER = 6;

    /**
     * @deprecated This value is no longer used
     */
    public static final int TYPE_TEST = 4;



    /**
     * How many rules use me?
     */

    private int m_usecount = 0;


    Node[] m_succ;
    int m_nSucc;

    /**
     * Returns an Enumeration over all the nodes fed from this one.
     *
     * @return the enumeration
     * @deprecated use {@link #successors()} instead
     */
    public Enumeration getSuccessors() {
        return new NodeEnumeration();
    }

    /**
     * Returns an Iterator over all the nodes fed from this one.
     *
     * @return the iterator
     */
    public Iterator successors() {
        return new NodeIterator();
    }

    Node resolve(Node n) {
        for (int i = 0; i < m_nSucc; i++) {
            if (m_succ[i].equals(n))
                return m_succ[i];
        }
        return n;
    }

    Node addSuccessor(Node n, NodeSink r)
            throws JessException {
        if (m_succ == null || m_nSucc == m_succ.length) {
            Node[] temp = m_succ;
            m_succ = new Node[m_nSucc + 5];
            if (temp != null)
                System.arraycopy(temp, 0, m_succ, 0, m_nSucc);
        }
        m_succ[m_nSucc++] = n;
        r.addNode(n);
        return n;
    }

    Node mergeSuccessor(Node n, NodeSink r)
            throws JessException {
        for (int i = 0; i < m_nSucc; i++) {
            Node test = m_succ[i];
            if (n.equals(test)) {
                r.addNode(test);
                return test;
            }
        }
        // No match found
        addSuccessor(n, r);
        return n;
    }


    /**
     * Some Nodes compare equal using equals(), but aren't the same
     * physical node. We only want to remove the ones that are
     * physically equal, not just conceptually.
     */

    void removeSuccessor(Node s) {
        for (int i = 0; i < m_nSucc; i++)
            if (s == m_succ[i]) {
                System.arraycopy(m_succ, i + 1, m_succ, i, (--m_nSucc) - i);
                return;
            }
    }

    /**
     * Do the business of this node.
     */

    abstract void callNodeLeft(int tag, Token token, Context context) throws JessException;

    abstract void callNodeRight(int tag, Token t, Context context) throws JessException;

    private transient List m_listeners;

    public synchronized void addJessListener(JessListener jel) {
        if (m_listeners == null)
            m_listeners = Collections.synchronizedList(new ArrayList());

        m_listeners.add(jel);
    }

    /**
     * @param jel
     */
    public synchronized void removeJessListener(JessListener jel) {
        if (m_listeners == null)
            return;
        m_listeners.remove(jel);
    }

    // It's possible for this to miss recently-installed listeners due to
    // thread cache coherence issues. It's worth it, though, because we can't afford
    // to pay for synchronization here.
    void broadcastEvent(int tag, int type, Object data, Context context) throws JessException {
        if (m_listeners != null) {
            synchronized (this) {
                if (m_listeners != null) {
                    JessEvent event = new JessEvent(this, type, tag, data, context);
                    for (Iterator it = m_listeners.iterator(); it.hasNext();) {
                        JessListener jessListener = (JessListener) it.next();
                        jessListener.eventHappened(event);
                    }
                }
            }
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    private class NodeEnumeration implements Enumeration {
        private int m_index = 0;

        public boolean hasMoreElements() {
            return m_index < m_nSucc;
        }

        public Object nextElement() {
            if (!hasMoreElements())
                throw new RuntimeException("No more elements");
            return m_succ[m_index++];
        }
    }

    private class NodeIterator implements Iterator {
        private int m_index = 0;

        public boolean hasNext() {
            return m_index < m_nSucc;
        }

        public Object next() {
            if (!hasNext())
                throw new RuntimeException("No more elements");
            return m_succ[m_index++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    abstract String getCompilationTraceToken();

    public abstract int getNodeType();

    // This should do nothing for stateless nodes, and for stateful nodes
    // it should make them ignore (but pass along) further update tokens.
    abstract void setOld();

    protected int incrementUseCount() {
        return ++m_usecount;
    }

    protected int decrementUseCount() {
        return --m_usecount;
    }

    protected int getUseCount() {
        return m_usecount;
    }
}






