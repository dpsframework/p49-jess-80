package jess;

import java.util.*;

/**
 * Either a pattern, or a group of patterns; Jess's internal, more complex view of
 * a ConditionalElement.
 */

interface ConditionalElementX extends ConditionalElement, Cloneable {

    void setBoundName(String name) throws JessException;
    void setNegated() throws JessException;
    void setExplicit() throws JessException;
    void setLogical() throws JessException;

    boolean getLogical();
    boolean getNegated();
    boolean getBackwardChaining();
    ConditionalElementX getConditionalElementX(int i);
    int getPatternCount();
    boolean isBackwardChainingTrigger();

    ConditionalElementX canonicalize() throws JessException;
    void addToGroup(Group g) throws JessException;
    Object clone();


    void findVariableDefinitions(int startIndex, Map bindingsSoFar, Map newBindings)
            throws JessException;
    void addDirectlyMatchedVariables(Set map) throws JessException;
    int renameUnmentionedVariables(Set set, Map subs, int sequenceNumber, String groupPrefix)
        throws JessException;

    void recordTestedSlots(Set testedSlots) throws JessException;

    void transformOrConjunctionsIntoOrFuncalls(int startIndex, Map bindings, Rete engine) throws JessException;
}
