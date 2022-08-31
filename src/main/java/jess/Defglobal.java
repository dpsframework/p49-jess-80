package jess;
import java.io.Serializable;

/**
 * A Defglobal is a globally-accessible Jess variable. It has a
 * special naming convention: the name must begin and end with an
 * asterisk (*). You can create Defglobals and add them to a Rete
 * engine using {@link jess.Rete#addDefglobal(Defglobal)} .
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public class Defglobal implements Serializable, Visitable, Named {
    private String m_name;
    private Value m_value;

    /**
     * Create a defglobal. Should be added to a Rete object with
     * Rete.addDefglobal. Note that a separate Defglobal object must be
     * created for each global variable, even though one (defglobal)
     * construct may represent more than one such variable.
     *
     * @param name The defglobal's variable name. Note that the name
     * must begin and end with an asterisk.
     * @param val The initial value for the defglobal; can be an
     * RU.FUNCALL value.  */

    public Defglobal(String name, Value val) {
        // ###
        m_name = name;
        m_value = val;
    }

    /**
     * Reinject this Defglobal into the engine
     */

    public void reset(Rete engine) throws JessException {
        try {
            Context gc = engine.getGlobalContext();
            gc.setVariable(m_name, m_value.resolveValue(gc));
        } catch (JessException re) {
            re.addContext("definition for defglobal ?" + m_name, engine.getGlobalContext());
            throw re;
        }
    }
    
    /**
     * Get this defglobal's variable name
     * @return The variable name
     */
    public String getName() { return m_name; }

    /**
     * Get this defglobal's initialization value. The returned Value may be a
     * simple value, a Variable, or a FuncallValue, so be careful how you
     * interpret it.
     * @return The value this variable was originally initialized to
     */
    public Value getInitializationValue() { return m_value; }

    /**
     * Describe myself
     * @return A pretty-printed version of the defglobal, suitable for
     * parsing
     */
    public String toString() {
        return "[defglobal " + m_name + "]";
    }

    public Object accept(Visitor v) {
        return v.visitDefglobal(this);
    }

    static boolean isADefglobalName(String name) {
        return name.startsWith("*") && name.endsWith("*");
    }

    public final String getConstructType() {
        return "defglobal";
    }

    /**
     * Always returns null
     * @return null
     */
    public final String getDocstring() {
        return null;
    }
}


