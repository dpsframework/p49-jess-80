package jess;

import java.io.Serializable;
import java.util.*;

/**
 * JessEvent listener broadcaster helper functions.
 */

class JessEventSupport implements Serializable {

    private final java.util.List m_listeners = Collections.synchronizedList(new ArrayList());
    private final java.util.List m_debugListeners = Collections.synchronizedList(new ArrayList());
    private int m_eventMask = 0;
    private boolean m_debug;

    JessEventSupport(Rete source) {
        addJessListener(source);
    }

    public void addJessListener(JessListener jel) {
        m_listeners.add(jel);
    }

    public void removeJessListener(JessListener jel) {
        m_listeners.remove(jel);
    }


    public Iterator listJessListeners() {
        synchronized (m_listeners) {
            return new ArrayList(m_listeners).iterator();
        }
    }

    public synchronized int getEventMask() {
        return m_eventMask;
    }

    public synchronized void setEventMask(int i) {
        m_eventMask = i;
    }

    final void broadcastEvent(Object source, int type, Object data, Context context) throws JessException {
        // only broadcast active events
        if ((type & getEventMask()) == 0)
            return;

        sendEventToListeners(source, type, data, m_listeners, context);
    }

    public boolean isDebug() {
        return m_debug;
    }

    public void addDebugListener(JessListener jel) {
        m_debugListeners.add(jel);
    }

    public void setDebug(boolean debug) {
        m_debug = debug;
    }

    public void removeDebugListener(JessListener jel) {
        m_debugListeners.remove(jel);
    }


    public Iterator listDebugListeners() {
        synchronized (m_debugListeners) {
            return new ArrayList(m_debugListeners).iterator();
        }
    }

    public void broadcastDebugEvent(Object source, int type, Object data, Context context) throws JessException {
        if (m_debug) {
            if (type == JessEvent.USERFUNCTION_CALLED) {
                context.pushStackFrame((Funcall) data);
            }

            try {
                sendEventToListeners(source, type, data, m_debugListeners, context);
            } finally {
                if (type == JessEvent.USERFUNCTION_RETURNED) {
                    context.popStackFrame((Funcall) data);
                }
            }
        }
    }

    private void sendEventToListeners(Object source, int type, Object data, final java.util.List listeners, Context context) throws JessException {
        ArrayList snapshot;
        int size;

        synchronized (listeners) {
            if ((size = listeners.size()) == 0)
                return;

            snapshot = new ArrayList(listeners);
        }
        for (int i = 0; i < size; i++) {
            try {
                JessEvent theEvent = new JessEvent(source, type, data, context);
                ((JessListener) snapshot.get(i)).eventHappened(theEvent);
            } catch (JessException je) {
                throw je;
            } catch (Exception e) {
                throw new JessException("JessEventSupport.broadcastEvent",
                                        "Event handler threw an exception",
                                        e);
            }
        }
    }
}
