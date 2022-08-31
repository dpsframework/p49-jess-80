package jess.swing;

import jess.*;

import java.awt.Graphics;

/**
 * Lets you do Java2D graphics from Jess code.
 * <P>
 * Construct one of these with the name of a 'painting' Userfunction as
 * the first argument (a deffunction, usually.) The function should expect a Swing
 * Component as its first argument, and a java.awt.Graphics2D object
 * as its second argument (both as {@link RU#JAVA_OBJECT}s).
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public class JPanel extends javax.swing.JPanel {
    private Funcall m_fc;
    private Rete m_engine;

    /**
     * Will generally be called from Jess language code via reflection.
     *
     * @param uf the name of a Jess function to call from paintComponent()
     * @param engine the engine that should run the function
     * @throws jess.JessException if something goes wrong
     */
    public JPanel(String uf, Rete engine) throws JessException {
        m_engine = engine;
        m_fc = new Funcall(uf, engine);
        m_fc.add(new Value(this));
        m_fc.setLength(3);
    }

    /**
     * The paintComponent() method that all Swing components have. This implementation
     * calls super.paintComponent(), and then calls the
     * Jess function named in the constructor argument. When that function is called, its
     * first argument will be this JPanel object, and the second will be the graphics
     * context.
     *
     * @param g a graphics context
     */

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            m_fc.set(new Value(g), 2);
            m_fc.execute(m_engine.getGlobalContext());
        } catch (Exception ex) {
            m_engine.getErrStream().println(ex);
            m_engine.getErrStream().flush();
        }
    }
}
