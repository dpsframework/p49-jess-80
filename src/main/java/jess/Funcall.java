package jess;

import jess.server.LineNumberRecord;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 <p>
 <tt>jess.Funcall</tt> is a specialized subclass of
 {@link ValueVector} that represents a Jess function call. It contains
 the name of the function, an internal pointer to the actual
 {@link Userfunction} object containing the function code, and
 the arguments to pass to the function.
 </p>

 <p>You can call Jess functions using <tt>Funcall</tt> if you prefer,
 rather than using {@link Rete#eval} This method
 has somewhat less overhead since there is no parsing to be done, but of course
 it's less convenient.</p>

 <pre>
 Rete r = new Rete();

 // The following is equivalent to the Jess code
 // (defclass dimension "java.awt.Dimension")
 Funcall f = new Funcall("defclass", r);
 f.arg("dimension").arg("java.awt.Dimension");
 f.execute(r.getGlobalContext());
 </pre>

 <p>
 The first entry in a <tt>Funcall</tt>'s <tt>ValueVector</tt> is the
 name of the function, even though you don't explicitly set
 it. Changing the first entry will not automatically change the
 function the <tt>Funcall</tt> will call! On the other hand, redefining
 a function in the {@link Rete} object a <tt>Funcall</tt> is tied to will
 cause the <tt>Funcall</tt> to call the new function.
 </p>

 <p>The <tt>Funcall</tt> class also contains some public static constant
 <tt>Value</tt> member objects that represent the special symbols
 <tt>nil,</tt> <tt>TRUE</tt>, <tt>FALSE,</tt> <tt>EOF,</tt> etc. You
 are encouraged to use these.</p>

 (C) 2013 Sandia Corporation<br>

 @see FuncallValue
 */

public class Funcall extends ValueVector implements Serializable, Visitable {

    private Object m_scratchPad;

    /**
     * Formats a Funcall as a String
     *
     * @return The pretty-print form of this Funcall
     * @noinspection EqualsBetweenInconvertibleTypes
     */
    public String toString() {
        try {
            if (get(0).equals("assert")) {
                ListRenderer l = new ListRenderer("assert");
                for (int i = 1; i < size(); i++)
                    l.add(get(i).factValue(null));
                return l.toString();
            } else if (get(0).equals("modify") ||
                    get(0).equals("duplicate")) {
                ListRenderer l = new ListRenderer(get(0).symbolValue(null));
                l.add(get(1));
                for (int i = 2; i < size(); i++) {
                    Value vv = get(i);
                    l.add(vv.toStringWithParens());
                }

                return l.toString();
            } else if (get(0).equals("lambda")) {
                ListRenderer l = new ListRenderer("lambda");
                l.add(get(1).toStringWithParens());
                for (int i = 2; i < size(); i++)
                    l.add(get(i).toString());
                return l.toString();
            } else {
                return new ListRenderer(super.toString()).toString();
            }

        } catch (JessException re) {
            return re.toString();
        }
    }

    /**
     * Just calls {@link #toString}
     * @return the pretty-print form of this Funcall
     */
    public String toStringWithParens() {
        return toString();
    }

    /**
     * The object representing the value TRUE
     */
    public static Value TRUE;
    /**
     * The object representing the value FALSE
     */
    public static Value FALSE;
    /**
     * The object representing the value NIL
     */
    public static Value NIL;
    /**
     * An object representing an empty list.
     */
    public static Value NILLIST;
    /**
     * The object representing end-of-file
     */
    public static Value EOF;
    /**
     * The object representing a newline to printout
     */
    public static Value CRLF;
    /**
     * The object representing printout's standard router
     */
    public static Value T;

    static Value s_else;
    static Value s_elif;

    static Value s_then;
    static Value s_do;

    private static final HashMap m_intrinsics = new HashMap();

    static {
        try {
            TRUE = new Value("TRUE", RU.SYMBOL);
            FALSE = new Value("FALSE", RU.SYMBOL);
            NIL = new Value("nil", RU.SYMBOL);
            NILLIST = new Value(new ValueVector(), RU.LIST);
            EOF = new Value("EOF", RU.SYMBOL);
            T = new Value("t", RU.SYMBOL);
            CRLF = new Value("crlf", RU.SYMBOL);
            s_else = new Value("else", RU.SYMBOL);
            s_elif = new Value("elif", RU.SYMBOL);
            s_then = new Value("then", RU.SYMBOL);
            s_do = new Value("do", RU.SYMBOL);

            loadIntrinsics();

        } catch (JessException re) {
            System.out.println("*** FATAL ***: Can't initialize Jess");
            re.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Load in all the intrinsic functions
     */

    static Userfunction getIntrinsic(String name) {
        return (Userfunction) m_intrinsics.get(name);
    }

    /**
     * Lists all the functions built into this version of Jess.
     * @return the Iterator
     */
    public static Iterator listIntrinsics() {
        return m_intrinsics.values().iterator();
    }

    private static void addIntrinsic(Userfunction uf) {
        m_intrinsics.put(uf.getName(), uf);
    }

    private static void addPackage(IntrinsicPackage ip) {
        ip.add(m_intrinsics);
    }

    private static void loadIntrinsics() throws JessException {

        try {
            addIntrinsic(new Remove());
            addIntrinsic(new Help());
            addIntrinsic(new DoBackwardChaining());
            addIntrinsic(new GensymStar());
            addIntrinsic(new Bind());
            addIntrinsic(new And());
            addIntrinsic(new Or());
            addIntrinsic(new Not());
            addIntrinsic(new SymCat());
            addIntrinsic(new UnDefrule());
            addIntrinsic(new UnDeffacts());
            addIntrinsic(new Batch());
            addIntrinsic(new Implement());
            addIntrinsic(new Lambda());
            addIntrinsic(new Require());
            addIntrinsic(new RequireStar());
            addIntrinsic(new Provide());
            addIntrinsic(new Watch());
            addIntrinsic(new Unwatch());
            addIntrinsic(new SetWatchRouter());
            addIntrinsic(new JessVersion(JessVersion.NUMBER));
            addIntrinsic(new JessVersion(JessVersion.STRING));

            addIntrinsic(new HaltEtc(HaltEtc.HALT));
            addIntrinsic(new HaltEtc(HaltEtc.EXIT));
            addIntrinsic(new HaltEtc(HaltEtc.CLEAR));
            addIntrinsic(new HaltEtc(HaltEtc.RUN));
            addIntrinsic(new HaltEtc(HaltEtc.RESET));
            addIntrinsic(new HaltEtc(HaltEtc.RETURN));

            addIntrinsic(new StoreFetch(StoreFetch.STORE));
            addIntrinsic(new StoreFetch(StoreFetch.FETCH));
            addIntrinsic(new StoreFetch(StoreFetch.CLEAR_STORAGE));

            addIntrinsic(new Defadvice(Defadvice.ADVICE));
            addIntrinsic(new Defadvice(Defadvice.UNADVICE));

            addPackage(new FactFunctions());
            addPackage(new IOFunctions());
            addPackage(new ControlFunctions());
            addPackage(new ArithmeticFunctions());
            addPackage(new ReflectFunctions());
            addPackage(new StringFunctions());
            addPackage(new PredFunctions());
            addPackage(new MultiFunctions());
            addPackage(new MiscFunctions());
            addPackage(new ModuleFunctions());
            addPackage(new MathFunctions());
            addPackage(new LispFunctions());
            addPackage(new DumpFunctions());
            addPackage(new ReflectFunctions());
            addPackage(new BagFunctions());
            addPackage(new RegexpFunctions());
            addPackage(new DebugFunctions());
            addPackage(new QueryFunctions());
                        
        } catch (Throwable t) {
            throw new JessException("Funcall.loadIntrisics",
                                    "Missing non-optional function class",
                                    t);
        }
    }

    private FunctionHolder m_function;

    /**
     * Create a Funcall given the name. The Funcall's arguments must
     * then be added using methods inherited from ValueVector.
     *
     * @param name   The name of the function
     * @param engine The Rete engine where the function is defined
     * @throws JessException If something goes wrong.
     */

    public Funcall(String name, Rete engine) throws JessException {
        add(engine.getValueFactory().get(name, RU.SYMBOL));
        m_function = engine.findFunctionHolder(name);
    }

    /**
     * Construct a funcall without specifying the function to call.
     * @param size
     */
    private Funcall(int size) {
        super(size);
    }

    Funcall negate() throws JessException {
        Funcall f = new Funcall(2);
        f.add(new Value("not", RU.SYMBOL));
        f.add(new FuncallValue(this));
        return f;
    }


    /**
     * Returns the name of the function this Funcall will invoke
     * @return the name of the function
     */
    public String getName() {
        try {
            return get(0).stringValue(null);
        } catch (JessException cantHappen) {
            return null;
        }
    }


    /**
     * Copies a Funcall
     *
     * @return A copy of the Funcall
     */

    public Object clone() {
        Funcall f = new Funcall(size());
        cloneInto(f);
        LineNumberRecord lnr = Rete.lookupFunction(this);
        if (lnr != null)
            Rete.recordFunction(f, lnr);
        return f;
    }

    /**
     * Makes the argument into a copy of this Funcall.
     *
     * @param vv The Funcall into which the copy should be made
     * @return The argument
     */
    private void cloneInto(Funcall vv) {
        super.cloneInto(vv);
        vv.m_function = m_function;
    }


    /**
     * Execute this funcall in a particular context.
     *
     * @param context An execution context for the function
     * @return The result of the function call
     * @throws JessException If something goes wrong
     */

    public Value execute(Context context) throws JessException {
        Rete engine = context.getEngine();
        try {
            resolve(engine);
            engine.broadcastEvent(JessEvent.USERFUNCTION_CALLED, this, context);
            return m_function.call(this, context);

        } catch (JessException re) {
            re.addContext(toStringWithParens(), context);
            throw re;

        } catch (Exception e) {
            String name = get(0).stringValue(context);
            JessException jex =
                    new JessException(name, "Error during execution", e);

            jex.addContext(toStringWithParens(), context);
            throw jex;

        } finally {
            engine.broadcastEvent(JessEvent.USERFUNCTION_RETURNED, this, context);
        }
    }

    /**
     * Returns the Userfunction object this Funcall would invoke if it were invoked now,
     * @param engine the Rete object that will supply the execution context
     * @return the Userfunction object
     * @throws JessException
     */
    public Userfunction getUserfunction(Rete engine) throws JessException {
        resolve(engine);
        return m_function.getFunction();
    }

    private void resolve(Rete engine) throws JessException {
        if (m_function == null) {
            String name = get(0).stringValue(null);
            m_function = engine.findFunctionHolder(name);
            if (m_function == null)
                undefinedFunction(name);
        }
    }

    private void undefinedFunction(String name) throws JessException {
        String message;
        if (Jesp.isAConstructName(name)) {
            message = "This construct can only be used at the top level of a program;" +
                    " it can't be used as if it were a function:";

        } else {
            message = "Undefined function";
        }

        throw new JessException("Funcall.execute", message, name);
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param v An argument to add to this Funcall
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(Value v) {
        add(v);
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param s An argument to add to this Funcall, interpreted as a symbol
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(String s) throws JessException {
        add(new Value(s, RU.SYMBOL));
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param i An argument to add to this Funcall, interpreted as an integer
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(int i) throws JessException {
        add(new Value(i, RU.INTEGER));
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param f An argument to add to this Funcall, interpreted as an integer
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(double f) throws JessException {
        add(new Value(f, RU.FLOAT));
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param l An argument to add to this Funcall, interpreted as a long
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(long l) throws JessException {
        add(new LongValue(l));
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param f An argument to add to this Funcall, interpreted as a function call
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(Funcall f) throws JessException {
        add(new FuncallValue(f));
        return this;
    }

    /**
     * Calls {@link ValueVector#add}, then returns this object. This method does the same thing as
     * <tt>add</tt> but returns this <tt>Funcall</tt> as a <tt>Funcall</tt> so the last call in a chain can be
     * a call to {@link #execute}.
     *
     * @param o An argument to add to this Funcall, interpreted as a Java object
     * @return This Funcall
     * @see ValueVector#add
     */
    public Funcall arg(Object o)  {
        add(new Value(o));
        return this;
    }

    /**
     * A  version of ValueVector.get with more appropriate error messages.
     * Fetch the entry at position i in this Funcall.
     *
     * @param i The 0-based index of the Value to fetch
     * @return The Value
     */
    public Value get(int i) throws JessException {
        if (i < 0)
            throw new JessException("Funcall.get",
                                    "Negative index " + i + " out of bounds on this Funcall:",
                                    toStringWithParens());
        else if (i >= m_ptr)
            throw new JessException("Funcall.get",
                                    "Missing argument(s) in function call",
                                    toStringWithParens());
        else
            return m_v[i];
    }

    public Object accept(Visitor v) {
        return v.visitFuncall(this);
    }

    Object getScratchPad() {
        return m_scratchPad;
    }

    void setScratchPad(Object scratchPad) {
        m_scratchPad = scratchPad;
    }

    public void reset() {
        m_function = null;
    }
}


/**
 * *** gensym*  ***
 */

class GensymStar implements Userfunction, Serializable {
    public String getName() {
        return "gensym*";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return new Value(RU.gensym("gen"), RU.SYMBOL);
    }
}

/**
 * *** bind ***
 */

class Bind implements Userfunction, Serializable {
    public String getName() {
        return "bind";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value rv = vv.get(2).resolveValue(context);
        String name = vv.get(1).variableValue(context);
        Rete engine = context.getEngine();
        if (name.indexOf(engine.getMemberChar()) > -1)
            changeObject(name, rv, engine, context);
        else
            context.setVariable(name, rv);
        return rv;
    }

    private void changeObject(String name, Value value, Rete engine, Context context) throws JessException {
        int index = name.indexOf(engine.getMemberChar());
        String target = name.substring(0, index);
        String property = name.substring(index+1);
        Object obj = context.getVariable(target).javaObjectValue(context);
        if (obj instanceof Fact) {
            Fact f = (Fact) obj;
            context.getEngine().modify(f, property, value);
        } else {
            // TODO Refactoring so that we can do this without building the Funcall
            Funcall funcall = new Funcall("set", context.getEngine());
            funcall.arg(obj);
            funcall.arg(property);
            funcall.arg(value);
            funcall.execute(context);
        }
    }
}

/**
 * *** and ***
 */

class And implements Userfunction, Serializable {

    public String getName() {
        return "and";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        for (int i = 1; i < vv.size(); i++) {
            Value v = vv.get(i).resolveValue(context);

            if (v.equals(Funcall.FALSE))
                return Funcall.FALSE;
        }

        return Funcall.TRUE;
    }
}

/**
 * *** or ***
 */
class Or implements Userfunction, Serializable {

    public String getName() {
        return "or";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        for (int i = 1; i < vv.size(); i++) {
            Value v = vv.get(i).resolveValue(context);

            if (!v.equals(Funcall.FALSE))
                return Funcall.TRUE;
        }
        return Funcall.FALSE;
    }
}

/**
 * *** not ***
 */

class Not implements Userfunction, Serializable {

    public String getName() {
        return "not";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        if (vv.get(1).resolveValue(context).equals(Funcall.FALSE))
            return Funcall.TRUE;
        else
            return Funcall.FALSE;
    }
}

/**
 * *** sym-cat ***
 */
class SymCat implements Userfunction, Serializable {

    public String getName() {
        return "sym-cat";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {

        StringBuffer buf = new StringBuffer("");
        for (int i = 1; i < vv.size(); i++) {
            Value val = vv.get(i).resolveValue(context);
            if (val.type() == RU.STRING)
                buf.append(val.stringValue(context));
            else if (val.type() == RU.JAVA_OBJECT)
                buf.append(val.javaObjectValue(context).toString());
            else
                buf.append(val.toString());
        }

        return new Value(buf.toString(), RU.SYMBOL);
    }
}

/**
 * *** store, fetch **
 */

class StoreFetch implements Userfunction, Serializable {
    static final int STORE = 0, FETCH = 1, CLEAR_STORAGE = 2;
    private static final String[] s_names = {"store", "fetch", "clear-storage"};
    private final int m_name;

    StoreFetch(int name) {
        m_name = name;
    }

    public String getName() {
        return s_names[m_name];
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value v;
        switch (m_name) {
            case STORE:
                Value val = vv.get(2).resolveValue(context);
                if (val.equals(Funcall.NIL))
                    val = null;
                v = context.getEngine().store(vv.get(1).stringValue(context), val);

                if (v != null)
                    return v;
                else
                    return Funcall.NIL;

            case CLEAR_STORAGE:
                context.getEngine().clearStorage();
                return Funcall.TRUE;

            case FETCH:
            default:
                v = context.getEngine().fetch(vv.get(1).stringValue(context));
                if (v != null)
                    return v.resolveValue(context);
                else
                    return Funcall.NIL;
        }
    }
}

/**
 * *** HaltEtc ***
 */
class HaltEtc implements Userfunction, Serializable {
    static final int HALT = 0;
    static final int EXIT = 1;
    static final int CLEAR = 2;
    static final int RUN = 3;
    static final int RESET = 4;
    static final int RETURN = 5;
    private static final int BREAK = 6;
    private static final String[] s_names = {"halt", "exit", "clear", "run", "reset", "return", "break"};
    private final int m_name;

    HaltEtc(int name) {
        m_name = name;
    }

    public String getName() {
        return s_names[m_name];
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        switch (m_name) {
            case HALT: {
                engine.halt();
                break;
            }

            case EXIT: {
                PrintThread.getPrintThread().waitForCompletion();
                System.exit(0);
                break;
            }

            case CLEAR: {
                engine.clear();
                break;
            }

            case RUN: {
                if (vv.size() == 1)
                    return context.getEngine().getValueFactory().get(engine.run(context), RU.INTEGER);
                else
                    return context.getEngine().getValueFactory().get(engine.run(vv.get(1).intValue(context), context),
                                     RU.INTEGER);
            }
            case RESET: {
                engine.reset();
                break;
            }

            case RETURN: {
                if (vv.size() > 1)
                    return context.setReturnValue(vv.get(1).
                            resolveValue(context));
                else
                    return context.setReturnValue(Funcall.NIL);
            }

            case BREAK: {
                throw BreakException.INSTANCE;
            }

        }
        return Funcall.TRUE;
    }
}

class Remove implements Userfunction, Serializable {

    public String getName() {
        return "remove";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        context.getEngine().removeFacts(vv.get(1).stringValue(context));
        return Funcall.NIL;
    }
}

/**
 * *** unwatch ***
 */

class Unwatch implements Userfunction, Serializable, WatchConstants {

    public String getName() {
        return "unwatch";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String what = vv.get(1).stringValue(context);
        Rete engine = context.getEngine();

        if (what.equals("rules"))
            engine.unwatch(RULES);

        else if (what.equals("facts"))
            engine.unwatch(FACTS);

        else if (what.equals("activations"))
            engine.unwatch(ACTIVATIONS);

        else if (what.equals("compilations"))
            engine.unwatch(COMPILATIONS);

        else if (what.equals("focus"))
            engine.unwatch(FOCUS);

        else if (what.equals("all"))
            engine.unwatchAll();
        else
            throw new JessException("unwatch",
                                    "unwatch: can't unwatch", what);

        return Funcall.TRUE;
    }
}

/**
 * *** watch ***
 */

class Watch implements Userfunction, Serializable, WatchConstants {

    public String getName() {
        return "watch";
    }

    // Note that the ordering of things (when installListener, THEN
    // set flag, but unset, then remove) is carefully orchestrated. Be
    // careful when modifying.

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        for (int i = 1; i < vv.size(); ++i) {
            String what = vv.get(i).stringValue(context);

            if (what.equals("rules"))
                engine.watch(RULES);

            else if (what.equals("facts"))
                engine.watch(FACTS);

            else if (what.equals("activations"))
                engine.watch(ACTIVATIONS);

            else if (what.equals("compilations"))
                engine.watch(COMPILATIONS);

            else if (what.equals("focus"))
                engine.watch(FOCUS);

            else if (what.equals("all"))
                engine.watchAll();

            else
                throw new JessException("watch",
                                        "watch: can't watch/unwatch", what);
        }
        return Funcall.TRUE;
    }


    public String toString() {
        return "[The watch command]";
    }
}

class SetWatchRouter implements Userfunction, Serializable {

    public String getName() {
        return "set-watch-router";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String oldWatchRouterName = engine.setWatchRouter(vv.get(1).symbolValue(context));
        return new Value(oldWatchRouterName, RU.SYMBOL);
    }
}

class UnDefrule implements Userfunction, Serializable {
    public String getName() {
        return "undefrule";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String rulename = vv.get(1).stringValue(context);
        Rete engine = context.getEngine();
        HasLHS rule = engine.findDefrule(rulename);
        engine.removeDefrule(rulename);
        return rule == null ? Funcall.FALSE : Funcall.TRUE;

    }
}

class UnDeffacts implements Userfunction, Serializable {
    public String getName() {
        return "undeffacts";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String name = vv.get(1).stringValue(context);
        return context.getEngine().unDeffacts(name);

    }
}


/**
 * Do backward-chaining (goal-seeking) for a particular deftemplate.
 */

class DoBackwardChaining implements Userfunction, Serializable {
    public String getName() {
        return "do-backward-chaining";
    }

    public Value call(ValueVector vv, Context context)
            throws JessException {
        Rete engine = context.getEngine();
        String name = vv.get(1).stringValue(context);
        if (name.equals("test") || Group.isGroupName(name))
            throw new JessException("do-backward-chaining",
                                    "Can't backchain on special CEs", name);
        Deftemplate dt = engine.findDeftemplate(name);
        if (dt == null)
            dt = engine.createDeftemplate(name);

        dt.doBackwardChaining(engine);
        return Funcall.TRUE;
    }

}


