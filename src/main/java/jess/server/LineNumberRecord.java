package jess.server;

/**
 * A LineNumberRecord represents the line in a file where some Jess
 * construct is defined. This class is used by the debugger.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class LineNumberRecord {
    private String m_fileName;
    private int m_lineno;

    public LineNumberRecord(String fileName, int lineno) {
        m_fileName = fileName;
        m_lineno = lineno;
    }

    public String getFileName() {
        return m_fileName;
    }

    public int getLineno() {
        return m_lineno;
    }

    public String toString() {
      return "[LNR " + m_fileName + ";" + m_lineno + "]";
    }
}
