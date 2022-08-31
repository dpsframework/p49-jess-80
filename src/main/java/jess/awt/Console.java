package jess.awt;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;

import jess.Main;
import jess.Rete;

/**
 * A simple graphical console for Jess.
 * Basically just runs the class {@link ConsolePanel} in a window.
 * <P>
 * (C) 2007 Sandia National Laboratories<BR>
 */

public class Console extends Frame implements Serializable {
    ConsolePanel m_panel;
    Rete m_engine;
    boolean m_doEcho = true;

    /**
     * Create a Console. This constructor creates a new Rete object,
     * which you can get ahold of with getEngine().
     * @param title The title for the Frame.
     * */

    public Console(String title) {
        this(title, true);
    }

    /**
     * Create a console which optionally doesn't echo commands.
     * @param title The title for the window
     * @param doEcho True only if the typed commands should be echoed to the window.
     */

    public Console(String title, boolean doEcho) {
        this(title, new Rete(), doEcho);
    }

    /**
     * Create a Console, using a prexisting Rete object.
     * @param title The title for the Frame.
     * @param engine A prexisting Rete object.
     */

    public Console(String title, Rete engine) {
        this(title, engine, true);
    }

    /**
     * Create a Console, using a prexisting Rete object, that optionally doesn't echo commands.
     * @param title The title for the frame
     * @param engine A Rete object
     * @param doEcho  True only if the typed commands should be echoed to the window.
     */
    public Console(String title, Rete engine, boolean doEcho) {
        super(title);
        // ###
        m_engine = engine;
        m_panel = new ConsolePanel(engine, doEcho);
        m_doEcho = doEcho;

        add("Center", m_panel);
        validate();
        setSize(500, 300);
        setVisible(true);
    }

    /**
     * Return the Rete engine being used by this Console.
     * @return The Rete object used by this console.
     */
    public Rete getEngine() {
        return m_engine;
    }

    /**
     * Pass the argument array on to an instance of jess.Main connected
     * to this Console, and call Main.execute().
     * @param argv Arguments for jess.Main.initialize().
     */

    public void execute(String[] argv) {
        // ###
        Main m = new Main();
        m.initialize(argv, m_engine);
        m_panel.setFocus();
        while (true)
            m.execute(m_doEcho);
    }

    /**
     * Trivial main() to display this frame and call execute, passing along the arguments.
     * @param argv Arguments passed to execute().
     */

    public static void main(String[] argv) {
        final Console c = new Console("Jess Console");

        // ###
        c.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        c.execute(argv);
    }

}



