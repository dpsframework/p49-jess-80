package jess;

import jess.server.LineNumberRecord;

import java.io.Serializable;
import java.util.*;

/**
  <p>A Context represents a scope in which variables can be declared. It
  also holds a pointer to a Rete object in which code can be
  executed.</p>


 <p>
 You can use {@link #getVariable} and
 {@link #setVariable} to get and change the value of a variable from
 Java code, respectively.
 </p>
 <p>The function {@link #getEngine} gives any {@link Userfunction}
 access to the {@link Rete} object in which it is executing.
 When a <tt>Userfunction</tt> is called, a <tt>Context</tt>
 argument is passed in as the final argument. You should pass this
 <tt>jess.Context</tt> to any methods you call that expect a <tt>Context</tt> argument themselves.
 </p>
  (C) 2007 Sandia National Laboratories<BR>
 */
public class Context implements Serializable {
    private Map m_variables;
    private final Context m_parent;
    private boolean m_return;
    private Value m_retval;
    private transient Rete m_engine;
    private LogicalNode m_logicalSupportNode;
    private Fact m_slotSpecificModifiedFact;
    private String[] m_modifiedSlots;

    private Token m_token;
    private Fact m_fact;
    private static final String STACK = "%STACK";

    /**
     * If this context represents a join network node from a rule LHS,
     * this will return the left input of the node.
     * @return The Token
     */

    public final Token getToken() {
        if (m_token == null)
            if (m_parent != null)
                return m_parent.getToken();
        return m_token;
    }

    // get/setToken and get/setFact are deliberately not synchronized. Making them so
    // has a large negative performance impact, but the truth is that they're never used in a
    // cross-thread situation. Either they're set during a fire() call and used while
    // executing the rule RHS, or they're set in the Rete network and used during matching.
    final void setToken(Token t) { m_token = t; }

    /**
     * If this context represents a join network node from a rule LHS,
     * this will return the right input of the node.
     * @return The Fact object
     */

    public final Fact getFact() { return m_fact; }
    final void setFact(Fact f) { m_fact = f; }


    /** If this context represents the RHS of a rule which is firing, and
     * the LHS of the rule has provided logical support, this method will
     * return the LogicalNode that lends support.
     * @return The supporting node.
     */
    public synchronized final LogicalNode getLogicalSupportNode() {
        if (m_logicalSupportNode == null)
            if (m_parent != null)
                return m_parent.getLogicalSupportNode();
        return m_logicalSupportNode;
    }

    final void setLogicalSupportNode(LogicalNode node) {
        m_logicalSupportNode = node;
    }

    private boolean m_inAdvice;

    synchronized boolean getInAdvice() {return m_inAdvice;}
    synchronized void setInAdvice(boolean  v) {m_inAdvice = v;}

    synchronized void setEngine(Rete r) {
        m_engine = r;
    }

    /**
     * Create a new context subordinate to an existing one. The method
     * getEngine() will return c.getEngine(). Use push() instead of
     * calling this directly.
     * @param c The parent for the new context
     * @see #push
     * */
    public Context(Context c) {
        m_engine = c.m_engine;
        m_parent = c;
    }

    /**
     * @param engine The value to be returned from getEngine
     */
    Context(Rete engine) {
        m_engine = engine;
        m_parent = null;
    }

    /**
     * Create a new context subordinate to an existing one. The method
     * getEngine() will return the given Rete object.
     * @param c The parent for the new context
     * @param engine The value to be returned from getEngine
     */

    public Context(Context c, Rete engine) {
        m_engine = engine;
        m_parent = c;
    }

    /**
     * Make this context absolutely brand-new again.
     */
    synchronized void clear() {
        m_fact = null;
        m_token = null;
        m_inAdvice = false;
        m_logicalSupportNode = null;
        m_return = false;
        m_retval = null;
        m_variables = null;
        m_slotSpecificModifiedFact = null;
    }


    /**
     * Returns true if the return flag has been set in this
     * context. The Jess (return) function sets the return flag in the
     * local context.
     * @return The value of the return flag
     * */
    public synchronized final boolean returning() {
        return m_return;
    }

    /**
     * Set the return flag to true, and supply a value to be returned.
     * @param val The value that should be returned from this context
     * @return The argument
     */
    public synchronized final Value setReturnValue(Value val) {
        m_return = true;
        m_retval = val;
        return val;
    }

    /**
     * Get the value set via setReturnValue
     * @return The return value
     */
    public synchronized  final Value getReturnValue() {
        return m_retval;
    }

