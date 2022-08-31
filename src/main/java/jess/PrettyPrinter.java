package jess;

import java.util.Iterator;

/**
 * <p>A PrettyPrinter knows how to format various Jess constructs for
 * display. Used in the implementation of the "ppdefrule" command and
 * such.</p>
 * <p/>
 * <p>This class can produce a formatted
 * rendering of many Jess objects, including {@link Defrule}s,
 * {@link Deffunction}s {@link Defquery}s, etc -- anything
 * that implements the {@link Visitable} interface.<p/>
 * <p/>
 * <tt>PrettyPrinter</tt> is very simple to use: you just create
 * an instance, passing the object to be rendered as a constructor
 * argument, and then call {@link #toString} to get the formatted
 * result.
 * <p/>
 * <pre>
 * Rete r = new Rete();
 * r.eval("(defrule myrule (A) => (printout t \"A\" crlf))");
 * Defrule dr = (Defrule) r.findDefrule("myrule");
 * System.out.println(new PrettyPrinter(dr));
 * </pre>
 * <p/>
 * The above code produces the following output:
 * <pre>
 * (defrule MAIN::myrule
 * (MAIN::A)
 * =>
 * (printout t "A" crlf))
 * </pre>
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 *
 * @see Visitable
 */

public class PrettyPrinter implements Visitor {

    private Visitable m_visitable;
    private boolean m_inTestCE = false;

    /**
     * Construct a PrettyPrinter for the given object.
     *
     * @param v the object to be rendered
     */
    public PrettyPrinter(Visitable v) {
        m_visitable = v;
    }

    public Object visitDeffacts(Deffacts facts) {
        ListRenderer l = new ListRenderer("deffacts", facts.getName());

        if (facts.getDocstring() != null &&
                facts.getDocstring().length() > 0)
            l.addQuoted(facts.getDocstring());


        for (int i = 0; i < facts.getNFacts(); ++i) {
            l.newLine();
            l.add(facts.getFact(i));
        }

        return l.toString();
    }

    public Object visitDeftemplate(Deftemplate template) {
        ListRenderer list = new ListRenderer("deftemplate", template.getName());
        list.indent("  ");
        if (template.getParent() != null &&
                template.getParent() != Deftemplate.getRootTemplate()) {
            list.add("extends");
            list.add(template.getParent().getName());
        }

        if (template.getDocstring() != null &&
                template.getDocstring().length() > 0) {
            list.newLine();
            list.addQuoted(template.getDocstring());
        }

        ListRenderer declarations = new ListRenderer("declare");


        if (template.isSlotSpecific()) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer autoFocus = new ListRenderer("slot-specific");
            autoFocus.add("TRUE");
            declarations.add(autoFocus);
        }

