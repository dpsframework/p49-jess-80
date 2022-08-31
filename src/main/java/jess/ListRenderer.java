package jess;

import java.util.ArrayList;

class ListRenderer {
    private String m_head;
    private String m_indent = "";
    private ArrayList m_data = new ArrayList();

    private char m_open = '(';
    private char m_close = ')';

    /**
     * @param head
     */
    public ListRenderer(String head) {
        m_head = head;
    }

    public ListRenderer() {
        this("");
    }

    /**
     * @param head
     * @param o
     */
    public ListRenderer(String head, Object o) {
        this(head);
        add(o);
    }

    /**
     * @param o
     */
    public ListRenderer add(Object o) {
        m_data.add(o);
        return this;
    }

    /**
     * @param s
     */
    public ListRenderer addQuoted(String s) {
        add("\"" + s + "\"");
        return this;
    }

    /**
     * @param s
     */
    public void indent(String s) {
        m_indent = s;
    }

    public void newLine() {
        add("\n" + m_indent);
    }

    /**
     * @return
     */
    public StringBuffer toStringBuffer() {
        StringBuffer sb = new StringBuffer(m_data.size() * 6);
        sb.append(m_open);
        sb.append(m_head);
        for (int i = 0; i < m_data.size(); i++) {
            if (sb.length() > 1) sb.append(' ');
            sb.append(m_data.get(i));
        }
        sb.append(m_close);
        return sb;
    }

    public String toString() {
        return toStringBuffer().toString();
    }

    public boolean hasContent() {
        return m_data.size() > 0;
    }

}
