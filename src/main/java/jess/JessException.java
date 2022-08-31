package jess;

import java.io.Serializable;

/**
  <p>This is the base class of all
  exceptions thrown by any functions in the Jess
  library. <tt>JessException</tt> is rather complex, as
  exception classes go. An instance of this class can contain a
  wealth of information about an error that occurred in
  Jess. Besides the typical error message, a
  <tt>JessException</tt> may be able to tell you the name of
  the routine in which the error occurred, the name of the Jess
  constructs that were on the exceution stack, the relevant text
  and line number of the executing Jess language program, and the
  Java exception that triggered the error (if any.)
   </p>
  <p>One of the most important pieces of advice for working with the Jess
  library is that in your catch clauses for <tt>JessException</tt>, <i>display the
  exception object</i>. Print it to System.out, or convert to a String
  and display it in a dialog box, or log it, or something. The exceptions
  are there to help you by telling when something goes wrong; please
  don't ignore them.
  </p>
  <p>Another important tip: the JessException class has a method
  {@link #getCause} which returns non-null when a particular
  <tt>JessException</tt> is a wrapper for another kind of exception. For
  example, if your Jess code calls a Java
  function that throws an exception, then the Jess code will throw
  a <tt>JessException</tt>, and calling {@link #getCause} will
  return the real exception that was thrown. Your JessException
  handlers should always check <tt>getCause</tt>; if your handler
  simply displays a thrown exception, then it should display the
  return value of <tt>getCause</tt>, too.
  </P>
  (C) 2013 Sandia Corporation<br>
 */

public class JessException extends Exception implements Serializable {

    public static final int NO_ERROR = 0;
    public static final int GENERAL_ERROR = 1;
    public static final int CLASS_NOT_FOUND = 2;

    private int m_errorCode = GENERAL_ERROR;

    private Context m_executionContext;
    private String m_filename;

    /**
     * Constructs a JessException containing a descriptive message.
     * The three separate arguments make it easy to
     * assemble a message from pieces.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param data    usually some data; appended to msg after a space.
     */

    public JessException(String routine, String msg, String data) {
        super(msg + " " + data);
        m_routine = routine;
        m_detail = msg;
        m_data = data;
    }

    /**
     * Constructs a JessException containing a descriptive message.
     * The three separate arguments make it easy to
     * assemble a message from pieces.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param data    usually some data; appended to msg after a space.
     * @param filename the name of the Jess language source file in which the error occurred
     */

    public JessException(String routine, String msg, String data, String filename) {
        super(msg + " " + data);
        m_routine = routine;
        m_detail = msg;
        m_data = data;
        m_filename = filename;
    }

    /**
     * Constructs a JessException containing a descriptive message.
     * The three separate arguments make it easy to
     * assemble a message from pieces.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param data    usually some data; appended to msg after a space.
     */

    public JessException(String routine, String msg, int data) {
        this(routine, msg, String.valueOf(data));
    }

    /**
     * Constructs a JessException containing a nested exception.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param t       another type of exception that this object wraps
     */

    public JessException(String routine, String msg, Throwable t) {
        super(msg, t);
        m_routine = routine;
        m_detail = msg;
    }

    /**
     * Constructs a JessException containing a nested exception.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param t       another type of exception that this object wraps
     */

    public JessException(String routine, String msg, String data, Throwable t) {
        super(msg + " " + data, t);
        m_routine = routine;
        m_detail = msg;
        m_data = data;
    }

    /**
     * Constructs a JessException containing a nested exception.
     *
     * @param routine the name of the routine this exception occurred in.
     * @param msg     an informational message.
     * @param t       another type of exception that this object wraps
     * @param filename the name of the Jess language source file in which the error occurred
     */

    public JessException(String routine, String msg, String data, String filename, Throwable t) {
        super(msg + " " + data, t);
        m_routine = routine;
        m_detail = msg;
        m_data = data;
        m_filename = filename;
    }

