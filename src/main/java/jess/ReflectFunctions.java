package jess;

import java.beans.*;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import static jess.ClassResearcher.Property;

/**
 * The classes in this file implement Java reflection for Jess.
 * <P>
 * This stuff is suprisingly powerful! Right now we don't handle
 * multi-dimensional arrays, but I think we don't miss anything else.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class ReflectFunctions extends IntrinsicPackageImpl {
    public void add(HashMap table) {
        addFunction(new Engine(), table);
        addFunction(new FetchContext(), table);
        addFunction(new JessImport(), table);
        addFunction(new JessNew(), table);
        addFunction(new Call(), table);
        addFunction(new JessField("set-member"), table);
        addFunction(new JessField("get-member"), table);
        addFunction(new SetProperty(), table);
        addFunction(new Get(), table);
        addFunction(new Defclass(), table);
        addFunction(new UnDefinstance(), table);
        addFunction(new Definstance(), table);
        addFunction(new InstanceOf(), table);
    }
}

class Engine implements Userfunction, Serializable {
    public String getName() {
        return "engine";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(context.getEngine());
    }
}

class FetchContext implements Userfunction, Serializable {
    public String getName() {
        return "context";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(context);
    }
}


class SetProperty extends Call {
    SetProperty() {
        m_name = "set";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        try {
            Value target = vv.get(1).resolveValue(context);
            Value property = vv.get(2).resolveValue(context);
            String propName = property.stringValue(context);
            Value value = vv.get(3).resolveValue(context);

            // Pass this along to 'call'
            // We can't self-modify since we're not copied
            Funcall f = new Funcall(Jesp.JAVACALL, context.getEngine());
            f.arg(target);
            f.arg(Funcall.NIL); // Placeholder for property name
            f.arg(value);

            ClassResearcher cr = context.getEngine().getClassResearcher();
            Property[] pd =
            		cr.getBeanProperties(target.javaObjectValue(context).getClass().getName());

            for (int i = 0; i < pd.length; i++) {
                Method m;
                final Property descriptor = pd[i];
                if (descriptor.getName().equals(propName) && (m = descriptor.getWriteMethod()) != null) {
                    f.set(new Value(m.getName(), RU.STRING), 2);
                    return f.execute(context);
                }
            }

            // No Bean property by this name; try set-member in case it's an instance variable
            try {
                f = new Funcall("set-member", context.getEngine());
                f.arg(target);
                f.arg(property);
                f.arg(value);
                return f.execute(context);
            } catch (JessException ex) {
                throw new JessException("set", "No such property or property is read-only: " + propName, ex);
            }
        } catch (ClassNotFoundException ie) {
            throw new JessException("set", "Class not found:", ie);
        }
    }
}

class Get extends Call {
    Get() {
        m_name = "get";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        try {
            Value target = vv.get(1).resolveValue(context);
            Value property = vv.get(2).resolveValue(context);
            String propName = property.stringValue(context);

            Object t = target.javaObjectValue(context);
            if (t instanceof Fact) {
                return ((Fact) t).getSlotValue(propName);
            }
            
            // Pass this along to 'call'
            // We can't self-modify since we're not copied
            Funcall f = new Funcall(Jesp.JAVACALL, context.getEngine());
            f.arg(target);
            f.arg(Funcall.NIL); // Placeholder for property name

            // note that these are cached, so all the introspection
            // only gets done once.
            ClassResearcher cr = context.getEngine().getClassResearcher();
            Property[] pd =
            		cr.getBeanProperties(f.get(1).javaObjectValue(context).getClass().getName());
            for (int i = 0; i < pd.length; i++) {
                Method m;
                final Property descriptor = pd[i];
                if (descriptor.getName().equals(propName) && (m = pd[i].getReadMethod()) != null) {
                    f.set(context.getEngine().getValueFactory().get(m.getName(), RU.STRING), 2);
                    return super.call(f, context);
                }
            }

            // No Bean property by this name; try set-member in case it's an instance variable
            try {
                f = new Funcall("get-member", context.getEngine());
                f.arg(target);
                f.arg(property);
                return f.execute(context);
            } catch (JessException ex) {
                throw new JessException("get", "No such property or property unreadable: " + propName, ex);
            }

        } catch (ClassNotFoundException ie) {
            throw new JessException("get", "Class not found", ie);
        }
    }
}

class JessImport implements Userfunction, Serializable {

    public String getName() {
        return "import";
    }

    public Value call(ValueVector vv, Context c) throws JessException {
        // ###
        String arg = vv.get(1).symbolValue(c);
        if (arg.indexOf("*") != -1)
            c.getEngine().importPackage(arg.substring(0, arg.indexOf("*")));

        else
            c.getEngine().importClass(arg);
        return Funcall.TRUE;
    }
}

/**
 * Create a Java object from Jess.
 * The first argument is the full-qualified typename; later arguments are
 * the contructor arguments.  We pick methods based on a first-fit algorithm,
 * not necessarily a best-fit. If you want to be super selective, you can
 * disambiguate by wrapping basic types in object wrappers.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class JessNew implements Userfunction, Serializable {
    public String getName() {
        return "new";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Class c = null;
        String clazz = vv.get(1).stringValue(context);
        try {
            /*
              Find target class
            */

            c = context.getEngine().findClass(clazz);

            ValueVector resolved = new ValueVector();
            for (int i = 2; i < vv.size(); i++)
                resolved.add(vv.get(i).resolveValue(context));

            /*
             * Build argument list
             */

            int nargs = vv.size() - 2;
            Object args[] = new Object[nargs];

            Constructor[] cons = c.getConstructors();

            if (cons.length == 0)
                throw new JessException("new", "Class " + c.getName(), "has no public constructors");
            Object rv;
            int i;
            for (i = 0; i < cons.length; i++) {
                try {
                    Constructor constructor = cons[i];
                    Class[] argTypes = constructor.getParameterTypes();
                    if (nargs != argTypes.length)
                        continue;

                    // Otherwise give it a try!
                    for (int j = 0; j < nargs; j++) {
                        args[j]
                                = RU.valueToObject(argTypes[j],
                                                                 resolved.get(j),
                                                                 context);
                    }

                    rv = constructor.newInstance(args);
                    return new Value(rv);

                } catch (IllegalArgumentException iae) {
                    // Try the next one!
                }
            }

            throw new NoSuchMethodException(c.getName());

        } catch (InvocationTargetException ite) {
            throw new JessException("new", "Constructor threw an exception",
                                    ite.getTargetException());
        } catch (NoSuchMethodException nsm) {
            throw new JessException("new", "Constructor not found: " +
                                           vv.toStringWithParens(),
                                    nsm);
        } catch (ClassNotFoundException cnfe) {
            throw new JessException("new", "Class " + clazz + " not found", cnfe);

        } catch (IllegalAccessException iae) {
            throw new JessException("new",
                                    "Class or constructor is not accessible",
                                    iae);
        } catch (InstantiationException ie) {
            String msg = "Abstract class ";
            if (c.isInterface())
                msg = "Interface ";
            msg += c.getName();
            throw new JessException("new", msg + " cannot be instantiated", ie);
        }
    }
}

