package jess.xml;

/**
 * A utility class that lets you easily compose formatted code.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class Indenter
{
    private StringBuffer m_sb = new StringBuffer(512);
    private int m_indent;

    // True at start of line
    private boolean m_start = true;

    private final static String[] strings = {
        "",
        "  ",
        "    ",
        "      ",
        "        ",
        "          ",
        "            ",
        "              ",
        "                ",
        "                  ",
        "                    ",
        "                      ",
        "                        "
    };

    public Indenter() {
        clear();
    }

    /**
     * Erase the contents of this Indenter
     */
    public void clear()  {
        m_sb.setLength(0);
        m_indent = 0;
        m_start = true;
    }

    /**
     * Increase the indentation level by one.  */

    public Indenter indent() {
        m_indent += 1;
        if (m_indent >= strings.length)
            throw new RuntimeException("Nested too deep");
        return this;
    }

    /**
     * Decrease the indentation level by one.
     */

    public Indenter unindent() {
        m_indent -= 1;
        if (m_indent <  0)
            throw new RuntimeException("Negative indentation");
        return this;
    }


    /**
     * Call toString on the argument, call append(String), then append("\n");
     */

    public Indenter appendln(Object o) {
        append(o.toString());
        append("\n");
        return this;
    }

    /**
     * Call toString on the argument, call appendNoindent(String), then append("\n");
     */

    public Indenter appendlnNoindent(Object o) {
        appendNoindent(o.toString());
        appendNoindent("\n");
        return this;
    }

    /**
     * Call toString on the argument, call append(String).
     */

    public Indenter append(Object o) {
        append(o.toString());
        return this;
    }

    /**
     * Call toString on the argument, call appendNoindent(String).
     */

    public Indenter appendNoindent(Object o) {
        appendNoindent(o.toString());
        return this;
    }

    /**
     * Examine each character of the String, which may include
     * multiple lines. Accumulate lines, starting each one with a
     * number of spaces dictated by the current indentation level.
     */

    public Indenter append(String s) {
        if (s.length() > 0) {

            String is;

            if (m_indent < strings.length)
                is = strings[m_indent];
            else
                is = strings[strings.length - 1];

            int i = 0;
            do {
                if (m_start) {
                    m_sb.append(is);
                    m_start = false;
                }

                char c;
                m_sb.append(c = s.charAt(i++));
                if (c == '\n')
                    m_start = true;
            }
            while (i < s.length());
        }
        return this;

    }

    /**
     * Append the given string to the buffer with no translation at all.
     */

    public Indenter appendNoindent(String s) {
        m_sb.append(s);
        return this;
    }


    /**
     *  Fetch the formatted contents.
     */

    public String toString() { return m_sb.toString(); }
}
