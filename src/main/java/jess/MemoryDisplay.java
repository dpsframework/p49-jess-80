package jess;

import javax.swing.*;

/**
 * (C) 2007 Sandia National Laboratories
 */
abstract class MemoryDisplay extends JPanel {
    public abstract void updateGraph() throws JessException;
}
