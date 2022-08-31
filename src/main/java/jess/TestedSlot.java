package jess;

import java.io.Serializable;

class TestedSlot implements Serializable {
    private Deftemplate m_template;
    private int m_slotIndex;

    public TestedSlot(Deftemplate template, int slotIndex) {
        m_template = template;
        m_slotIndex = slotIndex;
    }

    public boolean equals(Object o) {
        if (! (o instanceof TestedSlot))
            return false;
        TestedSlot t = (TestedSlot) o;
        return t.m_template == m_template && t.m_slotIndex == m_slotIndex;
    }

    public int hashCode() {
        return m_template.hashCode()*10 + m_slotIndex;
    }

    public String toString() {
        return "[TestedSlot: template=" + m_template.getName() + "; slot=" + m_slotIndex + "]";
    }
}
