package jess;

import java.io.PrintWriter;
import java.io.Serializable;

/**
 * A list of facts that satisfy a rule. An activation contains
 * enough info to bind a rule's variables. You might use this class
 * if you're writing your own {@link Strategy} implementation, or in
 * a {@link JessListener} implementation.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 * @see jess.Strategy
 * @see JessListener
 */

public class Activation implements Serializable {

   /**
    * Storage index in priority queue
    */
    private int m_index = -1;

    /**
     Token is the token that got us fired.
     */

    private Token m_token;

    /**
     * Whether or not this activation has been cancelled.
     */
    private volatile boolean m_inactive;


    /**
     * The evaluated salience value
     */
    private int m_salience;


    /**
     * Get the Rete network Token that caused this Activation.
     * @return The token.
     */

    public final Token getToken() {
        return m_token;
    }

    /**
     m_rule is the rule we will fire.
     */

    private Defrule m_rule;

    /**
     * Return the activated rule.
     * @return The rule.
     */

    public final Defrule getRule() {
        return m_rule;
    }


    Activation(Rete engine, Token token, Defrule rule) throws JessException {
        m_token = token;
        m_rule = rule;
        m_salience = m_rule.getSalience(engine);
    }

    Activation(Rete engine, Activation other) throws JessException {
        this(engine, other.m_token, other.m_rule);
        m_index = other.m_index;
    }

    /**
     * Query if this activation has been cancelled, or false if it is valid.
     * @deprecated This method always returns "false". There's no such thing as an "inactive" activation.
     * @return True if this activation has been cancelled.
     */

    public boolean isInactive() {
        return false;
    }

    /**
     * Evaluate and return the current salience for the rule
     * referenced in this activation.
     * @return The salience value.
     */
    public int getSalience() {
        return m_salience;
    }

    void fire(Rete engine, Context context) throws JessException {
        m_rule.fire(m_token, engine, context);
    }

    private int m_seq;

    void setSequenceNumber(int i) {
        m_seq = i;
    }

    void debugPrint(PrintWriter ps) {
        m_rule.debugPrint(m_token, m_seq, ps);
    }

    /**
     Compare this object to another Activation.
     @param o The Activation to compare to.
     */

    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Activation))
            return false;

        else {
            Activation a = (Activation) o;
            return
                    this.m_rule == a.m_rule &&
                    this.m_token.dataEquals(a.m_token);
        }
    }

    public int hashCode() {
        return m_rule.getName().hashCode() ^ m_token.hashCode();
    }

    boolean getAutoFocus(Rete engine) throws JessException {
        return m_rule.getAutoFocus() || engine.findModule(m_rule.getModule()).getAutoFocus();
    }

    String getModule() {
        return m_rule.getModule();
    }

    void evalSalience(Rete r) throws JessException {
        m_salience = m_rule.evalSalience(r);
    }

    int getIndex() {
        return m_index;
    }

    void setIndex(int index) {
        m_index = index;
    }

    /**
     * Produce a string representation of this Activation for use in debugging.
     * @return The string representation
     */

    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Activation: ");
        sb.append(m_rule.getDisplayName());
        sb.append(" ");
        sb.append(m_token.factList());
        sb.append(" ; time=");
        sb.append(m_token.getTime());
        sb.append(" ; totalTime=");
        sb.append(m_token.getTotalTime());
        sb.append(" ; salience=");
        sb.append(getSalience());
        /* sb.append(" ; index=");
        sb.append(m_index); */ 
        sb.append("]");
        return sb.toString();
    }

    void replaceToken(Token token) {
        m_token = token;
    }
}