    /**
     * Get any nested exception object. If Jess encounters a Java exception during execution,
     * a <tt>JessException</tt> will be thrown and this method will return the original Java exception.
     * <tt>getCause</tt> replaces
     * the old <tt>getNextException()</tt> method, now removed.
     *
     * @return Value of nextException. Returns null if none.
     */
    public Throwable getCause() {
        return super.getCause();
    }

    private int m_lineNumber = -1;

    /**
     * Get the program line number where the error occurred.
     *
     * @return value of lineNumber.
     */
    public int getLineNumber() {
        return m_lineNumber;
    }

    /**
     * Set the program line number where the error occurred.
     *
     * @param v value to assign to lineNumber.
     */
    void setLineNumber(int v) {
        m_lineNumber = v;
    }


    /**
     * Get the name of the Jess language source file where the error occurred. Returns null if unknown.
     *
     * @return value of filename, or null
     */
    public String getFilename() {
        return m_filename;
    }


    /**
     * Set the name of the Jess language source file  where the error occurred.
     *
     * @param filename value to assign to filename
     */
    public void setFilename(String filename) {
        m_filename = filename;
    }

    private final String m_detail;

    /**
     * Get the error message.
     *
     * @return Value of message.
     */
    public String getDetail() {
        return m_detail;
    }


    private final String m_routine;

    /**
     * Get the Java routine name where this error occurred
     *
     * @return Value of routine.
     */
    public String getRoutine() {
        return m_routine;
    }

    private String m_data;

    /**
     * Get the extra error data.
     *
     * @return Value of data.
     */
    public String getData() {
        return m_data;
    }

    private String m_programText;

    /**
     * Get the Jess program fragment that led to this exception
     *
     * @return Value of programText.
     */
    public String getProgramText() {
        return m_programText;
    }

    /**
     * Set the Jess program fragment that led to this exception.
     *
     * @param v Value to assign to programText.
     */
    void setProgramText(String v) {
        m_programText = v;
    }


    private StringBuffer m_context;

    /**
     * Adds information about where an error happened to a JessException.
     * Contexts are tracked cumulatively, and the toString message will show all
     * contexts that have been added.
     *
     * @param s a description of an execution context: 'defrule Foo', for example.
     * @param c an execution context. Only the innermost context is stored.
     * @see jess.JessException#toString
     */
    void addContext(String s, Context c) {
        if (m_context == null) {
            m_context = new StringBuffer();
            m_executionContext = c;
        }
        m_context.append("\n\twhile executing ");
        m_context.append(s);
    }

    /**
     * Get the context information for this error. This is the Jess language equivalent of a Java stack trace. It
     * will include a line for each function, rule, Rete node, etc which is on Jess's call stack at the time of the error.
     *
     * @return The context, or null if no information
     */
    public String getContext() {
        return m_context == null ? null : m_context.toString();
    }

    /**
     * Get the {@link Context} active at the time of the error, if available.
     * @return The active Context or null
     */
    public Context getExecutionContext() {
        return m_executionContext;
    }


    /**
     * Returns a String representation of this JessException. The String includes the
     * routine name, message, and any contexts that have been added.
     *
     * @return a string containing this information.
     */

    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("Jess reported an error in routine ");
        sb.append(m_routine);
        if (m_context != null)
            sb.append(m_context.toString());
        sb.append(".\n");
        sb.append("  Message: ");
        sb.append(getMessage());
        sb.append(".");

        if (m_programText != null) {
            sb.append("\n  Program text: ");
            sb.append(m_programText);
            if (m_lineNumber != -1) {
                sb.append(" at line ");
                sb.append(m_lineNumber);
                if (m_filename != null) {
                    sb.append(" in file ");
                    sb.append(m_filename);
                }
                sb.append(".");
            }
        }

        return sb.toString();
    }


    /**
     * Returns one of the error codes in class {@link ParseException}.
     * @return the error code
     */
    public int getErrorCode() {
        return m_errorCode;
    }

    /**
     * Set the error code for this exception. The argument should be one of the error codes in class {@link ParseException}.
     * @param code the desired error code
     */
    public void setErrorCode(int code) {
        m_errorCode = code;
    }
}
