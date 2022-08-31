package jess;

import java.io.Serializable;
import java.util.*;

/**
 <p>A Deftemplate defines a class of Jess facts. Its definition
 includes a list of properties or "slots", each of which can have a
 default value and a type. Deftemplates can be created by hand, but
 they usually come from executing the "deftemplate" construct or the
 "defclass" function in the Jess language.
 To build a useful template, you need to specify the name to the constructor,
 then add the named slots one by one:</p>

 <pre>
 Rete r = new Rete();
 Deftemplate dt = new Deftemplate("point", "A 2D point", r);
 Value zero = new Value(0, RU.INTEGER);
 dt.addSlot("x", zero, "NUMBER");
 dt.addSlot("y", zero, "NUMBER");
 r.addDeftemplate(dt);
 </pre>

 (C) 2013 Sandia Corporation<br>

 */
public class Deftemplate implements Serializable, Visitable, Modular {

    // Keys are type names, values are type codes
    private static final Map s_typeCodes = new HashMap();

    // Keys are type codes, values are type names
    private static final Map s_typeNames = new HashMap();

    static final String[] TYPE_NAMES = {
        "ANY",
        "INTEGER",
        "FLOAT",
        "NUMBER",
        "ATOM",
        "SYMBOL",
        "STRING",
        "LEXEME",
        "OBJECT",
        "LONG"
    };

    static final int[] TYPE_CODES = {
        RU.ANY,
        RU.INTEGER,
        RU.FLOAT,
        RU.NUMBER,
        RU.SYMBOL,
        RU.SYMBOL,
        RU.STRING,
        RU.LEXEME,
        RU.JAVA_OBJECT,
        RU.LONG
    };

    private static final Deftemplate
            s_rootTemplate = new Deftemplate(RU.ROOT_DEFTEMPLATE, "Parent template");
    private static final Deftemplate s_clearTemplate = new Deftemplate("__clear", "(Implied)");
    private static final Deftemplate s_nullTemplate = new Deftemplate("__not_or_test_CE", "(Implied)");
    private static final Deftemplate s_initialTemplate = new Deftemplate("initial-fact", "(Implied)");
    private static final Deftemplate s_testTemplate = new Deftemplate("test", "(Implied)");
    private static final Deftemplate s_accumulateTemplate = new Deftemplate(Accumulate.RESULT, "(Accumulate result)");

    static {
        try {
            for (int i = 0; i < TYPE_NAMES.length; i++) {
                Value value = new Value(TYPE_CODES[i], RU.INTEGER);
                s_typeCodes.put(TYPE_NAMES[i], value);
                s_typeNames.put(value, TYPE_NAMES[i]);
            }

            s_accumulateTemplate.addSlot("value", Funcall.NIL, "ANY");
            s_testTemplate.addSlot("__data", Funcall.NILLIST, "ANY");

        } catch (JessException re) { /* can't happen */
        }
    }

    private boolean m_backchain;
    private final String m_baseName;
    private final String m_fullName;
    private String m_docstring = "";
    private Deftemplate m_parent;
    private final Map m_indexes;
    private final String m_module;
    private boolean m_slotSpecific;
    private Map m_slots;
    private boolean m_frozen;

    /**
     * Create a template that extends the root template.
     * @param name the deftemplate name
     * @param docstring the deftemplate's documentation string
     * @param engine the rule engine to create the template in
     * @throws JessException if anything goes wrong
     */
    public Deftemplate(String name, String docstring, Rete engine)
            throws JessException {

        this(name, docstring, s_rootTemplate, engine);
    }

    /**
     * Create a deftemplate 'derived from' another
     * one. If the name contains a module name, it will be
     * used. Otherwise, the template will be in the current module.
     * @param name the deftemplate name
     * @param docstring the deftemplate's documentation string
     * @param dt the 'parent' of this deftemplate
     * @param engine the engine to create the template in
     * @throws JessException if anything goes wrong
     */

