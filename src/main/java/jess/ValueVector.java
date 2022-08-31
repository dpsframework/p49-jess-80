package jess;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * A self-extending array that only holds {@link Value} objects.
 * <p/>
 * The <tt>ValueVector</tt> class is Jess's internal representation
 * of a Lisp list, and therefore has a central role in programming with
 * Jess in Java. The <tt>ValueVector</tt> class itself is used to
 * represent generic lists, while specialized subclasses
 * are used as function calls ({@link Funcall} and facts
 * ({@link Fact}).
 * <p/>
 * Working with <tt>ValueVector</tt> itself is simple. Its API is
 * similar to the <a href="http://java.sun.com/j2se/1.5.0/docs/api/java.util.List.html">List</a> interface.
 * <tt>ValueVector</tt> is a self-extending array: when new elements are added, the
 * <tt>ValueVector</tt> grows in size to accomodate them. Here is a bit
 * of example Java code in which we create the Jess list
 * <tt>(a b c)</tt>.
 * <p/>
 * <pre>
 * ValueVector vv = new ValueVector();
 * vv.add("a");
 * vv.add("b");
 * vv.add("c");
 * <p/>
 * // Prints "(a b c)"
 * System.out.println(vv.toStringWithParens());
 * </pre>
 * To pass a ValueVector from Java to Jess, you should enclose it in a
 * {@link Value} object of type {@link RU#LIST}.
 * <pre>
 * Value myList = new Value(vv, RU.LIST);
 * </pre>
 * ValueVector is not synchronized, so you must be careful of multithreading issues.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 *
 * @see Value
 */

public class ValueVector implements Cloneable, Serializable {

    /**
     * The elements themselves. Should never be null.
     */
    protected Value[] m_v;

    /**
     * The current number of elements. Should always be nonnegative.
     */
    protected int m_ptr = 0;

    /**
     * Construct a ValueVector of the default capacity (10).
     */

    public ValueVector() {
        this(10);
    }

    /**
     * Construct a ValueVector of the given capacity.
     *
     * @param size The number of Values this vector can hold at creation
     */
    public ValueVector(int size) {
        m_v = new Value[size];
    }

    /**
     * Returns the size of this ValueVector.
     *
     * @return the size of this ValueVector
     */
    public final int size() {
        return m_ptr;
    }

    /**
     * Create a shallow copy of this ValueVector.
     *
     * @return the copy
     */
    public Object clone() {
        return cloneInto(new ValueVector(m_ptr));
    }

    /**
     * Make the parameter into a copy of this ValueVector.
     *
     * @param vv a ValueVector, whose contents are erased.
     * @return the parameter
     */
    public ValueVector cloneInto(ValueVector vv) {
        if (m_ptr > vv.m_v.length) {
            vv.m_v = new Value[m_ptr];
        }
        vv.m_ptr = m_ptr;
        System.arraycopy(m_v, 0, vv.m_v, 0, m_ptr);
        return vv;
    }

    /**
     * Returns the entry at position i in this ValueVector.
     *
     * @param i the 0-based index of the Value to fetch
     * @return the Value
     */

    public Value get(int i) throws JessException {
        if (i < 0 || i >= m_ptr)
            throw new JessException("ValueVector.get",
                                    "Index " + i + " out of bounds on this vector:",
                                    toStringWithParens());
        else
            return m_v[i];
    }

    /**
     * Set the length of this ValueVector. If necessary the storage
     * will be extended. This can leave some of the elements in the
     * ValueVector with null contents.
     *
     * @param i The new length (>= 0)
     * @return this object, so methods can be chained.
     */
    public ValueVector setLength(int i) {
        if (i > m_v.length) {
            Value[] nv = new Value[i];
            System.arraycopy(m_v, 0, nv, 0, m_v.length);
            m_v = nv;
        }
        m_ptr = i;
        return this;
    }


    /**
     * Set the element at position i to the given value. The argument must be >= 0
     * and &lt; the return value of size().
     *
     * @param val the new value
     * @param i   the index at which to place it.
     * @return this object, so methods can be chained.
     */

    public final ValueVector set(Value val, int i) throws JessException {
        if (i < 0 || i >= m_ptr)
            throw new JessException("ValueVector.set",
                                    "Bad index " + i +
                                            " in call to set() on this vector:",
                                    toStringWithParens());

        m_v[i] = val;
        return this;
    }

    /**
     * Add a new element to the end of this ValueVector. The storage
     * will be extended if necessary. This method returns the <tt>ValueVector</tt> object
     * itself, so that <tt>add()</tt> calls can be chained together for convenience:
     * <p/>
     * <pre>
     * ValueVector vv = new ValueVector();
     * vv.add(new Value("a", RU.SYMBOL)).add(new Value("b", RU.SYMBOL)).add(new Value("c", RU.SYMBOL));
     * <p/>
     * // Prints "(a b c)"
     * System.out.println(vv.toStringWithParens());
     * </pre>
     *
     * @param val the value to add.
     * @return this object, so methods can be chained.
     */
    public final ValueVector add(Value val) {
        if (m_ptr >= m_v.length) {
            Value[] nv = new Value[m_v.length * 2];
            System.arraycopy(m_v, 0, nv, 0, m_v.length);
            m_v = nv;
        }
        m_v[m_ptr++] = val;
        return this;
    }

    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add, interpreted as a symbol
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(String val) throws JessException {
        return add(new Value(val, RU.SYMBOL));
    }

    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add, interpreted as an RU.INTEGER
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(int val) throws JessException {
        return add(new Value(val, RU.INTEGER));
    }

    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add, interpreted as an RU.FLOAT
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(double val) throws JessException {
        return add(new Value(val, RU.FLOAT));
    }

    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add; will become one of the symbols TRUE or FALSE
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(boolean val) {
        return add(val ? Funcall.TRUE : Funcall.FALSE);
    }


    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add, as an RU.LONG
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(long val) throws JessException {
        return add(new LongValue(val));
    }


    /**
     * Add a new element to the end of this ValueVector.
     *
     * @param val the value to add, as an RU.JAVA_OBJECT
     * @return this object, so methods can be chained.
     * @see #add(Value)
     */
    public final ValueVector add(Object val) {
        if (val instanceof Value)
            return add((Value) val);
        else
            return add(new Value(val));
    }


    /**
     * Remove the item at index i from the ValueVector; move all the
     * higher-numbered elements down.
     *
     * @return this object, so methods can be chained.
     */
    public final ValueVector remove(int i) throws JessException {
        if (i < 0 || i >= m_ptr)
            throw new JessException("ValueVector.set",
                                    "Bad index " + i +
                                            " in call to remove() on this vector:",
                                    toStringWithParens());
        if (i < (m_ptr - 1))
            System.arraycopy(m_v, i + 1, m_v, i, m_ptr - i);
        m_v[--m_ptr] = null;
        return this;
    }


    /**
     * Compare this ValueVector to another object.
     *
     * @param o another object
     * @return true if the object is a ValueVector of the same size
     *         containing Values that compare equal to the ones in this
     *         ValueVector.
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (! (o instanceof ValueVector))
            return false;

        ValueVector vv = (ValueVector) o;

        if (m_ptr != vv.m_ptr)
            return false;

        for (int i = m_ptr - 1; i >= 0; i--)
            if (!m_v[i].equals(vv.m_v[i]))
                return false;

        return true;
    }


    /**
     * System.arraycopy DeLuxe for ValueVectors.
     * Contributed by Thomas Barnekow.
     *
     * @param src     the ValueVector to copy from
     * @param srcPos  the index to copy frmo
     * @param dest    the ValueVector to copy to
     * @param destPos the index to copy to
     * @param length  the number of elements to copy
     */
    public static void copy(ValueVector src, int srcPos, ValueVector dest,
                            int destPos, int length) {
        // What is the minimal destination length?
        int minDestSize = destPos + length;

        // Extend destination length if necessary
        int oldDestSize = dest.m_ptr;
        if (minDestSize > oldDestSize)
            dest.setLength(minDestSize);

        // Fill in nil values if necessary
        for (int i = oldDestSize; i < destPos; i++)
            dest.m_v[i] = Funcall.NIL;

        System.arraycopy(src.m_v, srcPos, dest.m_v, destPos, length);
    }

    /**
     * Appends all Values in the argument ValueVector.
     * Contributed by Thomas Barnekow.
     *
     * @param vv a ValueVector to copy elements from.
     */
    public ValueVector addAll(ValueVector vv) {
        copy(vv, 0, this, m_ptr, vv.m_ptr);
        return this;
    }

    /**
     * Return a String version of this ValueVector, without parentheses.
     *
     * @return the String
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        for (int i = 0; i < m_ptr; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(m_v[i]);
        }
        return sb.toString();
    }

    /**
     * Return a String version of this ValueVector, with parentheses
     * around all ValueVectors.
     *
     * @return the String
     */
    public String toStringWithParens() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("(");
        for (int i = 0; i < m_ptr; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(m_v[i].toStringWithParens());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Add all the members of the collection to this ValueVector. All the objects
     * in the collection must be {@link Value}s.
     *
     * @param list a collection of jess.Value objects
     * @throws ClassCastException if any of the members of the collection are not jess.Value objects
     */
    public void addAll(Collection list) {
        for (Iterator it = list.iterator(); it.hasNext();) {
            Value value = (Value) it.next();
            add(value);
        }
    }

    /**
     * Returns the sum of the hashcodes of the members.
     */

    public int hashCode() {
        try {
            int retval = 0;
            for (int i = 0; i < size(); i++)
                retval = 31*retval + get(i).hashCode();
            return retval;
        } catch (JessException e) {
            return 0;
        }
    }

    /**
     * Returns true if this ValueVector contains the given Value.
     * @param value a Value
     * @return true if value is a member of this ValueVector
     */
    public boolean contains(Value value) {
        for (int i = 0; i < m_ptr; i++) {
            if (m_v[i].equals(value))
                return true;
        }
        return false;
    }
}


