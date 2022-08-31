package jess;

import java.util.EventObject;

/**
 * JessEvents are used by JessEvent sources (like the Rete class) to convey
 * information about interesting things that happen to registered event listeners.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 * @see JessListener
 * @see Rete#addJessListener
 * @see Rete#removeJessListener
 */
public class JessEvent extends EventObject {
    /**
     * A defrule has been added or removed
     * @noinspection PointlessBitwiseExpression
     */
    public static final int DEFRULE = 1 << 0;

    /**
     * A defrule has been fired
     */
    public static final int DEFRULE_FIRED = 1 << 1;

    /**
     * A defrule has been activated or deactivated
     */
    public static final int ACTIVATION = 1 << 2;

    /**
     * A deffacts has been added or removqed
     */
    public static final int DEFFACTS = 1 << 3;

    /**
     * A fact has been asserted or retracted
     */
    public static final int FACT = 1 << 4;

    /**
     * A definstance has been added or removed
     */
    public static final int DEFINSTANCE = 1 << 5;

    /**
     * A deftemplate has been added or removed
     */
    public static final int DEFTEMPLATE = 1 << 6;

    /**
     * A defclass has been added or removed
     */
    public static final int DEFCLASS = 1 << 7;

    /**
     * A defglobal has been added or removed
     */
    public static final int DEFGLOBAL = 1 << 8;

    /**
     * A userfunction has been added or removed
     */
    public static final int USERFUNCTION = 1 << 9;

    /**
     * A userpackage has been added or removed
     */
    public static final int USERPACKAGE = 1 << 10;

    /**
     * A (clear) has been executed
     */
    public static final int CLEAR = 1 << 11;

    /**
     * A (reset) has been executed
     */
    public static final int RESET = 1 << 12;

    /**
     * A (run) has been executed
     */
    public static final int RUN = 1 << 13;

    /**
     * A (run) has been executed
     */
    public static final int HALT = 1 << 14;

    /**
     * A Rete node has been reached by a token. Deliberately equal to RETE_TOKEN_LEFT.
     * @deprecated Use {@link JessEvent#RETE_TOKEN_LEFT} instead
     */
    public static final int RETE_TOKEN = 1 << 15;

    /**
     * A Rete node has been reached by a token, calltype left
     */
    public static final int RETE_TOKEN_LEFT = 1 << 15;

    /**
     * A Rete node has been reached by a token, calltype right
     */
    public static final int RETE_TOKEN_RIGHT = 1 << 16;

    /**
     * A userfunction has been called
     */
    public static final int USERFUNCTION_CALLED = 1 << 19;

    /**
     * The module focus has changed
     */
    public static final int FOCUS = 1 << 20;

    /**
     * A userfunction has returned from a call
     */

    public static final int USERFUNCTION_RETURNED = 1 << 21;

    /**
     * A defmodule has been added or removed
     */

    public static final int DEFMODULE = 1 << 22;

    /**
     * Added to other event-related flags to indicate modified fact
     */
    public static final int MODIFIED = 1 << 30;

    /**
     * Added to other event-related flags to indicate removal of construct
     */
    public static final int REMOVED = 1 << 31;

    Object m_obj;
    int m_type;
    int m_tag = -1;
    private Context m_context;

    /**
     * Construct a JessEvent containing the given information.
     *
     * @param theSource the object (usually an instance of Rete) generating the event.
     * @param type   one of the manifest constants in this class.
     * @param obj    data relevant to the specific type of this event.
     * @param context    current execution context of event
     */
    public JessEvent(Object theSource, int type, Object obj, Context context) {
        super(theSource);
        m_type = type;
        m_obj = obj;
        m_context = context;
    }


    /**
     * Construct a JessEvent containing the given information.
     *
     * @param theSource the object (often an instance of jess.Rete) generating the event
     * @param type   one of the manifest constants in this class
     * @param tag    Rete network tag
     * @param obj    data relevant to the specific type of this event
     * @param context    current execution context of event; can be null
     */
    JessEvent(Object theSource, int type, int tag, Object obj, Context context) {
        super(theSource);
        m_type = type;
        m_obj = obj;
        m_tag = tag;
        m_context = context;
    }


    /**
     * Gets the type of this event. The type should be one of the manifest constants in
     * this class.
     *
     * @return the event's type.
     */
    public int getType() {
        return m_type;
    }

    /**
     * Gets the tag of this event. The tag is a Rete network token tag. It is only meaningful for
     * some event types. It defaults to -1.
     *
     * @return the event's tag.
     */

    public int getTag() {
        return m_tag;
    }

    /**
     * Gets any optional data associated with this event. The type of
     * this data depends on the type of the event object. In general, it
     * is an instance of the type of object the event refers to; for example,
     * for {@link #DEFRULE_FIRED} events it is a {@link Defrule}.
     <p/>
     *
     * @return the optional data.
     * @see JessEvent#getType
     */
    public Object getObject() {
        return m_obj;
    }

    /**
     * Gets the execution context associated with the event. May be null
     * if not known. Should never be null for debug events.
     *
     * @return the execution context when the event occurred
     * @see JessEvent#getType
     */
    public Context getContext() {
        return m_context;
    }

    /**
     * Return a string suitable for debugging.
     *
     * @return Something like [JessEvent: a fact was asserted].
     */

    public String toString() {
        return "[JessEvent: " + getEventName(m_type) + "]";
    }

    /**
     * Displays a name for the numeric type of a JessEvent
     */

    public static String getEventName(int type) {
        switch(type) {
        case DEFRULE:
                return "a rule was added";
        case DEFRULE | REMOVED:
                return "a rule was removed";
        case DEFRULE_FIRED:
                return "a rule was fired";
        case ACTIVATION:
                return "a rule was activated";
        case ACTIVATION | REMOVED:
                return "a rule was deactivated";
        case DEFFACTS:
                return "a deffacts was added";
        case FACT:
                return "a fact was asserted";
        case FACT | MODIFIED:
                return "a fact was modified";
        case FACT | REMOVED:
                return "a fact was retracted";
        case DEFINSTANCE:
                return "an instance was added";
        case DEFINSTANCE |  REMOVED:
                return "an instance was removed";
        case DEFTEMPLATE:
                return "a template was added";
        case DEFCLASS:
                return "a class was defined";
        case DEFGLOBAL:
                return "a global was defined";
        case USERFUNCTION:
                return "a user function was added";
        case USERFUNCTION | REMOVED:
                return "a user function was removed";
        case USERPACKAGE:
                return "a user package was added";
        case CLEAR:
                return "the engine was cleared";
        case RESET:
                return "the engine was reset";
        case RUN:
                return "the engine was run";
        case HALT:
                return "the engine was halted";
        case RETE_TOKEN_LEFT:
                return "a left token in the Rete network";
        case RETE_TOKEN_RIGHT:
                return "a right token in the Rete network";
        case USERFUNCTION_CALLED:
                return "a userfunction was called";
        case USERFUNCTION_RETURNED:
                return "a call to a userfunction has returned";
        case FOCUS:
                return "a module got the focus";
        case FOCUS | REMOVED:
                return "a module lost the focus";
        default:
                return "unknown event type " + type;
        }
    }
}
