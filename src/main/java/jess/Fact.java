package jess;

import java.io.Serializable;

/**
 <p>A Fact is the fundamental unit of information in Jess's working
 memory.  A <tt>Fact</tt> is stored as a list in which all the entries
 correspond to slots. The head or name of the fact is stored in a
 separate variable (available via the <tt>getName()</tt> method.)</p>

 <P>
 To construct a Fact, you must specify the "head" either as a String
 to the constructor, or by specifying the deftemplate to use. Then
 you have to populate the slots using {@link #setSlotValue}. Finally, you
 can add it to a Rete object's working memory using the {@link Rete#assertFact}
 method.</p>

 <B>Constructing an Unordered Fact from Java</B>

 <p>In the following example, we create a template and assert an
 unordered fact that uses it.</p>

 <pre>
 Rete r = new Rete();
 r.eval("(deftemplate point \"A 2D point\" (slot x) (slot y))");

 Fact f = new Fact("point", r);
 f.setSlotValue("x", new Value(37, RU.INTEGER));
 f.setSlotValue("y", new Value(49, RU.INTEGER));
 r.assertFact(f);
 </pre>

 <B>Constructing an Unordered Fact from Java</B>

 <p>In this example, the template has a multislot. In Java, a
 multislot is represented by a {@link Value} of type {@link RU#LIST}.
 The <tt>Value</tt> object contains a {@link ValueVector}  containing
 the fields of the multislot.</p>

 <pre>
 Rete r = new Rete();
 r.eval("(deftemplate vector \"A named vector\" (slot name) (multislot list))");

 Fact f = new Fact("vector", r);
 f.setSlotValue("name", new Value("Groceries", RU.SYMBOL));
 ValueVector vv = new ValueVector();
 vv.add(new Value("String Beans", RU.STRING));
 vv.add(new Value("Milk", RU.STRING));
 vv.add(new Value("Bread", RU.STRING));
 f.setSlotValue("list", new Value(vv, RU.LIST));
 r.assertFact(f);
 </pre>

 <B>Constructing an Ordered Fact from Java</B>

 <p>An ordered fact is actually represented as an unordered fact with a
 single slot: a multislot named __data. You don't need to create a
 template for an ordered fact: one will be created automatically if
 it doesn't already exist.</p>

 <p>
 Once you assert a jess.Fact object, you no longer "own" it - it
 becomes part of the Rete object's internal data structures. As such,
 you must not change the values of any of the Fact's slots. If you
 retract the fact, the Fact object is released and you are free to
 alter it as you wish. Alternatively, you can use the method {@link Rete#modify}
 to modify a fact.</p>

 (C) 2013 Sandia Corporation<br>
 */

public class Fact extends ValueVector implements Serializable, Modular, Visitable {
    private int m_id = -1;
    private Deftemplate m_deft;

    /**
     * Return value from getShadowMode() that indicates a Fact is not
     * a shadow fact.
     */
    public static final int NO=0;

    /**
     * Return value from getShadowMode() that indicates a Fact is a
     * dynamic shadow fact. A dynamic shadow fact is one that is
     * updated by PropertyChangeEvents from its corresponding
     * JavaBean.
     */
    public static final int DYNAMIC=1;

    /**
     * Return value from getShadowMode() that indicates a Fact is a
     * static shadow fact. A static shadow fact is one that is not
     * registered to receive PropertyChangeEvents.
     */
    public static final int STATIC=2;
    private int m_shadow;
    private String m_name;
    private Fact m_icon;
    private int m_time;
    private boolean m_expanded = false;

    /**
     * Return the canonical representation of this Fact object. For
     * most Facts, this returns "this", but for certain Facts -- in
     * particular, for Facts extracted from jess.Token objects -- it
     * may return another object. If you are examining a jess.Token
     * object and obtain a reference to a Fact from that Token, then
     * call getIcon() to "canonicalize" that Fact before using it.
     *
     * @return the canonical version of this Fact
     */

