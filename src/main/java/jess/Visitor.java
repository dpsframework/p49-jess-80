package jess;

/**
 * Implementation of the standard Visitor pattern. Lets you, for
 * example, print out complex nested structures without putting the
 * printing code in the structures themselves. All Jess constructs,
 * and some other classes, are visitable.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public interface Visitor {

    Object visitDeffacts(Deffacts d);

    Object visitDeftemplate(Deftemplate d);

    Object visitDeffunction(Deffunction d);

    Object visitDefglobal(Defglobal d);

    Object visitDefrule(Defrule d);

    Object visitDefquery(Defquery d);

    Object visitPattern(Pattern p);

    Object visitGroup(Group p);

    Object visitTest1(Test1 t);

    Object visitAccumulate(Accumulate accumulate);

    Object visitDefmodule(Defmodule defmodule);

    Object visitFuncall(Funcall funcall);

    Object visitFact(Fact f);
}
