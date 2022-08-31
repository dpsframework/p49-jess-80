package jess;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Iterator;

/**
 * The actual agenda for a single module
 * <P>
 * (C) 2013 Sandia Corporation<BR>
 */


class ModuleAgenda implements Serializable {
    private final HeapPriorityQueue m_queue;
    private String m_name;


    ModuleAgenda(String name, Strategy s) {
        m_name = name;
        m_queue = new HeapPriorityQueue(s);
    }

    String setStrategy(Strategy s, Rete engine) throws JessException {
        synchronized (m_queue) {
            return m_queue.setStrategy(s).getName();
        }
    }

    HeapPriorityQueue getQueue() {
        return m_queue;
    }

    /**
     * Returns the name of this module
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    void reset() {
        m_queue.clear();
    }

    void confirmStrategy(Rete engine, Strategy strategy) throws JessException {
        if (m_queue.getStrategy() != strategy) {
            setStrategy(strategy, engine);
        }
    }
}
