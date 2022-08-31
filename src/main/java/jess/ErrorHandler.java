package jess;

/**
 * Objects of this class are notified when an error occurs while parsing Jess code.
 * The ErrorHandler can choose to abort or continue the parse.
 * <p/>
 * (C) 2013 Sandia Corporation<BR>
 * @see Rete#batch(String)
 * @see Batch#batch(String, Rete, Context, ErrorHandler)
 *
 */

public interface ErrorHandler {

    /**
     * When an error occurs during parsing, this method will be called.
     * @param ex the error that has occurred
     * @throws JessException to abort parsing, rethrow the exception parameter
     */
    void handleError(JessException ex) throws JessException;


    /**
     * A default error handler implementation that just rethrows the parameter. This
     * handler is used internally by the implementations of "batch".
     */
    public static class DefaultHandler implements ErrorHandler {
        public void handleError(JessException ex) throws JessException {
            throw ex;
        }
    }

}
