package jess;

/**
 * A hook in the Jess parser that lets you perform compile-time
 * validation of function-call arguments. Used by the JessDE editor.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 * @see Jesp#addArgumentChecker 
 */

public interface ArgumentChecker {
    /**
     * Check the validity of an argument about to be added to the given Funcall.
     * @param f The funcall
     * @param tok The token being parsed
     * @param errorSink A place to report errors and warnings
     * @return Whether the proposed argument is valid
     */
    boolean check(Funcall f, JessToken tok, ErrorSink errorSink) throws JessException;
}
