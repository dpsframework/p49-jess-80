package jess.server;

import jess.Funcall;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: ThreadState.java,v 1.3 2006-11-21 21:04:31 ejfried Exp $
 */
class ThreadState {
    static final ThreadState RUNNING = new ThreadState("RUNNING");
    static final ThreadState SUSPENDED = new ThreadState("SUSPENDED");
    static final ThreadState STEPPING = new ThreadState("STEPPING");
    static final ThreadState DEAD = new ThreadState("DEAD");
    private Funcall m_funcall;
    private String m_name;

    private ThreadState(String name) {m_name = name;}

    static ThreadState makeSteppingOver(Funcall f) {
        ThreadState state = new ThreadState("STEPPING_OVER");
        state.m_funcall = f;
        return state;
    }

    boolean isSteppingOver() {
        return m_funcall != null;
    }

    boolean isSuspended() {
        return this == SUSPENDED;
    }

    boolean isStepping() {
        return this == STEPPING;
    }

    Funcall getStepOverTarget() {
        if (isSteppingOver())
            return m_funcall;
        else
            throw new RuntimeException("INTERNAL ERROR");
    }

    public String toString() {
        return m_name;
    }
}
