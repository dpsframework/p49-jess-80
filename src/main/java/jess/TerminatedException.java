package jess;

/**
 * <p>An exception class used only by the JessDE debugger.</p>
 * (C) 2013 Sandia Corporation<BR>
 */
public class TerminatedException extends JessException {
    public TerminatedException(String routine, String msg, String data) {
        super(routine, msg, data);
    }
}