    public Deftemplate(String name, String docstring, Deftemplate dt, Rete engine)
            throws JessException {
        // ###

        int colons = name.indexOf("::");
        if (colons != -1) {
            m_module = name.substring(0, colons);
            engine.verifyModule(m_module);
            m_baseName = name.substring(colons + 2);
            m_fullName = name;
        } else {
            m_module = engine.getCurrentModule();
            m_baseName = name;
            m_fullName = engine.resolveName(name);
        }

        m_parent = dt;
        m_docstring = docstring;

        m_slots = Collections.synchronizedMap(new HashMap());
        for (Iterator it = dt.m_slots.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            Slot slot = (Slot) entry.getValue();
            m_slots.put(entry.getKey(), slot.clone());
        }
        m_indexes = Collections.synchronizedMap(new TreeMap(dt.m_indexes));
    }

    /*
     * Used only to construct (not), (test), (__fact), etc.
     */

    private Deftemplate(String name, String docstring) {
        // ###
        m_module = Defmodule.MAIN;
        m_baseName = name;
        m_fullName = RU.scopeName(m_module, m_baseName);
        m_docstring = docstring;
        if (!name.equals(RU.ROOT_DEFTEMPLATE))
            m_parent = s_rootTemplate;
        m_indexes = Collections.synchronizedMap(new TreeMap());
        m_slots = Collections.synchronizedMap(new HashMap());
    }


    /**
     * The root template that serves as the ultimate parent of all
     * other templates. This template is to deftemplates as java.lang.Object is to
     * Java objects.
     * @return The singleton root template.
     */
    public static Deftemplate getRootTemplate() {
        return s_rootTemplate;
    }

    /**
     * The template for "initial-fact".
     * @return A deftemplate.
     */
    public static Deftemplate getInitialTemplate() {
        return s_initialTemplate;
    }

    /**
     * The template for a special fact used internally by Jess.
     * @return A deftemplate
     */
    public static Deftemplate getClearTemplate() {
        return s_clearTemplate;
    }

    /** The template for a special fact used internally by Jess.
     * @return A deftemplate
     */
    public static Deftemplate getNullTemplate() {
        return s_nullTemplate;
    }

    /** The template for a special fact used internally by Jess.
     * @return A deftemplate
     */
    public static Deftemplate getTestTemplate() {
        return s_testTemplate;
    }

    /** The template for a special fact used internally by Jess.
     * @return A deftemplate
     */
    public static Deftemplate getAccumTemplate() {
        return s_accumulateTemplate;
    }

    static void addStandardTemplates(Rete r) throws JessException {
        r.addDeftemplate(getRootTemplate());
        r.addDeftemplate(getNullTemplate());
        r.addDeftemplate(getClearTemplate());
        r.addDeftemplate(getInitialTemplate());
    }


    /**
     * Compare this deftemplate to another one for equality. Two templates are equal if
     * the have the same full name, the same parent, and all the same slots.
     * @param o another template to compare this this one
     * @return true if the templates are the same.
     */
    public boolean equals(Object o) {
        if (!(o instanceof Deftemplate))
            return false;
        Deftemplate t = (Deftemplate) o;
        return (m_fullName.equals(t.m_fullName) &&
                (m_backchain == t.m_backchain) &&
                (m_parent == t.m_parent) &&
                m_slots.equals(t.m_slots));
    }

    /**
     * Return a hash code for this template.
     * @return the hashcode
     */
    public int hashCode() {
        return m_fullName.hashCode();
    }

    /**
     * Return the parent of this deftemplate. The parent is another
     * deftemplate this one extends. If this template extends no
     * other template explicitly, then it extends the root template.
     * @return the parent deftemplate
     */
    public Deftemplate getParent() {
        return m_parent;
    }

    /**
     * Sever the link with this deftemplate's parent. Useful when
     * creating similar, but unrelated deftemplates.
     */
    private void forgetParent() {
        m_parent = s_rootTemplate;
    }


    /**
     * Get the name of this deftemplate. The name is qualified by the module name.
     * @return the name of this deftemplate
     */
    public final String getName() {
        return m_fullName;
    }

    /**
     * Get the name of this deftemplate unqualified by the module name.
     * @return The name of this deftemplate
     */
    public final String getBaseName() {
        return m_baseName;
    }

    /**
     * Get the docstring of this deftemplate.
     * @return The docstring
     */
    public final String getDocstring() {
        return m_docstring;
    }

