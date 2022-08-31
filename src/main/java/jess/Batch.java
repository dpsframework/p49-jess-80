package jess;

import jess.xml.JessSAXParser;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.nio.charset.Charset;

/**
 * This class implements the "batch" command, and several static
 * methods are available that you can call to load Jess code.
 * <p/>
 * (C) 2007 Sandia National Laboratories<br>
 *
 * @see Rete#batch
 */

public class Batch implements Userfunction, Serializable {
    private static String s_defaultCharset;

    public String getName() {
        return "batch";
    }

    /**
     * Execute a file of Jess code, either in the Jess rule language or in JessML. The filename
     * will be looked for first on the file system; if it's not found, Jess will search the
     * classpath. If Jess is used in an Applet, the applet's document base will be checked first.
     * The code will be evaluated in the Rete object's global execution context. Any errors
     * will terminate parsing.
     *
     * @param filename the name of a file, or a relative or absolute path
     * @param engine   the Rete object to load with the constructs from the file
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     */
    public static Value batch(String filename, Rete engine)
            throws JessException {
        return batch(filename, engine, engine.getGlobalContext());
    }

    /**
     * Execute a file of Jess code, either in the Jess rule language or in JessML. The filename
     * will be looked for first on the file system; if it's not found, Jess will search the
     * classpath. If Jess is used in an Applet, the applet's document base will be checked first.
     * The code will be evaluated in the Rete object's global execution context. Any errors
     * will terminate parsing. You must specify the name of the charset to use.
     *
     * @param filename the name of a file, or a relative or absolute path
     * @param charset  the name of the character set to use in interpreting the file
     * @param engine   the Rete object to load with the constructs from the file
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     */
    public static Value batch(String filename, String charset, Rete engine)
            throws JessException {
        return batch(filename, charset, engine, engine.getGlobalContext());
    }

    /**
     * Execute a file of Jess code, either in the Jess rule language or in JessML. The filename
     * will be looked for first on the file system; if it's not found, Jess will search the
     * classpath. If Jess is used in an Applet, the applet's document base will be checked first.
     * The code will be evaluated in the given execution context. Any errors will terminate parsing.
     *
     * @param filename the name of a file, or a relative or absolute path
     * @param engine   the Rete object to load with the constructs from the file
     * @param context  the execution context to use
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     */
    public static Value batch(String filename, Rete engine, Context context)
            throws JessException {
        return batch(filename, engine, context, new ErrorHandler.DefaultHandler());
    }

    /**
     * Execute a file of Jess code, either in the Jess rule language or in JessML. The filename
     * will be looked for first on the file system; if it's not found, Jess will search the
     * classpath. If Jess is used in an Applet, the applet's document base will be checked first.
     * The code will be evaluated in the given execution context. Any errors will terminate parsing.
     *
     * @param filename the name of a file, or a relative or absolute path
     * @param charset  the name of the character set to use in interpreting the file
     * @param engine   the Rete object to load with the constructs from the file
     * @param context  the execution context to use
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     */
    public static Value batch(String filename, String charset, Rete engine, Context context)
            throws JessException {
        return batch(filename, charset, engine, context, new ErrorHandler.DefaultHandler());
    }

    /**
     * Execute a file of Jess code, either in the Jess rule language or in JessML. The filename
     * will be looked for first on the file system; if it's not found, Jess will search the
     * classpath. If Jess is used in an Applet, the applet's document base will be checked first.
     * The code will be evaluated in the given execution context. If any errors are encountered, the
     * given error handler will be invoked. If the error handler rethrows the exception, parsing
     * will terminate; otherwise parsing will continue.
     *
     * @param filename the name of a file, or a relative or absolute path
     * @param engine   the Rete object to load with the constructs from the file
     * @param context  the execution context to use
     * @param handler  notified in case of exceptions
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     * @see ErrorHandler
     */
    public static Value batch(String filename, Rete engine, Context context, ErrorHandler handler) throws JessException {
        return batch(filename, defaultCharset(), engine, context, handler);
    }

