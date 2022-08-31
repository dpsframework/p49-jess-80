package jess.awt;

import java.awt.TextArea;
import java.io.Serializable;
import java.io.Writer;

/**
 * A simple Writer, suitable for constructing a PrintWriter, which
 * uses a TextArea as its output. This class is a convenient way to
 * write a GUI in which Jess prints its output to a text
 * widget. Contents are kept to a maximum of 32000 characters,
 * assuming that no other code appends to this TextArea.
 * <P>
 * (C)2006 Sandia National Laboratories<br>
 * @see jess.swing.JTextAreaWriter
 */

public class TextAreaWriter extends Writer implements Serializable {

    private StringBuffer m_str;
    private TextArea m_ta;
    private int m_size = 0;
    private static final int MAXSIZE = 30000;


    /**
     * Call this with an already constructed TextArea
     * object, which you can put wherever you'd like.
     *
     * @param area The text area
     */
    public TextAreaWriter(TextArea area) {
        m_str = new StringBuffer(100);
        m_ta = area;
        m_size = m_ta.getText().length();
    }

    public synchronized void clear() {
        m_ta.setText("");
        m_size = 0;
    }

    /**
     * Does nothing
     */

    public void close() {
    }

    /**
     * Flushes pending output to the TextArea.
     */

    public synchronized void flush() {
        int len = m_str.length();
        if (m_size > MAXSIZE) {
            m_ta.replaceRange("", 0, len);
            m_size -= len;
        }

        m_ta.append(m_str.toString());
        m_size += len;
        m_str.setLength(0);
    }

    /**
     * Writes a portion of an array of characters to the TextArea. No
     * output is actually done until flush() is called.
     *
     * @param b   The array of characters
     * @param off The first character in the array to write
     * @param len The number of characters form the array to write
     * @see #flush
     */

    public synchronized void write(char b[], int off, int len) {
        m_str.append(b, off, len);
    }

}






