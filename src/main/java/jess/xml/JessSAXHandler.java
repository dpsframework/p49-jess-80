package jess.xml;

import jess.*;
import jess.xml.JessSAXHandler.Scope;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

/**
 * A SAX parser event handler. SAX events are translated into Jess
 * constructs in a Rete engine.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class JessSAXHandler extends DefaultHandler {
    private final Rete m_sink;
    private final Stack<Scope> m_scopes = new Stack<Scope>();
    private final String m_topLevelElement;

    private StringBuffer m_text;
    private static final String PAYLOAD = "payload";
    private static final String COMMENT = "comment";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String CONJUNCTION = "conjunction";
    private static final String CONTENTS = "contents";
    private static final String BINDING = "binding";
    private static final String FROM_CLASS = "from-class";
    private static final String PROPERTY = "property";
    private static final String SALIENCE = "salience";
    private static final String NO_LOOP = "no-loop";
    private static final String NODE_INDEX_HASH = "node-index-hash";
    private static final String ACCUM_BODY = "accum-body";
    private static final String ACCUM_RETURN = "accum-return";
    private static final String ACCUMULATE = "accumulate";
    private static final String PATTERN = "pattern";
    private static final String AUTO_FOCUS = "auto-focus";
    private static final String MAX_BACKGROUND_RULES = "max-background-rules";
    private static final String ACCUM_INIT = "accum-init";

    private static final HashSet<String> s_validTags = new HashSet<String>();

    private static final String[] LHS_OR_PROPERTIES = {"lhs", "properties"};

    private static final String[] VALUE_OR_FUNCALL = {"value", "funcall"};

    /** The top-level element of a regular JessML file. */
    public static final String RULEBASE = "rulebase";

    /** The top-level element of a JessML fact file. */
    public static final String FACTLIST = "fact-list";

    static {
        s_validTags.add(ACCUMULATE);
        s_validTags.add(ACCUM_BODY);
        s_validTags.add(ACCUM_RETURN);
        s_validTags.add(ACCUM_INIT);
        s_validTags.add("comment");
        s_validTags.add("properties");
        s_validTags.add(PROPERTY);
        s_validTags.add(PATTERN);
        s_validTags.add("function");
        s_validTags.add("arguments");
        s_validTags.add("argument");
        s_validTags.add(BINDING);
        s_validTags.add("test");
        s_validTags.add("value");
        s_validTags.add(TYPE);
        s_validTags.add(CONJUNCTION);
        s_validTags.add("rule");
        s_validTags.add("query");
        s_validTags.add("module");
        s_validTags.add("global");
        s_validTags.add("template");
        s_validTags.add("group");
        s_validTags.add("funcall");
        s_validTags.add("fact");
        s_validTags.add("facts");
        s_validTags.add("slot");
        s_validTags.add("multislot");
        s_validTags.add("name");
        s_validTags.add(RULEBASE);
        s_validTags.add("lhs");
        s_validTags.add("rhs");
        s_validTags.add("list");
        s_validTags.add("extends");
        s_validTags.add(FACTLIST);

        // Deftemplate declarables
        s_validTags.add(FROM_CLASS);
        s_validTags.add("slot-specific");
        s_validTags.add("backchain-reactive");
        s_validTags.add("include-variables");
        s_validTags.add("ordered");
    }

    /**
     * Construct a JessSAXHandler that will execute parsed code in the context of the given Rete object.
     *
     * @param sink the Rete object that will serve as an execution context
     */
    public JessSAXHandler(Rete sink, String topLevelElement) {
        m_sink = sink;
        m_topLevelElement = topLevelElement;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {

        if (!s_validTags.contains(qName))
            throw new SAXException("Unrecognized element \"" + qName + "\" in startElement");

        else if (m_scopes.size() == 0 && !qName.equals(m_topLevelElement))
            throw new SAXException("Top-level element must be \"" + m_topLevelElement + "\"");

        m_scopes.push(new Scope(qName, attributes));

        if (m_text != null)
            m_text.setLength(0);
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        try {
            Stack<Scope> args = new Stack<Scope>();
            while (!((Scope) m_scopes.peek()).open)
                args.push(m_scopes.pop());
            Scope scope = (Scope) m_scopes.peek();

            if (qName.equals("rule")) {
                String name = (String) scope.get(NAME);
                String comment = (String) scope.get(COMMENT, "");

                Defrule rule = new Defrule(name, comment, m_sink);
                String nextElement = lookingAt(args, LHS_OR_PROPERTIES);
                if (nextElement.equals("properties")) {
                    Map<?, ?> properties = (Map<?, ?>) ((Scope) args.pop()).get(PAYLOAD);
                    if (properties.get(SALIENCE) != null) {
                        Value data = (Value) properties.get(SALIENCE);
                        rule.setSalience(data, m_sink);
                    }
                    if (properties.get(AUTO_FOCUS) != null) {
                        Value data = (Value) properties.get(AUTO_FOCUS);
                        rule.setAutoFocus(Funcall.TRUE.equals(data));
                    }
                    if (properties.get(NO_LOOP) != null) {
                        Value data = (Value) properties.get(NO_LOOP);
                        rule.setNoLoop(Funcall.TRUE.equals(data));
                    }
                    if (properties.get(NODE_INDEX_HASH) != null) {
                        Value data = (Value) properties.get(NODE_INDEX_HASH);
                        rule.setNodeIndexHash(data.intValue(null));
                    }

                    lookingAt(args, "lhs");
                }
                Group lhs = (Group) ((Scope) args.pop()).get(PAYLOAD);
                rule.setLHS(lhs, m_sink);

                lookingAt(args, "rhs");
                ArrayList<?> rhs = (ArrayList<?>) ((Scope) args.pop()).get(PAYLOAD);
                for (Iterator<?> iterator = rhs.iterator(); iterator.hasNext();) {
                    Funcall f = (Funcall) iterator.next();
                    rule.addAction(f);
                }
                m_sink.addDefrule(rule);
                m_scopes.pop();

            } else if (qName.equals("query")) {
                String name = (String) scope.get(NAME);
                String comment = (String) scope.get(COMMENT, "");

                Defquery query = new Defquery(name, comment, m_sink);
                lookingAt(args, "arguments");
                Scope argumentsScope = (Scope) args.pop();
                List<?> arguments = (List<?>) argumentsScope.get(PAYLOAD);
                for (Iterator<?> it = arguments.iterator(); it.hasNext();) {
                    Variable var = (Variable) it.next();
                    query.addQueryVariable(var);
                }
                String nextElement = lookingAt(args, LHS_OR_PROPERTIES);
                if (nextElement.equals("properties")) {
                    Map<?, ?> properties = (Map<?, ?>) ((Scope) args.pop()).get(PAYLOAD);
                    if (properties.get(MAX_BACKGROUND_RULES) != null) {
                        Value data = (Value) properties.get(MAX_BACKGROUND_RULES);
                        query.setMaxBackgroundRules(data.intValue(null));
                    }
                    if (properties.get(NODE_INDEX_HASH) != null) {
                        Value data = (Value) properties.get(NODE_INDEX_HASH);
                        query.setNodeIndexHash(data.intValue(null));
                    }
                    lookingAt(args, "lhs");
                }

                Group lhs = (Group) ((Scope) args.pop()).get(PAYLOAD);
                query.setLHS(lhs, m_sink);

                m_sink.addDefrule(query);
                m_scopes.pop();

            } else if (qName.equals("name")) {
                storeTextInParentScope(NAME);

            } else if (qName.equals("type")) {
                storeTextInParentScope(TYPE);

            } else if (qName.equals("comment")) {
                storeTextInParentScope(COMMENT);

            } else if (qName.equals("binding")) {
                storeTextInParentScope(BINDING);

            } else if (qName.equals("extends")) {
                storeTextInParentScope("extends");

            } else if (qName.equals("properties")) {
                HashMap<Object, Object> map = new HashMap<Object, Object>();
                while (!args.isEmpty()) {
                    Object[] data = (Object[]) ((Scope) args.pop()).get(PAYLOAD);
                    map.put(data[0], data[1]);
                }
                scope.put(PAYLOAD, map);

            } else if (qName.equals("property")) {
                String name = (String) scope.get(NAME);
                String kind = lookingAt(args, new String[]{"funcall", "value"});
                Value value;
                Object payload = ((Scope) args.pop()).get(PAYLOAD);
                if (kind.equals("value"))
                    value = (Value) payload;
                else
                    value = new FuncallValue((Funcall) payload);

                scope.put(PAYLOAD, new Object[]{name, value});

            } else if (qName.equals("conjunction")) {
                storeTextInParentScope(CONJUNCTION);

            } else if (qName.equals("template")) {
                String name = (String) scope.get(NAME);
                Deftemplate template;

                Map<?, ?> properties = new HashMap<Object, Object>();
                if (args.size() > 0) {
                    Scope propertiesScope = (Scope) args.peek();
                    if (propertiesScope.name.equals("properties")) {
                        args.pop();
                        properties = (Map<?, ?>) propertiesScope.get(PAYLOAD);
                    }
                }

                String parent = (String) scope.get("extends", null);
                Value className = (Value) properties.get(FROM_CLASS);
                Value includeVariables = (Value) properties.get("include-variables");
                if (className != null) {
                    boolean includeFlag = includeVariables != null && Funcall.TRUE.equals(includeVariables);
                    m_sink.defclass(name, className.stringValue(null), parent, includeFlag);
                    template = m_sink.findDeftemplate(name);
                } else {
                    String comment = (String) scope.get(COMMENT, "");
                    if (parent == null)
                        template = new Deftemplate(name, comment, m_sink);
                    else {
                        Deftemplate parentTemplate = m_sink.findDeftemplate(parent);
                        template = new Deftemplate(name, comment, parentTemplate, m_sink);
                    }

                    while (!args.isEmpty()) {
                        lookingAt(args, new String[]{"slot", "multislot"});
                        Scope slotScope = (Scope) args.pop();
                        boolean multi = slotScope.name.equals("multislot");
                        String slotName = (String) slotScope.get(NAME);
                        ArrayList<?> contents = (ArrayList<?>) slotScope.get(CONTENTS);
                        Value value = defaultValue(contents, multi);
                        String type = "ANY";
                        if (slotScope.getNullOK(TYPE) != null)
                            type = (String) slotScope.get(TYPE);
                        if (multi)
                            template.addMultiSlot(slotName, value, type);
                        else
                            template.addSlot(slotName, value, type);
                    }
                }

                template.setSlotSpecific(Funcall.TRUE.equals(properties.get("slot-specific")));
                if (Funcall.TRUE.equals(properties.get("backchain-reactive")))
                    template.doBackwardChaining(m_sink);

                m_sink.addDeftemplate(template);
                m_scopes.pop();

            } else if (qName.equals("lhs")) {
                Group group = new Group(Group.AND);
                while (!args.isEmpty()) {
                    ConditionalElement ce = (ConditionalElement) ((Scope) args.pop()).get(PAYLOAD);
                    group.add(ce);
                }
                scope.put(PAYLOAD, group);

            } else if (qName.equals("group")) {
                String name = (String) scope.get(NAME);
                Group group = new Group(name);
                while (!args.isEmpty()) {
                    ConditionalElement ce = (ConditionalElement) ((Scope) args.pop()).get(PAYLOAD);
                    group.add(ce);
                }
                scope.put(PAYLOAD, group);

            } else if (qName.equals("function")) {
                String name = (String) scope.get(NAME);
                Deffunction function = new Deffunction(name, "");
                lookingAt(args, "arguments");
                Scope argumentsScope = (Scope) args.pop();
                List<?> arguments = (List<?>) argumentsScope.get(PAYLOAD);
                for (Iterator<?> it = arguments.iterator(); it.hasNext();) {
                    Variable var = (Variable) it.next();
                    function.addArgument(var.variableValue(null), var.type());
                }

                while (!args.isEmpty()) {
                    lookingAt(args, new String[]{"funcall", "value"});
                    Scope actionScope = (Scope) args.pop();
                    Object action = actionScope.get(PAYLOAD);
                    if (action instanceof Funcall)
                        function.addAction((Funcall) action);
                    else
                        function.addValue((Value) action);
                }
                m_sink.addUserfunction(function);
                m_scopes.pop();

            } else if (qName.equals("argument")) {
                String name = (String) scope.get(NAME);
                String typeString = (String) scope.get(TYPE);
                int type = RU.getTypeCode(typeString);
                Variable var = new Variable(name, type);
                scope.put(PAYLOAD, var);

            } else if (qName.equals("arguments")) {
                ArrayList<Variable> list = new ArrayList<Variable>();
                while (!args.isEmpty()) {
                    lookingAt(args, "argument");
                    Scope variableScope = (Scope) args.pop();
                    Variable variable = (Variable) variableScope.get(PAYLOAD);
                    list.add(variable);
                }
                scope.put(PAYLOAD, list);

            } else if (qName.equals(PATTERN)) {
                String name = (String) scope.get(NAME);
                String boundname = (String) scope.getNullOK(BINDING);
                Pattern pattern = new Pattern(name, m_sink);
                if (boundname != null)
                    pattern.setBoundName(boundname);
                Deftemplate template = pattern.getDeftemplate();
                if (args.size() == 0 && template.isOrdered()) {
                    pattern.setSlotLength("__data", 0);
                } else {
                    while (!args.isEmpty()) {
                        lookingAt(args, "slot");
                        Scope argScope = (Scope) args.pop();
                        String slotName = (String) argScope.get(NAME);

                        boolean isMultislot = template.getSlotType(slotName) == RU.MULTISLOT;
                        int subIndex = 0;
                        ArrayList<?> contents = (ArrayList<?>) argScope.get(CONTENTS);
                        boolean firstTime = true;
                        for (Iterator<?> it = contents.iterator(); it.hasNext();) {
                            Test1 test1 = (Test1) it.next();
                            if (isMultislot) {
                                if (!firstTime && test1.getConjunction() == RU.NONE)
                                    ++subIndex;
                                test1.setMultiSlotIndex(subIndex);
                            }
                            test1.setSlotName(slotName);
                            pattern.addTest(test1);
                            firstTime = false;
                        }

                        if (isMultislot) {
                            pattern.setSlotLength(slotName, subIndex + 1);
                            int index = pattern.getDeftemplate().getSlotIndex(slotName);
                            if (pattern.getNTests(index) == 0)
                                pattern.setSlotLength(slotName, 0);
                        }
                    }
                }
                scope.put(PAYLOAD, pattern);

            } else if (qName.equals("slot") || qName.equals("multislot")) {
                String name = (String) scope.getNullOK(NAME);
                ArrayList<Object> contents = new ArrayList<Object>();
                scope.put(NAME, name);
                scope.put(CONTENTS, contents);
                while (!args.isEmpty()) {
                    Scope argScope = (Scope) args.pop();
                    if (argScope.name.equals("type"))
                        scope.put("type", argScope.get(TYPE));
                    else
                        contents.add(argScope.get(PAYLOAD));
                }
                scope.put(PAYLOAD, new Object[]{name, contents});

            } else if (qName.equals("list")) {
                ValueVector vec = new ValueVector();
                while (!args.isEmpty()) {
                    Scope argScope = (Scope) args.pop();
                    Object val = argScope.get(PAYLOAD);
                    if (val instanceof Funcall)
                        vec.add(new FuncallValue((Funcall) val));
                    else
                        vec.add(val);
                }
                scope.put(PAYLOAD, new Value(vec, RU.LIST));

            } else if (qName.equals("global")) {
                String name = (String) scope.get(NAME);
                lookingAt(args, VALUE_OR_FUNCALL);
                Scope valueScope = (Scope) args.pop();
                Value init = makeValue(valueScope.get(PAYLOAD));
                Defglobal global = new Defglobal(name, init);
                m_sink.addDefglobal(global);
                m_scopes.pop();

            } else if (qName.equals("funcall")) {
                String name = (String) scope.get(NAME);
                Funcall f = new Funcall(name, m_sink);
                while (!args.isEmpty()) {
                    Scope argScope = (Scope) args.pop();
                    f.arg(makeValue(argScope.get(PAYLOAD)));
                }
                if (isTopLevelFunction()) {
                    try {
                        f.execute(m_sink.getGlobalContext());
                    } finally {
                        m_sink.getGlobalContext().clearReturnValue();
                        m_scopes.pop();
                    }
                } else {
                    scope.put(PAYLOAD, f);
                }

            } else if (qName.equals(ACCUM_INIT)) {
                Scope argScope = (Scope) args.pop();
                Value v = makeValue(argScope.get(PAYLOAD));
                scope.put(PAYLOAD, v);

            } else if (qName.equals(ACCUM_RETURN)) {
                Scope argScope = (Scope) args.pop();
                Value v = makeValue(argScope.get(PAYLOAD));
                scope.put(PAYLOAD, v);

            } else if (qName.equals(ACCUM_BODY)) {
                Scope argScope = (Scope) args.pop();
                Value v = makeValue(argScope.get(PAYLOAD));
                scope.put(PAYLOAD, v);

            } else if (qName.equals(ACCUMULATE)) {
                Accumulate accum = new Accumulate();

                String boundname = (String) scope.getNullOK(BINDING);
                if (boundname != null)
                    accum.setBoundName(boundname);

                lookingAt(args, ACCUM_INIT);
                Scope initScope = (Scope) args.pop();
                accum.setInitializer((Value) initScope.get(PAYLOAD));

                lookingAt(args, ACCUM_BODY);
                Scope bodyScope = (Scope) args.pop();
                accum.setBody((Value) bodyScope.get(PAYLOAD));

                lookingAt(args, ACCUM_RETURN);
                Scope returnScope = (Scope) args.pop();
                accum.setReturn((Value) returnScope.get(PAYLOAD));

                lookingAt(args, PATTERN);
                Scope patternScope = (Scope) args.pop();
                accum.add((Pattern) patternScope.get(PAYLOAD));

                scope.put(PAYLOAD, accum);

            } else if (qName.equals("value")) {
                String type = scope.getAttribute("type");
                if (type == null)
                    throw new JessException("JessSAXHandler.endElement", "Element 'value' missing required attribute", "'type'");
                int valueType = RU.getTypeCode(type);
                Value value = decodeValue(m_text.toString(), valueType);
                scope.put(PAYLOAD, value);

            } else if (qName.equals(RULEBASE)) {
                // Done!

            } else if (qName.equals("module")) {
                String name = (String) scope.get(NAME);
                String comment = (String) scope.get(COMMENT, "");
                Defmodule module = new Defmodule(name, comment);
                if (!args.isEmpty()) {
                    lookingAt(args, "properties");

                    Map<?, ?> properties = (Map<?, ?>) ((Scope) args.pop()).get(PAYLOAD);
                    if (properties.get(AUTO_FOCUS) != null) {
                        Value data = (Value) properties.get(AUTO_FOCUS);
                        module.setAutoFocus(!Funcall.FALSE.equals(data));
                    }
                }
                try {
                    m_sink.addDefmodule(module);
                } catch (JessException ex) {
                    m_sink.setCurrentModule(name);
                }
                m_scopes.pop();
                // Empty element, nothing to do

            } else if (qName.equals("rhs")) {
                ArrayList<Object> actions = new ArrayList<Object>();
                scope.put(PAYLOAD, actions);
                while (!args.isEmpty()) {
                    lookingAt(args, "funcall");
                    Scope actionScope = (Scope) args.pop();
                    actions.add(actionScope.get(PAYLOAD));
                }

            } else if (qName.equals(FACTLIST)) {
                while (!args.isEmpty()) {
                    lookingAt(args, "fact");
                    Scope factScope = (Scope) args.pop();
                    m_sink.assertFact((Fact) factScope.get(PAYLOAD));
                }

            } else if (qName.equals("facts")) {
                String name = (String) scope.get(NAME);
                Deffacts facts = new Deffacts(name, "", m_sink);
                while (!args.isEmpty()) {
                    lookingAt(args, "fact");
                    Scope factScope = (Scope) args.pop();
                    facts.addFact((Fact) factScope.get(PAYLOAD));
                }
                m_sink.addDeffacts(facts);
                m_scopes.pop();

            } else if (qName.equals("test")) {
                String type = (String) scope.get(TYPE);
                String conjunction = (String) scope.getNullOK(CONJUNCTION);
                if (conjunction == null)
                    conjunction = "none";
                int testType = type.equals("eq") ? Test1.EQ : Test1.NEQ;
                int conjunctionType =
                        conjunction.equals("and") ? RU.AND : (conjunction.equals("or") ? RU.OR : RU.NONE);
                Value value = makeValue(((Scope) args.pop()).get(PAYLOAD));
                scope.put(PAYLOAD, new Test1(testType, "", value, conjunctionType));

            } else if (qName.equals("fact")) {
                String name = (String) scope.get(NAME);
                Fact fact = new Fact(name, m_sink);
                while (!args.isEmpty()) {
                    Scope argScope = (Scope) args.pop();
                    String slotName = (String) argScope.get(NAME);
                    ArrayList<?> contents = (ArrayList<?>) argScope.get(CONTENTS);
                    if (fact.getDeftemplate().getSlotType(slotName) == RU.MULTISLOT) {
                        ValueVector data = new ValueVector(contents.size());
                        for (Iterator<?> iterator = contents.iterator(); iterator.hasNext();) {
                            Value v = makeValue(iterator.next());
                            data.add(v);
                        }
                        fact.setSlotValue(slotName, new Value(data, RU.LIST));
                    } else
                        fact.setSlotValue(slotName, makeValue(contents.get(0)));
                }
                scope.put(PAYLOAD, fact);

            } else {
                throw new SAXException("Unrecognized element \"" + qName + "\" in endElement");
            }

            if (m_text != null)
                m_text.setLength(0);
            scope.close();

        } catch (JessException e) {
            throw new SAXException(e);
        }
    }

    private boolean isTopLevelFunction() {
        int top = m_scopes.size();
        int container = top - 2;
        String scopeName = ((Scope) m_scopes.get(container)).name;
        return (RULEBASE.equals(scopeName) || "module".equals(scopeName));
    }

    private void storeTextInParentScope(final String name) throws JessException {
        m_scopes.pop();
        if (m_scopes.isEmpty())
            throw new JessException("JessSAXHandler.storeTextInParentScope", "Element not allowed at outer scope", name);
        String text = m_text.toString().trim();
        ((Scope) m_scopes.peek()).put(name, text);
    }

    private static Value defaultValue(ArrayList<?> data, boolean multi) throws JessException {
        if (!multi)
            return data.size() == 0 ? Funcall.NIL : makeValue(data.get(0));
        else if (data.size() == 0)
            return Funcall.NILLIST;
        else {
            ValueVector vv = new ValueVector();
            for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
                Value value = makeValue(iterator.next());
                vv.add(value);
            }
            return new Value(vv, RU.LIST);
        }
    }

    private static Value makeValue(Object o) throws JessException {
        if (o instanceof Value)
            return (Value) o;
        else if (o instanceof Funcall)
            return new FuncallValue((Funcall) o);
        else if (o instanceof Fact)
            return new FactIDValue((Fact) o);
        else if (o instanceof Object[]) {
            Object[] slot = (Object[]) o;
            ValueVector vv = new ValueVector(2);
            if (slot[0] != null)
                vv.add(new Value((String) slot[0], RU.SYMBOL));
            ArrayList<?> contents = (ArrayList<?>) slot[1];
            for (int i = 0; i < contents.size(); ++i)
                vv.add(makeValue(contents.get(i)));
            return new Value(vv, RU.LIST);
        }

        throw new JessException("JessSAXHandler.makeValue", "Expected value, saw", o.getClass().getName() + ":" + o.toString());
    }

    private static void lookingAt(Stack<Scope> stack, String name) throws JessException {
        Scope s = (Scope) stack.peek();
        if (!s.name.equals(name))
            throw new JessException("JessSAXHandler.endElement", "Unexpected element: wanted " + name + ", got ", s.name);
    }

    private static String lookingAt(Stack<Scope> stack, String[] names) throws JessException {
        Scope s = (Scope) stack.peek();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (name.equals(s.name))
                return name;
        }
        throw new JessException("JessSAXHandler.endElement", "Unexpected element: wanted " + Arrays.asList(names) + ", got ", s.name);
    }

    private static Value decodeValue(String text, int valueType) throws JessException {
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&amp;", "&");
        switch (valueType) {
            case RU.SYMBOL:
            case RU.STRING:
                return new Value(text, valueType);
            case RU.INTEGER:
                return new Value(Integer.parseInt(text), valueType);
            case RU.FLOAT:
                return new Value(Double.parseDouble(text), valueType);
            case RU.LONG:
                return new LongValue(Long.parseLong(text));
            case RU.VARIABLE:
            case RU.MULTIVARIABLE:
                text = RU.removePrefix(text);
                return new Variable(text, valueType);
            default:
                throw new JessException("JessSAXHandler.decodeValue", "Bad type", valueType);
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (m_text == null)
            m_text = new StringBuffer();
        m_text.append(ch, start, length);
    }

    void dumpStack() {
        System.out.println(m_scopes);
    }

    static class Scope {
        final String name;
        final HashMap<String, Object> payload;
        boolean open = true;
        private HashMap<String, String> m_attributes;

        public Scope(String aName, Attributes attr) {
            if (attr.getLength() > 0) {
                m_attributes = new HashMap<String, String>();
                for (int i=0; i<attr.getLength(); ++i)
                    m_attributes.put(attr.getQName(i), attr.getValue(i));
            }
            name = aName;
            payload = new HashMap<String, Object>();
        }

        public Object get(String key) throws SAXException {
            Object result = payload.get(key);
            if (result == null) {
                throw new SAXException("Missing child " + key + " in scope " + name);
            }
            return result;
        }

        public Object get(String key, Object deflt) {
            Object result = payload.get(key);
            if (result == null)
                result = deflt;
            return result;
        }

        public Object getNullOK(String key) {
            return payload.get(key);
        }

        public void put(String key, Object value) {
            payload.put(key, value);
        }

        public void close() {
            open = false;
        }

        public String toString() {
            return "[" + name + ": " + payload +
                    (open ? " (open)" : " (closed)") +
                    "]";
        }

        public String getAttribute(String s) {
            return (String) (m_attributes == null ? null : m_attributes.get(s));
        }
    }
}


