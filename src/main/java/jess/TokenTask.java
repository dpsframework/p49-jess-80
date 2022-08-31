package jess;

/**
 * A TokenTask is an action to take when a match occurs in the Rete network.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */
interface TokenTask {
    void tokenMatchesLeft(int tag, Token lt, Token rt, Context context) throws JessException;
    void tokenMatchesRight(int tag, Token lt, Token rt, Context context) throws JessException;
}
