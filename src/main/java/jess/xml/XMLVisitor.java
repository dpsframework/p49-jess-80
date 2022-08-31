package jess.xml;

import jess.*;

import java.util.Iterator;

/**
 * <p>Renders Jess constructs as JessML code. You
 * should construct an XMLVisitor with a Jess object as a constructor
 * argument, then call toString() to get the JessML version. Any
 * Jess object that implements Visitable can be rendered this way. </p>
 * <p/>
 * <p>The class XMLPrinter is a convenient wrapper around this class that
 * can be used to translate an entire file of Jess code into XML.</p>
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 *
 * @see jess.Visitable
 */
public class XMLVisitor implements Visitor {
    private final Visitable m_visitable;

    /**
     * Create an XMLVisitor to render the given Visitable. When toString() is called on this object,
     * it will return an XML version of the argument.
     *
     * @param v a construct or other Visitable to render
     * @see jess.Visitor
     */
    public XMLVisitor(Visitable v) {
        m_visitable = v;
    }

    public Object visitDeffacts(Deffacts d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("facts");
        xml.textElementNoSpace("name", d.getName());
        possiblyEmitDocstring(xml, d);
        int count = d.getNFacts();
        for (int i = 0; i < count; ++i) {
            Fact fact = d.getFact(i);
            xml.append(visitFact(fact));
        }
        xml.closeTag("facts");
        return xml.toString();
    }

    private static void possiblyEmitDocstring(XMLWriter xml, Named named) {
        String docstring = named.getDocstring();
        if (docstring != null && docstring.length() > 0) {
            xml.textElementNoSpace("comment", named.getDocstring());
        }
    }

    public Object visitDeftemplate(Deftemplate d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("template");
        xml.textElementNoSpace("name", d.getName());
        if (d.getParent() != Deftemplate.getRootTemplate()) {
            xml.textElementNoSpace("extends", d.getParent().getName());
        }
        possiblyEmitDocstring(xml, d);

        try {
            if (hasTemplateDeclarables(d)) {
                xml.openTag("properties");
                if (d.isShadowTemplate()) {
                    emitProperty(xml, "from-class", newSymbolValue(d.getShadowClassName()));
                    if (d.includesVariables())
                        emitProperty(xml, "include-variables", Funcall.TRUE);
                }
                if (d.isSlotSpecific()) {
                    emitProperty(xml, "slot-specific", Funcall.TRUE);
                }
                if (d.getBackwardChaining()) {
                    emitProperty(xml, "backchain-reactive", Funcall.TRUE);
                }
                xml.closeTag("properties");
            }

        } catch (JessException e) {
            return e.toString();
        }

        if (!d.isShadowTemplate()) {
            for (int i = 0; i < d.getNSlots(); ++i) {
                try {
                    String name = d.getSlotName(i);
                    Value v = d.getSlotDefault(i);
                    if (d.getSlotType(i) == RU.MULTISLOT) {
                        xml.openTag("multislot");
                        xml.textElementNoSpace("name", name);
                        int type = d.getSlotDataType(i);
                        if (type != -1) {
                            xml.textElementNoSpace("type", RU.getTypeName(type));
                        }
                        xml.append(visitValueVector(v.listValue(null)));
                        xml.closeTag("multislot");
                    } else {
                        xml.openTag("slot");
                        xml.textElementNoSpace("name", name);
                        int type = d.getSlotDataType(i);
                        if (type != -1) {
                            xml.textElementNoSpace("type", RU.getTypeName(type));
                        }
                        xml.appendln(visitValue(v));
                        xml.closeTag("slot");
                    }

                } catch (JessException e) {
                    return e.toString();
                }
            }
        }
        xml.closeTag("template");
        return xml.toString();
    }

    private void emitProperty(XMLWriter xml, String name, Value value) {
        xml.openTag("property");
        xml.textElementNoSpace("name", name);
        xml.append(visitValue(value));
        xml.closeTag("property");
    }

    private static Value newSymbolValue(String s) {
        try {
            return new Value(s, RU.SYMBOL);
        } catch (JessException cantHappen) {
            return null;
        }
    }

