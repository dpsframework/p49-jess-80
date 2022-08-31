package jess;

import jess.server.DebugListener;

import java.io.*;
import java.util.*;

/**
 * An interactive interface for Jess. The main() method uses this
 * class to implement a command-line interface. The Main class is also
 * displayed graphically by the jess.ConsolePanel.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class Main implements Observer {

    private Rete m_engine;
    private boolean m_exitOnError = false;
    private boolean m_fullStackTraces = false;
    private boolean m_running = false;
    private String m_filename;
    private boolean m_showWarnings;
    private boolean m_dumpContext;

    /**
     * Starts Jess as a command-line application. Create an instance of jess.Main and jess.Rete,
     * calls Main.initialize() with this method's arguments, then calls Main.execute().
     * @param argv command-line arguments
     * @see #initialize(String[], Rete)
     * @see #execute(boolean)
     */
    public static void main(String[] argv) {
        Main m = new Main();
        m.initialize(argv, new Rete());
        m.execute(m.m_filename == null);
    }


    /**
     * Display the Jess startup banner on the Rete object's standard
     * output. It will look something like
     * <p/>
     * <PRE>
     * Jess, the Rule Engine for the Java Platform
     * Copyright (C) 2006 Sandia Corporation
     * Jess Version 7.0 9/5/06. Revised 28/08/2022
     * </PRE>
     */

    public void showLogo() {
        PrintWriter outStream = m_engine.getOutStream();
        if (m_engine != null && outStream != null) {
            outStream.println("\nJess, the Rule Engine for the Java Platform");
            outStream.println(JessVersion.COPYRIGHT_STRING);
            outStream.println(JessVersion.VERSION_STRING);
            outStream.println();
            // &&&
        }
    }

    /**
     * Set a Main object up for later execution. Can take a filename argument preceded by
     * the following flags: -nologo, which surpresses the logo; -stacktrace, which
     * produces full stack traces on error; -warnings, which tells the Jess parser to
     * report warnings as well as errors; -exit, which forces the program to exit at the first error;
     * and -debugPorts, which should be followed by two numbers representing the TCP ports which
     * this application should listen on for debugger comments and send events, respectively.
     *
     * @param argv Command-line arguments
     * @param r an initialized Rete object, with routers set up
     * @return this object
     */
    public Main initialize(String[] argv, Rete r) {
        m_engine = r;

        // ###
        // Process any command-line switches
        int argIdx = 0;
        boolean doLogo = true;
        if (argv.length > 0) {
            while (argIdx < argv.length && argv[argIdx].startsWith("-")) {
                if (argv[argIdx].equals("-nologo"))  {
                    doLogo = false;
                } else if (argv[argIdx].equals("-help")) {
                	System.out.println("\nJess, the Rule Engine for the Java Platform");
                	System.out.println(JessVersion.COPYRIGHT_STRING);
                	System.out.println(JessVersion.VERSION_STRING);
                    System.out.println("\n"
                    		+ "Options: [-stacktrace] [-warnings] [-exit] "
                    		+ "[-developer] [-debugPorts Int-debugPort Int-eventPort]");
                    System.exit(0);
                } else if (argv[argIdx].equals("-stacktrace")) {
                    m_fullStackTraces = true;
                } else if (argv[argIdx].equals("-warnings")) {
                    m_showWarnings = true;
                } else if (argv[argIdx].equals("-exit")) {
                    m_exitOnError = true;
                } else if (argv[argIdx].equals("-developer")) {
                    m_showWarnings = true;
                    m_fullStackTraces = true;
                    m_dumpContext = true;
                    BindingValue.DEBUG = true;
                }  else if (argv[argIdx].equals("-debugPorts")) {
                    try {
                        int debugPort = Integer.parseInt(argv[++argIdx]);
                        int eventPort = Integer.parseInt(argv[++argIdx]);
                        setDebugMode(debugPort, eventPort);
                    } catch (NumberFormatException nfe) {
                        throw new RuntimeException("Invalid syntax for debugPorts flag");
                    }
                }
                argIdx++;
            }
        }

        // Print banner
        if (doLogo)
            showLogo();

        // ###
        // Open a file if requested
        m_filename = argv.length <= argIdx ? null : argv[argIdx];

        return this;
    }

    /**
     * Starts a debug server on the given ports. Commands are accepted on the debug port, and events
     * are sent to the event port.
     * @param debugPort the source for debug commands
     * @param eventPort the sink for debugger events
     */
    public void setDebugMode(int debugPort, int eventPort) {
        DebugListener listener = new DebugListener(m_engine);
        if (!listener.waitForConnections(debugPort, eventPort)) {
            m_engine.getErrStream().println("Could not open debug channels");
            System.exit(-1);
        }
        m_engine.addDebugListener(listener);
        listener.addObserver(this);
        m_engine.setDebug(true);
    }

    /**
     * Repeatedly parse and excute commands, from location determined
     * during initialize().
     *
     * @param doPrompt True if a prompt should be printed, false otherwise.
     *                 Prompts will never be printed during a (batch) command.
     */

    public void execute(boolean doPrompt) {
        // ###
        // Process input from file or keyboard

        PushbackReader fis = null;
        if (m_filename != null) {
            try {
                doPrompt = false;
                fis = Batch.findDocument(m_engine, m_filename);
                if (Batch.isXMLDocument(fis)) {
                    try {
                        Batch.parseXMLDocument(m_engine, fis, new ErrorHandler.DefaultHandler());
                    } finally {
                        m_filename = null;
                        fis.close();
                    }
                    return;
                }
            } catch (JessException ex) {
                displayJessException(ex);
                return;
            } catch (IOException e) {
                displayException(m_engine.getErrStream(), e);
                return;
            }
        }

        if (fis == null)
            fis = new PushbackReader(m_engine.getInputRouter("t"));
        Jesp parser = new Jesp(fis, m_engine);
        parser.setFileName(m_filename);
        if (m_showWarnings)
            parser.setIssueWarnings(true);
        m_running = true;
        Value val = Funcall.NIL;
        do {
            try {
                // Argument is 'true' for prompting, false otherwise
                val = parser.promptAndParseOneExpression(doPrompt, m_engine.getGlobalContext());

                if (m_showWarnings) {
                    List warnings = parser.getWarnings();
                    PrintWriter err = m_engine.getErrStream();
                    for (Iterator iterator = warnings.iterator(); iterator.hasNext();) {
                        ParseException exception = (ParseException) iterator.next();
                        err.println("Warning: " + exception.getMessage());
                    }
                    m_engine.getOutStream().flush();
                    parser.clearWarnings();
                }

            } catch (TerminatedException te) {
                break;

            } catch (JessException re) {
                displayJessException(re);

            } catch (Exception e) {
                m_engine.getErrStream().println("Unexpected exception:");
                displayException(m_engine.getErrStream(), e);
                if (m_exitOnError) {
                    m_engine.getErrStream().flush();
                    m_engine.getOutStream().flush();
                    System.exit(-1);
                }

            } finally {
                m_engine.getErrStream().flush();
                m_engine.getOutStream().flush();
            }

        }
        while (!val.equals(Funcall.EOF) && isRunning());
    }

    private void displayJessException(JessException re) {
        PrintWriter err = m_engine.getErrStream();
        displayException(err, re);
        if (re.getCause() != null) {
            err.write("\nNested exception is:\n");
            displayException(err, re.getCause());
        }

        if (m_exitOnError) {
            System.exit(-1);
        }
    }

    /**
     * Returns true until the debugger is commanded to exit.
     * @return true until the debug server is exiting
     */
    public synchronized boolean isRunning() {
        return m_running;
    }

    public void update(Observable o, Object arg) {
        if (DebugListener.QUIT.equals(arg)) {
            synchronized (this) {
                try {
                    m_engine.halt();
                } catch (JessException silentlyIgnore) {
                }
                m_running = false;
            }
        }
    }


    private void displayException(PrintWriter err, Throwable ex) {
        if (m_fullStackTraces) {
            ex.printStackTrace(err);
        } else {
            if (ex instanceof JessException) {
                err.println(ex.toString());
            } else
                err.println(ex.getMessage());
        }
        if (m_dumpContext && ex instanceof JessException) {
            JessException jex = (JessException) ex;
            if (jex.getExecutionContext() != null) {
                Context c = jex.getExecutionContext();
                if (c.getToken() != null) {
                    err.println("Looking at token :");
                    err.println(c.getToken());
                }
                if (c.getFact() != null) {
                    err.println("Matching fact :");
                    err.println(c.getFact());
                }
            }
        }


        err.flush();
    }
}




