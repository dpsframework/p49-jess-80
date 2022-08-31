package jess;

import java.io.Serializable;
import java.util.regex.*;
import java.util.regex.Pattern;

/**
 <p>The class <tt>jess.Value</tt> is probably the one you'll use the most
 in working with Jess. A <tt>Value</tt> is a self-describing data
 object. Every datum in Jess is contained in one. Once it is
 constructed, a <tt>Value</tt>'s type and contents cannot be
 changed; it is <i>immutable.</i></p>

 <p>
 <tt>Value</tt> objects are constructed by specifying the data and
 (usually) the type. Each overloaded constructor assures that the given
 data and the given type are compatible. Note that for each
 constructor, more than one value of the <tt>type</tt> parameter may be
 acceptable.</p>

 <p><tt>jess.Value</tt> has a number of subclasses:
 {@link Variable}, {@link FuncallValue}, {@link FactIDValue}, and
 {@link LongValue} are the four of
 most interest to the reader. When you wish to create a value to represent a
 variable, a function call, a fact, or a Java long, you must use the
 appropriate subclass.</p>

 <blockquote>
 Note to the design-minded: We could have used a Factory pattern here and
 hidden the subclasses from the programmer; now backwards compatibility
 prevents us from hiding the constructors although a factory will probably
 be introduced eventually.
 </blockquote>


 <p><tt>Value</tt> supports a number of functions to get the actual data out
 of a <tt>Value</tt>object. Some <tt>jess.Value</tt> objects may need to be <i>resolved</i> before
 use. To resolve a <tt>jess.Value</tt> means to interpret it in a
 particular context. <tt>jess.Value</tt> objects can represent both
 static values (symbols, numbers, strings) and dynamic ones (variables,
 function calls). It is the dynamic ones that obviously have to be
 interpreted in context. The class {@link Context} provides a context for interpreting <tt>Values</tt>.
 </p>

 <p>
 All the <tt>jess.Value</tt> member functions, like
 {@link #intValue}, that accept a <tt>jess.Context</tt> as an
 argument are <i>self-resolving;</i>  that is, if a <tt>jess.Value</tt>
 object represents a function call, the call will be executed in the
 given <tt>jess.Context</tt>, and the <tt>intValue()</tt> method will
 be called on the result. Therefore, you often don't need to worry
 about resolution as it is done automatically. There are several cases
 where you will, however.

 <ul>
 <li><i>When interpreting arguments to a function written in Java.</i>
 The parameters passed to a Java Userfunction may themselves represent
 function calls. It may be important, therefore, that these values be
 resolved only once, as these functions may have side-effects (I'm
 tempted to use the computer-science word: these functions may not be
 <i>idempotent.</i> Idempotent functions have no side-effects and thus
 may be called multiple times without harm.) You can accomplish this by
 calling one of the <tt>(x)Value()</tt> methods and storing the return
 value, using this return value instead of the parameter
 itseld. Alternatively, you may call {@link #resolveValue(Context)} and store
 the return value in a new <tt>jess.Value</tt> variable, using this
 value as the new parameter. Note that the <tt>type()</tt> method will
 return {@link RU#VARIABLE} for a <tt>jess.Value</tt> object that
 refers to a variable, regardless of the type of the value the variable
 is bound to. The resolved value will return the proper type.
 <p/>
 Note that arguments to <tt>deffunctions</tt> are resolved
 automatically, before your Jess language code runs.</li>

 <li><i>when returning a <tt>jess.Value</tt> object from a function
 written in Java.</i> If you return one of a function's parameters from
 a Java Userfunction, be sure to return the return value of
 <tt>resolveValue()</tt>, not the parameter itself.</li>

 <li><i>When storing a <tt>jess.Value</tt> object.</i> It is important
 that any values passed out of a particular execution context be
 resolved; for example, before storing a Value object in a Map,
 <tt>resolveValue()</tt> should always be called on both the key and
 object. </li>
 </ul>

 <p> If you try to convert random values by creating a Value and retrieving
 it as some other type, you'll generally get a {@link JessException}. However, some
 types can be freely interconverted: for example, integers and floats.
 <p/>

 <p>
 (C) 2013 Sandia Corporation<BR>
 */

