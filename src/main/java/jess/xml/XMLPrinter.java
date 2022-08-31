package jess.xml;

import jess.*;

import java.io.*;
import java.util.*;

/**
 * <p>This class can translate a whole file of Jess code into JessML. You can construct an
 * XMLPrinter that will send its output to a given Writer objects, then use the other methods
 * of the class to write an XML prelude and trailer surrounding an arbitrary amount of
 * JessML code translated directly from Jess code.</p>
 *
 * <p>This class includes a main() method that reads from a file and emits a complete JessML
 * document on standard output.</p>
 *
 * (C) 2013 Sandia Corporation<BR>
 */
public class XMLPrinter implements Serializable {

    // Functions that should be executed during parsing because they affect
    // the outcome of the parse
    private static final Set s_keepers = new HashSet();
    private static final String s_lineSeparator = System.getProperty("line.separator");

    static {
        s_keepers.add("import");
        s_keepers.add("defclass");
        s_keepers.add("add");
        s_keepers.add("do-backward-chaining");
        s_keepers.add("set-current-module");
        s_keepers.add("clear");
    }

    private Writer m_writer;
    private Rete m_engine = new Rete();

    /**
     * Constructor. Creates an XMLPrinter that sends its output to the given Writer.
     * @param writer the writer to send output to
     */
    public XMLPrinter(Writer writer) {
        m_writer = writer;
        StringWriter devNull = new StringWriter();
        m_engine.addOutputRouter("t", devNull);
        m_engine.addOutputRouter("WSTDOUT", devNull);
        m_engine.addOutputRouter("WSTDERR", devNull);

    }

    /**
     * Closes the underlying Writer.
     * @throws IOException if calling close() on the Writer throws it
     */
    public void close() throws IOException {
        m_writer.close();
    }

    /**
     * Reads a file of Jess code and emits equivalent XML on standard output, as a complete,
     * well-formed JessML document. Calls printFrontMatter(), translateToXML(), and
     * printBackMatter(), in that order.
     *
     * @param args first argument is the path to a file of Jess code
     * @throws java.io.IOException if the file doesn't exist
     * @throws jess.JessException if anything goes wrong
     */
    public static void main(String[] args) throws IOException, JessException {
        PrintWriter pw = new PrintWriter(System.out, true);
        FileReader fr = new FileReader(args[0]);
        XMLPrinter printer = new XMLPrinter(pw);
        try {
            printer.printFrontMatter();
            printer.translateToXML(fr);
            printer.printBackMatter();
        } finally {
            printer.close();
        }
    }

    /**
     * Reads Jess code from the Reader, and sends JessML equivalents to the underlying Writer.
     * No XML header or footer are written -- just the individual constructs.
     *
     * @param reader a source of Jess code
     * @throws JessException if anything goes wrong
     */
    public void translateToXML(Reader reader) throws JessException {
        Jesp j = new Jesp(reader, m_engine);

        PrintWriter pw = new PrintWriter(m_writer, true);
        Object o;
        do {
            o = j.parseExpression(m_engine.getGlobalContext(), false);
            if (o instanceof Visitable) {
                XMLVisitor p = new XMLVisitor((Visitable) o);
                pw.println(p.toString());

            } else if (o instanceof List) {
                List v = (List) o;
                for (int i = 0; i < v.size(); i++) {
                    XMLVisitor p = new XMLVisitor((Visitable) v.get(i));
                    pw.println(p.toString());
                }

            } else if (o instanceof FuncallValue) {
                Funcall funcall = ((FuncallValue) o).funcallValue(null);
                if (shouldExecute(funcall))
                    funcall.execute(m_engine.getGlobalContext());
                pw.println(new XMLVisitor(funcall));
            }
        } while (!Funcall.EOF.equals(o));
        pw.flush();
    }

    /**
     * Prints a closing "rulebase" tag to the underlying writer.
     *
     * @throws IOException  if anything goes wrong
     */
    public void printBackMatter() throws IOException {
        m_writer.write("</rulebase>");
        m_writer.write(s_lineSeparator);
    }

    /**
     * Prints an XML prelude, JessML version instruction, and opening "rulebase" tag
     * to the underlying writer.
     *
     * @throws IOException if anything goes wrong
     */
    public void printFrontMatter() throws IOException {
        m_writer.write("<?xml version='1.0' encoding='US-ASCII'?>");
        m_writer.write(s_lineSeparator);
        m_writer.write("<rulebase xmlns='http://www.jessrules.com/JessML/1.0'>");
        m_writer.write(s_lineSeparator);
    }

    private static boolean shouldExecute(Funcall funcall) {
        return s_keepers.contains(funcall.getName());
    }
}
