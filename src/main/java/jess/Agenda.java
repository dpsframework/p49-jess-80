package jess;

import java.io.Serializable;
import java.util.*;

/**
 * An Agenda represents the ordered lists of activated rules for all
 * modules defined in a single Rete engine.
 * <p/>
 * (C) 2007 Sandia National Laboratories<br>
 */

class Agenda implements Serializable {

    /** @noinspection RedundantStringConstructorCall*/
    private final Object m_activationSemaphore = new String("ACTIVATION LOCK");
    private volatile boolean m_halt = false;
    private int m_evalSalience = Rete.INSTALL;
    private HashMap m_moduleAgendas = new HashMap();
    private Strategy m_strategy = new depth();
    private final Stack m_focusStack = new Stack();
    private volatile Activation m_thisActivation;
    private volatile Thread m_runThread;

    void setEvalSalience(int method) throws JessException {
        if (method < Rete.INSTALL || method > Rete.EVERY_TIME)
            throw new JessException("Agenda.setEvalSalience",
                    "Invalid value", method);
        m_evalSalience = method;
    }

    int getEvalSalience() {
        return m_evalSalience;
    }

    void reset(Rete engine) throws JessException {
        for (Iterator modules = m_moduleAgendas.values().iterator(); modules.hasNext();) {
            ModuleAgenda module = (ModuleAgenda) modules.next();
            module.reset();
        }
        synchronized (m_focusStack) {
            m_focusStack.clear();
            m_focusStack.push(Defmodule.MAIN);
        }
        engine.broadcastEvent(JessEvent.FOCUS, Defmodule.MAIN, engine.getGlobalContext());
    }

    void clear(){
        m_moduleAgendas.clear();
        m_strategy = new depth();
        m_focusStack.clear();
        synchronized (m_activationSemaphore) {
            m_halt = false;
            m_activationSemaphore.notifyAll();
        }
    }

    Object getActivationSemaphore() {
        return m_activationSemaphore;
    }

    HeapPriorityQueue getQueue(Rete engine, Object module) throws JessException {
        verifyModule(engine, module);
        ModuleAgenda ma = (ModuleAgenda) m_moduleAgendas.get(module);
        ma.confirmStrategy(engine, m_strategy);
        return ma.getQueue();
    }

    Activation getNextActivation(Rete engine) throws JessException {
        synchronized (m_activationSemaphore) {
            synchronized (m_focusStack) {
                if (m_focusStack.empty())
                    return getQueue(engine, Defmodule.MAIN).pop();

                Context context = engine.getGlobalContext();
                while (!m_focusStack.empty()) {
                    HeapPriorityQueue q = getQueue(engine, m_focusStack.peek());
                    Activation a = q.pop();
                    if (a != null)
                        return a;
                    else {
                        Object oldFocus = m_focusStack.pop();
                        engine.broadcastEvent(JessEvent.FOCUS | JessEvent.REMOVED, oldFocus, context);
                        if (!getFocus().equals(oldFocus))
                            engine.broadcastEvent(JessEvent.FOCUS, getFocus(), context);
                    }
                }
                return getQueue(engine, Defmodule.MAIN).pop();
            }
        }
    }

    Activation peekNextActivation(Rete engine) throws JessException {
        synchronized (m_activationSemaphore) {
            synchronized (m_focusStack) {
                if (m_focusStack.empty())
                    return getQueue(engine, Defmodule.MAIN).peek();

                Context context = engine.getGlobalContext();
                while (!m_focusStack.empty()) {
                    HeapPriorityQueue q = getQueue(engine, m_focusStack.peek());
                    Activation a = q.peek();
                    if (a != null)
                        return a;
                    else {
                        Object oldFocus = m_focusStack.pop();
                        engine.broadcastEvent(JessEvent.FOCUS | JessEvent.REMOVED,
                                oldFocus, null);
                        if (!getFocus().equals(oldFocus))
                            engine.broadcastEvent(JessEvent.FOCUS, getFocus(), context);
                    }
                }
                return getQueue(engine, Defmodule.MAIN).peek();
            }
        }
    }

    boolean hasActivations(Rete engine) throws JessException {
        return !getQueue(engine, engine.getCurrentModule()).isEmpty();
    }

    boolean hasActivations(Rete engine, String moduleName) throws JessException {
        return !getQueue(engine, moduleName).isEmpty(); 
    }


    Iterator listActivationsInCurrentModule(Rete engine) throws JessException {
        return getQueue(engine, engine.getCurrentModule()).iterator(engine);
    }

    Iterator listActivations(Rete engine, String moduleName) throws JessException {
        return getQueue(engine, moduleName).iterator(engine);
    }

    private HashSet m_toAdd = new HashSet();
    private HashSet m_toRemove = new HashSet();

    void addActivation(Activation a) {
        synchronized (m_activationSemaphore) {
            m_toAdd.add(a);
        }
    }

    void removeActivation(Activation a) {
        synchronized (m_activationSemaphore) {
            if (a.getIndex() == -1) {
                m_toAdd.remove(a);
            } else {
                m_toRemove.add(a);
            }
        }
    }

