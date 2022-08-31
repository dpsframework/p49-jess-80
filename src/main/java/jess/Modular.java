package jess;

/**
 * <p>The Modular interface represents something that has a name and
 * belongs to a module. This includes rules, queries and templates,
 * among other things.
 * </P>
 * (C) 2013 Sandia Corporation<br>
 */

public interface Modular extends Named {
    /**
     * Returns the name of the module this construct belongs to.
     * @return a module name
     */
    public String getModule();
}