    /**
     * Make this deftemplate backwards-chaining reactive. Sets a flag in this template, and
     * creates the backward-chaining trigger template. You must wait until you've defined all the slots in a template
     * before calling this; otherwise the behavior is undefined.
     * @param engine the engine the new template is created in
     * @throws JessException if anything goes wrong
     */
    public final void doBackwardChaining(Rete engine) throws JessException {
        m_backchain = true;
        Deftemplate newDt = getBackchainingTemplate(engine);
        newDt.forgetParent();
        engine.addDeftemplate(newDt);
    }

    /**
     * Get the backchaining reactivity of this deftemplate.
     * @return True if this deftemplate can stimulate backwards chaining.
     */
    public final boolean getBackwardChaining() {
        return m_backchain;
    }

    /**
     * Create a new slot in this deftemplate. If the slot already
     * exists, just change the default value. The type parameter is currently stored
     * with the deftemplate, but not used by Jess for any purpose.
     * @param name name of the slot
     * @param value default value for the slot
     * @param typename type of the slot: INTEGER, FLOAT, ANY, etc.
     * @exception JessException If something goes wrong
     */
    public void addSlot(String name, Value value, String typename)
            throws JessException {
        Value type = getSlotTypeCode(typename);

        // Just set default if duplicate
        if (getSlotIndex(name) != -1) {
            setSlotDefault(name, value);
            setSlotDataType(name, type);

        } else {
            int idx = m_slots.size();
            Value nameValue = new Value(name, RU.SLOT);
            m_slots.put(name, new Slot(nameValue, value, type, idx));
            m_indexes.put(new Integer(idx), name);
        }
    }

    void setSlotDataType(String name, Value type) throws JessException {
        Slot slot = getSlot(name);
        slot.m_dataType = type;
    }

    void setSlotDefault(String name, Value value) throws JessException {
        Slot slot = getSlot(name);
        ValueVector allowed = slot.m_allowedValues;
        // TODO Multislots here
        if (allowed != null) {
            if (slot.getName().type() == RU.SLOT) {
                if (value.equals(Funcall.NIL)) {
                    slot.m_default = allowed.get(0);
                } else {
                    if (!allowed.contains(value))
                        throw new JessException("Deftemplate.setSlotAllowedValues",
                                "Default value for slot " + name + " not one of allowed values",
                                value.toString());
                }
            }
        }

        slot.m_default = value;
    }

    static boolean isValidSlotType(String typename) {
        return s_typeCodes.containsKey(typename.toUpperCase());
    }

    private static Value getSlotTypeCode(String typename) throws JessException {
        Value type = (Value) s_typeCodes.get(typename.toUpperCase());
        if (type == null)
            throw new JessException("Deftemplate.getSlotTypeCode",
                                    "Bad slot type:", typename);
        return type;
    }

    static String getSlotTypeName(Value typecode) throws JessException {
        String type = (String) s_typeNames.get(typecode);
        if (type == null)
            throw new JessException("Deftemplate.getSlotTypeName",
                                    "Bad slot type:", typecode.toString());
        return type;
    }

    /**
     * Create a new multislot in this deftemplate. If the slot already
     * exists, just change the default value. The type parameter is currently stored
     * with the deftemplate, but not used by Jess for any purpose.
     * @param name name of the slot
     * @param value default value for the slot
     * @param typename name of Jess data type for slot contents
     * @exception JessException if something goes wrong
     */
    public void addMultiSlot(String name, Value value, String typename) throws JessException {
        Value type = getSlotTypeCode(typename);

        // Just set default if duplicate
        if (getSlotIndex(name) != -1) {
            setSlotDefault(name, value);
        } else {
            Value nameValue = new Value(name, RU.MULTISLOT);
            int idx = m_slots.size();
            m_slots.put(name, new Slot(nameValue, value, type, idx));
            m_indexes.put(new Integer(idx), name);
        }
    }

    /**
     * Create a new multislot in this deftemplate. If the slot already
     * exists, just change the default value. The type parameter is currently stored
     * with the deftemplate, but not used by Jess for any purpose. The slot type will
     * be set to "ANY".
     * @param name name of the slot
     * @param value default value for the slot
     * @exception JessException if something goes wrong
     */
    public void addMultiSlot(String name, Value value) throws JessException {
        addMultiSlot(name, value, "ANY");
    }

