package jess;

/**
 * The exception type thrown by the "Break" function.
 * (C) 2007 Sandia National Laboratories
 */

class BreakException extends JessException {
    static BreakException INSTANCE = new BreakException();
    private BreakException() {
        super("break encountered", "", "");
    }
}
