package jess.xml;

import jess.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: XMLBuilder.java,v 1.5 2006-08-12 03:18:12 ejfried Exp $
 */
public class XMLBuilder implements Visitor {
    private Element m_rootElement;
    private Document m_document;
    public static final String NAMESPACE_URI = "http://www.jessrules.com/jessde";

    public XMLBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation domImpl = builder.getDOMImplementation();
        m_document = domImpl.createDocument(NAMESPACE_URI, "rulebase", null);
        m_rootElement = m_document.getDocumentElement();
        ProcessingInstruction pi = m_document.createProcessingInstruction("JessML-version", "1");
        m_rootElement.appendChild(pi);
    }


    public static void main(String[] args) throws ParserConfigurationException, TransformerException, JessException {
        Rete engine = new Rete();
        engine.eval("(deftemplate foo (slot bar1 (default 23)) (slot bar2))");
        Deftemplate template = engine.findDeftemplate("foo");

        XMLBuilder builder = new XMLBuilder();
        builder.visitDeftemplate(template);

        DOMSource source = new DOMSource(builder.getDocument());
        StreamResult result = new StreamResult(System.out);

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(source, result);
    }

    private Document getDocument() {
        return m_document;
    }

    public Object visitDeffacts(Deffacts d) {
        Element facts = m_document.createElement("facts");
        facts.appendChild(makeTextNode("name", d.getName()));
        int count = d.getNFacts();
        for (int i = 0; i < count; ++i) {
            Fact fact = d.getFact(i);
            facts.appendChild((Node) visitFact(fact));
        }
        return facts;
    }

    public Object visitDeftemplate(Deftemplate d) {
        Element template = m_document.createElement("template");

        template.appendChild(makeTextNode("name", d.getName()));

        if (d.isShadowTemplate()) {
            template.appendChild(makeTextNode("from-class", d.getShadowClassName()));
        } else {
            for (int i = 0; i < d.getNSlots(); ++i) {
                try {
                    String name = d.getSlotName(i);
                    Value v = d.getSlotDefault(i);
                    if (d.getSlotType(i) == RU.MULTISLOT) {
                        Element multislot = m_document.createElement("multislot");
                        template.appendChild(multislot);
                        multislot.appendChild(makeTextNode("name", name));
                        // xml.append(visitList(v.listValue(null), false));
                    } else {
                        Element slot = m_document.createElement("slot");
                        template.appendChild(slot);
                        slot.appendChild(makeTextNode("name", name));
                        slot.appendChild(visitValue(v));
                    }

                } catch (JessException e) {
                    return m_document.createTextNode(e.toString());
                }
            }
        }
        m_rootElement.appendChild(template);
        return template;
    }

    private Node visitValue(Value value) {
        try {
            if (value.type() == RU.FACT)
                return (Node) visitFact(value.factValue(null));
            else if (value.type() == RU.FUNCALL)
                return (Node) visitFuncall(value.funcallValue(null));
            else if (value.type() == RU.LIST)
                return makeSlot(value.listValue(null));

            Element result = m_document.createElement("value");
            Element type = makeTextNode("type", RU.getTypeName(value.type()));
            result.appendChild(type);

            String text;
            switch (value.type()) {
                case RU.SYMBOL:
                case RU.STRING:
                    text = value.stringValue(null);
                    break;
                case RU.VARIABLE:
                case RU.MULTIVARIABLE:
                    text = value.variableValue(null);
                    break;
                case RU.FLOAT:
                case RU.INTEGER:
                case RU.LONG:
                default:
                    text = value.toString();
                    break;
            }
            result.appendChild(m_document.createTextNode(text));
            return result;

        } catch (JessException e) {
            return m_document.createTextNode(e.toString());
        }
    }

    /* private Node visitList(ValueVector list) {
        return null;
        try {
            ;
            for (int i = 0; i < list.size(); ++i)
                ;
            writer.appendln(visitValue(list.get(i)));
            return writer.toString();
        } catch (JessException e) {
            return e.toString();
        }
    } */

    private Node makeSlot(ValueVector list) {
        Element slot = m_document.createElement("slot");
        try {
            slot.appendChild(makeTextNode("name", list.get(0).symbolValue(null)));
            for (int i = 1; i < list.size(); ++i)
                slot.appendChild(visitValue(list.get(i)));
        } catch (JessException e) {
            slot.appendChild(m_document.createTextNode(e.getMessage()));
        }
        return slot;
    }

    public Object visitFuncall(Funcall funcall) {
        Element result = m_document.createElement("funcall");
        try {
            result.appendChild(makeTextNode("name", escape(funcall.get(0).symbolValue(null))));
            for (int i = 1; i < funcall.size(); ++i)
                result.appendChild(visitValue(funcall.get(i)));
        } catch (JessException e) {
            result.appendChild(m_document.createTextNode(e.getMessage()));
        }
        return result;
    }

    private static String escape(String s) {
        if (needsNoEscaping(s))
            return s;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            switch (s.charAt(i)) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static boolean needsNoEscaping(String s) {
        return s.indexOf('<') < 0 &&
                s.indexOf('>') < 0 &&
                s.indexOf('&') < 0;
    }

    public Object visitFact(Fact fact) {
        Element result = m_document.createElement("fact");
        result.appendChild(makeTextNode("name", fact.getName()));
        try {
            Deftemplate dt = fact.getDeftemplate();
            for (int i = 0; i < dt.getNSlots(); ++i) {
                String name = dt.getSlotName(i);
                Value val = fact.getSlotValue(name);
                if (!dt.getSlotDefault(i).equals(val)) {
                    Element slot = m_document.createElement("slot");
                    result.appendChild(slot);
                    slot.appendChild(makeTextNode("name", name));
                    if (val.type() != RU.LIST)
                        slot.appendChild(visitValue(val));
                    else {
                        ValueVector vec = val.listValue(null);
                        for (int j = 0; j < vec.size(); ++j) {
                            slot.appendChild(visitValue(vec.get(j)));
                        }
                    }
                }
            }
        } catch (JessException e) {
            result.appendChild(m_document.createTextNode(e.getMessage()));
        }
        return result;
    }

    private Element makeTextNode(String tag, String text) {
        Text textNode = m_document.createTextNode(text);
        Element element = m_document.createElement(tag);
        element.appendChild(textNode);
        return element;
    }

    public Object visitDeffunction(Deffunction d) {
        return null;
    }

    public Object visitDefglobal(Defglobal d) {
        return null;
    }

    public Object visitDefrule(Defrule d) {
        return null;
    }

    public Object visitDefquery(Defquery d) {
        return null;
    }

    public Object visitPattern(Pattern p) {
        return null;
    }

    public Object visitGroup(Group p) {
        return null;
    }

    public Object visitTest1(Test1 t) {
        return null;
    }

    public Object visitAccumulate(Accumulate accumulate) {
        return null;
    }

    public Object visitDefmodule(Defmodule defmodule) {
        return null;
    }
}
