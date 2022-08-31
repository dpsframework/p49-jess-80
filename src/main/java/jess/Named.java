package jess;

/**
 * <p>A Named thing has a name and a construct type. All of the Jess
 * construct classes implement this interface.
 * </P>
 * (C) 2013 Sandia Corporation<br>
 */

public interface Named {
    /**
     * Return the name of this construct.
     * @return the name of the construct
     */
    String getName();

    /**
     * Return the type of construct this object is; for example, "defrule", "deftemplate", etc.
     * @return the construct type
     */
    String getConstructType();

    /**
     * Return a documentation comment for this object, or null if none is defined
     * @return the docstring, or null
     */

    String getDocstring();
}
