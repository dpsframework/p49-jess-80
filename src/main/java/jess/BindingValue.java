
package jess;
import java.io.Serializable;

/**
 * A class to represent a location within a rule LHS, used
 * internally. It is 'self-resolving' using Context. You shouldn't need
 * to use this directly except to implement an Accelerator.
 * <P>
 * (C) 2007 Sandia National Laboratories<BR>
 */

public class BindingValue extends Value implements Serializable {
    private String m_name;
    private int m_factNumber, m_slotIndex, m_subIndex, m_type;
    private ConditionalElementX m_pattern;
    static boolean DEBUG = true;

    BindingValue(String name, ConditionalElementX patt, int factIndex, int slotIndex,
                 int subIndex, int type) {
        m_name = name;
        m_pattern = patt;
        m_factNumber = factIndex;
        m_slotIndex = slotIndex;
        m_subIndex = subIndex;
        m_type = type;
    }

    BindingValue(BindingValue v) {
        super(v);
        m_name = v.m_name;
        m_factNumber = v.m_factNumber;
        m_slotIndex = v.m_slotIndex;
        m_subIndex = v.m_subIndex;
        m_type = v.m_type;
        m_pattern = v.m_pattern;
    }

    BindingValue(String name) {
        this(name, null, -1, -1, -1, RU.LOCAL);
    }

    /**
     * Sets the fact number associated with this binding to 0.
     */
    public void resetFactNumber() {
        m_factNumber = 0;
    }

    /**
     * Returns the name of the variable.
     * @return the variable name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the fact index within a Rete Token.
     * @return the fact index
     */
    public int getFactNumber() {
        return m_factNumber;
    }

    /**
     * Returns the slot index within a fact.
     * @return the slot index
     */
    public int getSlotIndex() {
        return m_slotIndex;
    }

    /**
     * Returns the index within a multifield.
     * @return the subindex, or -1 if none
     */
    public int getSubIndex() {
        return m_subIndex;
    }

    /**
     * Returns the type of the binding.
     * @return the binding type
     */
    public int getType() {
        return m_type;
    }

    ConditionalElementX getCE() { return m_pattern; }

    /**
     * Returns the value of the bound variable by extracting it from a Token
     * stored in the execution context.
     * @param c an execution context  containing a token
     * @return the value of the variable
     * @throws JessException if anything goes wrong
     */
    public Value resolveValue(Context c) throws JessException {
        if (c == null)
            throw new JessException("BindingValue.resolveValue",
                                    "Null context ", "");

        try {
            Token t = c.getToken();
            Fact f;

            Value var;
            if (t == null || m_factNumber == t.size()) {
                f = c.getFact();
            } else {
                f = t.fact(m_factNumber);
            }

            var = f.get(m_slotIndex);

            if (m_subIndex == -1) // -1 here means no subfield
                return var;

            else {
                /*
            // "New style" multifield matching.
            String name = f.getFactId() + "_" + m_slotIndex + "_" + m_subIndex;
            try {
                return c.getVariable(name);
            } catch (JessException ex) {
                System.out.println(c);
                throw ex;
            }  */
                ValueVector subv = var.listValue(null);
                return subv.get(m_subIndex);
            }
        } catch (JessException ex) {
            JessException ex2 = new JessException("BindingValue.resolveValue", "Error getting value of variable", toString(), ex);
            ex2.addContext("getting variable value", c);
            throw ex2;
        }
    }

    public final Object javaObjectValue(Context c) throws JessException {
        return resolveValue(c).javaObjectValue(c);
    }

    public final Fact factValue(Context c) throws JessException {
        return resolveValue(c).factValue(c);
    }

    public final ValueVector listValue(Context c) throws JessException {
        return resolveValue(c).listValue(c);
    }

    public final int intValue(Context c) throws JessException {
        return resolveValue(c).intValue(c);
    }

    public final double floatValue(Context c) throws JessException {
        return resolveValue(c).floatValue(c);
    }

    public final double numericValue(Context c) throws JessException {
        return resolveValue(c).numericValue(c);
    }

    public final String symbolValue(Context c) throws JessException {
        return resolveValue(c).symbolValue(c);
    }

    public final String variableValue(Context c)  {
        return m_name;
    }

    public final String stringValue(Context c) throws JessException {
        return resolveValue(c).stringValue(c);
    }

    public String toString() {
        if (DEBUG) {
            return "?" + m_name + "(" +
                    m_factNumber + "," +
                    m_slotIndex + "," +
                    m_subIndex + ")";
        } else {
            return "?" + m_name;
        }
    }

    public int hashCode() {
        return m_factNumber + 512*m_slotIndex + 512*512*m_subIndex;
    }


    /**
     * This overrides the overloaded equals() in Value.
     * @noinspection CovariantEquals
     * @param o a Value to compare to
     * @return true if the two Values are equal
     */
    public boolean equals(Value o) {
        if (! (o instanceof BindingValue))
            return false;

        BindingValue other = (BindingValue) o;

        return (m_factNumber == other.m_factNumber &&
                m_slotIndex == other.m_slotIndex &&
                m_subIndex == other.m_subIndex);
    }
}


