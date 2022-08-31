package jess;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

/**
 * General utilities and manifest constants for Jess. All fields and
 * methods in this class are static, and there is no constructor.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public class RU implements Serializable {
    private RU() {
    }

    /** Relative index of slot name within a deftemplate's slots */
    final public static int DT_SLOT_NAME = 0;
    /** Relative index of slot default value within a deftemplate's slots */
    final public static int DT_DFLT_DATA = 1;
    /** Relative index of slot data type within a deftemplate's slots */
    final public static int DT_DATA_TYPE = 2;
    /** Size of a slot in a deftemplate */
    final public static int DT_SLOT_SIZE = 3;

    /** Data type of "no value" */
    final public static int NONE = 0;
    /** @deprecated use SYMBOL instead
     * @noinspection PointlessBitwiseExpression*/
    final public static int ATOM = 1 << 0;
    /** Data type of symbol
     * @noinspection PointlessBitwiseExpression*/
    final public static int SYMBOL = 1 << 0;
    /** Data type of string */
    final public static int STRING = 1 << 1;
    /** Data type of integer */
    final public static int INTEGER = 1 << 2;
    /** Data type of a variable */
    final public static int VARIABLE = 1 << 3;
    /** Data type of a fact id */
    final public static int FACT = 1 << 4;
    /** Data type of float */
    final public static int FLOAT = 1 << 5;
    /** Data type of function call stored in a value */
    final public static int FUNCALL = 1 << 6;
    /** Data type of a list stored in a value */
    final public static int LIST = 1 << 9;
    /** Data type of arbitrary Java object */
    final public static int JAVA_OBJECT = 1 << 11;
    /** Data type of lexeme (symbol or string) */
    final public static int LEXEME = SYMBOL | STRING;
    /** Any data type */
    final public static int ANY =  -1;



    /**
     * Data type of external address
     * @deprecated Since Jess 7, use <a href="#JAVA_OBJECT">JAVA_OBJECT</a> instead.
     */
    final public static int EXTERNAL_ADDRESS = 1 << 11;
    /** Data type of variable binding stored in value (internal use) */
    final public static int BINDING = 1 << 12;
    /** Data type of multivariable */
    final public static int MULTIVARIABLE = 1 << 13;
    /** Data type of slot name stored in a value */
    final public static int SLOT = 1 << 14;
    /** Data type of multislot name stored in a value */
    final public static int MULTISLOT = 1 << 15;
    /** Data type of Java long*/
    final public static int LONG = 1 << 16;
    /** Data type of a lambda expression */
    final public static int LAMBDA = 1 << 17;
    /** Data type of numnber (integer, float, or long)*/
    final public static int NUMBER = INTEGER | FLOAT | LONG;


    final static HashMap s_typeNames = new HashMap();
    final static HashMap s_typeCodes = new HashMap();

    static {
        s_typeNames.put(String.valueOf(NONE), "NONE");
        s_typeNames.put(String.valueOf(SYMBOL), "SYMBOL");
        s_typeNames.put(String.valueOf(STRING), "STRING");
        s_typeNames.put(String.valueOf(INTEGER), "INTEGER");
        s_typeNames.put(String.valueOf(VARIABLE), "VARIABLE");
        s_typeNames.put(String.valueOf(FACT), "FACT");
        s_typeNames.put(String.valueOf(FLOAT), "FLOAT");
        s_typeNames.put(String.valueOf(FUNCALL), "FUNCALL");
        s_typeNames.put(String.valueOf(LIST), "LIST");
        s_typeNames.put(String.valueOf(JAVA_OBJECT), "OBJECT");
        s_typeNames.put(String.valueOf(BINDING), "BINDING");
        s_typeNames.put(String.valueOf(MULTIVARIABLE), "MULTIVARIABLE");
        s_typeNames.put(String.valueOf(SLOT), "SLOT");
        s_typeNames.put(String.valueOf(MULTISLOT), "MULTISLOT");
        s_typeNames.put(String.valueOf(LONG), "LONG");
        s_typeNames.put(String.valueOf(LAMBDA), "LAMBDA");
        s_typeNames.put(String.valueOf(LEXEME), "LEXEME");
        s_typeNames.put(String.valueOf(NUMBER), "NUMBER");

        s_typeCodes.put("NONE", new Integer(NONE));
        s_typeCodes.put("SYMBOL", new Integer(SYMBOL));
        s_typeCodes.put("STRING", new Integer(STRING));
        s_typeCodes.put("INTEGER", new Integer(INTEGER));
        s_typeCodes.put("VARIABLE", new Integer(VARIABLE));
        s_typeCodes.put("FACT", new Integer(FACT));
        s_typeCodes.put("FLOAT", new Integer(FLOAT));
        s_typeCodes.put("FUNCALL", new Integer(FUNCALL));
        s_typeCodes.put("LIST", new Integer(LIST));
        s_typeCodes.put("OBJECT", new Integer(JAVA_OBJECT));
        s_typeCodes.put("EXTERNAL_ADDRESS", new Integer(JAVA_OBJECT));
        s_typeCodes.put("BINDING", new Integer(BINDING));
        s_typeCodes.put("MULTIVARIABLE", new Integer(MULTIVARIABLE));
        s_typeCodes.put("SLOT", new Integer(SLOT));
        s_typeCodes.put("MULTISLOT", new Integer(MULTISLOT));
        s_typeCodes.put("LONG", new Integer(LONG));
        s_typeCodes.put("LAMBDA", new Integer(LAMBDA));
        s_typeCodes.put("LEXEME", new Integer(LEXEME));
        s_typeCodes.put("NUMBER", new Integer(NUMBER));
    }


    /**
     * Given a type constant (SYMBOL, STRING, INTEGER, etc.) return a
     * String version of the name of that type ("SYMBOL", "STRING",
     * "INTEGER", etc.)
     *
     * @param type One of the type constants in this class
     * @return The String name of this type, or null if the constant is out of range.
     */

    public static String getTypeName(int type) {
        return (String) s_typeNames.get(String.valueOf(type));
    }

    public static int getTypeCode(String typeName) throws JessException {
        Integer code = (Integer) s_typeCodes.get(typeName.toUpperCase());
        if (code != null)
            return code.intValue();
        else throw new JessException("RU.getSlotTypeCode", "No such type", typeName);
    }


    /** Add this token to the Rete network (internal use) */
    final static int ADD = 0;
    /** Remove this token from the Rete network (internal use) */
    final static int REMOVE = 1;
    /** Update this token in the Rete network (internal use) */
    final static int UPDATE = 2;
    /** Clear the Rete network (internal use) */
    final static int CLEAR = 3;
    /** Add a modified token back into the Rete network (internal use) */
    final static int MODIFY_ADD = 4;
    /** Remove a token from the Rete network for modification (internal use) */
    final static int MODIFY_REMOVE = 5;

    /*
      Constants specifying that a variable is bound to a fact-index
      or is created during rule execution
      */

    /** Variable is local to a defrule or deffunction */
    public final static int LOCAL = -2;
    /** Variable is global */
    public final static int GLOBAL = -3;
    /** Variable is an accumulate result */
    public final static int ACCUM_RESULT = -4;
    /** Variable contains a fact index */
    public final static int PATTERN = -1;

    /*
      Constants specifying connective constraints
    */

    /** Test is anded with previous */
    public final static int AND = 1;

    /** Test is ored with previous */
    public final static int OR = 2;


    /** String prepended to deftemplate names to form backwards chaining goals */
    final static String BACKCHAIN_PREFIX = "need-";

    /** Special multislot name used for ordered facts */
    public final static String DEFAULT_SLOT_NAME = "__data";

    /**  The name of the ultimate parent of all deftemplates */
    public final static String ROOT_DEFTEMPLATE = "__fact";

    /** A slot name for tests not attached to any slot */
    public static final String NO_SLOT = "";

    /*
      A number used in quickly generating semi-unique symbols.
      */

    static long s_gensymIdx = 0;

    /**
     * Generate a pseudo-unique symbol starting with "prefix"
     * @param prefix The alphabetic part of the symbol
     * @return The new symbol
     */
    public static synchronized String gensym(String prefix) {
        return prefix + s_gensymIdx++;
    }

    /**
     * Get a property, but return null on SecurityException
     * @param prop The property name to get
     * @return The value of the property, or null if none or security problem
     */
    public static String getProperty(String prop) {
        try {
            return System.getProperty(prop);
        } catch (SecurityException se) {
            return null;
        }
    }

    static String scopeName(String module, String name) {
        StringBuffer sb = new StringBuffer(module);
        sb.append("::");
        sb.append(name);
        return sb.toString();
    }

    static String getModuleFromName(String name, Rete rete) {
        int index = name.indexOf("::");
        if (index == -1)
            return rete.getCurrentModule();
        else
            return name.substring(0, index);
    }


    static long time() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a human-readable name for one of the tag values used by the {@link jess.Token} class.
     * @param tag a Rete network tag value
     * @return the name of the tag
     */
    public static String tagName(int tag) {
        switch(tag) {
            case RU.ADD: return "ADD";
            case RU.REMOVE: return "REMOVE";
            case RU.MODIFY_ADD: return "MODIFY_ADD";
            case RU.MODIFY_REMOVE: return "MODIFY_REMOVE";
            case RU.UPDATE: return "UPDATE";
            case RU.CLEAR: return "CLEAR";
            default: return "???";
        }
    }

    private static java.util.regex.Pattern s_pattern = java.util.regex.Pattern.compile("_[_0-9]+_(.+)");
    public static String removePrefix(String name) {
        Matcher m = s_pattern.matcher(name);
        if (m.matches())
            name = m.group(1);
        return name;
    }

    /**
     * Return a Java object derived from the Value which matches the
     * Class object as closely as possible. If the class is a primitive Class standin
     * like Integer.TYPE, then the returned value will be boxed; i.e.,
     * since this routine can't return an int, it will return an Integer. If there's no way
     * to match the Value to the given Class, an IllegalArgumentException will be thrown. Jess uses
     * this method internally to prepare arguments for reflective method calls.
     *
     * @param clazz the desired class of the result
     * @param value the Value to be converted
     * @param context a Context used for resolving the Value, if needed
     * @return a Java object of the given class
     * @throws IllegalArgumentException if the conversion is impossible
     * @throws JessException if anything else goes wrong
     */

    public static Object valueToObject(Class clazz, Value value, Context context)
            throws IllegalArgumentException, JessException {
        return valueToObject(clazz, value, context, true);
    }

    /**
     * Return a Java object derived from the Value which matches the
     * Class object as closely as possible. If the class is
     * a primitive Class standin like Integer.TYPE, then the returned value will be boxed; i.e.,
     * since this routine can't return an int, it will return an Integer. Optionally, this method can
     * allow generous conversions like parsing a String into a number. If there's no way
     * to match the Value to the given Class, an IllegalArgumentException will be thrown. Jess uses
     * this method internally to prepare arguments for reflective method calls.
     *
     * @param clazz the desired class of the result
     * @param value the Value to be converted
     * @param context a Context used for resolving the Value, if needed
     * @param strict false to allow Strings to be parsed as numbers
     * @return a Java object of the given class
     * @throws IllegalArgumentException if the conversion is impossible
     * @throws JessException if anything else goes wrong
     */

    public static Object valueToObject(Class clazz, Value value, Context context, boolean strict)
            throws IllegalArgumentException, JessException {
        value = value.resolveValue(context);
        switch (value.type()) {

            case JAVA_OBJECT:
            case FACT:
                {
                    if (clazz.isInstance(value.javaObjectValue(context)))
                        return value.javaObjectValue(context);
                    else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());
                }

            case SYMBOL:
                {
                    if ((clazz == Void.TYPE || !clazz.isPrimitive()) && value.equals(Funcall.NIL))
                        return null;
                }

            case STRING:
                {
                    String s = value.stringValue(context);

                    if (clazz.isAssignableFrom(String.class))
                        return s;

                    else if (clazz == Character.TYPE) {
                        if (s.length() == 1)
                            return new Character(s.charAt(0));
                        else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());
                    } else if (clazz == Boolean.TYPE) {
                        if (s.equals(Funcall.TRUE.stringValue(context)))
                            return Boolean.TRUE;
                        if (s.equals(Funcall.FALSE.stringValue(context)))
                            return Boolean.FALSE;
                        else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());
                    } else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());
                }

            case INTEGER:
                {

                    if (clazz == Long.TYPE || clazz == Long.class)
                        return new Long(value.longValue(context));

                    int i = value.intValue(context);

                    if (clazz == Integer.TYPE ||
                            clazz == Integer.class ||
                            clazz == Object.class)
                        return new Integer(i);

                    else if (clazz == Short.TYPE || clazz == Short.class)
                        return new Short((short) i);

                    else if (clazz == Float.TYPE || clazz == Float.class)
                        return new Float(i);

                    else if (clazz == Double.TYPE || clazz == Double.class)
                        return new Double(i);

                    else if (clazz == Character.TYPE || clazz == Character.class)
                        return new Character((char) i);

                    else if (clazz == Byte.TYPE || clazz == Byte.class)
                        return new Byte((byte) i);

                    else if (!strict && clazz == String.class)
                        return String.valueOf(i);

                    else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());

                }
            case LONG:
                {

                    if (clazz == Long.TYPE ||
                            clazz == Long.class ||
                            clazz == Object.class)
                        return new Long(value.longValue(context));

                    int i = value.intValue(context);

                    if (clazz == Integer.TYPE ||
                            clazz == Integer.class)
                        return new Integer(i);

                    else if (clazz == Short.TYPE || clazz == Short.class)
                        return new Short((short) i);

                    else if (clazz == Character.TYPE || clazz == Character.class)
                        return new Character((char) i);

                    else if (clazz == Byte.TYPE || clazz == Byte.class)
                        return new Byte((byte) i);

                    else if (!strict && clazz == String.class)
                        return String.valueOf(i);

                    else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());

                }

            case FLOAT:
                {
                    double d = value.floatValue(context);

                    if (clazz == Double.TYPE ||
                            clazz == Double.class ||
                            clazz == Object.class)
                        return new Double(d);

                    else if (clazz == Float.TYPE || clazz == Float.class)
                        return new Float((float) d);

                    else if (!strict && clazz == String.class)
                        return String.valueOf(d);

                    else
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());

                }

                // Turn lists into arrays.
            case LIST:
                {
                    if (clazz.isArray()) {
                        return listToArray(value, context, clazz.getComponentType());
                    } else if (clazz == Object.class) {
                        return listToArray(value, context, Object.class);
                    } else {
                        throw new IllegalArgumentException("Can't convert '" + value + "' to required type " + clazz.getName());
                    }
                }
            default:
                throw new IllegalArgumentException();
        }

    }

    private static Object listToArray(Value value, Context context, Class elemType) throws JessException {
        ValueVector vv = value.listValue(context);
        Object array = Array.newInstance(elemType, vv.size());
        for (int i = 0; i < vv.size(); i++)
            Array.set(array, i,
                      valueToObject(elemType, vv.get(i), context, false));
        return array;
    }

    /**
     * Create a Jess Value object out of a Java Object. The Class object indicates whether the given
     * object is boxed or not; i.e., the object may be an Integer while the Class is Integer.TYPE,
     * indicating that the argument is actually a boxed int. Jess uses this internally to interpret the
     * results of reflective method calls.
     *
     * @param clazz a Class representing the type of the other argument
     * @param obj an object to be converted
     * @return a Jess Value representing the value of the object
     * @throws JessException if anything goes wrong
     */

    public static Value objectToValue(Class clazz, Object obj) throws JessException {
        Class r = (obj == null) ? Void.TYPE : obj.getClass();

        if (obj == null && !clazz.isArray())
            return Funcall.NIL;

        if (clazz == Void.class)
            return Funcall.NIL;

        if (obj instanceof Value)
            return (Value) obj;

        if (clazz == String.class || r == String.class)
            return new Value(obj.toString(), STRING);

        if (clazz.isArray()) {
            int length = 0;
            if (obj != null)
                length = Array.getLength(obj);
            ValueVector vv = new ValueVector(length);

            for (int i = 0; i < length; i++)
                vv.add(objectToValue(clazz.getComponentType(), Array.get(obj, i)));

            return new Value(vv, LIST);
        }

        if (clazz == Boolean.TYPE || r == Boolean.TYPE ||
                clazz == Boolean.class || r == Boolean.class)
            return ((Boolean) obj).booleanValue() ? Funcall.TRUE : Funcall.FALSE;

        if (clazz == Byte.TYPE || clazz == Short.TYPE || clazz == Integer.TYPE ||
                r == Byte.TYPE || r == Short.TYPE || r == Integer.TYPE ||
                clazz == Byte.class || clazz == Short.class || clazz == Integer.class ||
                r == Byte.class || r == Short.class || r == Integer.class)

            return new Value(((Number) obj).intValue(), INTEGER);

        if (clazz == Long.TYPE || r == Long.TYPE ||
                clazz == Long.class || r == Long.class)
            return new LongValue(((Long) obj).longValue());

        if (clazz == Double.TYPE || clazz == Float.TYPE ||
                r == Double.TYPE || r == Float.TYPE ||
                clazz == Double.class || clazz == Float.class ||
                r == Double.class || r == Float.class)
            return new Value(((Number) obj).doubleValue(), FLOAT);

        if (clazz == Character.TYPE || r == Character.TYPE ||
                clazz == Character.class || r == Character.class)
            return new Value(obj.toString(), SYMBOL);

        return new Value(obj);
    }
}





