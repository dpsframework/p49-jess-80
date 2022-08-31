package jess;

import java.util.Iterator;

/**
 * A ResultSet-like class to report the result of executing a Jess query. One of these is
 * returned by the {@link Rete#runQueryStar}  method or by the run-query* function in Jess. Note that the
 * arguments to the getXXX() methods are the names of variables that appear in the query, not the names
 * of slots in the individual facts returned by the query.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 * @see Rete#runQueryStar
 */

public class QueryResult {
    private Iterator m_iterator;
    private Context m_context;
    private final Context m_globalContext;

    QueryResult(Iterator iterator, Context context) {
        m_iterator = iterator;
        m_globalContext = context;
    }

    /**
     * Advance the cursor to the next result record. When a QueryResult is first created, the
     * cursor is positioned before the first record, so that you must call next() before
     * attempting to access the first record.
     * @return true if the cursor is now pointing to a valid result record, or false at the end
     */
    public boolean next() {
        boolean result = m_iterator.hasNext();
        if (result) {
            QueryResultRow row = (QueryResultRow) m_iterator.next();
            Token token = row.getToken();
            Defquery query = row.getQuery();
            m_context = m_globalContext.push();
            try {
                query.ready(token, m_context);
            } catch (JessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * Dispose of this QueryResult.
     */
    public void close() {
        m_iterator = null;
    }

    /**
     * Return the value of the given variable in the current result record, as a String.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a String, or the cursor is invalid.
     */
    public String getString(String variableName) throws JessException {
        checkValidCursor();
        Value variable = m_context.getVariable(variableName);
        return variable.stringValue(m_context);
    }

    private void checkValidCursor() throws JessException {
        if (m_iterator == null) {
            throw new JessException("QueryResult.get", "QueryResult is closed", "");
        } else if (m_context == null) {
            if (m_iterator.hasNext())
                throw new JessException("QueryResult.get", "The cursor is before the first row; you must call next() before accessing query result", "");
            else
                throw new JessException("QueryResult.get", "Your query returned zero rows", "");            
        }
    }

    /**
     * Return the value of the given variable in the current result record, as a symbol.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a symbol, or the cursor is invalid.
     */
    public String getSymbol(String variableName) throws JessException {
        checkValidCursor();
        return m_context.getVariable(variableName).symbolValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, interpreted as a Jess Boolean value.
     * The symbol FALSE signifies false; anything else is true.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable can't be interpreted as a Boolean, or the cursor is invalid.
     */
    public boolean getBoolean(String variableName) throws JessException {
        checkValidCursor();
        Value var = m_context.getVariable(variableName);
        return !Funcall.FALSE.equals(var);
    }

    /**
     * Return the value of the given variable in the current result record, as a byte.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public byte getByte(String variableName) throws JessException {
        checkValidCursor();
        return (byte) m_context.getVariable(variableName).intValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a short.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public short getShort(String variableName) throws JessException {
        checkValidCursor();
        return (short) m_context.getVariable(variableName).intValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as an int.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public int getInt(String variableName) throws JessException {
        checkValidCursor();
        return m_context.getVariable(variableName).intValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a long.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public long getLong(String variableName) throws JessException {
        checkValidCursor();
        return m_context.getVariable(variableName).longValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a float.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public float getFloat(String variableName) throws JessException {
        checkValidCursor();
        return (float) m_context.getVariable(variableName).floatValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a double.
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a number, or the cursor is invalid.
     */
    public double getDouble(String variableName) throws JessException {
        checkValidCursor();
        return m_context.getVariable(variableName).floatValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a Java object
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable does not hold a Java object, or the cursor is invalid.
     */
    public Object getObject(String variableName) throws JessException {
        checkValidCursor();
        return m_context.getVariable(variableName).javaObjectValue(m_context);
    }

    /**
     * Return the value of the given variable in the current result record, as a jess.Value object
     * @param variableName the name of a variable mentioned anywhere in the query
     * @return the value of that variable
     * @throws JessException if the variable is undefined, or the cursor is invalid.
     */
    public Value get(String variableName) throws JessException {
        return m_context.getVariable(variableName);
    }
}
