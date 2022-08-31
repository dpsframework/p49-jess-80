package jess.server;

import jess.*;

import java.io.*;
import java.util.*;

/**
 * This class is part of the implementation of the JessDE
 * debugger. It's responsible for handling breakpoints and other
 * debugger commands.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class DebugListener extends Observable implements JessListener, Runnable {

    static final String STARTED = "STARTED";
    public static final String QUIT = "QUIT";
    static final String SUSPEND = "SUSPENDED";
    static final String RESUME = "RESUMED";
    static final String RESUME_STEP = "RESUMED_STEP";
    static final String SUSPEND_STEP = "SUSPENDED_STEP";
    static final String SUSPENDED_BREAK = "SUSPENDED_BREAK";
    static final String STACK = "STACK";
    static final String ERROR = "ERROR";
    static final String OK = "OK";
    static final String PRINT = "PRINT";
    static final String FACT = "FACT";

    private PrintWriter m_eventSink, m_cmdSink;
    private BufferedReader m_cmdSource;
    private final Map m_breakpoints = Collections.synchronizedMap(new HashMap());
    private final Map m_contexts = Collections.synchronizedMap(new HashMap());
    private final Map m_threads = Collections.synchronizedMap(new HashMap());
    private final Map m_states = Collections.synchronizedMap(new HashMap());
    private Rete m_engine;
    private Thread m_thread;
    private boolean m_listeningForEvents = true;
    private boolean m_terminated;
    private String m_pushBack;
    private ThreadSerialNumber m_threadIds = new ThreadSerialNumber();
    private static final boolean DEBUG = false;
    private static final String THREAD_CREATE = "THREAD_CREATE";
    private static final String THREAD_DEAD = "THREAD_DEAD";

    DebugListener(Reader input, Writer output, Writer events, Rete engine) {
        m_engine = engine;
        setCmdSource(input);
        setCmdSink(output);
        setEventSink(events);
        start();
    }

    public DebugListener(Rete engine) {
        m_engine = engine;
    }

    /**
     * DebugListener listens for special debug-mode JessEvents that signal Userfunctions
     * being called. Public due to interface implementation requirements; not for client use.
     *
     * @param je a JessEvent
     * @throws JessException if anything goes wrong or the debugger is terminated
     */

    public void eventHappened(JessEvent je) throws JessException {
        if (isTerminated())
            throw new TerminatedException("DebugListener", "Quit by user command", QUIT);

        if (Thread.currentThread() == m_thread) {
            return;
        }

        if (isListeningForEvents()) {
            respond(je);
        }
    }

    private void recordContext(JessEvent je) {
        synchronized (m_contexts) {
            int thread = m_threadIds.get();
            Context context = je.getContext();

            if (context != null) {
                setContext(thread, Thread.currentThread(), context);
            }
        }
    }

    private void respond(JessEvent je) throws JessException {
        int thread = m_threadIds.get();
        switch (je.getType()) {
            case JessEvent.USERFUNCTION_CALLED:
                sendRemark("About to call " + je.getObject() + " in " + thread + "(" + getState(thread) + ")");
                synchronized (m_states) {
                    recordContext(je);
                    checkForBreakpointHit(je, thread);
                    checkForStepping(thread);

                    while (isSuspended(thread)) {
                        try {
                            m_states.wait(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                break;

            case JessEvent.USERFUNCTION_RETURNED:
                sendRemark("Just called " + je.getObject() + " in " + thread);
                synchronized (m_states) {
                    recordContext(je);
                    checkForExit(je);
                    if (isSteppingOverThis(thread, je)) {
                        setState(thread, ThreadState.SUSPENDED);
                        sendEvent(SUSPEND_STEP + " " + thread);
                    }
                }
                break;

            default:
                /* Nothing */
        }
    }

    private void checkForStepping(int thread) {
        synchronized (m_states) {
            if (isStepping(thread)) {
                setState(thread, ThreadState.SUSPENDED);
                sendEvent(SUSPEND_STEP + " " + thread);
            }
        }
    }

    private boolean isSteppingOverThis(int thread, JessEvent je) {
        synchronized (m_states) {
            ThreadState state = getState(thread);
            if (state.isSteppingOver()) {
                return state.getStepOverTarget() == je.getObject();
            } else
                return false;
        }
    }

    private ThreadState getState(int thread) {
        synchronized (m_states) {
            Integer key = new Integer(thread);

            if (m_states.get(key) == null) {
                m_states.put(key, getInitialThreadState(thread));
            }
            return (ThreadState) m_states.get(key);
        }
    }

    private ThreadState getInitialThreadState(int thread) {
        switch (thread) {
            case 0:
                return ThreadState.SUSPENDED;
            default:
                return ThreadState.RUNNING;
        }
    }

    private void checkForExit(JessEvent je) {
        Funcall funcall = (Funcall) je.getObject();
        String functionName = funcall.getName();
        if (functionName.equals("exit")) {
            terminate();
        }
    }

    private void terminate() {
        synchronized (m_states) {
            setTerminated(true);
            setListeningForEvents(false);
            sendEvent(QUIT);
            for (Iterator it = m_states.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                m_states.put(key, ThreadState.RUNNING);
            }
            m_states.notifyAll();
        }
    }

    private void checkForBreakpointHit(JessEvent je, int thread) throws JessException {
        synchronized (m_states) {
            if (isSteppingOver(thread))
                return;

            Context context = je.getContext();
            LineNumberRecord lnr = context.getLineNumberRecord();
            if (lnr != null) {
                if (shouldBreakAt(lnr.getFileName(), lnr.getLineno())) {
                    StringWriter output = new StringWriter();
                    PrintWriter out = new PrintWriter(output);
                    out.print(SUSPENDED_BREAK + " ");
                    out.print(thread + " ");
                    out.print(lnr.getLineno() + " ");
                    out.print(lnr.getFileName().length());
                    out.print('\n');
                    out.print(lnr.getFileName());
                    out.flush();
                    setState(thread, ThreadState.SUSPENDED);
                    sendEvent(output.toString());
                }
            }
        }
    }

    private boolean isSteppingOver(int thread) {
        synchronized (m_states) {
            ThreadState state = getState(thread);
            return state.isSteppingOver();
        }
    }

    private boolean isSuspended(int thread) {
        synchronized (m_states) {
            ThreadState state = getState(thread);
            return state.isSuspended();
        }
    }

    private boolean isStepping(int thread) {
        synchronized (m_states) {
            ThreadState state = getState(thread);
            return state.isStepping();
        }
    }

    private void setState(int thread, ThreadState state) {
        synchronized (m_states) {
            sendRemark("Thread " + thread + " state changed from " + getState(thread) + " to " + state);
            m_states.put(new Integer(thread), state);
            m_states.notifyAll();
        }
    }

    private void execute(String command) {
        sendRemark("executing command " + command);
        try {
            if (command.equals("quit")) {
                terminate();
            } else if (command.startsWith("join ")) {
                handleJoinCommand(command);
            } else if (command.startsWith("wait ")) {
                handleWaitCommand(command);
            } else if (command.startsWith("suspend ")) {
                handleSuspendCommand(command);
            } else if (command.startsWith("resume ")) {
                handleResumeCommand(command);
            } else if (command.startsWith("step ")) {
                handleStepCommand(command);
            } else if (command.startsWith("stepo ")) {
                handleStepOverCommand(command);
            } else if (command.startsWith("stack ")) {
                handleStackCommand(command);
            } else if (command.startsWith("print ")) {
                handlePrintCommand(command);
            } else if (command.startsWith("break ")) {
                handleBreakCommand(command);
            } else if (command.startsWith("unbreak ")) {
                handleUnbreakCommand(command);
            } else if (command.startsWith("fact ")) {
                handleFactCommand(command);
            } else if (command.startsWith("activation")) {
                handleActivationCommand(command);
            } else if (command.startsWith("agenda")) {
                handleAgendaCommand(command);
            }
            sendResponse(OK);

        } catch (JessException ex) {
            sendResponse(ERROR + ": " + ex.getMessage());
        }
    }

    private void handleWaitCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleWaitCommand");
        getContext(threadId);
    }

    private void handleJoinCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleJoinCommand");
        synchronized (m_states) {
            try {
                while (!isSuspended(threadId)) {
                    m_states.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleStepOverCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleStepOverCommand");
        synchronized (m_states) {
            if (isSuspended(threadId)) {
                Context context = getContext(threadId);
                if (context != null) {
                    synchronized (context) {

                        Funcall funcall = getFuncall(context);
                        setState(threadId, ThreadState.makeSteppingOver(funcall));
                    }
                    sendEvent(RESUME_STEP + " " + threadId);
                }
            }
        }
    }

    private Funcall getFuncall(Context context) throws JessException {
        synchronized (context) {
            while (context.getFuncall() == null) {
                try {
                    context.wait(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return context.getFuncall();
    }

    private void setContext(int threadId, Thread thread, Context context) {
        synchronized (m_contexts) {
            Integer key = new Integer(threadId);
            if (threadId > 0 && m_contexts.get(key) == null) {
                sendEvent(THREAD_CREATE + " " + threadId);
            }
            m_contexts.put(key, context);
            m_threads.put(key, thread);
            m_contexts.notifyAll();
        }
    }

    private Context getContext(int threadId) {
        synchronized (m_contexts) {
            try {
                Integer key = new Integer(threadId);
                Thread thread = (Thread) m_threads.get(key);
                if (thread != null && !thread.isAlive()) {
                    destroyThread(threadId);
                    return null;
                }

                while (m_contexts.get(key) == null)
                    if (thread == null)
                        m_contexts.wait();
                return (Context) m_contexts.get(key);

            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    private void destroyThread(int threadId) {
        setState(threadId, ThreadState.DEAD);
        sendEvent(THREAD_DEAD + " " + threadId);
    }

    private void handleStepCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleStepCommand");
        Context context = getContext(threadId); // Wait for thread to actually show up
        if (context != null) {
            synchronized (m_states) {
                if (isSuspended(threadId)) {
                    setState(threadId, ThreadState.STEPPING);
                    sendEvent(RESUME_STEP + " " + threadId);
                }
            }
        }
    }

    private void handleResumeCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleResumeCommand");
        setState(threadId, ThreadState.RUNNING);
        sendEvent(RESUME + " " + threadId);
    }

    private void handleSuspendCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleSuspendCommand");
        setState(threadId, ThreadState.SUSPENDED);
        sendEvent(SUSPEND + " " + threadId);
    }

    private void handleAgendaCommand(String command) throws JessException {
        StringTokenizer tokens = new StringTokenizer(command, " \t");
        if (tokens.countTokens() > 3)
            throw new JessException("handleAgendaCommand", "Wrong number of arguments", tokens.countTokens());
        String agendaTokenIgnored = tokens.nextToken();
        String countString = tokens.nextToken();
        String module;
        if (tokens.countTokens() == 3)
            module = tokens.nextToken();
        else
            module = m_engine.getFocus();

        try {
            int requested = Integer.parseInt(countString);
            Iterator it = m_engine.listActivations(module);
            StringWriter output = new StringWriter();
            PrintWriter out = new PrintWriter(output, true);

            int count = 0;
            while (count < requested && it.hasNext()) {
                Activation a = (Activation) it.next();
                printActivation(a, out);
                ++count;
            }
            StringWriter output2 = new StringWriter();
            PrintWriter out2 = new PrintWriter(output2, true);
            out2.print("AGENDA ");
            out2.print(module);
            out2.print(" ");
            out2.print(count);
            out2.print('\n');
            out2.print(output.toString().trim());
            out2.flush();
            sendEvent(output2.toString());

        } catch (NumberFormatException nfe) {
            throw new JessException("handleAgendaCommand", "Bad argument", countString);
        }
    }

    private void handleActivationCommand(String command) throws JessException {
        StringTokenizer tokens = new StringTokenizer(command, " \t");
        if (tokens.countTokens() != 1)
            throw new JessException("handleActivationCommand", "Wrong number of arguments", tokens.countTokens());
        Activation activation = m_engine.getThisActivation();
        if (activation == null) {
            sendEvent("ACTIVATION NONE");
        } else {
            Writer contents = new StringWriter();
            printActivation(activation, new PrintWriter(contents));
            sendEvent("ACTIVATION " + contents.toString().trim());
        }
    }

    private void printActivation(Activation activation, PrintWriter out) throws JessException {
        out.print(activation.getRule().getName());
        Token token = activation.getToken();
        final int size = token.size();
        for (int i = 0; i < size; ++i) {
            out.print(" ");
            out.print(new FactIDValue(token.fact(i)));
        }
        out.print('\n');
    }

    private void handleFactCommand(String command) throws JessException {
        StringTokenizer tokens = new StringTokenizer(command, " \t");
        if (tokens.countTokens() != 2)
            throw new JessException("handleFactCommand", "Wrong number of arguments", tokens.countTokens());
        String factTokenIgnored = tokens.nextToken();
        String factNumberString = tokens.nextToken();
        int factNumber;
        try {
            factNumber = Integer.parseInt(factNumberString);
            Fact fact = m_engine.findFactByID(factNumber);
            if (fact == null) {
                sendEvent(FACT + " " + factNumber + " (retracted) " + 0 + '\n');
                return;
            }

            Deftemplate template = fact.getDeftemplate();
            int slotCount = template.getNSlots();
            StringWriter output = new StringWriter();
            PrintWriter out = new PrintWriter(output);
            out.print(FACT + " " + factNumber + " " + fact.getName() + " " + slotCount);
            out.print('\n');
            for (int i = 0; i < slotCount; ++i) {
                String slotValue = fact.get(i).toString();
                String slotName = template.getSlotName(i);
                out.print(slotName + ' ' + String.valueOf(slotValue.length()));
                out.print('\n');
                out.write(slotValue);
                out.flush();
            }
            sendEvent(output.toString().trim());

        } catch (NumberFormatException nfe) {
            throw new JessException("handleFactCommand", "Bad argument", factNumberString);
        }
    }

    /**
     * The implementation of the Runnable interface. The DebugListener should be run in
     * a dedicated thread by calling "start()".
     */
    public void run() {
        sendEvent(STARTED);
        try {
            while (isListeningForEvents()) {
                try {
                    String command = readCommand();

                    if (command != null) {
                        execute(command);
                    } else {
                        terminate();
                        break;
                    }
                    Thread.yield();

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            m_thread = null;
        }
    }

    public void start() {
        if (m_thread == null) {
            m_thread = new Thread(this);
            m_thread.setDaemon(true);
            m_thread.start();
        }
    }

    public void join() throws InterruptedException {
        if (m_thread != null)
            m_thread.join();
    }

    static class BreakCommandData {
        int line;
        String file;

        BreakCommandData(String aFile, int aLine) {
            file = aFile;
            line = aLine;
        }
    }

    void handleBreakCommand(String command) throws JessException {
        BreakCommandData data = parseBreakCommand(command);
        addBreakpoint(data.file, data.line);
    }

    private BreakCommandData parseBreakCommand(String command) throws JessException {
        String lineNumberToken = null;
        String file;

        try {
            // Strip off command token
            String args = command.substring(command.indexOf(' ') + 1);
            lineNumberToken = args.substring(0, args.indexOf(' '));
            file = args.substring(args.indexOf(' ') + 1);
            int line = Integer.parseInt(lineNumberToken);
            return new BreakCommandData(file, line);
        } catch (NumberFormatException e) {
            throw new JessException("parseBreakCommand", "Invalid line number", lineNumberToken);
        } catch (StringIndexOutOfBoundsException e) {
            throw new JessException("parseBreakCommand", "Wrong number of arguments", command);
        }
    }

    private void handleUnbreakCommand(String command) throws JessException {
        BreakCommandData data = parseBreakCommand(command);
        removeBreakpoint(data.file, data.line);
    }

    static class PrintableData {
        String expr;
        int thread;
        int level;

        PrintableData(int aThread, int aLevel, String exprValue) {
            thread = aThread;
            level = aLevel;
            expr = exprValue;
        }
    }

    void handlePrintCommand(String command) throws JessException {
        PrintableData data = parsePrintCommand(command);
        if (!isSuspended(data.thread))
            return;

        String result = ERROR;
        int pointer = data.level;
        Context context = getContext(data.thread);
        if (context != null) {
            context = findRequestedStackFrame(context, pointer);
            Rete engine = context.getEngine();
            synchronized (this) {
                try {
                    setListeningForEvents(false);
                    result = engine.eval(data.expr, context).toStringWithParens();
                } finally {
                    setListeningForEvents(true);
                }
            }
        }
        StringWriter output = new StringWriter();
        PrintWriter out = new PrintWriter(output);
        out.print(PRINT + " " + data.thread + " ");
        out.print(data.level + " " + data.expr + " " + result.length());
        out.print('\n');
        out.print(result);
        out.flush();
        sendEvent(output.toString());
    }

    private Context findRequestedStackFrame(Context context, int pointer) throws JessException {
        do {
            if (context != null) {
                int nFramesInContext = context.getStackData().size();
                if (nFramesInContext == 0)
                    return context;
                if (pointer < nFramesInContext)
                    return context;
                else {
                    pointer -= nFramesInContext;
                    context = context.getParent();
                }
            }
        } while (pointer > 0 && context != null);
        return context;
    }

    PrintableData parsePrintCommand(String command) throws JessException {
        StringTokenizer tokens = new StringTokenizer(command, " \t");
        if (tokens.countTokens() != 4)
            throw new JessException("parsePrintData", "Wrong number of arguments", tokens.countTokens());
        String printTokenIgnored = tokens.nextToken();
        int thread, level, count;
        String threadToken = tokens.nextToken();
        String levelToken = tokens.nextToken();
        try {
            try {
                thread = Integer.parseInt(threadToken);
            } catch (NumberFormatException e) {
                throw new JessException("parsePrintData", "Thread id not an integer", levelToken);
            }
            try {
                level = Integer.parseInt(levelToken);
            } catch (NumberFormatException e) {
                throw new JessException("parsePrintData", "Stack level not an integer", levelToken);
            }
            String charCountToken = tokens.nextToken();
            try {
                count = Integer.parseInt(charCountToken);
            } catch (NumberFormatException e) {
                throw new JessException("parsePrintData", "Char count not an integer", charCountToken);
            }
            char[] buffer = new char[count];
            synchronized (m_cmdSource) {
                m_cmdSource.read(buffer);
            }
            return new PrintableData(thread, level, new String(buffer));
        } catch (IOException e) {
            throw new JessException("parsePrintCommand", "I/O Error", e);
        }
    }

    private void handleStackCommand(String command) throws JessException {
        int threadId = getThreadId(command, "handleStackCommand");
        if (!isSuspended(threadId))
            return;

        Context context = getContext(threadId);
        String firstLine = STACK + " " + threadId + " ";
        StringBuffer result = new StringBuffer();

        while (context != null) {
            Stack stack = context.getStackData();
            for (int i = stack.size() - 1; i > -1; --i) {
                String fileName = "<unknown>";
                String functionName = "<unknown>";
                int lineNumber = 0;
                StackFrame frame = (StackFrame) stack.get(i);
                Funcall funcall = frame.getFuncall();
                LineNumberRecord lineNumberRecord = frame.getLineNumberRecord();
                if (lineNumberRecord != null) {
                    fileName = lineNumberRecord.getFileName();
                    lineNumber = lineNumberRecord.getLineno();
                    functionName = funcall.getName();
                }

                result.append(functionName).append(",").append(fileName).append(",").append(lineNumber);
                Set names = new HashSet();
                for (Iterator it = context.getVariableNames(); it.hasNext();) {
                    String name = (String) it.next();
                    if (!name.startsWith("%"))
                        names.add(name);
                }

                addBindingValues(context, names);
                addDefglobals(context, names);
                ArrayList list = new ArrayList(names);
                Collections.sort(list);
                for (Iterator it = list.iterator(); it.hasNext();) {
                    result.append(',');
                    result.append(it.next());
                }
                result.append("|");
            }
            context = context.getParent();
        }

        String stackData = result.toString().trim();
        if (stackData.length() == 0) {
            stackData = "<unknown>,<unknown>,0";
        }
        String event = firstLine + stackData.length() + "\n" + stackData;
        sendEvent(event);
    }

    private int getThreadId(String command, String routine) throws JessException {
        String[] tokens = command.split(" ");
        int threadId;
        try {
            threadId = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            throw new JessException(routine, "Thread id not an integer", tokens[1]);
        }
        return threadId;
    }

    static void addDefglobals(Context context, Set list) {
        Rete engine = context.getEngine();
        for (Iterator it = engine.listDefglobals(); it.hasNext();) {
            Defglobal global = (Defglobal) it.next();
            list.add(global.getName());
        }
    }

    static void addBindingValues(Context context, Set names) throws JessException {
        Funcall funcall = context.getFuncall();
        if (funcall != null) {
            for (int i = 1; i < funcall.size(); ++i) {
                Value v = funcall.get(i);
                if (v instanceof BindingValue) {
                    BindingValue bindingValue = (BindingValue) v;
                    names.add(bindingValue.getName());
                }
            }
        }
    }

    void setCmdSource(Reader cmdSource) {
        m_cmdSource = new BufferedReader(cmdSource);
    }

    void setCmdSink(Writer cmdSink) {
        m_cmdSink = new PrintWriter(cmdSink, true);
    }

    void setEventSink(Writer eventSink) {
        m_eventSink = new PrintWriter(eventSink, true);
    }

    void sendEvent(String event) {
        synchronized (m_eventSink) {
            m_eventSink.print(event.length() + 1);
            m_eventSink.print('\n');
            m_eventSink.print(event);
            m_eventSink.print('\n');
            m_eventSink.flush();
            setChanged();
            notifyObservers(event);
        }
    }

    private void sendRemark(String msg) {
        if (DEBUG)
            sendEvent("REMARK " + msg);
    }


    void sendResponse(String response) {
        m_cmdSink.print(response);
        m_cmdSink.print('\n');
        m_cmdSink.flush();
    }

    String readCommand() throws IOException {
        synchronized (m_cmdSource) {
            if (m_pushBack != null) {

                String cmd = m_pushBack;
                m_pushBack = null;
                return cmd;
            }
            return m_cmdSource.readLine();
        }
    }

    public boolean waitForConnections(int debugPort, int eventPort) {
        Thread t1 = new CommandSocketConnector(this, debugPort);
        Thread t2 = new EventSocketConnector(this, eventPort);

        try {
            t1.start();
            t2.start();
            t1.join();
            t2.join();

        } catch (InterruptedException e) {
            return false;
        }

        boolean result = m_cmdSink != null && m_cmdSource != null && m_eventSink != null;
        if (result)
            start();
        return result;
    }

    public void addBreakpoint(String filename, int lineNumber) {
        File file = new File(filename);
        String tag = file.getName();
        Set lineNumbers = (Set) m_breakpoints.get(tag);
        if (lineNumbers == null) {
            lineNumbers = new HashSet();
            m_breakpoints.put(tag, lineNumbers);
        }
        lineNumbers.add(new Integer(lineNumber));
    }

    public void removeBreakpoint(String filename, int lineNumber) {
        File file = new File(filename);
        String tag = file.getName();
        Set lineNumbers = (Set) m_breakpoints.get(tag);
        if (lineNumbers == null)
            return;
        lineNumbers.remove(new Integer(lineNumber));
    }

    public boolean shouldBreakAt(String filename, int lineNumber) {
        if (filename == null)
            return false;
        File file = new File(filename);
        String tag = file.getName();
        Set lineNumbers = (Set) m_breakpoints.get(tag);
        if (lineNumbers == null)
            return false;
        Integer key = new Integer(lineNumber);
        return lineNumbers.contains(key);
    }

    public synchronized boolean isTerminated() {
        return m_terminated;
    }

    synchronized void setTerminated(boolean terminated) {
        m_terminated = terminated;
    }

    synchronized boolean isListeningForEvents() {
        return m_listeningForEvents;
    }

    synchronized void setListeningForEvents(boolean listeningForEvents) {
        m_listeningForEvents = listeningForEvents;
    }

}
