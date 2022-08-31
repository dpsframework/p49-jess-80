package jess;

import jess.server.LineNumberRecord;

/**
 * A stack frame. Used by the JessDE debugger implementation, but not likely to be useful
 * to any other clients.<br>
 *
 * (C) 2013 Sandia Corporation<BR>
 */
public class StackFrame {
    private Funcall m_funcall;
    private LineNumberRecord m_record;

    public StackFrame(Funcall funcall, LineNumberRecord record) {
        m_funcall = funcall;
        m_record = record;
    }

    public LineNumberRecord getLineNumberRecord() {
        return m_record;
    }

    public Funcall getFuncall() {
        return m_funcall;
    }

    public String toString() {
        return "[StackFrame:" + m_funcall.toStringWithParens() + ";" + m_record + "]";
    }
}