public class Value implements Serializable {
    private static final int STRING_TYPES = RU.SYMBOL | RU.STRING | RU.VARIABLE |
            RU.MULTIVARIABLE | RU.SLOT | RU.MULTISLOT;

    private static final int NUM_TYPES = RU.INTEGER | RU.FLOAT | RU.LONG;

    private int m_type;
    private int m_intval;
    private double m_floatval;
    private Object m_objectval;
    private static Pattern s_factidPattern = Pattern.compile("<Fact-([0-9]+)>");

    /**
     * Contruct a value of integral type. Allowed type values are {@link RU#NONE}
     * and {@link RU#INTEGER}.
     * @param value the value
     * @param type the type
     * @exception JessException if the value and type don't match.
     */
    public Value(int value, int type) throws JessException {
        m_type = type;
        switch (m_type) {
            case RU.NONE:
            case RU.INTEGER:
                m_intval = value;
                break;

            default:
                throw typeError("Value", "an integral type", type);
        }
    }

    Value(int value) {
        m_type = RU.INTEGER;
        m_intval = value;
    }

    /**
     * Contruct a value that is a copy of another Value.
     * @param v the Value to copy
     */
    public Value(Value v) {
        m_type = v.m_type;
        m_intval = v.m_intval;
        m_floatval = v.m_floatval;
        m_objectval = v.m_objectval;
    }

    /**
     * Contruct a value of String type. Allowed type values are
     * {@link RU#SYMBOL}, {@link RU#STRING}, {@link RU#SLOT} and {@link RU#MULTISLOT}.
     * @param s the value
     * @param type the type
     * @exception JessException if the value and type don't match.
     */

    public Value(String s, int type) throws JessException {
        if (!(this instanceof Variable) && (type == RU.VARIABLE || type == RU.MULTIVARIABLE))
            throw new JessException("Value.Value",
                    "Cannot use jess.Value to represent variable " + s + ".",
                    "You must use class jess.Variable");

        if ((type & STRING_TYPES) == 0)
            throw typeError("Value", "a string type", type);

        m_type = type;
        m_objectval = s;
    }

    /**
     * Contruct a value of list type. Only allowed type value is {@link RU#LIST}.
     * @param f the value
     * @param type the type
     * @exception JessException if the value and type don't match.
     */

    public Value(ValueVector f, int type) throws JessException {
        if (f instanceof Fact && type == RU.FACT)
            throw new JessException("Value.Value",
                    "Cannot use jess.Value to represent fact-ids.",
                    "You must use class jess.FactIDValue");

        if (!(this instanceof FuncallValue) && type == RU.FUNCALL)
            throw new JessException("Value.Value",
                    "Cannot use jess.Value to represent the function call " +
                    f.toStringWithParens(),
                    "You must use class jess.FuncallValue");

        if (type != RU.FUNCALL && type != RU.LIST)
            throw typeError("Value", "a vector type", type);

        m_type = type;
        m_objectval = f;
    }

    /**
     * Contruct a value of floating-point type. Allowed type values are
     * {@link RU#FLOAT}, {@link RU#LONG}, and {@link RU#INTEGER}.
     * @param d the value
     * @param type the type
     * @exception JessException if the value and type don't match.
     */

    public Value(double d, int type) throws JessException {
        if (type == RU.LONG && !(this instanceof LongValue))
            throw typeError("Value", "You must use class jess.LongValue", type);
        else if ((type & NUM_TYPES) == 0)
            throw typeError("Value", "a float type", type);

        m_type = type;
        if (type == RU.FLOAT || type == RU.LONG)
            m_floatval = d;
        else
            m_intval = (int) d;

    }

    /**
     * Contruct a boolean value object (one of the RU.SYMBOLs "TRUE" or "FALSE".)
     * @param b the value
     */

