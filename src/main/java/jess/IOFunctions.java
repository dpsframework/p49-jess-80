package jess;

import jess.xml.JessSAXParser;
import jess.xml.JessSAXHandler;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

/**
 * Userfunctions related to I/O.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class IOFunctions extends IntrinsicPackageImpl {
    public void add(HashMap ht) {
        addFunction(new Open(), ht);
        addFunction(new Close(), ht);
        addFunction(new Read(), ht);
        addFunction(new Readline(), ht);
        addFunction(new LoadFacts(), ht);
        addFunction(new SaveFacts(), ht);
        addFunction(new SaveFactsXML(), ht);
        Printout p = new Printout();
        addFunction(p, ht);
        addFunction(new SetMultithreadedIO(p), ht);
        addFunction(new GetMultithreadedIO(p), ht);
    }
}

class SetMultithreadedIO implements Userfunction, Serializable {
    private final Printout m_printout;

    SetMultithreadedIO(Printout p) {
        m_printout = p;
    }

    public String getName() {
        return "set-multithreaded-io";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        boolean tmp = m_printout.isMultithreadedIO();
        m_printout.setMultithreadedIO(!(vv.get(1).equals(Funcall.FALSE)));
        return tmp ? Funcall.TRUE : Funcall.FALSE;
    }
}

class GetMultithreadedIO implements Userfunction, Serializable {
    private final Printout m_printout;

    GetMultithreadedIO(Printout p) {
        m_printout = p;
    }

    public String getName() {
        return "get-multithreaded-io";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        return m_printout.isMultithreadedIO() ? Funcall.TRUE : Funcall.FALSE;
    }
}


class Printout implements Userfunction, Serializable {
    private boolean m_multithreadedIO = false;
    private static final String NEWLINE = System.getProperty("line.separator");

    public String getName() {
        return "printout";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String routerName = vv.get(1).stringValue(context);
        Writer os = context.getEngine().getOutputRouter(routerName);
        if (os == null)
            throw new JessException("printout",
                                    "printout: bad router",
                                    routerName);

        StringBuffer sb = new StringBuffer(100);
        for (int i = 2; i < vv.size(); i++) {
            Value v = vv.get(i).resolveValue(context);
            switch (v.type()) {
                case RU.SYMBOL:
                    //noinspection EqualsBetweenInconvertibleTypes
                    if (v.equals("crlf")) {
                        sb.append(NEWLINE);
                        break;
                    }

                    // FALL THROUGH
                case RU.STRING:
                    sb.append(v.stringValue(context));
                    break;
                case RU.INTEGER:
                    sb.append(v.intValue(context));
                    break;
                case RU.FLOAT:
                    sb.append(v.numericValue(context));
                    break;
                case RU.FACT:
                    sb.append(v);
                    break;
                case RU.LIST:
                    sb.append(v.listValue(context).
                              toStringWithParens());
                    break;
                case RU.JAVA_OBJECT:
                    sb.append(v.toString());
                    break;
                default:
                    sb.append(v.toString());
            }

        }
        try {
            os.write(sb.toString());
            if (m_multithreadedIO)
                PrintThread.getPrintThread().assignWork(os);
            else
                os.flush();
        } catch (IOException ioe) {
            throw new JessException("printout", "I/O Exception", ioe);
        }

        return Funcall.NIL;
    }

    boolean isMultithreadedIO() {
        return m_multithreadedIO;
    }

    void setMultithreadedIO(boolean multithreadedIO) {
        m_multithreadedIO = multithreadedIO;
    }
}


class Open implements Userfunction, Serializable {
    public String getName() {
        return "open";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();

        // Obtain parameters
        String filename = vv.get(1).stringValue(context);
        String router = vv.get(2).stringValue(context);
        String access = "r";
        if (vv.size() > 3)
            access = vv.get(3).stringValue(context);

        try {
            if (access.equals("r")) {
                engine.addInputRouter(router,
                                      new BufferedReader(new FileReader(filename)),
                                      false);

            } else if (access.equals("w")) {
                engine.addOutputRouter(router,
                                       new BufferedWriter(new FileWriter(filename)));

            } else if (access.equals("a")) {
                RandomAccessFile raf = new RandomAccessFile(filename, "rw");
                raf.seek(raf.length());
                FileWriter fos = new FileWriter(raf.getFD());
                engine.addOutputRouter(router, new BufferedWriter(fos));
            } else
                throw new JessException("open", "Unsupported access mode",
                                        access);

        } catch (IOException ioe) {
            throw new JessException("open", "I/O Exception", ioe);
        }
        return new Value(router, RU.SYMBOL);
    }
}

class Close implements Userfunction, Serializable {
    public String getName() {
        return "close";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        if (vv.size() > 1)
            for (int i = 1; i < vv.size(); i++) {
                Writer os;
                Reader is;
                String router = vv.get(i).stringValue(context);
                try {
                    if ((os = engine.getOutputRouter(router)) != null) {
                        os.close();
                        engine.removeOutputRouter(router);
                    }
                } catch (IOException ioe) {
                }
                try {
                    if ((is = engine.getInputRouter(router)) != null) {
                        is.close();
                        engine.removeInputRouter(router);
                    }
                } catch (IOException ioe) {
                }
            }
        else
            throw new JessException("close", "Must close files by name", "");

        return Funcall.TRUE;
    }
}

class Read implements Userfunction, Serializable {

    public String getName() {
        return "read";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        // Find input source
        String routerName = "t";

        if (vv.size() > 1)
            routerName = vv.get(1).stringValue(context);

        Rete engine = context.getEngine();
        Tokenizer t = engine.getInputWrapper(engine.getInputRouter(routerName));

        if (t == null)
            throw new JessException("read", "bad router", routerName);
        JessToken jt = t.nextToken();

        // Console-like streams read a token, then throw away to newline.
        if (engine.getInputMode(routerName))
            t.discardToEOL();

        return jt.rawValueOf(context);
    }

}

class Readline implements Userfunction, Serializable {
    public String getName() {
        return "readline";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String routerName = "t";

        if (vv.size() > 1)
            routerName = vv.get(1).stringValue(context);

        Rete engine = context.getEngine();
        Tokenizer t = engine.getInputWrapper(engine.getInputRouter(routerName));

        String line = t.readLine();
        if (line == null)
            return Funcall.EOF;
        else
            return new Value(line, RU.STRING);
    }
}

class LoadFacts implements Userfunction, Serializable {
    public String getName() {
        return "load-facts";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Reader fis;
        Rete engine = context.getEngine();
        String filename = vv.get(1).stringValue(context);
        // ###
        try {
            if (engine.getDocumentBase() == null)
                fis = new FileReader(filename);
            else {
                URL url = new URL(engine.getDocumentBase(),
                                  filename);
                fis = new InputStreamReader(url.openStream());
            }
        } catch (Exception e) {
            try {
                // Try to find a resource file, too.
                URL u = engine.getResource(filename);
                if (u == null)
                    throw new JessException("load-facts",
                                            "Cannot open file", e);
                InputStream is = u.openStream();
                fis = new InputStreamReader(is);

            } catch (IOException ioe) {
                throw new JessException("load-facts",
                        "Network error", ioe);
            }
        }
        PushbackReader reader = new PushbackReader(fis);
        try {
            if (Batch.isXMLDocument(reader)) {
                JessSAXParser parser = new JessSAXParser(engine, JessSAXHandler.FACTLIST);
                parser.parse(new InputSource(reader));
                return Funcall.TRUE;

            } else {
                Jesp jesp = new Jesp(reader, context.getEngine());
                jesp.setFileName(filename);
                return jesp.loadFacts(context);
            }
        } catch (IOException e) {
            throw new JessException("load-facts", "I/O error", e);
        }
    }
}

class SaveFacts implements Userfunction, Serializable {
    public String getName() {
        return "save-facts";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Writer f;
        Rete engine = context.getEngine();
        if (engine.getDocumentBase() == null) {
            try {
                f = new FileWriter(vv.get(1).stringValue(context));
            } catch (IOException t) {
                throw new JessException(getName(), "I/O Exception", t);
            }

        } else {
            try {
                URL url =
                        new URL(engine.getDocumentBase(),
                                vv.get(1).stringValue(context));
                URLConnection urlc = url.openConnection();
                urlc.setDoOutput(true);
                f = new OutputStreamWriter(urlc.getOutputStream());

            } catch (Exception t) {
                throw new JessException(getName(), "Network error", t);
            }
        }

        try {
            try {
                // ###
                if (vv.size() > 2) {
                    for (int i = 2; i < vv.size(); i++)
                        engine.ppFacts(vv.get(i).stringValue(context), f);

                } else {
                    engine.ppFacts(f);
                }
            } finally {
                f.close();
            }

        } catch (IOException ioe) {
            throw new JessException(getName(), "I/O Exception", ioe);
        }
        return Funcall.TRUE;
    }
}

class SaveFactsXML implements Userfunction, Serializable {
    public String getName() {
        return "save-facts-xml";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        if (vv.size() > 3) {
            throw new JessException("save-facts-xml", "Only one template name argument is allowed, you gave", vv.size()-2);
        }

        Writer f;
        Rete engine = context.getEngine();
        if (engine.getDocumentBase() == null) {
            try {
                f = new FileWriter(vv.get(1).stringValue(context));
            } catch (IOException t) {
                throw new JessException(getName(), "I/O Exception", t);
            }

        } else {
            try {
                URL url =
                        new URL(engine.getDocumentBase(),
                                vv.get(1).stringValue(context));
                URLConnection urlc = url.openConnection();
                urlc.setDoOutput(true);
                f = new OutputStreamWriter(urlc.getOutputStream());

            } catch (Exception t) {
                throw new JessException(getName(), "Network error", t);
            }
        }

        try {
            try {
                // ###
                if (vv.size() > 2) {
                    engine.ppFacts(vv.get(2).stringValue(context), f, true);

                } else {
                    engine.ppFacts(f, true);
                }
            } finally {
                f.close();
            }

        } catch (IOException ioe) {
            throw new JessException(getName(), "I/O Exception", ioe);
        }
        return Funcall.TRUE;
    }
}
