package jess.tools;

import java.io.*;

/**
 * A very simple reader, something like StringBufferReader but
 * you can add text to it. Useful for creating GUIs in which you want Jess to
 * continually read input from a text widget.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class TextReader extends Reader implements Serializable {
    private StringBuffer m_buf = new StringBuffer(256);
    private int m_ptr = 0;
    private boolean m_dontWait = false, m_open = true;

    /**
     * Create a TextReader.
     *
     * @param dontWait If true, read() will return EOF (-1) immediately when
     *                 its internal buffer is empty. If false, read() will block until more input
     *                 becomes available.
     */

    public TextReader(boolean dontWait) {
        m_dontWait = dontWait;
    }

    /**
     * Read a character from the internal buffer. Depending on the value of the
     * dontWait parameter passed to the constructor, this method may block if no input
     * is available.
     *
     * @return The character or EOF (-1)
     */
    public synchronized int read() throws IOException {
        if (!m_open)
            throw new IOException("Closed");

        while (m_ptr >= m_buf.length()) {
            if (m_dontWait)
                return -1;

            else if (!m_open) {
                return -1;

            } else
                try {
                    wait(100);
                } catch (InterruptedException ie) {
                    if (!m_open)
                        return -1;
                }
        }

        int c = m_buf.charAt(m_ptr++);

        if (m_ptr >= m_buf.length())
            clear();

        return c;
    }

    /**
     * Read an array of characters from the internal buffer. Depending on the value of the
     * dontWait parameter passed to the constructor, this method may block if no input
     * is available.
     *
     * @return The number of characters read.
     */

    public int read(char[] c) throws IOException {
        return read(c, 0, c.length);
    }

    /**
     * Read part of an array of characters from the internal
     * buffer. Depending on the value of the dontWait parameter passed
     * to the constructor, this method may block if no input is
     * available.
     *
     * @return The number of characters read.
     */

    public int read(char[] c, int start, int count) throws IOException {
        for (int i = start; i < start + count; i++) {
            int ch = read();
            if (ch == -1)
                return (i > start) ? i - start : -1;
            else
                c[i] = (char) ch;
        }
        return count;
    }

    /**
     * read() immediately returns EOF and subsequently throws IOException.
     */
    public void close() {
        m_open = false;
    }

    /**
     * Find out if any input is waiting to be read.
     *
     * @return True if internal buffer is not empty.
     */

    public int available() {
        return m_buf.length() - m_ptr;
    }

    /**
     * Add text to the internal buffer. The text will become available for reading
     * via read().
     *
     * @param s New text to add to the buffer
     * @see #read
     */
    public synchronized void appendText(String s) {
        m_buf.append(s);
        notifyAll();
    }

    /**
     * Remove all text from the internal buffer.
     */

    public synchronized void clear() {
        m_buf.setLength(0);
        m_ptr = 0;
    }

}
