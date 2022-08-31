package jess;

import java.util.Iterator;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: IntMarkerImpl.java,v 1.3 2007-05-11 19:22:40 ejfried Exp $
 */
class IntMarkerImpl implements WorkingMemoryMarker {
    private int m_factId;

    IntMarkerImpl(int factId) {
        m_factId = factId;
    }

    public void restore(Rete engine) throws JessException {
        synchronized (engine.getWorkingMemoryLock()) {
            for (Iterator it = engine.listFacts(); it.hasNext();) {
                Fact fact = (Fact) it.next();
                if (m_factId <= fact.getFactId())
                    engine.retract(fact);
            }
        }
    }
}