    public Value(boolean b) {
        m_type = RU.SYMBOL;
        if (b)
            m_objectval = Funcall.TRUE.m_objectval;
        else
            m_objectval = Funcall.FALSE.m_objectval;
    }

    /**
     * Contruct a value of Java object type.
     * @param o the value -- any Java object
     */

    public Value(Object o) {
        if (o != null) {
            m_type = RU.JAVA_OBJECT;
            m_objectval = o;
        } else {
            m_type = Funcall.NIL.m_type;
            m_objectval = Funcall.NIL.m_objectval;
        }
    }

    /**
     * Contruct a value of lambda type.
     * @param function a Deffunction
     */

    public Value(Userfunction function) {
        m_type = RU.LAMBDA;
        m_objectval = function;
    }

    Value(Fact f) throws JessException {
        if (!(this instanceof FactIDValue))
            throw new JessException("Value.Value",
                    "Cannot use jess.Value to represent fact-ids.",
                    "You must use class jess.FactIDValue");

        else if (f == null)
            f = Fact.getNullFact();

        else
            while (f.getIcon() != f)
                f = f.getIcon();

        m_type = RU.FACT;
        m_objectval = f;
    }

    Value() {
        m_type = RU.BINDING;
    }

    /**
     * Returns the contents of this value, as a Java object.
     * @exception JessException if this value does not contain a Java object
     * @return the Java object
     * @deprecated As of Jess 7, use {@link #javaObjectValue(Context)} instead.
     * @param c the execution context used to resolve the value
     */
    public Object externalAddressValue(Context c) throws JessException {
        return javaObjectValue(c);
    }

    /**
     * Returns the contents of this value, as a Java object.
     * @exception JessException if this value does not contain a Java object
     * @return the Java object
     * @param c the execution context used to resolve the value
     */
    public Object javaObjectValue(Context c) throws JessException {
        switch (m_type) {
            case RU.JAVA_OBJECT:
            case RU.LAMBDA:
            case RU.STRING:
            case RU.SYMBOL:
            case RU.FACT:
                return m_objectval;
            case RU.INTEGER:
                return new Integer(m_intval);
            case RU.FLOAT:
                return new Double(m_floatval);

        }
        throw typeError("javaObjectValue", "a Java object");
    }
    /**
     * Returns the contents of this value, as a function call.
     * @exception JessException if this value does not contain a function call.
     * @return the function call object
     * @param c the execution context used to resolve the value
     */

    public Funcall funcallValue(Context c) throws JessException {
        if (m_type == RU.FUNCALL)
            return (Funcall) m_objectval;
        throw typeError("funcallValue", "a function call");
    }

    /**
     * Returns the contents of this value, as a fact.
     * @exception JessException if this value does not contain a fact
     * @return the fact
     * @param c the execution context used to resolve the value
     */

    public Fact factValue(Context c) throws JessException {
        switch (m_type) {
            case RU.JAVA_OBJECT: {
                if (m_objectval instanceof Fact)
                    return (Fact) m_objectval;
                break;
            }
            case RU.INTEGER: {
                return c.getEngine().findFactByID(m_intval);
            }
            case RU.SYMBOL: {
                Matcher m = s_factidPattern.matcher(m_objectval.toString());
                if (m.matches()) {
                    return c.getEngine().findFactByID(Integer.parseInt(m.group(1)));
                }
                break;
            }

        }
        throw typeError("factValue", "a fact");
    }

    /**
     * Returns the contents of this value, as a list.
     * @exception JessException if this value does not contain a list
     * @return the list
     * @param c the execution context used to resolve the value
     * @throws JessException if something goes wrong during value resolution
     */

    public ValueVector listValue(Context c) throws JessException {
        if (m_type == RU.LIST)
            return (ValueVector) m_objectval;
        throw typeError("listValue", "a list");
    }

    /**
     * Returns the contents of this value, as a number.
     * @exception JessException if this value does not contain any kind of number
     * @return the number as a double
     * @param c the execution context used to resolve the value
     */

