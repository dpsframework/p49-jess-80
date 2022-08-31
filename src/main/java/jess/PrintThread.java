package jess;

import java.io.IOException;
import java.io.Writer;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: PrintThread.java,v 1.3 2007-10-18 19:29:03 ejfried Exp $
 */

class PrintThread extends Thread {
    private static final PrintThread s_printThread;

    static {
        s_printThread = new PrintThread();
        s_printThread.setDaemon(true);
        s_printThread.start();
    }

    static PrintThread getPrintThread() {
        return s_printThread;
    }

    private Writer m_os;

    synchronized void assignWork(Writer os) {
        m_os = os;
        notify();
    }

    public synchronized void run() {
        while (true) {
            try {
                while (m_os == null)
                    wait();
                try {
                    m_os.flush();
                } catch (IOException ioe) {
                } finally {
                    m_os = null;
                }
            } catch (InterruptedException ie) {
                break;
            }
            notifyAll();
        }
    }

    // Must return a value so it is not inlined and optimized away!
    synchronized int waitForCompletion() {
        return 1;
    }
}

