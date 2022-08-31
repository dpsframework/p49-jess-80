package jess;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;

/**
 * A named module containing rules, templates, and other constructs.
 * <P>
 * (C) 2013 Sandia Corporation<BR>
 */


public class Defmodule implements Serializable, Named, Visitable {
    public static final String MAIN = "MAIN";
    private final Map<String, Deftemplate> m_deftemplates;
    private String m_name;
    private String m_comment;
    private boolean m_autoFocus;

    /**
     * Construct a module named MAIN.
     */
    public Defmodule() {
        this(MAIN, "Default module");
    }

    /**
     * Construct a new module.
     * @param name the name of the new module
     * @param comment documentation for the module, or null.
     */
    public Defmodule(String name, String comment) {
        m_name = name;
        m_comment = comment;
        m_deftemplates = Collections.synchronizedMap(new TreeMap());
    }

    /**
     * Returns the name of this module
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the docmentation string for this module.
     * @return the documentation string, or null if none.
     */
    public String getDocstring() {
        return m_comment;
    }

    /**
     * Return an iterator over all the deftemplates defined in this module.
     * @return the iterator
     */
    public Iterator<Deftemplate> listDeftemplates() {
        return m_deftemplates.values().iterator();
    }

    /**
     * Return a deftemplate defined in this module, by name.
     * @param name the name of the desired deftemplate
     * @return the deftemplate if found, or null
     */
    public Deftemplate getDeftemplate(String name) {
        int index = name.indexOf("::");
        if (index != -1) {
            name = name.substring(index+2);
        }
        return (Deftemplate) m_deftemplates.get(name);
    }

    /**
     * Add a new deftemplate to this module. The deftemplate must
     * already have been created in this module; it must already report
     * this module's name as part of its name. You can redefine a template
     * only if the definition is identical to the old one.
     * @param dt a new deftemplate
     * @param engine the Rete engine this module belongs to
     * @return the new deftemplate, or an existing identical template
     * @throws JessException if anything goes wrong.
     */
    public Deftemplate addDeftemplate(Deftemplate dt, Rete engine)
        throws JessException {
        synchronized (m_deftemplates) {
            if (!dt.getModule().equals(m_name))
                throw new JessException("Defmodule.addDeftemplate",
                                        "Wrong module name",
                                        dt.getModule());
            String name = dt.getBaseName();
            Deftemplate existing = (Deftemplate) m_deftemplates.get(name);
            if (existing == null) {
                engine.broadcastEvent(JessEvent.DEFTEMPLATE, dt, engine.getGlobalContext());
                m_deftemplates.put(name, dt);
            }  else if (existing.equals(dt)) {
                return existing;
            }  else {
                throw new JessException("Defmodule.addDeftemplate",
                                        "Cannot redefine deftemplate",
                                        dt.getName());
            }
            return dt;
        }
    }

    /**
     * Return the type of this construct.
     * @return the string "defmodule".
     */
    public final String getConstructType() {
        return "defmodule";
    }

    public Object accept(Visitor v) {
        return v.visitDefmodule(this);
    }

    public void removeDeftemplate(Deftemplate template) {
        m_deftemplates.remove(template.getBaseName());
    }

    /**
     * Turns on auto-focus for this module. Equivalent to marking every rule in this module with the auto-focus property.
     * @see Defrule#setAutoFocus(boolean)
     * @param val true to turn on module autofocus
     */
    public void setAutoFocus(boolean val) {
        m_autoFocus = val;
    }

    /**
     * Return the auto-focus property for this module.
     * @see #setAutoFocus(boolean)
     * @return the autofocus property for this module
     */
    public boolean getAutoFocus() {
        return m_autoFocus;
    }
}