    public Fact getIcon() {
        return m_icon;
    }

    void setIcon(Fact f) {
        m_icon = f;
    }

    private static Fact s_nullFact, s_initialFact, s_clearFact;

    static {
        try {
            s_nullFact = new Fact(Deftemplate.getNullTemplate());
            s_clearFact = new Fact(Deftemplate.getClearTemplate());
            s_initialFact = new Fact(Deftemplate.getInitialTemplate());
        } catch (JessException je) {
            // Can't happen
        }
    }


    static Fact getNullFact() {
        return s_nullFact;
    }

    static Fact getInitialFact() {
        return (Fact) s_initialFact.clone();
    }

    static Fact getClearFact() {
        return s_clearFact;
    }

    /**
     * Returns the name or "head" of this Fact. For example, for the
     * fact <BR> <tt>(person (name Fred) (age 16))</tt>,<BR> given
     * that the "person" template is defined in the "MAIN" module,
     * this method will return "MAIN::person".
     * @return the name of this Fact.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns this Fact's fact-id. Each Fact in a Rete object's
     * working memory has a unique fact-id which is assigned when the
     * Fact is asserted and doesn't change. Fact-id's start at 0 and
     * increment from there.
     * @return the fact-id
     */
    public int getFactId() {
        return m_id;
    }

    void setFactId(int i) {
        m_id = i;
    }

    void setShadowMode(int mode) {
        m_shadow = mode;
    }

    /**
     * Indicates whether this Fact is a "shadow fact". A shadow fact
     * is the projection of a plain Java object into working
     * memory. Regular facts -- non-shadow facts -- don't have an
     * associated Java object.
     * @return true is this is a shadow fact
     */
    public boolean isShadow() {
        return m_shadow > 0;
    }

    /**
     * Indicates whether this fact is a shadow fact, and if so, what
     * type. Shadow facts can be "dynamic", in which case property
     * change events from the Java object will be used to update the
     * fact, or "static", in which case no change events are expected.
     * @return NO, DYNAMIC, or STATIC from this class
     */
    public int getShadowMode() {
        return m_shadow;
    }

    /**
     * Return the deftemplate for this fact. The jess.Deftemplate
     * object determines the name of the fact and the names and
     * properties of all its slots.
     * @return the deftemplate for this fact
     */

    public final Deftemplate getDeftemplate()  {
        return m_deft;
    }

    /**
     * Return the name of the module this fact is a member of. Every
     * fact belongs to a module, either the default module MAIN or
     * another user-defined module.
     * @return the name of the module
     */

    public final String getModule() {
        return m_deft.getModule();
    }


    /**
     * Return the "pseudotime" at which this Fact was asserted or last
     * modified. Each Rete object counts pseudotime in increments that
     * increase each time a fact is asserted or modified. Pseudotime
     * is used in determining which rules will fire first.
     * @return the pseudotime at which this Fact was asserted or last modified
     */
    public int getTime() {
        return m_time;
    }

    void updateTime(int time) {
        m_time = time;
    }

    /*
     * Return the contents of a slot in this fact. Slots are numbered
     * starting at zero, and they appear in the order in which they
     * are defined in the corresponding deftemplate. If you pass -1
     * for the argument, a FactIdValue representing this Fact's
     * fact-id is returned.
     * @param i the index of the slot to inspect, or -1
     * @return the contents of the corresponding slot, or the fact-id
     */
    public Value get(int i) throws JessException {
        if (i >= 0 && i < m_ptr)
            return m_v[i];
        else if (i == -1)
            return new FactIDValue(this);
        else
            throw new JessException("Fact.get",
                    "Slot index " + i + " out of bounds on this fact:",
                    toStringWithParens());
    }


