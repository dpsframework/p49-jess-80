package jess;

import org.w3c.dom.*;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Help implements the "help" function in Jess. You can use the static method getHelpFor(String)
 * to get help information about various Jess functions and constructs.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */
public class Help implements Userfunction, Serializable {
    private static Map s_helpMap;

    public String getName() {
        return "help";
    }

    private static synchronized void fillHelpMap() throws JessException {
        if (s_helpMap == null) {
            s_helpMap = new HashMap();
            addHelpFile("/xmlsrc/functions.xml", "functiondef");
            addHelpFile("/xmlsrc/constructs.xml", "constructdef");
        }
    }

    private static void addHelpFile(final String file, final String elementName) throws JessException {
        InputStream stream = Help.class.getResourceAsStream(file);
        if (stream == null)
            throw new JessException("help", "Can't find file", file);

        try {

            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(stream);
            NodeList items = document.getElementsByTagName(elementName);
            for (int i = 0; i < items.getLength(); ++i) {
                Element function = (Element) items.item(i);
                String name = function.getAttribute("name");
                Node description = function.getElementsByTagName("description").item(0);
                String helpText = addLineBreaks(emitNodeAsText(description), 65);
                helpText = helpText.trim();
                s_helpMap.put(name, helpText);
            }

        } catch (IOException e) {
            throw new JessException("help", "I/O error on help file", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new JessException("help", "XML parser error", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException silentlyIgnore) {
                }
            }
        }
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        PrintWriter pw = engine.getOutStream();
        if (vv.size() == 1) {
            pw.println("Type \"(help name)\" to get help for the function named \"name\".");
            pw.println("Type \"(help intro)\" to get some help getting started.");
            pw.flush();
            return Funcall.NIL;
        }

        String target = vv.get(1).symbolValue(context);
        String result = getHelpFor(target);
        if (result == null)
            pw.println("No help found for function '" + target + "'");
        else
            pw.println(result);

        return Funcall.NIL;
    }

    public static String getHelpFor(String target) throws JessException {
        try {
            fillHelpMap();
        } catch (NoClassDefFoundError e) {
            throw new JessException("help", "Help system unavailable; can't find class", e.getMessage());
        }

        if (target.equals("intro")) {
            return "(list-function$) -- list all Jess functions.\n" +
                "(help defrule) -- learn to define a rule.\n" +
                "(facts) -- show the contents of working memory.\n" +
                "(rules) -- list all the defined rules.\n";
        }

        return (String) s_helpMap.get(target);
    }

    private static String emitNodeAsText(Node node) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node child = nodes.item(i);
            if (child.hasChildNodes()) {
                buffer.append(emitNodeAsText(child));
            } else {
                if (child instanceof Element) {
                    Element element = (Element) child;
                    String tag = element.getTagName();
                    if (tag.equals("jessf"))
                        buffer.append(element.getAttribute("name"));
                    else if (tag.equals("javac"))
                        buffer.append(element.getAttribute("class"));
                    else if (tag.equals("apic"))
                        buffer.append(element.getAttribute("class"));
                    else if (tag.equals("construct"))
                        buffer.append(element.getAttribute("name"));
                }
                String text = child.getNodeValue();
                if (text != null)
                    buffer.append(flowText(text));
                buffer.append(" ");
            }
        }
        return buffer.toString();
    }

    private static java.util.regex.Pattern m_pattern = java.util.regex.Pattern.compile("[ \t\n\r]+");
    static String flowText(String input) {
        Matcher m = m_pattern.matcher(input.trim());
        return m.replaceAll(" ");
    }

    static String addLineBreaks(String input, int width) {
        String[] tokens = input.split(" ");
        StringBuffer buffer = new StringBuffer();
        int length = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            buffer.append(token);
            length += token.length() + 1;
            if (length > width) {
                length = 0;
                buffer.append('\n');
            } else {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }
}