    /**
     * Clear the return flag and return value for this context.
     */
    public synchronized final void clearReturnValue() {
        m_return = false;
        m_retval = null;
    }

    private synchronized int nVariables() {
        if (m_variables == null)
            return 0;
        else
            return m_variables.size();
    }

    private synchronized Map getVariables() {
        if (m_variables == null)
            m_variables = Collections.synchronizedMap(new HashMap());

        return m_variables;
    }

    /**
     * Returns an iterator over the names of all the variables defined in this context.
     * @return the iterator
     */

    public Iterator getVariableNames() {
        Map variables = getVariables();
        return variables.keySet().iterator();
    }

    /**
     * Returns the Rete engine associated with this context.
     * @return The engine to use with this context
     */
    public final Rete getEngine() {
        return m_engine;
    }

    /**
     * Create and return a new context subordinate to this one.
     * @return The next context
     */
    public Context push() {
        return new Context(this);
    }

    /**
     * Pop this context off the execution stack.
     * If this context has no parent, just return this context. If it
     * has a parent, transfer the values of the return flag and the
     * return value to the parent, then return the parent.
     * @return The context as described
     */
    public synchronized Context pop() {
        if (m_parent != null) {
            synchronized (m_parent) {
                m_parent.m_return = m_return;
                m_parent.m_retval = m_retval;
                return m_parent;
            }
        }
        else
            return this;
    }

    /**
     * Return the parent of this context. The parent represents the stack frame "below" this one.
     *
     * @return the parent context, or null if none
     */

    public Context getParent() {
        return m_parent;
    }

    private synchronized Map findVariable(String key) {
        Context c = this;
        while (c != null) {
            Map ht = c.getVariables();
            Value v = (Value) ht.get(key);
            if (v != null)
                return ht;
            else
                c = c.m_parent;
        }
        return null;
    }

    synchronized void removeNonGlobals() {
        if (m_variables == null)
            return;

        Map ht = new HashMap(10);
        for (Iterator it = m_variables.keySet().iterator(); it.hasNext();) {
            String s = (String) it.next();
            if (s.startsWith("%") || m_engine.findDefglobal(s) != null)
                ht.put(s, m_variables.get(s));
        }
        m_variables = Collections.synchronizedMap(ht);
    }

    /**
     * Get the value of a variable
     * @param name The name of the variable with no leading '?' or '$'
     * characters
     * @exception JessException If the variable is undefined
     * */
    public Value getVariable(String name) throws JessException {
        if (name.indexOf(m_engine.getMemberChar()) > -1)
            return getDottedVariable(name);

        Map ht = findVariable(name);
        if (ht == null) {
            try {
                return getBindingVariable(name);
            } catch (JessException ex) {
                variableNotFound(name);
            }
        }
        return ((Value) ht.get(name)).resolveValue(this);
    }

    private void variableNotFound(String name) throws JessException {
        throw new JessException("Context.getVariable", "No such variable", name);
    }

    private Value getDottedVariable(String name) throws JessException {
        int index = name.indexOf(m_engine.getMemberChar());
        String target = name.substring(0, index);
        String property = name.substring(index+1);
        Map map = findVariable(target);
        if (map == null)
            variableNotFound(target);
        Value var = (Value) map.get(target);
        Object obj = var.javaObjectValue(this);
        if (obj instanceof Fact)
            return var.factValue(this).getSlotValue(property);
        else {
            // TODO Refactoring so that we can do this without building the Funcall
            Funcall funcall = new Funcall("get", m_engine);
            funcall.arg(obj);
            funcall.arg(property);
            return funcall.execute(this);
        }            
    }

    /**
     * Report whether a variable has a value in this context
     * @param name The name of the variable with no leading '?' or '$'
     * characters
     * */
    public boolean isVariableDefined(String name) {
        return findVariable(name) != null;
    }

    /**
     * Set a variable in this context to some type and value. Defglobals are
     * always set in the global context.
     * @param name Name of the variable
     * @param value The value of the variable
     */
    public synchronized void setVariable(String name, Value value) throws JessException {
        if (Defglobal.isADefglobalName(name)) {
            m_engine.getGlobalContext().getVariables().put(name, value);
            return;
        }
        Map ht = getVariables();
        ht.put(name, value);
    }


    /**
     * Set an existing variable in an ancestor of this context to a value;
     * if no existing variable, value is set in this context.
     * @param name Name of the variable
     * @param value The value of the variable
     */
    public void setExistingVariable(String name, Value value) throws JessException {
        Map ht = findVariable(name);
        if (ht == null)
            ht = getVariables();
        ht.put(name, value);
    }