  /**
   * Basic constructor. If the deftemplate is an unordered
   * deftemplate, default values are copied from the deftemplate.
   * @param template the deftemplate to use
   * @exception JessException if anything goes wrong
   */

    public Fact(Deftemplate template) throws JessException {
        m_deft = template;
        createNewFact();
        m_time = 0;
        m_icon = this;
    }

  /**
   * Basic constructor. If name is not a known deftemplate, an implied
   * ordered deftemplate is created. If it is a known unordered
   * deftemplate, default values are copied from the deftemplate.<P/>
   *
   * After you create a Fact, you can populate its slots using the
   * setSlotValue() method.
   * @param name the head or name of the fact
   * @param engine the engine in which to find the deftemplate
   * @exception JessException if anything goes wrong
   */

    public Fact(String name, Rete engine) throws JessException {
        if (name.equals("not") ||
            name.equals("test") ||
            name.equals("explicit") ||
            name.indexOf(' ') != -1 ||
            name.indexOf('(') != -1)
            throw new JessException("Fact.Fact",
                                    "Illegal fact name:", name);

        m_deft = engine.createDeftemplate(name);
        createNewFact();
        m_time = engine.getTime();
        m_icon = this;
    }

    /**
     * Create a Fact from another Fact. No default values are filled
     * in; the Fact is assumed to already be complete.
     * @param f another Fact object to copy
     * @exception JessException if anything goes wrong
     */
    public Fact(Fact f) throws JessException {
        m_name = f.m_name;
        m_deft = f.m_deft;
        setLength(f.size());
        for (int i=0; i<size(); i++)
            set(f.get(i), i);
        m_time = f.m_time;
        m_id = f.m_id;
        m_icon = this;
    }


    /**
     * Make a copy of this fact
     * @return the copy
     */
    public Object clone() {
        try {
            // TODO This is actually wrong; clone() methods
            // <i>must</i> use super.clone() or odd things can happen
            // in a multi-classloader environment.
            return new Fact(this);
        } catch (JessException re) {
            // can't happen
            return null;
        }
    }

    private void createNewFact() throws JessException {
        int size = m_deft.getNSlots();
        setLength(size);
        m_name = m_deft.getName();
        m_shadow = NO;

        for (int i=0; i<size; i++)
            set(m_deft.getSlotDefault(i), i);
    }

    private int findSlot(String slotname) throws JessException {
        int index = m_deft.getSlotIndex(slotname);
        if (index == -1)
            throw new JessException("Fact.findSlot",
                                    "No slot " + slotname + " in deftemplate ",
                                    m_deft.getName());
        return index;
    }

  /**
   * Return the value from the named slot.
   * @param slotname the name of a slot in this fact
   * @exception JessException if anything goes wrong
   * @return the value from the named slot
   */
    public final Value getSlotValue(String slotname) throws JessException {
        return get(findSlot(slotname));
    }

    /**
     * Set the value in the named slot. If the slot is a multislot,
     * and the value is not a list, then a list will be created to
     * hold the single item. This method should never be called on a
     * Fact object that's been asserted into working memory, or the
     * behavior is undefined (and generally bad;) use Rete.modify()
     * instead.
     *
     * @param slotname the name of the slot
     * @param value the new value for the slot
     * @exception JessException if anything goes wrong
     */
    public final void setSlotValue(String slotname, Value value)
        throws JessException {
        int slot = findSlot(slotname);
        if (m_deft.getSlotType(slot) == RU.MULTISLOT)
            if (value.type() != RU.LIST)
                value = new Value(new ValueVector().add(value), RU.LIST);
        set(value, slot);
    }

    /**
     * Clone this fact and expand any variable references in the clone; evaluate dynamic defaults too.
     * @exception JessException If anything goes wrong.
     * @return The new fact.
     */

    Fact expand(Context context) throws JessException {
        Fact fact = (Fact) clone();
        fact.expandInPlace(context);
        return fact;
    }

