package jess.awt;

import jess.JessException;
import jess.Rete;

import java.awt.event.KeyEvent;

/**
 * An AWT Event Adapter for Jess.
 * <P>
 * (C) Sandia National Laboratories<br>
 *
 * @deprecated Since Jess 7.0, superceded by the <a
 *             href="http://www.jessrules.com/jess/docs/70/functions.html#implement" target="_top">implement</a> and <a
 *             href="http://www.jessrules.com/jess/docs/70/functions.html#lambda" target="_top">lambda</a> functions in Jess.
 */

public class KeyListener extends JessAWTListener
        implements java.awt.event.KeyListener {

    /**
     * Connect the Jess function specified by name to this event handler object. When this
     * handler receives an AWT event, the named function will be invoked in the given
     * engine.
     *
     * @param uf     The name of a Jess function
     * @param engine The Jess engine to execute the function in
     * @throws JessException If anything goes wrong.
     */
    public KeyListener(String uf, Rete engine) throws JessException {
        super(uf, engine);
    }

    /**
     * An event-handler method. Invokes the function passed to the constructor with the
     * received event as the argument.
     *
     * @param e The event
     */
    public void keyPressed(KeyEvent e) {
        receiveEvent(e);
    }

    /**
     * An event-handler method. Invokes the function passed to the constructor with the
     * received event as the argument.
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {
        receiveEvent(e);
    }

    /**
     * An event-handler method. Invokes the function passed to the constructor with the
     * received event as the argument.
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {
        receiveEvent(e);
    }
}
