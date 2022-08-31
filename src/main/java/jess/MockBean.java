package jess;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/*
 * A Java Bean for testing.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class MockBean implements java.io.Serializable {
    private String m_foo;

    public MockBean() {
        m_foo = "blah";
    }

    public String getFoo() {
        return m_foo;
    }

    public void setFoo(String val) {
        String tmp = m_foo;
        m_foo = val;
        pcs.firePropertyChange("foo", tmp,
                m_foo);
    }

    private int m_nChangeListeners;

    public int numChangeListeners() {
        return m_nChangeListeners;
    }

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        ++m_nChangeListeners;
        pcs.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        --m_nChangeListeners;
        pcs.removePropertyChangeListener(pcl);
    }
}