    void expandInPlace(Context context) throws JessException {
            for (int j=0; j<size(); j++){
                Value current = get(j).resolveValue(context);
                if (current.type() == RU.LIST) {
                    ValueVector vv = new ValueVector();
                    ValueVector list = current.listValue(context);
                    for (int k=0; k<list.size(); k++) {
                        Value listItem = list.get(k).resolveValue(context);
                        if (listItem.type() == RU.LIST) {
                            ValueVector sublist = listItem.listValue(context);
                            for (int m=0; m<sublist.size(); m++)
                                vv.add(sublist.get(m).resolveValue(context));

                        } else
                            vv.add(listItem);
                    }
                    if (getDeftemplate().getSlotType(j) == RU.SLOT)
                       current = vv.get(0);
                    else
                       current = new Value(vv, RU.LIST);
                }
                set(current, j);
            }
            setExpanded();
        }



    ListRenderer toList() {
        try {
            ListRenderer l = new ListRenderer(m_name);

            int nslots = size();
            // Make "Ordered" facts look ordered
            if (nslots == 1 &&
                m_deft.getSlotName(0).equals(RU.DEFAULT_SLOT_NAME)) {
                if (get(0).type() != RU.LIST) {
                    l.add(get(0));
                    return l;
                } else if (get(0).listValue(null).size() == 0)
                    return l;
                else {
                    // Omit slot name and parens
                    l.add(get(0));
                    return l;
                }
            }

            for (int i=0; i< nslots; i++) {
                l.add(new ListRenderer(m_deft.getSlotName(i), get(i)));
            }
            return l;
        } catch (JessException re) {
            return new ListRenderer(re.toString());
        }
    }
    /**
     * Pretty-print this fact into a String. Should always be a
     * parseable fact, except when a slot holds a Java object.
     * @return the pretty-printed String
     */

    public String toString() {
        return toList().toString();
    }

    /**
     * Pretty-print this fact into a String. Should always be a
     * parseable fact, except when a slot holds a Java object.
     * @return the pretty-printed String
     */

    public String toStringWithParens() {
        return toList().toString();
    }

    /**
     * Compare this Fact to another Fact to determine their
     * equality. Two facts are equal if they are the same object, or
     * they have the same name and their contents are the same.
     * @param o a Fact to compare to
     * @return true if the two Facts are equal
     */

    public boolean equals(Object o) {
        if (o == this)
            return true;

        else if (! (o instanceof Fact))
            return false;

        Fact f = (Fact) o;
        if (!m_name.equals(f.m_name))
            return false;

        return super.equals(o);

    }

    /**
     * Return a hash code for this fact based on its name and the
     * contents of all of its slots.
     * @return the hash code
     */

    public int hashCode() {
        int code = m_name.hashCode();
        for (int i=0; i<size(); ++i)
            code = 31 * code + m_v[i].hashCode();
        return code;
    }

    /**
     * Return the String "fact".
     * @return the String "fact"
     */

    public final String getConstructType() {
        return "fact";
    }

    /**
     * Returns the documentation string for this fact's deftemplate
     * @return a description of the template for this fact
     */
    public final String getDocstring() {
        return m_deft.getDocstring();
    }

    public Object accept(Visitor v) {
        return v.visitFact(this);
    }

    /**
     * Throws an informative exception if
     * slot constraints defined in this fact's template are violated by the slot values.
     * @throws JessException to indicate violated constraints
     */
    public void checkConstraints() throws JessException {
        Deftemplate deft = m_deft;
        for (int i=0; i<deft.getNSlots(); ++i) {
            String name = m_deft.getSlotName(i);
            if (!m_deft.isAllowedValue(name, m_v[i]))
                throw new JessException("Fact.checkConstraints", "Invalue value in slot " + name, m_v[i].toString());
        }

    }

    boolean isExpanded() {
        return m_expanded;
    }

    void setExpanded() {
        m_expanded = true;
    }
}