    /**
     * Returns the slot data type (one of the constants in jess.RU)
     * for the slot given by the zero-based index.
     * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
     * @return The data type of that slot (RU.INTEGER, RU.SYMBOL, etc., or RU.ANY)
     * @throws JessException if anything goes wrong
     */

    public int getSlotDataType(int index) throws JessException {
        return getSlot(index).getDataType().intValue(null);
    }

    /**
     * Returns the slot data type (one of the constants in jess.RU)
     * for the named slot
     * @param slotName The name of the slot
     * @return The data type of that slot (RU.INTEGER, RU.SYMBOL, etc., or RU.ANY)
     * @throws JessException if anything goes wrong
     */

    public int getSlotDataType(String slotName) throws JessException {
        return getSlot(slotName).getDataType().intValue(null);
    }

    /**
     * Returns the default value of a slot given by the zero-based
     * index.
     * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
     * @return The default value for that slot (can be Funcall.NIL or Funcall.NILLIST for none
     * @throws JessException if anything goes wrong
     */

    public Value getSlotDefault(int index) throws JessException {
        return getSlot(index).getDefault();
    }

    /**
     * Returns the default value of a named slot
     * @param slotName The name of the slot
     * @return The default value for that slot (can be Funcall.NIL or Funcall.NILLIST for none)
     * @throws JessException if anything goes wrong
     */
    public Value getSlotDefault(String slotName) throws JessException {
        return getSlot(slotName).getDefault();
    }

    /**
     * Returns a list of allowed values for a slot. If any value is allowed, this method returns null.
     * @param slotName the slot of interest
     * @return a list of allowed values, or null if none are specified
     */
    public ValueVector getSlotAllowedValues(String slotName) throws JessException {
        return getSlot(slotName).getAllowedValues();
    }

    /**
     * Returns the slot type (RU.SLOT or RU.MULTISLOT) of the slot in
     * this deftemplate given by the zero-based index.
     * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
     * @return The type of that slot (RU.SLOT or RU.MULTISLOT)
     * @throws JessException if anything goes wrong
     * */

    public int getSlotType(int index)
            throws JessException {
        return getSlot(index).getName().type();
    }

    /**
     * Returns the slot type (RU.SLOT or RU.MULTISLOT) of the slot in
     * this deftemplate given by the zero-based index.
     * @param name The name of the slot
     * @return The type of that slot (RU.SLOT or RU.MULTISLOT)
     * @throws JessException if anything goes wrong
     * */

    public int getSlotType(String name) throws JessException {
        int index = getSlotIndex(name);
        if (index == -1)
            throw new JessException("Deftemplate.getSlotType",
                                    "No such slot " + name + " in template", m_fullName);
        return getSlotType(index);
    }

    /**
     * Returns true if the slot at the given index is a multislot.
     * @param index the index of the slot
     * @return true if the slot is a multislot
     * @throws JessException if the index is invalid
     */
    public boolean isMultislot(int index) throws JessException {
        return getSlotType(index) == RU.MULTISLOT;
    }

    /**
     * Return the index (0 through getNSlots()-1) of the named slot,
     * or -1 if there is no such slot.
     * @param slotname The name of the slot
     * @return The zero-based index of the slot
     */
    public int getSlotIndex(String slotname) {
        Slot slot = (Slot) m_slots.get(slotname);
        if (slot == null)
            return -1;
        else
            return slot.getIndex();
    }

    /**
     * Return the name of a given slot in this deftemplate.
     * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
     * @return The name of that slot
     * @exception JessException If something is horribly wrong
     */
    public String getSlotName(int index) throws JessException {
        return getSlot(index).getName().stringValue(null);
    }

    /**
     * Return a new array containing the names of all the slots in this deftemplate.
     * @return a new array
     */
    public String[] getSlotNames() {
        String[] names = new String[getNSlots()];
        for (int i = 0; i < names.length; i++) {
            try {
                names[i] = getSlotName(i);
            } catch (JessException silentlyIgnore) {
            }
        }
        return names;
    }


    /**
     * Return the number of slots in this deftemplate.
     * @return The number of slots in this deftemplate
     */
    public int getNSlots() {
        return m_slots.size();
    }

