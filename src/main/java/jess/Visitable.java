package jess;

/**
 * Implementation of the standard Visitor pattern. Lets you, for
 * example, print out complex nested structures without putting the
 * printing code in the structures themselves.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public interface Visitable {

    /**
     * A proper accept() implementation should call one of the visitXXX() methods on the Visitor.
     * @param v a visitor to invoke
     * @return whatever the invoked Visitor method returns.
     */
    public Object accept(Visitor v);
}