    public double numericValue(Context c) throws JessException {
        Value v = resolveValue(c);
        switch (v.m_type) {
            case RU.FLOAT:
                return v.m_floatval;
            case RU.INTEGER:
                return v.m_intval;
            case RU.STRING:
            case RU.SYMBOL:
                try {
                    return Double.parseDouble((String) m_objectval);
                } catch (NumberFormatException nfe) {
                    /* FALL THROUGH */
                }
            default:
                throw typeError("numericValue", "a number");
        }
    }

    /**
     * Returns the contents of this value, as an int.
     * @exception JessException if this value does not contain any kind of number
     * @return the number as an int
     * @param c the execution context used to resolve the value
     */

    public int intValue(Context c) throws JessException {
        switch (m_type) {
            case RU.FLOAT:
                return (int) m_floatval;
            case RU.INTEGER:
                return m_intval;
            case RU.STRING:
            case RU.SYMBOL:
                try {
                    return Integer.parseInt((String) m_objectval);
                } catch (NumberFormatException nfe) {
                    /* FALL THROUGH */
                }
            default:
                throw typeError("intValue", "an integer");
        }
    }

    /**
     * Returns the contents of this value, as a long.
     * @exception JessException if this value does not contain any kind of number.
     * @return the number as a long
     * @param c the execution context used to resolve the value
     */

    public long longValue(Context c) throws JessException {
        return (long) numericValue(c);
    }


    /**
     * Returns the contents of this value, as a double.
     * @exception JessException if this value does not contain any kind of number.
     * @return the number as a double
     * @param c the execution context used to resolve the value
     */

    public double floatValue(Context c) throws JessException {
        return numericValue(c);
    }

    /**
     * Returns the contents of this value, as a symbol.
     * @exception JessException if this value does not contain any kind of String.
     * @return the symbol
     * @deprecated use symbolValue instead
     * @param c the execution context used to resolve the value
     */

    public final String atomValue(Context c) throws JessException {
        return symbolValue(c);
    }

    /**
     * Returns the contents of this value, as a symbol.
     * @exception JessException if this value does not contain any kind of String
     * @return the symbol
     * @param c the execution context used to resolve the value
     */

    public String symbolValue(Context c) throws JessException {
        return stringValue(c);
    }

    /**
     * Returns the contents of this value, as a String (a variable name).
     * @exception JessException if this value does not contain a variable.
     * @return the name of the variable
     * @param c the execution context used to resolve the value
     */

    public String variableValue(Context c) throws JessException {
        if (m_type != RU.VARIABLE && m_type != RU.MULTIVARIABLE)
            throw typeError("variableValue", "a variable");
        return stringValue(c);
    }

    /**
     * Returns the contents of this value, as a String.
     * @exception JessException if this value does not contain any kind of String.
     * @return the string
     * @param c the execution context used to resolve the value
     */

    public String stringValue(Context c) throws JessException {
        switch (m_type) {
            case RU.SYMBOL:
            case RU.STRING:
            case RU.VARIABLE:
            case RU.MULTIVARIABLE:
            case RU.BINDING:
            case RU.SLOT:
            case RU.MULTISLOT:
                return (String) m_objectval;
            case RU.INTEGER:
                return String.valueOf(m_intval);
            case RU.FLOAT:
                return String.valueOf(m_floatval);
            case RU.JAVA_OBJECT:
                return m_objectval.toString();
            default:
                throw typeError("stringValue", "a string");
        }
    }

    /**
     * Return the value of this object as a Userfunction. If this object is a lexeme, try to interpret
     * it as the name of a function. If it's a lambda value, then return the lambda.
     * @param c the execution context used to resolve the value
     * @return the Userfunction this value represents
     * @throws JessException if this is not a function
     */

