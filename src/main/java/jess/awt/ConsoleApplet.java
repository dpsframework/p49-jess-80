package jess.awt;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Label;
import java.io.Serializable;

import jess.Main;
import jess.Rete;

/**
 *
 * A simple Applet which uses {@link ConsolePanel}. It could
 * serve as the basis for any number of 'interview' style Expert System
 * GUIs.
 * <P>
 * Applet Parameters:
 * <UL>
 * <LI> INPUT: if present, the applet will find the named document relative
 *    to the applet's document base and interpret the file in batch mode,
 *    then fall into a parse loop when the file completes.
 * </UL>
 * <P>
 * (C) 2007 Sandia National Laboratories<BR>
 */

public class ConsoleApplet extends Applet implements Runnable, Serializable {
    // The display panel
    private ConsolePanel m_panel;
    // Thread in which the parse loop runs
    private Thread m_thread;
    // Main object used to drive Rete
    private Main m_main;


    /**
     * Set up the applet's window and process the parameters. Reads any
     * input file and prepares to parse it.
     */
    public void init() {
        setLayout(new BorderLayout());
        Rete engine = new Rete(this);
        m_panel = new ConsolePanel(engine);
        add("Center", m_panel);
        add("South", new Label());

        // ###
        String[] argv = new String[]{};
        // Process Applet Parameters
        String appParam = getParameter("INPUT");
        if (appParam != null)
            argv = new String[]{appParam};

        m_main = new Main();
        m_main.initialize(argv, engine);
    }

    /**
     * Called by this applet to execute Jess in another Thread.
     */
    public synchronized void run() {
        do {
            try {
                m_panel.setFocus();
                while (true)
                    m_main.execute(true);
            } catch (Throwable t) {
                m_thread = null;
            }
        } while (m_thread != null);
    }

    /**
     * Starts the engine running in another thread.
     */
    public void start() {
        if (m_thread == null) {
            // ###
            m_thread = new Thread(this);
            m_thread.start();
        }
    }

    /**
     * Terminates Jess.
     */
    public void stop() {
        m_thread = null;
    }
}

