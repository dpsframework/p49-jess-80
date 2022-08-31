package jess;
import java.io.Serializable;

/**
 * The depth-first conflict resolution strategy.
 *<P>
 * (C) 2013 Sandia Corporation<br>
 */

class depth implements Strategy, Serializable {

    public int compare(Activation a1, Activation a2) {
        int s1 = a1.getSalience();
        int s2 = a2.getSalience();

        if (s1 != s2)
            return s2 - s1;

        Token t1 = a1.getToken();
        Token t2 = a2.getToken();

        if (t1.getTime() != t2.getTime())
            return t2.getTime() - t1.getTime();
        else
            return t2.getTotalTime() - t1.getTotalTime();

    }

    public String getName() { return "depth"; }
}