    /**
     * Turn this deftemplate into a String.
     * @return a string representation of the Deftemplate
     */
    public String toString() {
        return "[deftemplate " + m_fullName + "]";
    }

    /**
     * This class participates in the Visitor pattern.
     * @param v a Visitor that is interested in working with this object
     * @return whatever the visitor's visitDeftemplate() method returns
     */
    public Object accept(Visitor v) {
        return v.visitDeftemplate(this);
    }

    /**
     * Return the name of the module this deftemplate is in.
     * @return the module name
     */
    public String getModule() {
        return m_module;
    }

    /**
     * Indicate whether this template is a backward chaining trigger.
     * @return true if this template is a backward chaining trigger
     */

    public boolean isBackwardChainingTrigger() {
        return m_baseName.startsWith(RU.BACKCHAIN_PREFIX);
    }

    /**
     * Get the name of the backward chaining trigger template that would be associated with
     * this template, whether it exists or not.
     * @return the template name
     */
    public String getBackchainingTemplateName() {
        return RU.scopeName(m_module, RU.BACKCHAIN_PREFIX + m_baseName);
    }

    /**
     * Return the undecorated template name. If this template is not a backward chaining trigger,
     * then just return its name. If it is, then return the name of the template this template serves as a
     * trigger for.
     * @return the name
     */
    public String getNameWithoutBackchainingPrefix() {
        if (!isBackwardChainingTrigger())
            return m_fullName;
        else
            return RU.scopeName(m_module,
                                m_baseName.substring(RU.BACKCHAIN_PREFIX.length()));
    }


    /**
     * Return a new backward-chaining trigger template for this template.
     * @return the template
     * @param engine the engine containing the template
     * @throws JessException if anything goes wrong
     */

    public Deftemplate getBackchainingTemplate(Rete engine)
            throws JessException {

        return new Deftemplate(getBackchainingTemplateName(),
                               "Goal seeker for " + m_fullName,
                               this, engine);
    }

    /**
     * Return true if this is an ordered template. An ordered template is one that has a single
     * multislot named "__data".
     * @return true if this is an ordered template.
     */
    public boolean isOrdered() {
        return /*getNSlots() == 0 ||*/ getSlotIndex(RU.DEFAULT_SLOT_NAME) == 0;
    }

    /**
     * Set the slot-specific activation behavior for this template.
     * @param b true if this template should use slot-specific behavior
     */
    public void setSlotSpecific(boolean b) {
        m_slotSpecific = b;
    }

    /**
     * Query the slot-specific activation behavior for this template.
     * @return true if this template uses slot-specific behavior
     */
    public boolean isSlotSpecific() {
        return m_slotSpecific;
    }

    /**
     * Return the String "deftemplate".
     * @return "deftemplate"
     */
    public final String getConstructType() {
        return "deftemplate";
    }

    /**
     * If this is a template for shadow facts, return the Java class name.
     * Otherwise, return null.
     * @return a class name, or null
     */
    public String getShadowClassName() {
        if (isShadowTemplate())
            return m_docstring.substring(DefinstanceList.JAVA_OBJECT.length());
        else
            return null;
    }

    /**
     * Return true if this is a template for shadow facts.
     * @return true if this is a shadow template
     */
    public boolean isShadowTemplate() {
        return getSlotIndex("OBJECT") != -1 && m_docstring.indexOf(DefinstanceList.JAVA_OBJECT) == 0;
    }

    /**
     * Return true if this is a template for shadow facts, and public variables were included when it was created.
     * @return true if this is a shadow template with variables included
     * *throws JessException if anything goes wrong
     */
    public boolean includesVariables() throws JessException {
        if (!isShadowTemplate())
            return false;

        Value v = getSlotDefault(getNSlots()-2);

        if (v.type() != RU.JAVA_OBJECT)
            return false;

        return v.javaObjectValue(null) instanceof SerializableVD;
    }


    /**
     * Returns true if the argument is a special template name.
     * @param name the name of a template
     * @return true for "test" or "initial-fact", false otherwise.
     */
    public static boolean isSpecialName(String name) {
        return name.equals("test") || name.equals("initial-fact");
    }

