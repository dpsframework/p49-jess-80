package jess.jsr94;

import jess.*;
import jess.xml.JessSAXHandler;
import jess.xml.JessSAXParser;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.rules.*;
import javax.rules.admin.RuleExecutionSet;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import java.io.*;
import java.util.*;

class RuleExecutionSetImpl extends NameDescriptionProperties implements RuleExecutionSet, Cloneable {
    private Rete m_engine;
    private String m_filter;
    private String m_uri;

    public RuleExecutionSetImpl(Reader reader, Map map) throws IOException, JessException {
        super(map);
        m_engine = new Rete();

        PushbackReader pbr = new PushbackReader(reader, 512);
        if (Batch.isXMLDocument(pbr)) {
            JessSAXParser parser = new JessSAXParser(m_engine);
            parser.parse(new InputSource(pbr));
        } else {
            Jesp j = new Jesp(pbr, m_engine);
            j.parse(false, m_engine.getGlobalContext());
        }
    }

    public RuleExecutionSetImpl(Rete engine, Map map) {
        super(map);
        m_engine = engine;
    }

    public RuleExecutionSetImpl(Element elem, Map map) throws JessException {
        super(map);
        try {
            m_engine = new Rete();
            Source source = new DOMSource(elem);
            Result result = new SAXResult(new JessSAXHandler(m_engine, JessSAXHandler.RULEBASE));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new JessException("RuleExecutionSetImpl", "Can't build rules from DOM", e);
        }
    }

    public void setDefaultObjectFilter(String name) {
        m_filter = name;
    }

    public String getDefaultObjectFilter() {
        return m_filter;
    }

    public List getRules() {
        ArrayList list = new ArrayList();
        for (Iterator it = m_engine.listDefrules(); it.hasNext();)
            list.add(new RuleImpl((Defrule) it.next()));
        return list;
    }

    public Rete getEngine() {
        return m_engine;
    }

    public void run() throws InvalidRuleSessionException {
        try {
            m_engine.run();
        } catch (JessException e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    public void reset() throws InvalidRuleSessionException {
        try {
            for (Iterator it = m_engine.listDefinstances(); it.hasNext();)
                m_engine.undefinstance(it.next());
            m_engine.reset();
        } catch (JessException e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    public void release() {
        try {
            m_engine.clear();
        } catch (JessException silentlyIgnore) {
            // Ignore
        }
    }

    public Handle addObject(Object o) throws InvalidRuleSessionException {
        try {
            m_engine.add(o);
            return new HandleImpl(o);
        } catch (JessException e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    public boolean containsObject(Handle handle) {
        Object o = ((HandleImpl) handle).getObject();
        return m_engine.containsObject(o);
    }

    public void removeObject(Handle handle) throws InvalidRuleSessionException {
        try {
            Object o = ((HandleImpl) handle).getObject();
            m_engine.undefinstance(o);
        } catch (JessException e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    public List getObjects() {
        ArrayList list = new ArrayList();
        for (Iterator it = m_engine.listDefinstances(); it.hasNext();)
            list.add(it.next());
        return list;
    }

    public String getURI() {
        return m_uri;
    }

    public void setURI(String uri) {
        m_uri = uri;
    }

    public List getObjects(ObjectFilter objectFilter) {
        ArrayList filtered = new ArrayList();
        for (Iterator it = m_engine.listDefinstances(); it.hasNext();) {
            Object o = objectFilter.filter(it.next());
            if (o != null)
                filtered.add(o);
        }
        return filtered;
    }

    public Object clone() {
        try {
            RuleExecutionSetImpl copy =  (RuleExecutionSetImpl) super.clone();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m_engine.bsave(baos);
            Rete engine = new Rete();
            engine.bload(new ByteArrayInputStream(baos.toByteArray()));
            copy.m_engine = engine;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
