package jess.server;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: DebugThreadGroup.java,v 1.1 2006-07-06 02:28:03 ejfried Exp $
 */
class DebugThreadGroup extends ThreadGroup {
    public DebugThreadGroup(String name) {
        super(name);
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (!(e instanceof UndeclaredThrowableException)) {
            super.uncaughtException(t, e);
        }
    }
}