    void commitActivations(Rete engine) throws JessException {
        synchronized (m_activationSemaphore) {
            if (m_toAdd.size() == 0 && m_toRemove.size() == 0)
                return;

            try {
                for (Iterator it = m_toRemove.iterator(); it.hasNext();) {
                    Activation a = (Activation) it.next();
                    if (a.getIndex() > -1)
                        getQueue(engine, a.getModule()).remove(a);
                }

                for (Iterator it = m_toAdd.iterator(); it.hasNext();) {
                    Activation a = (Activation) it.next();

                    if (m_evalSalience != Rete.INSTALL)
                        a.evalSalience(engine);

                    getQueue(engine, a.getModule()).push(a);

                    if (a.getAutoFocus(engine))
                        setFocus(a.getModule(), engine);

                }

            } finally {
                if (m_toAdd.size() > 0) {
                    m_activationSemaphore.notifyAll();
                }

                m_toAdd.clear();
                m_toRemove.clear();
            }
        }
    }

    void waitForActivations(Rete engine) {
        try {
            synchronized (m_activationSemaphore) {
                while (getQueue(engine, getFocus()).isEmpty())
                    m_activationSemaphore.wait();
            }
        } catch (InterruptedException ie) { /* FALL THROUGH */
        } catch (JessException je) { /* FALL THROUGH */            
        }
    }

    Strategy getStrategy() {
        return m_strategy;
    }

    String setStrategy(Strategy s)  {
        String rv = m_strategy.getName();
        m_strategy = s;
        return rv;
    }

    void halt() {
        synchronized (m_activationSemaphore) {
            m_halt = true;
            m_activationSemaphore.notifyAll();
        }
    }

    public boolean isHalted() {
        return m_halt;
    }

    int runUntilHalt(Rete engine, Context context) throws JessException {
        int count = 0;
        do {
            count += run(engine, context);
            synchronized (m_activationSemaphore) {
                if (m_halt)
                    break;
                waitForActivations(engine);
                if (m_halt)
                    break;
            }

        } while (true);

        return count;
    }

    synchronized int run(Rete r, Context context) throws JessException {
        int i = 0, j;
        // ###
        do {
            j = run(Integer.MAX_VALUE, r, context);
            i += j;
            synchronized (m_activationSemaphore) {
                if (m_halt)
                    break;
            }

        } while (j > 0);
        return i;
    }

    synchronized int run(int max, Rete engine, Context context) throws JessException {
        int n = 0;
        m_halt = false;
        Activation a;
        // ###

        while (n < max && (a = getNextActivation(engine)) != null) {
            a.setSequenceNumber(++n);
            engine.broadcastEvent(JessEvent.DEFRULE_FIRED, a, context);
            try {
                engine.aboutToFire(a);
                m_thisActivation = a;
                m_runThread = Thread.currentThread();
                a.fire(engine, context);
            } finally {
                m_thisActivation = null;
                m_runThread = null;
                engine.justFired(a);
            }

            if (m_evalSalience == Rete.EVERY_TIME) {
                setStrategy(getQueue(engine, getFocus()).getStrategy());
            }
            synchronized (m_activationSemaphore) {
                if (m_halt)
                    break;
            }
        }
        return n;
    }

    Iterator listFocusStack() {
        return m_focusStack.iterator();
    }

    void clearFocusStack() {
        m_focusStack.clear();
    }

    String getFocus() {
        synchronized (m_focusStack) {
            if (m_focusStack.empty())
                return Defmodule.MAIN;
            else
                return (String) m_focusStack.peek();
        }
    }

    String popFocus(Rete engine, String expect) throws JessException {
        synchronized (m_focusStack) {
            if (m_focusStack.empty())
                return Defmodule.MAIN;
            else if (expect != null && !expect.equals(getFocus()))
                return expect;
            else {
                String oldFocus = (String) m_focusStack.pop();
                Context context = engine.getGlobalContext();
                engine.broadcastEvent(JessEvent.FOCUS | JessEvent.REMOVED,
                        oldFocus, context);
                engine.broadcastEvent(JessEvent.FOCUS, getFocus(), context);
                return oldFocus;
            }
        }
    }

    void setFocus(String name, Rete engine) throws JessException {
        if (getFocus().equals(name))
            return;
        verifyModule(engine, name);
        Context context = engine.getGlobalContext();
        engine.broadcastEvent(JessEvent.FOCUS | JessEvent.REMOVED, getFocus(), context);
        engine.broadcastEvent(JessEvent.FOCUS, name, context);
        m_focusStack.push(name);
    }


    void verifyModule(Rete engine, Object moduleName) throws JessException {
        String name = moduleName.toString();
        engine.verifyModule(name);
        if (m_moduleAgendas.get(name) == null) {
            ModuleAgenda moduleAgenda = new ModuleAgenda(name, m_strategy);
            m_moduleAgendas.put(name, moduleAgenda);
        }
    }

    Activation getThisActivation() {
        return m_thisActivation;
    }

    Thread getRunThread() {
        return m_runThread;
    }

    void removeActivationsOfRule(Defrule rule, Rete engine) throws JessException {
        while (rule != null) {
            HeapPriorityQueue queue = getQueue(engine, rule.getModule());
            queue.removeActivationsOfRule(rule);
            rule = (Defrule) rule.getNext();
        }
    }
}