    public Userfunction functionValue(Context c) throws JessException {
        Value resolved = resolveValue(c);
        Userfunction function;
        if (resolved.isLexeme(c)) {
            String name = resolved.symbolValue(c);
            function = c.getEngine().findUserfunction(name);
            if (function == null)
                throw new JessException("functionValue", "No such function", name);

        } else if (resolved.m_type == RU.LAMBDA) {
            function = (Userfunction) resolved.m_objectval;
        } else
            throw resolved.typeError("functionValue", "a function");
        return function;
    }

    private JessException typeError(String routine, String msg) {
        return typeError(routine, msg, m_type);
    }

    private JessException typeError(String routine, String msg, int type) {
        return typeError(this, "Value." + routine, msg, type);
    }

    static JessException typeError(Value val, String routine, String msg, int type) {
        return new JessException(routine,
                "'" + val.toString() + "' is " + getTypeName(type) + ", not ",
                msg);
    }


    private static String getTypeName(int type) {
        switch (type) {
            case RU.SYMBOL:
                return "a symbol";
            case RU.STRING:
                return "a string";
            case RU.INTEGER:
                return "an integer";
            case RU.FLOAT:
                return "a float";
            case RU.LIST:
                return "a list";
            case RU.FUNCALL:
                return "a function call";
            case RU.FACT:
                return "a fact";
            case RU.LONG:
                return "a long";
            default:
                return RU.getTypeName(type);
        }
    }

