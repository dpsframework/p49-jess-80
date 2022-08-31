package jess.xml;

class XMLWriter {
    private Indenter m_indenter = new Indenter();

    void openTag(String tag) {
        m_indenter.append("<");
        m_indenter.append(tag);
        m_indenter.appendln(">");
        m_indenter.indent();
    }

    void openTagNoNewline(String tag) {
        m_indenter.append("<");
        m_indenter.append(tag);
        m_indenter.append(">");
        m_indenter.indent();
    }

    void openTagNoNewline(String tag, String attribute, String value) {
        m_indenter.append("<");
        m_indenter.append(tag);
        m_indenter.append(" ");
        m_indenter.append(attribute);
        m_indenter.append("='");
        m_indenter.append(value);
        m_indenter.append("'>");
        m_indenter.indent();
    }

    void closeTag(String tag) {
        m_indenter.unindent();
        m_indenter.append("</");
        m_indenter.append(tag);
        m_indenter.appendln(">");
    }

    void closeTagNoIndentation(String tag) {
        m_indenter.unindent();
        m_indenter.appendNoindent("</");
        m_indenter.appendNoindent(tag);
        m_indenter.appendlnNoindent(">");
    }


    public Indenter unindent() {
        return m_indenter.unindent();
    }

    public Indenter indent() {
        return m_indenter.indent();
    }

    public void clear()  {
        m_indenter.clear();
    }

    public Indenter appendNoindent(String s) {
        return m_indenter.appendNoindent(s);
    }

    public Indenter appendNoindent(Object o) {
        return m_indenter.appendNoindent(o);
    }

    public Indenter appendlnNoindent(Object o) {
        return m_indenter.appendlnNoindent(o);
    }

    public Indenter appendln(Object o) {
        return m_indenter.appendln(o);
    }

    public Indenter append(String s) {
        return m_indenter.append(s);
    }

    public Indenter append(Object o) {
        return m_indenter.append(o);
    }

    public String toString() {
        return m_indenter.toString();
    }

    public void textElementNoSpace(String tag, String text) {
        openTagNoNewline(tag);
        appendNoindent(escape(text));
        closeTag(tag);
    }

    public static String escape(String s) {
        if (needsNoEscaping(s))
            return s;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            switch (s.charAt(i)) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static boolean needsNoEscaping(String s) {
        return s.indexOf('<') < 0 &&
                s.indexOf('>') < 0 &&
                s.indexOf('&') < 0;
    }

}