/**
 * Set or get a data member of a Java object from Jess
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class JessField implements Userfunction, Serializable {

    private final String m_name;

    public String getName() {
        return m_name;
    }

    JessField(String functionName) {
        // name should be get-member or set-member
        m_name = functionName;
    }


    public Value call(ValueVector vv, Context context) throws JessException {
        String field = vv.get(2).stringValue(context);

        boolean doSet = false;

        if (vv.get(0).stringValue(context).equals("set-member"))
            doSet = true;

        Class c = null;
        Object target = null;

        Value v = vv.get(1).resolveValue(context);

        if (v.type() == RU.STRING || v.type() == RU.SYMBOL) {
            try {
                c = context.getEngine().findClass(v.stringValue(context));
            } catch (ClassNotFoundException ex) {
                throw new JessException(vv.get(0).stringValue(context),
                                        "No such class",
                                        v.stringValue(context));
            }
        }
        if (c == null) {
            target = v.javaObjectValue(context);
            c = target.getClass();
        }

        Value v2 = null;
        if (doSet)
            v2 = vv.get(3).resolveValue(context);

        try {
            Field f = c.getField(field);
            Class argType = f.getType();
            if (doSet) {
                f.set(target,
                      RU.valueToObject(argType, v2, context));
                return v2;
            } else {
                Object o = f.get(target);
                return RU.objectToValue(argType, o);
            }
        } catch (NoSuchFieldException nsfe) {
            throw new JessException(vv.get(0).stringValue(context),
                                    "No such field " + field +
                                    " in class ", c.getName());
        } catch (IllegalAccessException iae) {
            throw new JessException(vv.get(0).stringValue(context),
                                    "Field is not accessible",
                                    field);
        } catch (IllegalArgumentException iae) {
            throw new JessException(vv.get(0).stringValue(context),
                                    "Invalid argument",
                                    v2.toString());
        }
    }
}


/**
 * Remove an object from working memory.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class UnDefinstance implements Userfunction, Serializable {

    public String getName() {
        return "undefinstance";
    }

    /** @noinspection EqualsBetweenInconvertibleTypes*/
    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        Value v = vv.get(1).resolveValue(context);
        if (v.type() == RU.JAVA_OBJECT) {
            Fact f = engine.undefinstance(v.javaObjectValue(context));
            if (f == null)
                return Funcall.NIL;
            else
                return new FactIDValue(f);
        } else if (v.equals("*")) {
            for (Iterator e = engine.listDefinstances(); e.hasNext();)
                engine.undefinstance(e.next());
            return Funcall.TRUE;
        } else
            throw new JessException("undefinstance",
                                    "Invalid argument", v.toString());
    }
}

/**
 * Tell Jess to prepare to match on properties of a Java class.
 * Generates a deftemplate from the class.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Defclass implements Userfunction, Serializable {

    public String getName() {
        return "defclass";
    }

    /**
     * SYNTAX: (defclass <jess-classname> <Java-classname> [extends <parent>])
     */
    public Value call(ValueVector vv, Context context) throws JessException {
        // ###
        String jessName = vv.get(1).stringValue(context);
        String clazz = vv.get(2).stringValue(context);
        String parent = vv.size() > 4 ? vv.get(4).stringValue(context) : null;

        //noinspection EqualsBetweenInconvertibleTypes
        if (parent != null && !vv.get(3).equals("extends"))
            throw new JessException("defclass",
                                    "expected 'extends <classname>'",
                                    vv.get(3).toString());

        return context.getEngine().defclass(jessName, clazz, parent);
    }
}


class InstanceOf implements Userfunction, Serializable {
    public String getName() {
        return "instanceof";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Object o = vv.get(1).javaObjectValue(context);
        String className = vv.get(2).stringValue(context);
        try {
            Class clazz = context.getEngine().findClass(className);
            return clazz.isInstance(o) ? Funcall.TRUE : Funcall.FALSE;
        } catch (ClassNotFoundException cnfe) {
            throw new JessException("instanceof",
                                    "Class not found: " + className,
                                    cnfe);
        }

    }
}