    /**
     * Store stack information. Used by the JessDE debugger. Won't be useful to any other clients.
     * @param funcall a function call about to be invoked
     * @throws JessException if anything goes wrong
     */
    public synchronized void pushStackFrame(Funcall funcall) throws JessException {
        Stack stack = getStackData();
        LineNumberRecord record = Rete.lookupFunction(funcall);
        stack.push(new StackFrame(funcall, record));
    }

    /**
     * Returns some information about the runtime stack. Only works in debug mode. Used by
     * the JessDE implementation, but won't be useful to other clients.
     * @return information about the runtime stack
     * @throws JessException
     */
    public synchronized Stack getStackData() throws JessException {
        Value stackValue = (Value) getVariables().get(STACK);
        if (stackValue == null)
            getVariables().put(STACK, stackValue = new Value(new Stack()));
        return (Stack) stackValue.javaObjectValue(this);
    }

    /**
     * Remove stack information. Used by the JessDE debugger. Won't be useful to any other clients.
     * @param funcall a function call that has just been invoked
     * @throws JessException if anything goes wrong
     */
    public synchronized void popStackFrame(Funcall funcall) throws JessException {
        Stack stack = getStackData();
        if (stack.size() > 0)
            stack.pop();
    }

    /**
     * Returns a useful debug representation of the context.
     * @return A string with information about this context.
     */
    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[Context, ").append(nVariables()).append(" variables: ");
        for (Iterator it = getVariables().keySet().iterator(); it.hasNext(); ) {
            Object o = it.next();
            sb.append(o).append("=").append(m_variables.get(o)).append(";");
        }
        sb.append("]");
        return sb.toString();
    }

    synchronized Fact getSlotSpecificModifiedFact() {
        if (m_slotSpecificModifiedFact == null)
            if (m_parent != null)
                return m_parent.getSlotSpecificModifiedFact();
        return m_slotSpecificModifiedFact;
    }

    synchronized void setSlotSpecificModifiedFact(Fact slotSpecificModifiedFact) {
        m_slotSpecificModifiedFact = slotSpecificModifiedFact;
    }

    synchronized String[] getModifiedSlots() {
        if (m_modifiedSlots == null)
            if (m_parent != null)
                return m_parent.getModifiedSlots();
        return m_modifiedSlots;
    }

    synchronized void setModifiedSlots(String[] modifiedSlots) {
        m_modifiedSlots = modifiedSlots;
    }

    /**
     * Return the currently executing Funcall object. Only works in debug mode. Used by the JessDE implementation.
     * @return the Funcall being executed.
     * @throws JessException if anything goes wrong.
     */
    public synchronized Funcall getFuncall() throws JessException {
        Stack stack = getStackData();
        if (stack.size() > 0) {
            StackFrame data = (StackFrame) stack.peek();
            return data.getFuncall();
        }

        return null;
    }

    /**
     * Return information about the currently executing function. Only works in debug mode. Used by the JessDE implementation.
     * @return information about the currently executing function
     * @throws JessException if anything goes wrong
     */
    public synchronized LineNumberRecord getLineNumberRecord() throws JessException {
        Stack stack = getStackData();
        if (stack.size() > 0) {
            StackFrame data = (StackFrame) stack.peek();
            return data.getLineNumberRecord();
        }

        return null;
    }

    /**
     * Return the value represented by a variable on the LHS of a rule; works only during pattern matching. Used internally.
     * @param varName the name of a variable
     * @return the value of the variable
     * @throws JessException if anything goes wrong.
     */
    public synchronized Value getBindingVariable(String varName) throws JessException {
        Funcall funcall = getFuncall();
        if (funcall == null)
            throw new JessException("Context.getBindingVariable", "No current function", "");
        for (int i=1; i<funcall.size(); ++i) {
            Value v = funcall.get(i);
            if (v instanceof BindingValue) {
                BindingValue bindingValue = (BindingValue) v;
                if (bindingValue.getName().equals(varName))
                    return bindingValue.resolveValue(this);
            }
        }
        throw new JessException("Context.getBindingVariable", "No such variable", varName);
    }

    /**
     * Undefine any variable by the given name in this context or any parent context.
     * @param name the name of the variable to remove.
     */
    public synchronized void removeVariable(String name) {
        if (isVariableDefined(name)) {
            Map map = findVariable(name);
            map.remove(name);
        }



    }
}


