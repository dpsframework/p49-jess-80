package jess;

/**
 * Just a query/token tuple. This is needed because to resolve the variables in a QueryResult,
 * you may need to know which branch of an "or" a token came from.
 *
 * (C) 2007 Sandia National Laboratories
 */
class QueryResultRow {
    private final Defquery m_query;
    private final Token m_token;

    public QueryResultRow(Defquery defquery, Token token) {
        m_query = defquery;
        m_token = token;
    }

    public Defquery getQuery() {
        return m_query;
    }

    public Token getToken() {
        return m_token;
    }


    public boolean equals(Object obj) {
        if (! (obj instanceof QueryResultRow))
            return false;
        QueryResultRow other = (QueryResultRow) obj;
        return m_query == other.m_query && m_token.equals(other.m_token); 
    }
}