        if (template.getBackwardChaining()) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer reactive = new ListRenderer("backchain-reactive");
            reactive.add("TRUE");
            declarations.add(reactive);
        }


        try {
            if (template.isShadowTemplate()) {
                indentIfNotFirstDeclaration(declarations);
                ListRenderer fromClass = new ListRenderer("from-class");
                fromClass.add(template.getShadowClassName());
                declarations.add(fromClass);
                if (template.includesVariables()) {
                    ListRenderer includesVariables = new ListRenderer("include-variables");
                    includesVariables.add("TRUE");
                    declarations.add(includesVariables);
                }
            }
        } catch (JessException ex) {
            declarations.add(ex);
        }

        if (declarations.hasContent()) {
            list.newLine();
            list.add(declarations);
        }

        int start = firstSlotToPrint(template);
        for (int i = start; i < template.getNSlots(); ++i) {
            try {
                Deftemplate.Slot theSlot = template.getSlot(i);
                Value name = theSlot.getName();
                ListRenderer slot =
                        new ListRenderer(name.type() == RU.SLOT ? "slot" : "multislot", name);
                if (shouldIncludeSlotDefault(template)) {
                    Value dflt = theSlot.getDefault();
                    if (!valueIsNil(dflt)) {
                        String kind = getDefaultType(dflt);
                        slot.add(new ListRenderer(kind, dflt));
                    }

                    Value type = theSlot.getDataType();
                    if (type.intValue(null) != -1)
                        slot.add(new ListRenderer("type", Deftemplate.getSlotTypeName(type)));
                }

                list.newLine();
                list.add(slot);

            } catch (JessException re) {
                list.add(re.toString());
                break;
            }
        }

        return list.toString();
    }

    private String getDefaultType(Value val) throws JessException {
        int type = val.type();
        if (type == RU.FUNCALL) {
            return "default-dynamic";
        } else if (type == RU.LIST) {
            ValueVector vec = val.listValue(null);
            for (int j = 0; j < vec.size(); ++j) {
                if (vec.get(j).type() == RU.FUNCALL) {
                    return "default-dynamic";
                }
            }
        }
        return "default";
    }

    private boolean valueIsNil(Value val) {
        int type = val.type();
        if (type == RU.SYMBOL && val.equals(Funcall.NIL))
            return true;
        else if (type == RU.LIST && val.equals(Funcall.NILLIST))
            return true;
        else
            return false;
    }

    private boolean shouldIncludeSlotDefault(Deftemplate template) {
        if (template.isShadowTemplate())
            return false;
        else if (template.isBackwardChainingTrigger() && template.getSlotIndex("OBJECT") != -1)
            return false;
        else
            return true;

    }

    private int firstSlotToPrint(Deftemplate template) {
        if (template.isShadowTemplate())
            return Integer.MAX_VALUE;
        int start = 0;
        if (template.getParent() != null)
            start = template.getParent().getNSlots();
        return start;
    }

    public Object visitDefglobal(Defglobal global) {
        ListRenderer l = new ListRenderer("defglobal");
        l.add("?" + global.getName());
        l.add("=");
        l.add(global.getInitializationValue());
        return l.toString();
    }

    public Object visitDeffunction(Deffunction func) {
        ListRenderer l = new ListRenderer("deffunction", func.getName());
        ListRenderer args = new ListRenderer();
        for (Iterator it = func.getArguments(); it.hasNext();) {
            Deffunction.Argument a = (Deffunction.Argument) it.next();
            String prefix = a.m_type == RU.VARIABLE ? "?" : "$?";
            args.add(prefix + a.m_name);
        }
        l.add(args);

        if (func.getDocstring() != null && func.getDocstring().length() > 0)
            l.addQuoted(func.getDocstring());

        for (Iterator e = func.getActions(); e.hasNext();) {
            l.newLine();
            l.add(e.next());
        }
        return l.toString();
    }

    public Object visitDefrule(Defrule rule) {
        ListRenderer list = new ListRenderer("defrule");
        list.add(rule.getName());
        list.indent("  ");

        if (rule.m_docstring != null && rule.m_docstring.length() > 0) {
            list.newLine();
            list.addQuoted(rule.m_docstring);
        }

        ListRenderer declarations = new ListRenderer("declare");

        if (rule.hasNonDefaultSalience()) {
            ListRenderer salience = new ListRenderer("salience");
            salience.add(rule.getSalienceValue());
            declarations.add(salience);
        }

        if (rule.getAutoFocus()) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer autoFocus = new ListRenderer("auto-focus");
            autoFocus.add("TRUE");
            declarations.add(autoFocus);
        }

        addNodeIndexHashDeclaration(rule, declarations);

        if (rule.isNoLoop()) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer autoFocus = new ListRenderer("no-loop");
            autoFocus.add("TRUE");
            declarations.add(autoFocus);
        }

        if (declarations.hasContent()) {
            list.newLine();
            list.add(declarations);
        }

        if (rule.getNext() == null)
            doSimpleLHS(list, rule);
        else
            doOrLHS(list, rule);

        list.newLine();
        list.add("=>");
        for (int i = 0; i < rule.getNActions(); ++i) {
            list.newLine();
            list.add(rule.getAction(i).toString());
        }

        return list.toString();
    }

    private void indentIfNotFirstDeclaration(ListRenderer declarations) {
        if (declarations.hasContent()) {
            declarations.indent("           ");
            declarations.newLine();
        }
    }

    private void doOrLHS(ListRenderer list, HasLHS rule) {
        ListRenderer or = new ListRenderer(Group.OR);
        or.indent("    ");
        while (rule != null) {
            ListRenderer and = new ListRenderer(Group.AND);
            and.indent("      ");
            doSimpleLHS(and, rule);
            or.newLine();
            or.add(and);
            rule = rule.getNext();
        }
        list.newLine();
        list.add(or);
    }

    private void doSimpleLHS(ListRenderer list, HasLHS rule) {
        ConditionalElement ce = rule.getConditionalElements();
        for (int i = 0; i < ce.getGroupSize(); ++i) {
            ConditionalElement elem = ce.getConditionalElement(i);
            if (elem.getName().indexOf(Defquery.QUERY_TRIGGER) == -1) {
                list.newLine();
                list.add(((Visitable) elem).accept(this));
            }
        }
    }

    public Object visitGroup(Group g) {
        ListRenderer list = new ListRenderer(g.getName());
        for (int i = 0; i < g.getGroupSize(); ++i) {
            ConditionalElementX lhsc = g.getConditionalElementX(i);
            list.add(((Visitable) lhsc).accept(this));
            // TODO Cumulative indenting.
            // list.newLine();
        }
        if (g.getBoundName() != null && !g.getName().equals(Group.NOT))
            return '?' + g.getBoundName() + " <- " + list.toString();
        else
            return list.toString();
    }

    public Object visitPattern(Pattern p) {
        m_inTestCE = p.getName().equals("test");
        String name = contextualize(m_visitable, p.getName());
        ListRenderer list = new ListRenderer(name);
        Deftemplate dt = p.getDeftemplate();
        try {
            for (int i = 0; i < p.getNSlots(); ++i) {
                if (p.getNTests(i) != 0 || p.getSlotLength(i) > -1) {
                    ListRenderer slot;
                    if (dt.getSlotName(i).equals(RU.DEFAULT_SLOT_NAME))
                        slot = list;
                    else
                        slot = new ListRenderer(dt.getSlotName(i));

                    for (int k = -1; k <= p.getSlotLength(i); ++k) {
                        StringBuffer sb = new StringBuffer();
                        for (Iterator it = p.getTests(i); it.hasNext();) {
                            Test1 t = (Test1) it.next();
                            if (t.m_subIdx == k) {
                                if (sb.length() > 0)
                                    sb.append("&");
                                sb.append(t.accept(this));
                            }
                        }
                        if (sb.length() > 0)
                            slot.add(sb);
                    }
                    if (!dt.getSlotName(i).equals(RU.DEFAULT_SLOT_NAME))
                        list.add(slot);
                }
            }
        } catch (JessException je) {
            list.add(je.getDetail());
        }

        StringBuffer result;
        if (p.getBoundName() != null) {
            result = new StringBuffer('?' + p.getBoundName() + " <- " + list.toString());
        } else {
            result = new StringBuffer(list.toString());
        }

        try {
            Iterator it = p.getTests(RU.NO_SLOT);
            if (it.hasNext()) {
                ListRenderer group = new ListRenderer("and");
                group.indent("       ");
                group.add(result);
                while (it.hasNext()) {
                    list = new ListRenderer(Group.TEST);
                    Test1 test = (Test1) it.next();
                    list.add(test.m_slotValue.funcallValue(null));
                    group.newLine();
                    group.add(list);
                }
                return group.toString();
            }
        } catch (JessException ex) {
            ex.printStackTrace();
            result.append(ex.getDetail());
        }

        return result.toString();
    }

    static String contextualize(Visitable v, String name) {
        if (! (v instanceof HasLHS))
            return name;
        HasLHS rule = (HasLHS) v;
        String prefix = rule.getModule() + "::";
        if (name.startsWith(prefix))
            return name.substring(prefix.length());
        else
            return name;
    }

    public Object visitTest1(Test1 t) {
        StringBuffer sb = new StringBuffer();
        if (t.m_test == Test1.NEQ)
            sb.append("~");
        if (t.m_slotValue.type() == RU.FUNCALL && !m_inTestCE)
            sb.append(":");

        sb.append(t.m_slotValue);
        return sb.toString();
    }

    public Object visitAccumulate(Accumulate accumulate) {
        ListRenderer list = new ListRenderer(Group.ACCUMULATE);

        ConditionalElementX lhsc = accumulate.getConditionalElementX(0);
        list.add(accumulate.getInitializer());
        list.add(accumulate.getBody());
        list.add(accumulate.getReturn());
        list.add(((Visitable) lhsc).accept(this));
        if (accumulate.getBoundName() != null)
            return '?' + accumulate.getBoundName() + " <- " + list.toString();
        else
            return list.toString();
    }

    public Object visitDefmodule(Defmodule defmodule) {
        ListRenderer list = new ListRenderer("defmodule");
        list.add(defmodule.getName());
        if (defmodule.getDocstring() != null) {
            list.newLine();
            list.indent("  ");
            list.add(defmodule.getDocstring());
        }
        return list.toString();
    }

    public Object visitFuncall(Funcall funcall) {
        return funcall.toString();
    }

    public Object visitFact(Fact f) {
        return f.toString();
    }

    public Object visitDefquery(Defquery query) {
        ListRenderer list = new ListRenderer("defquery");
        list.add(query.getName());
        list.indent("  ");

        if (query.m_docstring != null && query.m_docstring.length() > 0) {
            list.newLine();
            list.addQuoted(query.m_docstring);
        }

        ListRenderer declarations = new ListRenderer("declare");

        if (query.getNVariables() > 0) {
            ListRenderer variables = new ListRenderer("variables");
            for (int i = 0; i < query.getNVariables(); ++i)
                variables.add(query.getQueryVariable(i));
            declarations.add(variables);
        }

        if (query.getMaxBackgroundRules() > 0) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer background = new ListRenderer("max-background-rules");
            background.add(String.valueOf(query.getMaxBackgroundRules()));
            declarations.add(background);
        }

        addNodeIndexHashDeclaration(query, declarations);

        if (declarations.hasContent()) {
            list.newLine();
            list.add(declarations);
        }

        if (query.getNext() == null)
            doSimpleLHS(list, query);
        else
            doOrLHS(list, query);

        return list.toString();
    }

    private void addNodeIndexHashDeclaration(HasLHS hasLHS, ListRenderer declarations) {
        if (hasLHS.getNodeIndexHash() > 0) {
            indentIfNotFirstDeclaration(declarations);
            ListRenderer background = new ListRenderer("node-index-hash");
            background.add(String.valueOf(hasLHS.getNodeIndexHash()));
            declarations.add(background);
        }
    }

    /**
     * Returns the rendered version of the constructor argument.
     *
     * @return the formatted String
     */
    public String toString() {
        return (String) m_visitable.accept(this);
    }

    static String pp(HasLHS lhs, ConditionalElement ce) {
        PrettyPrinter pp = new PrettyPrinter(lhs);
        if (ce instanceof Group)
            return pp.visitGroup((Group) ce).toString();
        else if (ce instanceof Pattern)
            return pp.visitPattern((Pattern) ce).toString();
        else if (ce instanceof Accumulate)
            return pp.visitAccumulate((Accumulate) ce).toString();
        else
            return ce.toString();
    }

}





