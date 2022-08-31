package jess.server;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: ThreadSerialNumber.java,v 1.3 2006-07-06 02:28:03 ejfried Exp $
 */

class ThreadSerialNumber {
    private int m_nextSerialNum = 0;

    private ThreadLocal m_serialNum = new ThreadLocal() {
        protected Object initialValue() {
            synchronized(ThreadSerialNumber.this) {
                return new Integer(m_nextSerialNum++);
            }
        }
    };

    public synchronized int get() {
        return ((Integer) (m_serialNum.get())).intValue();
    }
}
