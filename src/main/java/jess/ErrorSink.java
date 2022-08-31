package jess;

/**
 * An error reporting tool for the Jess parser.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

public interface ErrorSink {
    void error(String routine, String msg, int code, JessToken errorToken) throws JessException;

    void error(String routine, String msg, int code, JessToken errorToken, Named construct) throws JessException;

    void error(String routine, String msg, String[] alternatives, int code, JessToken errorToken) throws JessException;

    void error(String routine, String msg, String[] alternatives, int code, JessToken errorToken, Named construct) throws JessException;

    void warning(String routine, String msg, String[] alternatives, int code, JessToken errorToken);

    Rete getEngine();
}
