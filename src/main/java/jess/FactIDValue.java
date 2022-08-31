
package jess;
import java.io.Serializable;

/**
 <p>Use this subclass of Value when you want to create a Value that
 represents a <tt>Fact</tt>. </p>

 <p>In previous versions of Jess, fact-id's were more like integers; now
 they are really references to facts. As such, a fact-id must represent
 a valid {@link Fact} object.  Call
 {@link #javaObjectValue} to get the
 <tt>Fact</tt> object, and call
 {@link Fact#getFactId} to get the fact-id as an integer. This
 latter manipulation will now rarely, if ever, be necessary.
  </P>
  (C) 2013 Sandia Corporation<br>
 */

public class FactIDValue extends Value implements Serializable, Comparable {
    /**
     * Create a FactIDValue
     * @param f The fact
     * @exception JessException If the type is invalid
     */

    public FactIDValue(Fact f) throws JessException {
        super(f);
    }

    public int intValue(Context c) {
        return getFactId();
    }

    public double numericValue(Context c) {
        return intValue(c);
    }

    public Fact factValue(Context c) throws JessException {
        return (Fact) javaObjectValue(c);
    }

    public String toString() {
        return ("<Fact-" + getFactId() + ">");
    }

    public int hashCode() {
        return getFactId();
    }

    public int getFactId() {
        try {
            return ((Fact) factValue(null)).getFactId();
        } catch (JessException e) {
            throw new AssertionError("Not reached");
        }
    }

    public boolean equals(Value v) {
        if (this == v)
            return true;

        if (v.type() != type())
            return false;

        return getFactId() == ((FactIDValue) v).getFactId();
    }

    public int compareTo(Object c) {
        return getFactId() - ((FactIDValue) c).getFactId();
    }

}

