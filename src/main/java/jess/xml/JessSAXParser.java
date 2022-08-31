package jess.xml;

import jess.JessException;
import jess.Rete;
import org.xml.sax.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 A SAX-based parser for JessML. Construct a JessSAXParser with a
 Rete object as an argument, then pass an InputSource to the parse()
 method. All the constructs and other code in the file will be
 installed into the Rete object.
 <pre>
 JessSAXParser parser = new JessSAXParser(engine);
 parser.parse(new org.xml.sax.InputSource(new FileInputStream(filename));
 </pre>
 (C) 2013 Sandia Corporation<br>
 @see Rete#batch
 */

public class JessSAXParser {

    private SAXParser m_parser;
    private final JessSAXHandler m_handler;

    /**
     * Constructor. This parser object will read JessML code and install the results into the given Rete object.
     * The parsed document must start with the named top level element,
     * one of {@link jess.xml.JessSAXHandler#RULEBASE} or {@link jess.xml.JessSAXHandler#FACTLIST}.
     * @param sink the Rete object to receive any parsed constructs
     * @param topLevelElement the name of the top-level element to expect
     * @throws JessException if anything goes wrong
     */
    public JessSAXParser(Rete sink, String topLevelElement) throws JessException {
        m_handler = new JessSAXHandler(sink, topLevelElement);
        try {
            m_parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception e) {
            throw new JessException("JessSAXParser", "Can't initialize", e);
        }
    }

    /**
     * Constructor. This parser object will read JessML code and install the results into the given Rete object.
     * The parse document must start with a {@link jess.xml.JessSAXHandler#RULEBASE} element.
     * @param sink  the Rete object to receive any parsed constructs
     * @throws JessException if anything goes wrong
     */
    public JessSAXParser(Rete sink) throws JessException {
        this(sink, JessSAXHandler.RULEBASE);
    }

    /**
     * Parse a document full of JessML code. This should be a well-formed XML document including the
     * &lt;?xml?&gt; entity line. The document root element should be either a &lt;rulebase&gt; element
     * or a &lt;fact-list&gt; element.
     *
     * @param source the input source
     * @throws JessException if anything goes wrong
     */
    public void parse(InputSource source) throws JessException {
        try {
            m_parser.parse(source, m_handler);
        } catch (SAXException e) {
            if (e.getException() instanceof JessException)
                throw (JessException) e.getException();
            else {
                throw new JessException("JessSAXParser.parse", "Parse error: " + e.getMessage(), e.getException());
            }
        } catch (Exception e) {
            throw new JessException("JessSAXParser.parse", "Parse error: " + e.getMessage(), e);
        }
    }

}