    public static Value batch(String filename, String charset, Rete engine, Context context, ErrorHandler handler)
            throws JessException {

        Value v = Funcall.FALSE;
        PushbackReader fis = null;
        try {
            fis = findDocument(engine, filename, charset);

            if (isXMLDocument(fis)) {
                parseXMLDocument(engine, fis, handler);
                v = Funcall.TRUE;
            } else {
                Jesp j = new Jesp(fis, engine);
                j.setFileName(filename);
                while (true) {
                    try {
                        v = j.parse(false, context);
                        break;
                    } catch (JessException ex) {
                        handler.handleError(ex);
                    }
                }
            }

        } catch (IOException ex) {
            handler.handleError(new JessException("batch", "I/O Exception", "", filename, ex));

        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException ioe) {
            }
        }
        return v;
    }

    public static Value batch(Reader reader, Rete engine, Context context, ErrorHandler handler) throws JessException {
        PushbackReader fis = new PushbackReader(reader);
        Value v = Funcall.NIL;
        Jesp j = new Jesp(fis, engine);
        j.setFileName("<reader>");
        while (true) {
            try {
                v = j.parse(false, context);
                break;
            } catch (JessException ex) {
                handler.handleError(ex);
            }
        }
        return v;
    }

    static void parseXMLDocument(Rete engine, PushbackReader fis, ErrorHandler handler) throws JessException {
        JessSAXParser parser = new JessSAXParser(engine);
        while (true) {
            try {
                parser.parse(new InputSource(fis));
                break;
            } catch (JessException ex) {
                handler.handleError(ex);
            }
        }
    }

    /**
     * Searches for the named file of Jess code, opens it if found and returns a Reader for the data.
     * This method implements the algorithm used by batch and by {@link jess.Main} to find
     * named source code files.<P>
     * If Jess is running in an applet, this method will look for the named file at the applet's document base.<p>
     * If not in an applet, the method will first assume the argument is a relative path on the local file system.<p>
     * If the file does not exist, Jess will attempt to use {@link jess.Rete#getResource(String)} to find the file.<p>
     * Failing that, the method will throw an exception. Uses the platform default character set.
     *
     * @param engine   the active rule engine
     * @param filename the path to a file of Jess code
     * @return an open Reader to return the file's data
     * @throws JessException if all attempts to find the file fail
     * @throws IOException   if the file is found but an error occurs on opening it
     */
    public static PushbackReader findDocument(Rete engine, String filename)
            throws JessException, IOException {
        return findDocument(engine, filename, defaultCharset());
    }


    // Ugh. No other way to do this on JDK 1.4, I'm afraid.
    private static String defaultCharset() {
        synchronized ("JESS_FIND_CHARSET") {
            if (s_defaultCharset == null)
                s_defaultCharset = new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
            return s_defaultCharset;
        }
    }

    /**
     * Searches for the named file of Jess code, opens it if found and returns a Reader for the data.
     * This method implements the algorithm used by batch and by {@link jess.Main} to find
     * named source code files.<P>
     * If Jess is running in an applet, this method will look for the named file at the applet's document base.<p>
     * If not in an applet, the method will first assume the argument is a relative path on the local file system.<p>
     * If the file does not exist, Jess will attempt to use {@link jess.Rete#getResource(String)} to find the file.<p>
     * Failing that, the method will throw an exception.
     *
     * @param engine   the active rule engine
     * @param filename the path to a file of Jess code
     * @param charset  the name of the character set used in the file
     * @return an open Reader to return the file's data
     * @throws JessException if all attempts to find the file fail
     * @throws IOException   if the file is found but an error occurs on opening it
     */

    public static PushbackReader findDocument(Rete engine, String filename, String charset)
            throws JessException, IOException {
        PushbackReader fis;
        try {
            if (engine.getDocumentBase() == null)
                fis = new PushbackReader(new InputStreamReader(new FileInputStream(filename), charset));
            else {
                URL url = new URL(engine.getDocumentBase(),
                        filename);
                fis = new PushbackReader(new InputStreamReader(url.openStream(), charset));
            }
        } catch (Exception e) {
            // Try to find a resource file, too.
            URL u = engine.getResource(filename);
            if (u == null)
                throw new JessException("batch", "Cannot open file", e);

            InputStream is = u.openStream();
            fis = new PushbackReader(new InputStreamReader(is, charset));
        }
        return fis;
    }

    /**
     * Returns true if the first character of the file is a "&lt;", so that it's possible the file contains XML.
     * Such a file can't be Jess code.
     *
     * @param fis the open file
     * @return true if the file looks like XML rather than Jess code
     * @throws IOException if anything goes wrong
     */
    public static boolean isXMLDocument(PushbackReader fis) throws IOException {
        int c = fis.read();
        if (c != -1)
            fis.unread(c);
        return (c == '<');
    }

    /**
     * The implementation of the Jess language "batch" command, which just calls {@link #batch}.
     *
     * @param vv      the function call
     * @param context the execution context
     * @return the last expression parsed
     * @throws JessException if anything goes wrong
     */
    public Value call(ValueVector vv, Context context) throws JessException {
        if (vv.size() < 3) {
            String filename = vv.get(1).stringValue(context);
            return batch(filename, context.getEngine(), context);
        } else {
            String filename = vv.get(1).stringValue(context);
            String charset = vv.get(2).stringValue(context);
            return batch(filename, charset, context.getEngine(), context);
        }
    }
}
