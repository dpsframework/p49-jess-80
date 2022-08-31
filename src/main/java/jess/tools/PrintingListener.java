package jess.tools;

import jess.*;

/**
 * A trivial JessListener which displays information about the event; useful for debugging. <p>
 * (C) Sandia National Laboratories
 */
public class PrintingListener implements JessListener {
    public void eventHappened(JessEvent je) throws JessException {
        System.out.print(">>> ");
        System.out.println(je.getSource());
        System.out.print("  ");
        System.out.print(JessEvent.getEventName(je.getType()));
        if (je.getTag() != -1) {
            System.out.print(", tag=");
            System.out.print(RU.tagName(je.getTag()));
        }
        System.out.println();
        System.out.print("  -> ");
        System.out.println(je.getObject());
    }
}
