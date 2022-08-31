package jess;

/**
 * The exception type thrown by the "Continue" function.
 * (C) 2007 Sandia National Laboratories
 */

class ContinueException extends JessException {
    static ContinueException INSTANCE = new ContinueException();
    private ContinueException() {
        super("continue encountered", "", "");
    }
}
