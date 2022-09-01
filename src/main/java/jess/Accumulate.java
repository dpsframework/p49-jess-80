package jess;

import java.util.*;

/**
 * A special ConditionalElement used to implement "accumulate"
 * conditional elements.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

@SuppressWarnings("serial")
public class Accumulate implements ConditionalElement, ConditionalElementX, Visitable {
    private Group m_group;
    private Value m_body;
    private Value m_initializer;
    private Value m_return;
    private String m_boundName;
    public static final String RESULT = "accumulate-result";

    public Accumulate() throws JessException {
        m_group = new Group(Group.AND);
    }

    public void setBoundName(String name) throws JessException {
        m_boundName = name;
    }

    public void setNegated() throws JessException {
        throw new JessException("Accumulate.setExplicit", "accumulate can't be enclosed by", "negated");
    }

    public void setExplicit() throws JessException {
        throw new JessException("Accumulate.setExplicit", "accumulate can't be enclosed by", "explicit");
    }

    public void setLogical() throws JessException {
        throw new JessException("Accumulate.setLogical", "accumulate can't be enclosed by", "logical");
    }

    public boolean getLogical() {
        return false;
    }

    public boolean getNegated() {
        return false;
    }

    public ConditionalElementX canonicalize() throws JessException {
        return this;
    }

    public boolean getBackwardChaining() {
        return false;
    }

    public void addToGroup(Group g) throws JessException {
        g.add((Accumulate) this.clone());
    }

    public void addDirectlyMatchedVariables(Set map) throws JessException {
        if (getBoundName() != null)
            map.add(getBoundName());
    }

    public int renameUnmentionedVariables(Set map, Map subs, int sequenceNumber, String groupPrefix)
            throws JessException {
        sequenceNumber = m_group.renameUnmentionedVariables(map, subs, sequenceNumber, groupPrefix);

        m_body = substExpression(m_body,  subs);
        m_initializer = substExpression(m_initializer,  subs);
        m_return = substExpression(m_return,  subs);
        return sequenceNumber;
    }

    public void recordTestedSlots(Set testedSlots) throws JessException {
        m_group.recordTestedSlots(testedSlots);
    }

    public void transformOrConjunctionsIntoOrFuncalls(int startIndex, Map bindings, Rete engine) throws JessException {
        m_group.transformOrConjunctionsIntoOrFuncalls(startIndex, bindings, engine);
    }

    private Value substExpression(Value value, Map subs) throws JessException {
        switch (value.type()) {
            case RU.VARIABLE:
            case RU.MULTIVARIABLE:
                String varname = value.variableValue(null);
                if (subs.get(varname) != null)
                    value = new Variable((String) subs.get(varname), RU.VARIABLE);
                break;
            case RU.FUNCALL:
                Funcall f  = value.funcallValue(null);
                for (int i = 0; i<f.size(); ++i)
                    f.set(substExpression(f.get(i), subs), i);
                break;
        }
        return value;
    }


    public boolean isBackwardChainingTrigger() {
        return false;
    }

    public int getPatternCount() {
        // TODO Is this right?
        return 1;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException silentlyIgnore) {
            return null;
        }
    }

    public String getName() {
        return Group.ACCUMULATE;
    }

    public String getBoundName() {
        return m_boundName;
    }

    public int getGroupSize() {
        // TODO is this right?
        return m_group.getGroupSize();
    }

    public ConditionalElementX getConditionalElementX(int i) {
        return m_group.getConditionalElementX(i);
    }

    public boolean isGroup() {
        return true;
    }

    public void add(ConditionalElementX p) throws JessException {
        m_group.add(p);
    }

    public ConditionalElement getConditionalElement(int i) {
        return getConditionalElementX(i);
    }

    public void setBody(Value s) {
        m_body = s;
    }

    public Value getBody() {
        return m_body;
    }

    public Value getInitializer() {
        return m_initializer;
    }

    public Value getReturn() {
        return m_return;
    }

    public void setInitializer(Value initializer) {
        m_initializer = initializer;
    }

    public void setReturn(Value aReturn) {
        m_return = aReturn;
    }

    public Object accept(Visitor v) {
        return v.visitAccumulate(this);
    }

    private void setupBindingValue(Map bindingsSoFar, Map newBindings) throws JessException {
        m_return = bindValue(bindingsSoFar, m_return, newBindings);
        m_body = bindValue(bindingsSoFar, m_body, newBindings);
        m_initializer = bindValue(bindingsSoFar, m_initializer, newBindings);

    }

    private Value bindValue(Map bindingsSoFar, Value v, Map newBindings) throws JessException {
        if (v.type() == RU.VARIABLE || v.type() == RU.MULTIVARIABLE) {
            String name = v.variableValue(null);
            BindingValue binding = (BindingValue) bindingsSoFar.get(name);
            if (binding == null)
                binding = (BindingValue) newBindings.get(name);
            if (binding == null) {
                return v;
            }
            return new BindingValue(binding);
        } else if (v.type() == RU.FUNCALL) {
            Funcall f = v.funcallValue(null);
            for (int i=0; i<f.size(); ++i)
                f.set(bindValue(bindingsSoFar, f.get(i), newBindings), i);
            return v;
        } else
            return v;
    }

    public void findVariableDefinitions(int startIndex, Map bindingsSoFar, Map newBindings)
            throws JessException {

        if (m_boundName != null) {
            BindingValue value = new BindingValue(m_boundName, this, startIndex, 0, -1, RU.NONE);
            newBindings.put(m_boundName, value);
        }

        m_group.findVariableDefinitions(startIndex, bindingsSoFar, newBindings);
        setupBindingValue(bindingsSoFar, newBindings);
    }
}