    private static String escape(String s) {
        if (s.indexOf('"') == -1 && s.indexOf('\\') == -1)
            return s;
        else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"' || c == '\\')
                    sb.append('\\');
                sb.append(c);
            }
            return sb.toString();
        }
    }

    /**
     * Return a pretty-print version of this value, without adding parens to any lists.
     * @return the formatted string
     */
    public String toString() {
        switch (m_type) {
            case RU.INTEGER:
                return String.valueOf(m_intval);
            case RU.FLOAT:
                return String.valueOf(m_floatval);
            case RU.STRING:
                return "\"" + escape((String) m_objectval) + "\"";
            case RU.SYMBOL:
            case RU.SLOT:
            case RU.MULTISLOT:
                return (String) m_objectval;
            case RU.VARIABLE:
                return "?" + m_objectval;
            case RU.MULTIVARIABLE:
                return "$?" + m_objectval;
            case RU.FUNCALL:
            case RU.LIST:
                return m_objectval.toString();
            case RU.JAVA_OBJECT:
                return "<Java-Object:" + m_objectval.getClass().getName() + ">";
            case RU.LAMBDA:
                return "<LAMBDA>";
            case RU.NONE:
                return Funcall.NIL.toString();
            default:
                return "<UNKNOWN>";
        }
    }

    /**
     * Return a pretty-print version of this value, adding parens to any lists.
     * @return the formatted string
     */

    public String toStringWithParens() {
        switch (m_type) {
            case RU.FUNCALL:
            case RU.LIST:
                return ((ValueVector) m_objectval).toStringWithParens();
            default:
                return toString();
        }
    }

    /**
     * Return the type of this variable. Always one of the constants in {@link RU}.
     * @return the type
     */
    public int type() {
        return m_type;
    }

    /**
     * Compare this value to another object. As a convenience, if the
     * parameter is not a Value, it will be compared to any contained
     * Object inside this Value (a String or Java object.)
     *
     * @param v the object to compare to.
     * @return true if the objects are equivalent.
     */
    public boolean equals(Object v) {
        if (this == v)
            return true;

        if (v instanceof Value)
            return equals((Value) v);
        else if (v != null)
            return v.equals(m_objectval);
        else
            return false;
    }

    /**
     * Compare this value to another value. Believe it or not, using this
     * separate overloaded routine has a measurable impact on performance -
     * since so much time is spent comparing Values.
     *
     * @param v the Value to compare to.
     * @return true if the Values are equivalent.
     */

    public boolean equals(Value v) {
        if (this == v)
            return true;

        if (v.m_type != m_type)
            return false;

        switch (m_type) {
            case RU.INTEGER:
                return (m_intval == v.m_intval);
            case RU.FLOAT:
                return (m_floatval == v.m_floatval);

            default:
                // TODO Use isValueObject when appropriate?
                return m_objectval.equals(v.m_objectval);
        }
    }

    /**
     * Like equals(Value), but returns true for 3 == 3.0
     * @param v Value to compare to
     * @return true if the values are loosely equivalent
     */

    public boolean equalsStar(Value v) {
        if (this == v)
            return true;

        try {
            if ((m_type & NUM_TYPES) != 0 && (v.m_type & NUM_TYPES) != 0) {
                return (numericValue(null) == v.numericValue(null));
            } else
                return equals(v);
        } catch (JessException cantHappen) {
            return false;
        }
    }

    /**
     * Return a hashcode for the object.
     * @return the hashcode
     */

    public int hashCode() {
        switch (m_type) {
            case RU.NONE:
                return 0;

            case RU.INTEGER:
                return m_intval;

            case RU.FLOAT:
                return (int) m_floatval;

            case RU.FUNCALL:
            case RU.LIST:
                m_objectval.hashCode();
            case RU.STRING:
            case RU.SYMBOL:
                return (m_objectval != null) ? m_objectval.hashCode() : 0;

            case RU.JAVA_OBJECT:
                return HashCodeComputer.hashCode(m_objectval);

            default:
                return 0;
        }
    }

    /**
     * Given an evaluation context, return the "true value" of this
     * Value.  For this class, the true value is always "this". For
     * subclasses, the Context may be used to compute a new Value.
     * @see jess.Variable
     * @see jess.Funcall
     * @param c An execution context. You can pass null if you are sure
     * that you're not calling this method on a subclass that uses the
     * argument.
     * @return this object
     * @throws JessException if something goes wrong during value resolution
     */
    public Value resolveValue(Context c) throws JessException {
        return this;
    }

    /**
     * Indicate whether this object represents a number.  This
     * includes FLOAT, INTEGER, LONG, and FACT values, as well as
     * Strings and symbols that could be parsed as an INTEGER, LONG,
     * or FLOAT.
     * @return true if this Value is a number.
     * @param c the execution context used to resolve the value
     * @throws JessException if something goes wrong during value resolution
     */
    public boolean isNumeric(Context c) throws JessException {
        Value v = resolveValue(c);
        switch (v.m_type) {
            case RU.FLOAT:
            case RU.INTEGER:
            case RU.FACT:
            case RU.LONG:
                return true;
            case RU.STRING:
            case RU.SYMBOL:
                String value = v.stringValue(c);
                if (ReaderTokenizer.isAnInteger(value) || ReaderTokenizer.isALong(value))
                    return true;
                else if (ReaderTokenizer.couldBeADouble(value)) {
                    try {
                        Double.parseDouble(value);
                        return true;
                    } catch (NumberFormatException nfe) {
                        /* FALL THROUGH */
                    }
                }
            default:
                return false;
        }
    }

    /**
     * Indicate whether this object represents a lexeme.  This
     * includes Strings and symbols.
     * @return true if this Value is a lexeme
     * @param c the execution context used to resolve the value
     * @throws JessException if something goes wrong during value resolution
     */

    public boolean isLexeme(Context c) throws JessException {
        Value v = resolveValue(c);
        switch (v.m_type) {
                case RU.STRING:
                case RU.SYMBOL:
                    return true;
                default:
                    return false;
        }
    }

    /** Indicate whether this value represents a literal. A literal has
     * type STRING, SYMBOL, INTEGER, FLOAT, or LONG, and is not a variable.
     * @return
     */
    public boolean isLiteral() {
        return !isVariable() &&
                m_type == RU.SYMBOL ||
                m_type == RU.INTEGER ||
                m_type == RU.STRING ||
                m_type == RU.FLOAT;
    }


    /**
     * Indicate whether this is a variable.
     * @return true if the type is VARIABLE or MULTIVARIABLE
     */
    public boolean isVariable() {
        return false;
    }
}


