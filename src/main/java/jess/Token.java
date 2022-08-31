package jess;
import java.io.Serializable;

/**
 <p>A Token is the fundamental unit of communication in the Rete
 network. Each Token represents one or more facts and an indication
 of whether those facts are being asserted or being retracted.</p>

 <p>The Token class is used to represent partial matches in
 the Rete network. You'll use it if
 you're writing an {@link Accelerator} (not documented here), if
 you're working with old-style queries, or possibly if you're writing a
 {@link Strategy} implementation.
 </p>

 (C) 2013 Sandia Corporation<br>
 @see jess.Accelerator
 @see jess.Strategy
 */

public class Token implements Serializable {

    // Tokens refer to 'parents' (of which they are a superset)
    private Token m_parent;
    int m_negcnt;

    // sortcode is used by the engine to hash tokens and prevent long
    // linear memory searches
    int m_sortcode;

    private int m_size;
    private Fact m_fact;


    // Needed for "logical" CE.
    Token getParent() {
        return m_parent;
    }

    /**
     * Return the last fact in the Token (the "most recent" one). This
     * may not be a Fact from working memory, but rather a "view" of a
     * working memory element. You can call getIcon() on the returned
     * fact to get the canonical working memory element corresponding
     * to the returned fact.
     * @see fact(int)
     * @return the fact
     */

    public final Fact topFact() {
        return m_fact;
    }

    /**
     * Return a fact from this token. This may not be a Fact from
     * working memory, but rather a "view" of a working memory
     * element. You can call getIcon() on the returned fact to get the
     * canonical working memory element corresponding to the returned
     * fact.
     * @param i the index (0-based) of the fact to retrieve. The fact that
     * matches the first pattern on the LHS of a corresponding rule is fact(0);
     * the next is fact(1), etc.
     * @see Fact#getIcon()
     * @return the fact
     */

    public final Fact fact(int i) {
        int j = m_size - i;

        if (j == 1)
            return m_fact;
        else if (j < 0)
            throw new IndexOutOfBoundsException("Internal error: fact index " + i + " too large for token size " + m_size);

        Token where = this;
        while (--j > 0)
            where = where.m_parent;

        return where.m_fact;
    }

    /**
     * Returns the number of facts in this token
     * @return the size
     */
    public final int size() {
        return m_size;
    }

    /**
     * Construct a token containing a single Fact.
     */

    public Token(Fact firstFact) throws JessException {
        // m_parent = null;
        ++m_size;
        m_fact  = firstFact;
        m_totalTime = firstFact.getTime();
        m_time = m_totalTime;
        // m_negcnt = 0;
        m_sortcode = firstFact.getFactId();
    }


    /**
     * Create a new Token containing the same data as an old one
     */
    public Token(Token t, Fact newFact) throws JessException {
        m_fact = newFact;
        m_parent = t;
        // m_negcnt = 0;
        m_size = t.m_size + 1;
        m_sortcode = (t.m_sortcode << 3) + newFact.getFactId();
        int time = newFact.getTime();
        m_totalTime = t.m_totalTime + time;
        m_time = Math.max(time, t.m_time);
    }

    /**
     * Create a new Token containing the same data as an old one
     */

    public Token(Token lt, Token rt) throws JessException {
        this(lt, rt.topFact());
    }

    /**
     * Create a new Token identical to an old one
     */
    public Token(Token t) throws JessException {
        m_fact = t.m_fact;
        m_parent = t.m_parent;
        // m_negcnt = 0;
        m_size = t.m_size;
        m_sortcode = t.m_sortcode;
        m_time = t.m_time;
        m_totalTime = t.m_totalTime;
        m_negcnt = t.m_negcnt;
    }


    /** The total time step represented by all of this token's facts. */
    private int m_totalTime, m_time;
    void updateTime(Rete engine) {
        int time = engine.getTime();
        m_totalTime = m_totalTime - m_fact.getTime() + time;
        m_time = time;
    }

    /**
     * Returns the largest <i>pseudotime</i> recorded in any of the facts in this Token. The rule engine
     * keeps track of a monotonically increasing quantity called pseudotime; facts asserted later will have
     * larger pseudotime values. You can use pseudotime to order activations when writing a Strategy implementation.
     * @return the largest pseudotime of any fact in this Token.
     * @see Strategy
     */
    public int getTime() {
        return m_time;
    }

    /**
     * Returns the sum of the <i>pseudotimes</i> recorded in all of the facts in this Token. The rule engine
     * keeps track of a monotonically increasing quantity called pseudotime; facts asserted later will have
     * larger pseudotime values. You can use pseudotime to order activations when writing a Strategy implementation.
     * @return the sum of all pseudotimes of the facts in this Token.
     * @see Strategy
     */
    public int getTotalTime() {
        return m_totalTime;
    }

    /**
     * Compare the data in this token to another token.  The tokens are
     * assumed to be of the same size (same number of facts).  We have
     * to compare all the fact data if the fact IDs are the same, since
     * each fact can exist in different multifield versions. This could
     * be skipped if we had a fast test for multislot existence...
     * @param t Another token to compare to
     * @return True if the tokens represent the same list of facts (tags
     * are irrelevant)
     */
    final public boolean dataEquals(Token t) {
        if (t == this)
            return true;

        else if (m_sortcode != t.m_sortcode)
            return false;

        else if (m_fact.getFactId() != t.m_fact.getFactId())
            return false;

        else if (!m_fact.equals(t.m_fact))
            return false;

        else if (m_parent == t.m_parent)
            return true;

        else
            return m_parent.dataEquals(t.m_parent);
    }

    final public boolean fastDataEquals(Token t) {
        if (t == this)
            return true;

        else if (m_sortcode != t.m_sortcode)
            return false;

        else if (m_fact.getFactId() != t.m_fact.getFactId())
            return false;

        else if (m_parent == t.m_parent)
            return true;

        else
            return m_parent.fastDataEquals(t.m_parent);
    }


    /**
     * Compare this token to another object.
     * @param o Another object to compare to
     * @return True if the object is a Token and dataEquals returns true.
     */

    public boolean equals(Object o) {
        if (o instanceof Token)
            return dataEquals((Token) o);
        else
            return false;
    }

    /**
     * Return a string (useful for debugging) describing this token.
     * @return The formatted String
     */
    public String toString() {

        StringBuffer sb = new StringBuffer(100);
        sb.append("[Token: size=");
        sb.append(m_size);
        sb.append(";sortcode=");
        sb.append(m_sortcode);
        sb.append(";negcnt=");
        sb.append(m_negcnt);
        for (int i=0; i<m_size; i++) {
            sb.append("\nf-");
            Fact f = fact(i);
            sb.append(f.getFactId());
            sb.append(" ");
            sb.append(f.toString());
            sb.append(";");
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * Return a string describing a list of facts
     */
    String factList() {
        StringBuffer sb = new StringBuffer(100);
        boolean first = true;
        for (int i=0; i<size(); i++) {
            if (!first)
                sb.append(",");
            int id = fact(i).getFactId();
            if (id > -1) {
                sb.append(" f-");
                sb.append(id);
            }
            first = false;
        }
        return sb.toString();
    }

    /**
     * Use the sortcode, based on the contained facts, as the hashcode.
     * @return A semi-unique identifier */

    public int hashCode() {
        return m_sortcode;
    }

    /**
     * A chance for a token to duplicate itself
     * Used by extensions
     * @return this token, or possibly a duplicate, modified token
     * @param b true if this token will represent test success
     * @throws JessException if anything goes wrong
     */

    public Token prepare(boolean b) throws JessException {
        return this;
    }

}

