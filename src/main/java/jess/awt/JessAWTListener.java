package jess.awt;

import jess.*;

import java.awt.AWTEvent;

/*
 * An AWT Event Adapter for Jess.
 * This class is used to implement all the other listeners in this package.
 * <P>
 * (C) 2013 Sandia Corporation
 */

class JessAWTListener {
    private Funcall m_fc;
    private Rete m_engine;

    /**
     * @param uf
     * @param engine
     * @throws JessException
     */
    JessAWTListener(String uf, Rete engine) throws JessException {
        m_engine = engine;
        m_fc = new Funcall(uf, engine);
        m_fc.setLength(2);
    }

    /**
     * @param e
     */
    final void receiveEvent(AWTEvent e) {
        try {
            m_fc.set(new Value(e), 1);
            m_fc.execute(m_engine.getGlobalContext());
        } catch (JessException re) {
            m_engine.getErrStream().println(re);
            m_engine.getErrStream().flush();
        }
    }
}