    private static boolean hasTemplateDeclarables(Deftemplate d) {
        return d.isShadowTemplate() || d.isSlotSpecific() || d.getBackwardChaining();
    }

    public Object visitDeffunction(Deffunction d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("function");
        xml.textElementNoSpace("name", d.getName());
        possiblyEmitDocstring(xml, d);
        xml.openTag("arguments");
        for (Iterator it = d.getArguments(); it.hasNext();) {
            Deffunction.Argument arg = (Deffunction.Argument) it.next();
            xml.openTag("argument");
            xml.textElementNoSpace("name", arg.getName());
            xml.textElementNoSpace("type", RU.getTypeName(arg.getType()));
            xml.closeTag("argument");
        }
        xml.closeTag("arguments");
        for (Iterator it = d.getActions(); it.hasNext();) {
            xml.appendln(visitValue((Value) it.next()));
        }
        xml.closeTag("function");
        return xml.toString();
    }

    public Object visitDefglobal(Defglobal d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("global");
        xml.textElementNoSpace("name", d.getName());
        xml.appendln(visitValue(d.getInitializationValue()));
        xml.closeTag("global");
        return xml.toString();
    }

    public Object visitDefrule(Defrule d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("rule");
        xml.textElementNoSpace("name", d.getName());
        possiblyEmitDocstring(xml, d);
        if (hasRuleDeclarables(d)) {
            xml.openTag("properties");
            if (d.hasNonDefaultSalience()) {
                emitProperty(xml, "salience", d.getSalienceValue());
            }
            if (d.getAutoFocus()) {
                emitProperty(xml, "auto-focus", Funcall.TRUE);
            }
            if (d.isNoLoop()) {
                emitProperty(xml, "no-loop", Funcall.TRUE);
            }
            if (d.getNodeIndexHash() != 0) {
                emitProperty(xml, "node-index-hash", makeIntValue(d.getNodeIndexHash()));
            }
            xml.closeTag("properties");
        }
        xml.openTag("lhs");
        if (d.getNext() != null) {
            xml.openTag("group");
            xml.textElementNoSpace("name", "or");
        }
        Defrule rule = d;
        while (rule != null) {
            xml.append(visitGroup((Group) rule.getConditionalElements()));
            rule = (Defrule) rule.getNext();
        }
        if (d.getNext() != null)
            xml.closeTag("group");

        xml.closeTag("lhs");
        xml.openTag("rhs");
        for (int i = 0; i < d.getNActions(); ++i)
            xml.append(visitFuncall(d.getAction(i)));
        xml.closeTag("rhs");
        xml.closeTag("rule");
        return xml.toString();
    }

    private static boolean hasRuleDeclarables(Defrule d) {
        return d.hasNonDefaultSalience() ||
                d.getAutoFocus() ||
                d.isNoLoop() ||
                d.getNodeIndexHash() != 0;
    }

    private static boolean hasQueryDeclarables(Defquery d) {
        return d.getMaxBackgroundRules() > 0 || d.getNodeIndexHash() != 0;
    }

    public Object visitDefquery(Defquery d) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("query");
        xml.textElementNoSpace("name", d.getName());
        possiblyEmitDocstring(xml, d);
        int nvars = d.getNVariables();
        xml.openTag("arguments");
        for (int i = 0; i < nvars; ++i) {
            Variable arg = d.getQueryVariable(i);
            xml.openTag("argument");
            xml.textElementNoSpace("name", RU.removePrefix(arg.variableValue(null)));
            xml.textElementNoSpace("type", RU.getTypeName(arg.type()));
            xml.closeTag("argument");
        }
        xml.closeTag("arguments");

        if (hasQueryDeclarables(d)) {
            xml.openTag("properties");
            if (d.getMaxBackgroundRules() > 0) {
                emitProperty(xml, "max-background-rules", makeIntValue(d.getMaxBackgroundRules()));
            }
            if (d.getNodeIndexHash() != 0) {
                emitProperty(xml, "node-index-hash", makeIntValue(d.getNodeIndexHash()));
            }
            xml.closeTag("properties");
        }

