package jess;

import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implements the Jess functions <tt>bload</tt> and <tt>bsave</tt>.
 * <p>
 * (C) 2013 Sandia Corporation<br>
 */

class DumpFunctions extends IntrinsicPackageImpl {

    public void add(HashMap table) {
        addFunction(new Dumper(Dumper.DUMP), table);
        addFunction(new Dumper(Dumper.RESTORE), table);
    }
}

class Dumper implements Userfunction, Serializable {
    public static final int DUMP = 0, RESTORE = 1;
    private int m_cmd;

    public Dumper(int cmd) {
        m_cmd = cmd;
    }

    public String getName() {
        return m_cmd == DUMP ? "bsave" : "bload";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String filename = vv.get(1).stringValue(context);
        try {
            switch (m_cmd) {
                case DUMP:
                    // ###
                    OutputStream os =
                            new GZIPOutputStream(new FileOutputStream(filename),
                                    10000);
                    context.getEngine().bsave(os);
                    os.flush();
                    os.close();
                    return Funcall.TRUE;

                default:
                    // ###
                    InputStream is =
                            new GZIPInputStream(new FileInputStream(filename),
                                    10000);
                    context.getEngine().bload(is);
                    is.close();
                    return Funcall.TRUE;
            }
        } catch (IOException ioe) {
            throw new JessException(m_cmd == DUMP ?
                    "bsave" : "bload", "IO Exception", ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new JessException("bload", "Class not found", cnfe);
        }
    }
}


