package jess;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.lang.reflect.*;

/**
 * Call a Java method from Jess. First argument is EITHER a Java
 * y * object, or the name of a class. The latter works only for Static methods, of
 * course. Later arguments are the contructor arguments. We pick methods based
 * on a first-fit algorithm, not necessarily a best-fit. If you want to be super
 * selective, you can disambiguate by wrapping basic types in object wrappers.
 * If it absolutely won't work, well, you can always write a Java Userfunction
 * as a wrapper!
 * <p/>
 * (C) 2007 Sandia National Laboratories<br>
 */

class Call implements Userfunction, Serializable {

    String m_name = "call";

    public String getName() {
        return m_name;
    }

    private final static Map<Class, Method[]> s_methods = Collections.synchronizedMap(new HashMap<Class, Method[]>());
    private final static Object[] NO_ARGS = {};
    private final static Value[] NO_PARAMS = {};

    static Method[] getMethods(Class c) {
        synchronized (s_methods) {
            if (s_methods.get(c) != null)
                return s_methods.get(c);
            else {
                Method[] m = c.getMethods();
                Arrays.sort(m, MethodNameComparator.getSortInstance());
                
                s_methods.put(c, m);
                return m;
            }
        }
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        int vvSize = vv.size();

        if (vvSize < 2)
            throw new JessException("call", "Target object missing", "");
        Value targetValue = vv.get(1).resolveValue(context);

        if (targetValue.type() == RU.LAMBDA)
            return callLambda(targetValue, vv, context);

        if (vvSize < 3)
            throw new JessException("call", "Expected method name", "");
        String methodName = vv.get(2).symbolValue(context);

        Class c = null;
        try {
            Object target = null;

            if (targetValue.type() == RU.STRING || targetValue.type() == RU.SYMBOL) {
                boolean stringHasMethod = hasMethodOfName(java.lang.String.class, methodName);


                // If it's an RU.STRING, call any String method first.
                if (targetValue.type() == RU.STRING && stringHasMethod) {
                    target = targetValue.stringValue(context);
                    c = java.lang.String.class;

                } else if (targetValue.equals(Funcall.NIL)) {
                    throw new JessException("call",
                            "Can't call method on nil reference:",
                            methodName);
                }

                if (c == null) {
                    try {
                        // For SYMBOLs, try to load first -- also get here on STRING if method isn't a String method
                        c = context.getEngine().findClass(targetValue.stringValue(context));
                    } catch (ClassNotFoundException cnfe) {
                        // If it's a SYMBOL, fall back to calling String method only if class can't be loaded
                        if (stringHasMethod) {
                            target = targetValue.stringValue(context);
                            c = java.lang.String.class;
                        } else {
                            throw new JessException("call", "Class not found", cnfe);
                        }
                    }
                }
            }

            if (c == null) {
                target = targetValue.javaObjectValue(context);
                c = target.getClass();
            }

            /*
            * Build argument list
            */

            int nargs = vvSize - 3;
            Object args[] = nargs == 0 ? NO_ARGS : new Object[nargs];

            Value[] resolved = nargs == 0 ? NO_PARAMS : new Value[nargs];
            for (int i = 3; i < vvSize; ++i)
                resolved[i - 3] = vv.get(i).resolveValue(context);


            Method[] methods = Call.getMethods(c);
            Object rv;
            int i;
            int start = Arrays.binarySearch(methods, methodName, MethodNameComparator.getSearchInstance());
            if (start < 0) {
                try {
                    if (nargs == 0)
                        return getFieldValue(c, methodName, target);
                } catch (NoSuchFieldException e) {
                    // FALL THROUGH
                }

                throw new JessException("call", "No method named '" + methodName + "' found",
                        "in class " + c.getName());

            }
            while (start > 0 && methods[start - 1].getName().equals(methodName))
                --start;
            for (i = start; i < methods.length && methods[i].getName().equals(methodName); i++) {
                try {
                    Method m = methods[i];

                    Class[] argTypes = m.getParameterTypes();
                    if (nargs != argTypes.length)
                        continue;

                    // OK, found a method. Problem is, it might be a public
                    // method of a private class. We'll check for this, and
                    // if so, we have to find a more appropriate method
                    // descriptor. Can't believe we have to do this.

                    if (!Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
                        Class d = m.getDeclaringClass();
                        m = null;

                        Class[] interfaces = c.getInterfaces();
                        for (int ii = 0; ii < interfaces.length; ii++) {
                            try {
                                m = interfaces[ii].getMethod(methodName, argTypes);
                                break;
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                        if (m == null) {
                            try {
                                m = d.getMethod(methodName, argTypes);

                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                        if (m == null)
                            throw new JessException("call",
                                    "Method not accessible:",
                                    methodName);
                    }

                    if (target == null && !Modifier.isStatic(m.getModifiers()))
                        continue;

                    // Now give it a try!
                    try {
                        if (!m.isAccessible())
                            m.setAccessible(true);
                    } catch (SecurityException ignore) {
                        throw new JessException("call",
                                "Method not accessible:",
                                methodName);
                    }

                    for (int j = 0; j < nargs; j++)
                        args[j] = RU.valueToObject(argTypes[j], resolved[j], context);

                    rv = m.invoke(target, args);

                    methods[i] = m;

                    return RU.objectToValue(m.getReturnType(), rv);

                } catch (IllegalArgumentException iae) {
                    // Try the next one!
                }
            }
            try {
                if (nargs == 0)
                    return getFieldValue(c, methodName, target);
            } catch (NoSuchFieldException e) {
                // FALL THROUGH
            }
            throw new NoSuchMethodException(methodName);


        } catch (NoSuchMethodException nsm) {
            if (!hasMethodOfName(c, methodName))
                throw new JessException("call", "No method named '" + methodName + "' found",
                        "in class " + c.getName());

            else
                throw new JessException("call", "No overloading of method '" + methodName + "'",
                        "in class " + c.getName() +
                                " I can call with these arguments: " +
                                vv.toStringWithParens());

        } catch (InvocationTargetException ite) {
            if (ite.getTargetException() instanceof JessException)
                throw (JessException) ite.getTargetException();
            else
                throw new JessException("call", "Called method threw an exception",
                        ite.getTargetException());
        } catch (IllegalAccessException iae) {
            throw new JessException("call", "Method is not accessible", iae);
        } catch (IllegalArgumentException iae) {
            throw new JessException("call", "Invalid argument to " + methodName, iae);
        }
    }

    private Value getFieldValue(Class c, String fieldName, Object target) throws NoSuchFieldException, IllegalAccessException, JessException {
        Field f = c.getField(fieldName);
        Class argType = f.getType();
        Object o = f.get(target);
        return RU.objectToValue(argType, o);
    }

    Value callLambda(Value targetValue, ValueVector vv, Context context) throws JessException {
        Userfunction function = targetValue.functionValue(context);
        ValueVector vector = new ValueVector();
        vector.add(targetValue);
        int size = vv.size();
        for (int i=2; i<size; ++i)
            vector.add(vv.get(i));
        return function.call(vector, context);
    }

    static boolean hasMethodOfName(Class c, String name) {
        Method[] m = Call.getMethods(c);
        return Arrays.binarySearch(m, name, MethodNameComparator.getSearchInstance()) > -1;
    }

}
