package jess;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage input and output routers
 * <P>
 * Routers are kept in two maps: input ones and output ones.
 * Names that are read-write are kept in both tables as separate entries.
 * This means we don't need a special 'Router' class.
 * <P>
 * Every input router is wrapped in a BufferedReader so we get reliable
 * treatment of end-of-line. We need to keep track of the association, so
 * we keep the original stream paired with the wrapper in m_inWrappers.
 * <P>
 * Console-like streams act differently than file-like streams under
 * read and readline , so when you create a router, you need to specify
 * how it should act.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Routers {

    private final Map m_outRouters = new HashMap(13);
    private final Map m_inRouters = new HashMap(13);
    private final Map m_inWrappers = new HashMap(13);
    private final Map m_inModes = new HashMap(13);

    Routers() {
        addInputRouter("t", new InputStreamReader(System.in), true);
        addOutputRouter("t", new PrintWriter(System.out, false));
        addInputRouter("WSTDIN", getInputRouter("t"), true);
        addOutputRouter("WSTDOUT", getOutputRouter("t"));
        addOutputRouter("WSTDERR", getOutputRouter("t"));
    }

    synchronized void addInputRouter(String s, Reader is,
                                     boolean consoleLike) {
        Tokenizer t = (Tokenizer) m_inWrappers.get(is);
        if (t == null)
            t = new ReaderTokenizer(is, false);

        m_inRouters.put(s, is);
        m_inWrappers.put(is, t);
        m_inModes.put(s, Boolean.valueOf(consoleLike));
    }

    synchronized void removeInputRouter(String s) {
        m_inRouters.remove(s);
    }

    synchronized Reader getInputRouter(String s) {
        return (Reader) m_inRouters.get(s);
    }

    synchronized Tokenizer getInputWrapper(Reader is) {
        return (Tokenizer) m_inWrappers.get(is);
    }

   synchronized boolean getInputMode(String s) {
       return ((Boolean) m_inModes.get(s)).booleanValue();
    }

    synchronized void addOutputRouter(String s, Writer os) {
        m_outRouters.put(s, os);
    }

    synchronized void removeOutputRouter(String s) {
        m_outRouters.remove(s);
    }

    synchronized Writer getOutputRouter(String s) {
        return (Writer) m_outRouters.get(s);
    }

    synchronized PrintWriter getErrStream() {
        return getOutputRouterAsPrintWriter("WSTDERR");
    }

    synchronized PrintWriter getOutStream() {
        return getOutputRouterAsPrintWriter("WSTDOUT");
    }

    synchronized PrintWriter getOutputRouterAsPrintWriter(String s) {
        // Coerce to PrintWriter;
        PrintWriter ps;
        Writer os = getOutputRouter(s);
        if (os == null)
            ps = null;
        else if (os instanceof PrintWriter)
            ps = (PrintWriter) os;
        else {
            ps = new PrintWriter(os);
            addOutputRouter(s, ps);
        }
        return ps;
    }
}

