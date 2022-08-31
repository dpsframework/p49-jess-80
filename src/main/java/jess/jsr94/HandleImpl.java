package jess.jsr94;

import javax.rules.Handle;

class HandleImpl implements Handle {
    private Object m_object;

    HandleImpl(Object o) {
        m_object = o;
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof HandleImpl))
            return false;
        return m_object == ((HandleImpl) obj).m_object;
    }

    public int hashCode() {
        return System.identityHashCode(m_object);
    }

    Object getObject() {
        return m_object;
    }

    void setObject(Object o) {
        m_object = o;
    }
}