        xml.openTag("lhs");
        if (d.getNext() != null) {
            xml.openTag("group");
            xml.textElementNoSpace("name", "or");
        }
        Defquery query = d;
        while (query != null) {
            xml.append(visitGroup((Group) query.getConditionalElements()));
            query = (Defquery) query.getNext();
        }
        if (d.getNext() != null)
            xml.closeTag("group");

        xml.closeTag("lhs");
        xml.closeTag("query");
        return xml.toString();
    }

    private static Value makeIntValue(int i) {
        try {
            return new Value(i, RU.INTEGER);
        } catch (JessException cantHappen) {
            return null;
        }
    }

    public Object visitPattern(Pattern p) {
        if (p.getName().indexOf(Defquery.QUERY_TRIGGER) > 0)
            return "";
        XMLWriter xml = new XMLWriter();

        Deftemplate dt = p.getDeftemplate();
        try {
            Iterator testCEs = p.getTests(RU.NO_SLOT);
            if (testCEs.hasNext()) {
                xml.openTag("group");
                xml.textElementNoSpace("name", "and");
            }

            xml.openTag("pattern");
            String name = p.getName();
            if (name.equals("MAIN::test"))
                name = "test";
            xml.textElementNoSpace("name", name);
            if (p.getBoundName() != null) {
                xml.textElementNoSpace("binding", p.getBoundName());
            }

            for (int i = 0; i < p.getNSlots(); ++i) {
                if (p.getNTests(i) > 0 || p.getSlotLength(i) != -1) {
                    xml.openTag("slot");
                    xml.textElementNoSpace("name", dt.getSlotName(i));
                    for (Iterator tests = p.getTests(i); tests.hasNext();) {
                        Test1 t = (Test1) tests.next();
                        xml.append(visitTest1(t));
                    }
                    xml.closeTag("slot");
                }
            }

            xml.closeTag("pattern");

            if (testCEs.hasNext()) {
                while (testCEs.hasNext()) {
                    Test1 test = (Test1) testCEs.next();
                    xml.openTag("pattern");
                    xml.textElementNoSpace("name", "test");
                    xml.openTag("slot");
                    xml.textElementNoSpace("name", RU.DEFAULT_SLOT_NAME);
                    xml.append(visitTest1(test));
                    xml.closeTag("slot");
                    xml.closeTag("pattern");
                }
                xml.closeTag("group");
            }
        } catch (JessException e) {
            return e.toString();
        }
        return xml.toString();
    }

    public Object visitGroup(Group p) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("group");
        xml.textElementNoSpace("name", p.getName());

        for (int i = 0; i < p.getGroupSize(); ++i) {
            ConditionalElement ce = p.getConditionalElement(i);
            if (ceWasProbablyInserted(p, i))
                continue;
            xml.append(new XMLVisitor((Visitable) ce));
        }
        xml.closeTag("group");
        return xml.toString();
    }

    private boolean ceWasProbablyInserted(Group group, int i) {
        ConditionalElement ce = group.getConditionalElement(i);
        if (i > 0 || !ce.getName().equals(Deftemplate.getInitialTemplate().getName()))
            return false;
        Pattern pat = (Pattern) ce;
        if (pat.getNTests() > 0)
            return false;
        if (group.getGroupSize() == 1)
            return true;
        String nextName = group.getConditionalElement(i+1).getName();
        if (Group.isNegatedName(nextName) || nextName.equals(Group.LOGICAL))
            return true;
        else
            return false;
    }

    public Object visitTest1(Test1 t) {
        XMLWriter writer = new XMLWriter();
        writer.openTag("test");
        writer.textElementNoSpace("type", t.getTest() == Test1.EQ ? "eq" : "neq");
        if (t.getConjunction() != RU.NONE) {
            writer.textElementNoSpace("conjunction", t.getConjunction() == RU.AND ? "and" : "or");
        }

        writer.appendln(visitValue(t.getValue()));
        writer.closeTag("test");
        return writer.toString();
    }

    private Object visitValue(Value value) {
        try {
            int type = value.type();
            if (type == RU.FACT)
                return visitFact(value.factValue(null));
            else if (type == RU.FUNCALL)
                return visitFuncall(value.funcallValue(null));
            else if (type == RU.LIST)
                return makeList(value.listValue(null));
            else if (type == RU.BINDING)
                type = RU.VARIABLE;

            XMLWriter writer = new XMLWriter();
            writer.openTagNoNewline("value", "type", RU.getTypeName(type));

            switch (type) {
                case RU.SYMBOL:
                case RU.STRING:
                    writer.append(XMLWriter.escape(value.stringValue(null)));
                    break;
                case RU.VARIABLE:
                case RU.MULTIVARIABLE:
                    writer.append(RU.removePrefix(value.variableValue(null)));
                    break;
                case RU.FLOAT:
                case RU.INTEGER:
                case RU.LONG:
                default:
                    writer.append(value.toString());
                    break;
            }
            writer.closeTagNoIndentation("value");
            return writer;

        } catch (JessException e) {
            return e.toString();
        }
    }

    private Object visitValueVector(ValueVector list) {
        try {
            XMLWriter writer = new XMLWriter();
            for (int i = 0; i < list.size(); ++i)
                writer.appendln(visitValue(list.get(i)));
            return writer.toString();
        } catch (JessException e) {
            return e.toString();
        }
    }

    private Object makeList(ValueVector list) {
        XMLWriter xml = new XMLWriter();
        try {

            xml.openTag("list");
            for (int i = 0; i < list.size(); ++i)
                xml.appendln(visitValue(list.get(i)));
            xml.closeTag("list");

            return xml.toString();
        } catch (JessException e) {
            xml.append(e.toString());
            return xml.toString();
        }
    }


    public Object visitFact(Fact fact) {
        try {
            XMLWriter xml = new XMLWriter();
            Deftemplate dt = fact.getDeftemplate();
            xml.openTag("fact");
            xml.textElementNoSpace("name", fact.getName());
            for (int i = 0; i < dt.getNSlots(); ++i) {
                String name = dt.getSlotName(i);
                Value val = fact.getSlotValue(name);
                if (!dt.getSlotDefault(i).equals(val)) {
                    xml.openTag("slot");
                    xml.textElementNoSpace("name", name);
                    if (val.type() != RU.LIST) {
                        xml.append(visitValue(val));
                        xml.appendln("");
                    } else
                        xml.append(visitValueVector(val.listValue(null)));
                    xml.closeTag("slot");
                }
            }
            xml.closeTag("fact");
            return xml.toString();
        } catch (JessException e) {
            return e.toString();
        }
    }

    public Object visitFuncall(Funcall funcall) {
        XMLWriter xml = new XMLWriter();
        try {
            xml.openTag("funcall");
            xml.textElementNoSpace("name", funcall.get(0).symbolValue(null));
            for (int i = 1; i < funcall.size(); ++i)
                xml.appendln(visitValue(funcall.get(i)));
            xml.closeTag("funcall");
            return xml.toString();
        } catch (JessException e) {
            return e.toString();
        }
    }

    public Object visitAccumulate(Accumulate accumulate) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("accumulate");
        if (accumulate.getBoundName() != null) {
            xml.textElementNoSpace("binding", accumulate.getBoundName());
        }

        xml.openTag("accum-init");
        xml.append(visitValue(accumulate.getInitializer()));
        xml.closeTag("accum-init");

        xml.openTag("accum-body");
        xml.append(visitValue(accumulate.getBody()));
        xml.closeTag("accum-body");

        xml.openTag("accum-return");
        xml.append(visitValue(accumulate.getReturn()));
        xml.closeTag("accum-return");

        xml.append(new XMLVisitor((Visitable) accumulate.getConditionalElement(0)));

        xml.closeTag("accumulate");
        return xml.toString();
    }

    public Object visitDefmodule(Defmodule defmodule) {
        XMLWriter xml = new XMLWriter();
        xml.openTag("module");
        xml.textElementNoSpace("name", defmodule.getName());
        possiblyEmitDocstring(xml, defmodule);
        if (defmodule.getAutoFocus()) {
            xml.openTag("properties");
            emitProperty(xml, "auto-focus", Funcall.TRUE);
            xml.closeTag("properties");            
        }        
        xml.closeTag("module");
        return xml.toString();
    }

    public String toString() {
        return (String) m_visitable.accept(this);
    }

}