    /**
     * Returns one of the special templates "test" or "initial-fact" by name.
     * @param name "test" or "initial-fact"
     * @return the names template or null
     */
    public static Deftemplate getSpecialTemplate(String name) {
        if (name.equals("test"))
            return s_testTemplate;
        else if (name.equals("initial-fact"))
            return s_initialTemplate;
        else
            return null;
    }

    public void setSlotAllowedValues(String slotName, ValueVector allowed) throws JessException {
        if (allowed != null && allowed.size() == 0)
            throw new JessException("Deftemplate.setSlotAllowedValues",
                    "Allowed values can't have zero members for slot",
                    slotName);
        Slot slot = getSlot(slotName);
        slot.m_allowedValues = allowed;
        if (allowed != null) {
            if (slot.getName().type() == RU.SLOT) {
                if (slot.getDefault().equals(Funcall.NIL)) {
                    slot.m_default = allowed.get(0);
                } else {
                    if (!allowed.contains(slot.m_default))
                        throw new JessException("Deftemplate.setSlotAllowedValues",
                                "Default value for slot " + slotName + " not one of allowed values",
                                slot.m_default.toString());
                }
            }
        }
    }

    Slot getSlot(int index) throws JessException {
        String name = (String) m_indexes.get(new Integer(index));
        if (name == null)
            throw new JessException("Deftemplate.getSlot", "No such slot in template " + m_fullName, index);
        return getSlot(name);
    }

    private Slot getSlot(String name) throws JessException {
        Slot slot = (Slot) m_slots.get(name);
        if (slot == null)
            throw new JessException("Deftemplate.getSlot", "No such slot in template " + m_fullName, name);
        return slot;
    }

    public boolean isAllowedValue(String slotName, Value slotValue) throws JessException {
        Slot slot = getSlot(slotName);
        ValueVector allowed = slot.getAllowedValues();
        if (allowed == null)
            return true;
        else if (slot.m_name.type() == RU.SLOT) {	    
            return !slotValue.isLiteral() || allowed.contains(slotValue);
	}
        else /* MULTISLOT */ {
            ValueVector list = slotValue.listValue(null);
            for (int i=0; i<list.size(); ++i) {
                Value v = list.get(i);
		if (!v.isLiteral())
		    continue;
		if (!allowed.contains(v))
                    return false;
            }
            return true;
        }
    }

    /**
     * Complete the initialization of this template. In particular, this method will propagate inherited
     * template properties to this child template. You generally don't have to call it, because {@link Rete#addDeftemplate(Deftemplate)}
     * calls it automatically.
     * @throws JessException if anything goes wrong
     */
    public void freeze(Rete engine) throws JessException {
        if (!m_frozen) {
            m_frozen = true;
            if (m_parent != null) {
                if (m_parent.getBackwardChaining())               
                    doBackwardChaining(engine);
                if (m_parent.isSlotSpecific())
                    setSlotSpecific(true);
            }
        }
    }

    /**
     * A single slot within a deftemplate.
     */

    static class Slot implements Serializable, Cloneable {
        private Value m_name;
        private Value m_default;
        private Value m_dataType;
        private ValueVector m_allowedValues;
        private int m_index;

        Slot(Value name, Value dflt, Value dataType, int index) {
            m_name = name;
            m_default = dflt;
            m_dataType = dataType;
            m_index = index;
        }

        public Value getName() {
            return m_name;
        }

        public Value getDefault() {
            return m_default;
        }

        public Value getDataType() {
            return m_dataType;
        }

        public ValueVector getAllowedValues() {
            return m_allowedValues;
        }

        public int getIndex() {
            return m_index;
        }

        public int hashCode() {
            return m_name.hashCode();
        }

        public boolean equals(Object o) {
            if (! (o instanceof Slot))
                return false;
            Slot slot = (Slot) o;
            return objectsEqual(m_name, slot.m_name) &&
                   objectsEqual(m_default, slot.m_default) &&
                   objectsEqual(m_dataType, slot.m_dataType) &&
                   objectsEqual(m_allowedValues, slot.m_allowedValues);
        }

        static boolean objectsEqual(Object o1, Object o2) {
            if (o1 == o2)
                return true;
            if ((o1 == null) != (o2 == null))
                return false;
            return (o1 == null || o1.equals(o2));

        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException cnse) {
                // Can't happen
                return null;
            }
        }

    }
}


