package jess;

import jess.tools.TextReader;
import jess.factory.Factory;
import jess.factory.FactoryImpl;
import jess.server.LineNumberRecord;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <p>The central class in the Jess library. Executes the built Rete network,
 * and coordinates many
 * other activities. Rete is basically a facade for all the other classes
 * in the Jess library.</p>
 * <p/>
 * <p/>
 * <p>The <tt>jess.Rete</tt> class is the rule engine itself. Each
 * <tt>jess.Rete</tt> object has its own working memory, agenda, rules,
 * etc. To embed Jess in a Java application, you'll simply need to create
 * one or more <tt>jess.Rete</tt> objects and manipulate them
 * appropriately.</p>
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 * @noinspection SynchronizeOnNonFinalField
 */

public class Rete implements Serializable, JessListener {

    /**
     * Constant passed to setEvalSalience() to indicate that dynamic
     * salience expressions should be evaluated only when a rule is
     * installed (the default.)
     */

    public static final int INSTALL = 0;

    /**
     * Constant passed to setEvalSalience() to indicate that dynamic
     * salience expressions should be evaluated whenever a rule is
     * activated.
     */

    public static final int ACTIVATE = 1;

    /**
     * Constant passed to setEvalSalience() to indicate that dynamic
     * salience expressions should be evaluated before every rule is
     * fired. This has a large impact on performance.
     */

    public static final int EVERY_TIME = 2;


    /**
     * The name of a system property. If this property is defined, Jess will assume it names the directory from which
     * the script library "scriptlib.clp" should be loaded.
     */
    public static final String LIBRARY_PATH_ID = "jess.library.path.id";

    /**
     * The name of the script library file. Rete instances load this file when they are created.
     */

    public static final String LIBRARY_NAME = "scriptlib.clp";

    public static Map<Funcall, LineNumberRecord> s_lineNumberTable;
    private static Factory s_factory = new FactoryImpl();

    // TODO Synchronization?
    private Map<Object, Object> m_keyedStorage = new HashMap<Object, Object>();
    
    private Context m_globalContext = new Context(this);
    private transient Routers m_routers = new Routers();
    private transient TextReader m_tis = new TextReader(true);
    private transient Jesp m_jesp = initInternalParser();
    private transient JessEventSupport m_jes = new JessEventSupport(this);
    private boolean m_resetGlobals = true;
    private Map<String, Value> m_storage = Collections.synchronizedMap(new HashMap<String, Value>());
    private FactList m_factList = new FactList();
    private DefinstanceList m_definstanceList = new DefinstanceList(this);
    private Agenda m_agenda = new Agenda();
    private boolean[] m_watchInfo = new boolean[5];
    private boolean m_passiveMode;
    private String m_watchRouter = "WSTDOUT";
    private HashSet<String> m_features = new HashSet<String>();
    /** @noinspection RedundantStringConstructorCall*/
    private final Object m_workingMemoryLock = new String("WM_LOCK");
    private boolean m_dynamicChecking;

    private ValueFactory m_valueFactory;
    private java.util.List<Deffacts> m_deffacts;
    private java.util.List<Defglobal> m_defglobals;
    private Map<String, FunctionHolder> m_functions;
    private Map<String, HasLHS> m_rules;
    private Map<String, Defmodule> m_modules;
    private ReteCompiler m_compiler;
    private ClassSource m_classSource;
    private transient ClassResearcher m_classResearcher;
    private String[] m_currentModule;
    // Keys are Jess defclass names, values are Java class names
    private Map<String, String> m_javaClasses;
    private Map<String, String> m_templatesByClass;
    private boolean m_peered;
    private char m_memberChar = '.';

    // **********************************************************************
    // Constructors and pseudoconstructors
    // **********************************************************************


    private Rete(Rete peer) {
        m_peered = true;
        peer.m_peered = true;
        m_valueFactory = peer.m_valueFactory;
        m_deffacts = peer.m_deffacts;
        m_defglobals = peer.m_defglobals;
        m_functions = peer.m_functions;
        m_rules = peer.m_rules;
        m_compiler = peer.m_compiler;
        m_modules = peer.m_modules;
        m_classResearcher = peer.m_classResearcher;
        m_classSource = peer.m_classSource;
        m_currentModule = peer.m_currentModule;
        m_javaClasses = peer.m_javaClasses;
        m_templatesByClass = peer.m_templatesByClass;
        setEventMask(0);
    }

    /**
     * <p>Return a new Rete object that is a "peer" of this one.
     * The two Rete instances will share a single Rete
     * network, rule base, defglobal list, function list, and class
     * source, but will otherwise be independent; in particular, each
     * has its own separate set of Rete memories. A rule added to any
     * of a set of peered Rete instances will appear in the Rete
     * network of all such instances.</p> <p>Constructing a peered
     * Rete object is a fast operation, and two peered Rete objects
     * use a lot less memory than two independent ones loaded with the
     * same rules. Therefore peering is a useful way to service
     * multiple clients with the same set of rules, as in a Web
     * application.</p>
     * @return a new Rete object connected to this one as a peer
     */
    public Rete createPeer() {
        return new Rete(this);
    }


    /**
     * Construct a Rete object and supply an example of an application
     * class.  Jess will try to find things (like files passed as
     * arguments to "batch") "near" where this class was loaded; i.e.,
     * it will find them using the classloader used to load the argument.
     *
     * <p>If the system property "jess.library.path.id" exists, Jess assumes it names
     * the directory in which the script library <tt>scriptlib.clp</tt> should be found. Otherwise, the
     * script library will be loaded from the class path.</p>
     *
     * @see #LIBRARY_PATH_ID
     * @param appObject any object loaded by the application-level class loader
     */
    
    public Rete(Object appObject) {
        m_valueFactory = new ValueFactory();
        m_deffacts = Collections.synchronizedList(new ArrayList<Deffacts>());
        m_defglobals = Collections.synchronizedList(new ArrayList<Defglobal>());
        m_functions = Collections.synchronizedMap(new HashMap<String, FunctionHolder>(101));
        m_rules = Collections.synchronizedMap(new TreeMap<String, HasLHS>());
        m_modules = Collections.synchronizedMap(new HashMap<String, Defmodule>());
        m_javaClasses = Collections.synchronizedMap(new HashMap<String, String>());
        m_templatesByClass = Collections.synchronizedMap(new HashMap<String, String>());
        m_compiler = new ReteCompiler();
        m_classResearcher = getDefaultClassResearcherInstance();
        m_currentModule = new String[] { Defmodule.MAIN };

        // ###
        try {
            m_classSource = new ClassSource(appObject, this);
            addDefmodule(new Defmodule());
            Deftemplate.addStandardTemplates(this);
        } catch (JessException je) {
            throw new RuntimeException(je);
        }

        setEventMask(0);

        loadScriptlib();
    }

	private ClassResearcher getDefaultClassResearcherInstance() {
		try {
			new Object() {{
				// If this fails, then we can't use java.beans.Introspector,
				// so must be on Android
				java.beans.Introspector.decapitalize("Hello");
			}};
			return new DefaultClassResearcher(this);
		} catch (Throwable t) {
			return new AndroidClassResearcher(this);
		}
	}

    /**
     * Construct a single rule engine instance. It will use the
     * context ClassLoader to load classes named in Jess code.
     *
     * <p>If the system property "jess.library.path.id" exists, Jess assumes it names
     * the directory in which the script library <tt>scriptlib.clp</tt> should be found. Otherwise, the
     * script library will be loaded from the class path.</p>
     * @see #LIBRARY_PATH_ID
     */
    public Rete() {
        this((Object) null);
    }


    // **********************************************************************
    // I/O Router functions
    // **********************************************************************

    /**
     * Add an "input router" to this object. An input router is
     * basically just a Reader that Jess can read from. It's stored in
     * a map and looked up by name. The router "t" corresponds to
     * standard input, but you can define your own routers, or
     * reassign existing ones, using this method.
     *
     * @param s           the router name
     * @param is          a Reader where the router's data comes from
     * @param consoleLike see the Jess manual
     */
    public void addInputRouter(String s, Reader is, boolean consoleLike) {
        m_routers.addInputRouter(s, is, consoleLike);
    }

    /**
     * Remove the named router from the engine. The Reader is not
     * closed -- if it needs to be closed, you must do that yourself.
     *
     * @param s the name of the router to remove
     */
    public void removeInputRouter(String s) {
        m_routers.removeInputRouter(s);
    }

    /**
     * Return the Reader registered under a given router name.
     *
     * @param s the router name
     * @return the router, or null if none
     */
    public Reader getInputRouter(String s) {
        return m_routers.getInputRouter(s);
    }

    Tokenizer getInputWrapper(Reader is) {
        return m_routers.getInputWrapper(is);
    }

    /**
     * Add an "output router" to this object. An input router is
     * basically just a Writer that Jess can send data to. It's stored
     * in a map and looked up by name. The router "t" corresponds to
     * standard output, but you can define your own routers, or
     * reassign existing ones, using this method.
     *
     * @param s  the router name
     * @param os where the data should go
     */
    public void addOutputRouter(String s, Writer os) {
        m_routers.addOutputRouter(s, os);
    }

    /**
     * Remove the named router from the engine. The Writer is not
     * closed -- if it needs to be closed, you must do that yourself.
     *
     * @param s the name of the router
     */
    public void removeOutputRouter(String s) {
        m_routers.removeOutputRouter(s);
    }

    /**
     * Returns the <tt>consoleLike</tt> property for the named input
     * router. The <tt>boolean</tt> argument <tt>consoleLike</tt> to
     * the <tt>addInputRouter</tt> method specifies whether the
     * stream should be treated like the standard input or like a
     * file. The difference is that on console-like streams, a
     * <tt>read</tt> call consumes an entire line of input, but only
     * the first token is returned; while on file-like streams, only
     * the characters that make up each token are consumed on any one
     * call. That means, for instance, that a <tt>read</tt> followed
     * by a <tt>readline</tt> will consume two lines of text from a
     * console-like stream, but only one from a file-like stream,
     * given that the first line is of non-zero length.
     *
     * @param s the router name
     * @return the console-like property for that router, or null if
     *         the router doesn't exist.
     */
    public boolean getInputMode(String s) {
        return m_routers.getInputMode(s);
    }

    /**
     * Return the Writer registered under a given router name. If you
     * create a router using {@link Rete#addOutputRouter}, the return
     * value <i>may</i> be the Writer you passed to that method, but
     * it may instead be a PrintWriter wrapping that Writer.
     *
     * @param s The router name
     * @return The router, or null if none
     */
    public Writer getOutputRouter(String s) {
        return m_routers.getOutputRouter(s);
    }

    /**
     * Return the WSTDERR router, where Jess sends error messages.
     *
     * @return The WSTDERR router
     */
    public PrintWriter getErrStream() {
        return m_routers.getErrStream();
    }

    /**
     * Return the WSTDOUT router, where Jess sends much of its
     * standard output. The "t" router is distinct from WSTDOUT, but
     * they initially both go to System.out.
     *
     * @return The WSTDOUT router
     */
    public PrintWriter getOutStream() {
        return m_routers.getOutStream();
    }

    // **********************************************************************
    // Working memory stuff
    // **********************************************************************

    /**
     * This method is called as a Fact is about to be asserted. It is
     * meant to be overridden by extensions that need to do additional
     * processig when facts are added to working memory.
     *
     * @param f a Fact about to be asserted
     * @return non-zero if the assertion should not proceed
     */
    protected int doPreAssertionProcessing(Fact f) throws JessException {
        return m_factList.doPreAssertionProcessing(f);
    }

    /**
     * Reinitializes this rule engine. Working memory is cleared, all
     * rules are deleted, deffunctions are removed, all templates are
     * forgotten, and a JessEvent of type JessEvent.CLEAR is sent. The watch state, event mask,
     * and event handlers are unchanged.
     *
     * @throws JessException if anything goes wrong
     */
    public synchronized void clear() throws JessException {

        clearStorage();
        m_globalContext.clear();

        m_factList.clear(this);
        m_rules.clear();
        m_agenda.clear();
        m_modules.clear();
        addDefmodule(new Defmodule());
        m_definstanceList.clear(this);
        m_features.clear();
        m_javaClasses.clear();
        m_templatesByClass.clear();


        keepJavaUserFunctionsOnly();
        Deftemplate.addStandardTemplates(this);

        m_compiler.clear();
        m_keyedStorage.clear();

        m_deffacts.clear();
        m_defglobals.clear();
        m_classSource.clear();

        broadcastEvent(JessEvent.CLEAR, this, m_globalContext);

        loadScriptlib();

        System.gc();
    }

    private void keepJavaUserFunctionsOnly() {
        ArrayList<Userfunction> al = new ArrayList<Userfunction>();
        synchronized(m_functions) {
            for (Iterator<String> it = m_functions.keySet().iterator(); it.hasNext();) {
                Userfunction uf = findUserfunction(it.next());
                if (!(uf instanceof Deffunction))
                    al.add(uf);
            }
            m_functions.clear();
            for (Iterator<Userfunction> it = al.iterator(); it.hasNext();)
                addUserfunction(it.next());        
        }
    }

    void setPendingFact(Fact fact, boolean assrt) {
        m_factList.setPendingFact(fact, assrt);
    }

    void removeAllFacts() throws JessException {
        m_factList.clear(this);
    }

    /**
     * Reset the rule engine. Clears working memory and the agenda,
     * but rules remain defined.  Clears all non-globals from the          
     * global scope. Asserts (initial-fact), reasserts all deffacts
     * and definstances, then broadcasts a JessEvent of type JessEvent.RESET.
     *
     * @throws JessException if anything goes wrong
     */

    public void reset() throws JessException {

        synchronized (getWorkingMemoryLock()) {
            removeAllFacts();
            m_globalContext.removeNonGlobals();
            resetDefglobals();
            m_agenda.reset(this);
            assertFact(Fact.getInitialFact(), getGlobalContext());
            resetDeffacts();
            m_definstanceList.reset(this);
        }
        broadcastEvent(JessEvent.RESET, this, m_globalContext);
        // EJFH TODO Make this switchable!
        // System.gc();
    }

    private void resetDeffacts() throws JessException {
        synchronized(m_deffacts) {
            for (Iterator<Deffacts> it = m_deffacts.iterator(); it.hasNext();)
                it.next().reset(this);
        }
    }

    private void resetDefglobals() throws JessException {
        if (getResetGlobals()) {
            synchronized(m_defglobals) {
                for (Iterator<Defglobal> e = m_defglobals.iterator(); e.hasNext();)
                    e.next().reset(this);
            }
        }
    }

    /**
     * Turns dynamic constraint checking on or off. When checking is on, slot constraints
     * are checked before a fact is asserted. By default, this feature is turned off.
     * @param doCheck true if this engine should do dynamic constraint checking
     */
    public void setDynamicChecking(boolean doCheck) {
        m_dynamicChecking = doCheck;
    }


    /**
     * Assert a fact, as a String. For example, you can say<p/>
     * <tt>Rete engine = ...<br/>
     * Context context = ...<br/>
     * engine.assertString("(person (name Fred))", context);</tt>
     *
     * @param s a String representing a fact
     * @param c an execution context for resolving variables
     * @return the fact that was asserted
     * @throws JessException if something goes wrong
     */

    public Fact assertString(String s, Context c) throws JessException {
        synchronized (m_tis) {
            m_tis.clear();
            m_jesp.clear();
            m_tis.appendText("(assert " + s + ")");
            Value v = m_jesp.parseAndExecuteFuncall(null, c, this, m_jesp.getTokenStream(), false);
            if (v.type() == RU.FACT)
                return v.factValue(c);
            else
                return null;
        }
    }

    /**
     * Assert a fact, as a String, using the global execution
     * context. For example, you can say<p/>
     * <tt>Rete engine = ...<br/>
     * engine.assertString("(person (name Fred))");</tt>
     *
     * @param s a String representing a fact
     * @return the fact that was asserted
     * @throws JessException if something goes wrong
     */

    public Fact assertString(String s) throws JessException {
        return assertString(s, getGlobalContext());
    }

    /**
     * Assert a fact. Adds the given Fact to working memory. This fact
     * becomes the property of Jess after calling assertFact() --
     * don't change any of its fields until the fact is retracted!
     * You're also not allowed to call this method on a Fact that
     * already belongs to the working memory of another Rete
     * instance. The behavior in either case is undefined, and
     * generally bad.
     *
     * @param f a Fact object.
     * @return the fact
     * @throws JessException if anything goes wrong
     */

    public Fact assertFact(Fact f) throws JessException {
        return assertFact(f, getGlobalContext());
    }

    /**
     * Assert a fact, using the given execution context. Adds the
     * given Fact to working memory. This fact becomes the property of
     * Jess after calling assertFact() -- don't change any of its
     * fields until the fact is retracted! You're also not allowed to
     * call this method on a Fact that already belongs to the working
     * memory of another Rete instance. The behavior in either case is
     * undefined, and generally bad.
     *
     * @param f a Fact object.
     * @param c an execution context
     * @return the fact
     * @throws JessException if anything goes wrong
     */

    public Fact assertFact(Fact f, Context c) throws JessException {
        if (m_dynamicChecking)
            f.checkConstraints();
        return m_factList.assertFact(f, this, c);
    }

    /**
     * Retract a fact represented as a string. Parses the String to
     * create a Fact object, then tries to remove that fact from
     * working memory.
     *
     * @param s a String form of a Fact
     * @throws JessException
     */
    public Fact retractString(String s) throws JessException {
        try {
            synchronized (m_tis) {
                m_tis.clear();
                m_jesp.clearStack();
                m_tis.appendText(s);
                Fact f = m_jesp.parseFact(this, m_jesp.getTokenStream());
                return retract(f);
            }
        } catch (Exception t) {
            throw new JessException("Rete.retractString", s, t);
        }
    }

    /**
     * Retract a fact. Removes the Fact from working memory. Doesn't
     * need to be the actual object that appears on the fact-list; can
     * just be a Fact that could compare equal to one.
     *
     * @param f a Fact object
     * @throws JessException if anything goes wrong
     */

    public Fact retract(Fact f) throws JessException {
        if (f.isShadow()) {
            Object ov = f.getSlotValue("OBJECT").javaObjectValue(null);
            return remove(ov);
        } else {
            return m_factList.retract(f, this);
        }
    }

    /**
     * Retract all the facts using a given template that are currently
     * in working memory.  If the name is a defclass name, the Java
     * objects will be removed from working memory as well.
     *
     * @param name the name of a template
     * @throws JessException if anything goes wrong
     */
    public void removeFacts(String name) throws JessException {
        m_factList.removeFacts(name, this);
    }

    Fact retractNoUndefinstance(Fact f) throws JessException {
        return m_factList.retract(f, this);
    }

    /**
     * Modify one slot in a fact. This function works for both
     * plain and shadow facts. For shadow facts, the corresponding
     * Java object is also modified. Uses the global context to
     * resolve the value.
     *
     * @param fact      a fact that's currently in working memory
     * @param slotName  the name of a slot in the fact
     * @param slotValue a new value for the named slot
     * @return the fact argument
     * @throws JessException if the slot name is bad or any other error occurs
     */
    public Fact modify(Fact fact, String slotName, Value slotValue) throws JessException {
        return modify(fact, new String[]{slotName}, new Value[]{slotValue});
    }


    /**
     * Modify one slot in a fact. This function works for both
     * plain and shadow facts. For shadow facts, the corresponding
     * Java object is also modified. Uses the supplied context to
     * resolve the value.
     *
     * @param fact      a fact that's currently in working memory
     * @param slotName  the name of a slot in the fact
     * @param slotValue a new value for the named slot
     * @param context   an execution context
     * @return the fact argument
     * @throws JessException if the slot name is bad or any other error occurs
     */
    public Fact modify(Fact fact, String slotName, Value slotValue, Context context) throws JessException {
        return modify(fact, new String[]{slotName}, new Value[]{slotValue}, context);
    }

    /**
     * Modify any number of slots in a fact. This function works for both
     * plain and shadow facts. For shadow facts, the corresponding
     * Java object is also modified. Uses the global context to
     * resolve the values.
     *
     * @param fact       a fact that's currently in working memory
     * @param slotNames  the names of some slots in the fact
     * @param slotValues new values for the named slots
     * @return the fact argument
     * @throws JessException if any slot name is bad or any other error occurs
     */
    public Fact modify(Fact fact, String[] slotNames, Value[] slotValues) throws JessException {
        return modify(fact, slotNames, slotValues, m_globalContext);
    }

    /**
     * Modify any number of slots in a fact. This function works for both
     * plain and shadow facts. For shadow facts, the corresponding
     * Java object is also modified. Uses the given context to
     * resolve the values.
     *
     * @param fact       a fact that's currently in working memory.
     * @param slotNames  the names of some slots in the fact
     * @param slotValues new values for the named slots.
     * @param context    the execution context
     * @return the fact argument
     * @throws JessException if any slot name is bad or any other error occurs
     */
    public Fact modify(Fact fact, String[] slotNames, Value[] slotValues, Context context) throws JessException {
        if (m_dynamicChecking)
            checkFact(fact, slotNames, slotValues);
        return m_factList.modify(fact, slotNames, slotValues, context, this);
    }

    /**
     * For internal use only. Ignores the shadow-fact nature of a fact.
     */
    Fact modifyRegularFact(Fact fact, String[] slotNames, Value[] slotValues, Rete engine,
                           Context context) throws JessException {
        if (m_dynamicChecking)
            checkFact(fact, slotNames, slotValues);
        return m_factList.modifyRegularFact(fact, slotNames, slotValues, engine, context);
    }

    private void checkFact(Fact fact, String[] slotNames, Value[] slotValues) throws JessException {
        Deftemplate template = fact.getDeftemplate();
        for (int i=0; i<slotNames.length; ++i) {
            if (!template.isAllowedValue(slotNames[i], slotValues[i]))
                throw new JessException("Rete.modify", "Invalid value for slot " + slotNames[i], slotValues[i].toString());
        }
    }


    /**
     * Throws if called for shadow facts
     */

    Fact duplicate(ValueVector vv, Context context) throws JessException {
        return Duplicate.duplicate(vv, context);
    }

    /**
     * Return a Fact object given its numeric fact-id. This method is
     * very slow; don't use it unless you have to.  Consider the
     * returned Fact to be READ-ONLY!
     *
     * @param id the fact-id
     * @return the fact, or null if none
     * @throws JessException if something goes wrong
     */

    public Fact findFactByID(int id) throws JessException {
        return m_factList.findFactByID(id);
    }

    /**
     * Find a Fact object in working memory, given a Fact object that
     * is identical to it. This find is fast, and can be used to find
     * out quickly if a given fact is on the fact-list and if so,
     * obtain a reference to it. The argument doesn't have to be a
     * fact in working memory -- only a Fact object identical to one
     * that is.
     *
     * @param f a fact
     * @return a fact from working memory, or null if none
     * @throws JessException if something goes wrong
     */

    public Fact findFactByFact(Fact f) throws JessException {
        return m_factList.findFactByFact(f);
    }


    /**
     * Return a marker for the current state of working memory. The marker can later be used to
     * retract all facts asserted since the marker was placed using resetToMark().
     *
     * @return a marker
     * @see Rete#resetToMark(WorkingMemoryMarker)
     */


    public WorkingMemoryMarker mark() {
        return new IntMarkerImpl(m_factList.peekFactId());
    }

    /**
     * Restore working memory by retracting all facts asserted after the given marker was placed.
     *
     * @param marker the marker
     * @throws JessException if anything goes wrong
     * @see Rete#mark()
     */
    public void resetToMark(WorkingMemoryMarker marker) throws JessException {
        marker.restore(this);
    }

    /**
     * Return a subset of Java objects (definstances) in working memory. The Iterator will
     * include only those objects for which the {@link Filter#accept} method returns true.
     *
     * @param filter a filter to apply to working memory
     * @return an Iterator over the selected objects
     */
    public Iterator<?> getObjects(Filter filter) {
        return m_definstanceList.listDefinstances(this, filter).iterator();
    }

    // **********************************************************************
    // Storing, finding, listing
    // **********************************************************************

    /**
     * Pretty-print all the facts with the given head to the Writer. Output is in
     * Jess rule language format.
     *
     * @param head   the name or "head" of the facts of interest
     * @param output the Writer to send the data to
     * @throws IOException if anything goes wrong
     */

    public void ppFacts(String head, Writer output) throws IOException {
        ppFacts(head, output, false);
    }

    /**
     * Pretty-print all the facts with the given head to the Writer. Output is
     * either in Jess rule language format or in XML, depending on the value of the
     * inXML parameter. XML files will be a complete well-formed document with a
     * "fact-list" root element.
     *
     * @param head   the name or "head" of the facts of interest
     * @param output the Writer to send the data to
     * @param inXML  true for XML output, false for Jess language output
     * @throws IOException if anything goes wrong
     */

    public void ppFacts(String head, Writer output, boolean inXML) throws IOException {
        m_factList.ppFacts(resolveName(head), output, inXML);
    }

    /**
     * Pretty-print all the facts in working memory to the Writer. Output is in
     * the Jess rule language format.
     *
     * @param output the Writer to send the data to
     * @throws IOException if anything goes wrong
     */

    public void ppFacts(Writer output) throws IOException {
        ppFacts(output, false);
    }

    /**
     * Pretty-print all the facts in working memory to the Writer. Output is
     * either in Jess rule language format or in XML, depending on the value of the
     * inXML parameter. XML files will be a complete well-formed document with a
     * "fact-list" root element.
     *
     * @param output the Writer to send the data to
     * @param inXML  true for XML output, false for Jess language output
     * @throws IOException if anything goes wrong
     */

    public void ppFacts(Writer output, boolean inXML) throws IOException {
        m_factList.ppFacts(output, inXML);
    }

    /**
     * Return an Iterator over all the deffacts in this engine.
     *
     * @return the iterator
     */
    public Iterator<Deffacts> listDeffacts() {
        synchronized (m_deffacts) {
            return new ArrayList<Deffacts>(m_deffacts).iterator();
        }
    }

    /**
     * Return the named deffacts object.
     *
     * @param name the name of a deffacts construct
     * @return a Deffacts, or null if none
     */
    public Deffacts findDeffacts(String name) {
        name = resolveName(name);
        for (Iterator<Deffacts> it = listDeffacts(); it.hasNext();) {
            Deffacts df = it.next();
            if (df.getName().equals(name))
                return df;
        }
        return null;
    }

    /**
     * Return an Iterator over all the deftemplates in this engine,
     * both explicit and implied.
     *
     * @return the iterator
     */
    public Iterator<Deftemplate> listDeftemplates() {
        ArrayList<Deftemplate> al = new ArrayList<Deftemplate>();
        for (Iterator<String> modules = listModules(); modules.hasNext();) {
            try {
                Defmodule module = findModule(modules.next());
                for (Iterator<Deftemplate>  dts = module.listDeftemplates();
                     dts.hasNext();)
                    al.add(dts.next());
            } catch (JessException je) {
                // continue;
            }
        }
        return al.iterator();
    }

    /**
     * Return an Iterator over all the rules and queries defined in this engine.
     *
     * @return the iterator
     */
    public Iterator<HasLHS> listDefrules() {
        synchronized (m_rules) {
            return new ArrayList<HasLHS>(m_rules.values()).iterator();
        }
    }

    /**
     * Return an Iterator over all the facts currently in working memory.
     * The facts are returned in fact-id order.
     *
     * @return the iterator
     */
    public Iterator listFacts() {
        return m_factList.listFacts();
    }


    /**
     * Return an Iterator over all the Java objects currently
     * represented by shadow facts in working memory.
     *
     * @return the iterator
     */
    public Iterator listDefinstances() {
        return m_definstanceList.listDefinstances(this).iterator();
    }

    /**
     * Indicates whether a given object is being held in working memory. Will return true only for
     * the same physical object, and not for distinct but equal() objects.
     *
     * @param o the object
     * @return true if the object is in working memory
     */
    public boolean containsObject(Object o) {
        return m_definstanceList.containsObject(o);
    }

    /**
     * Return an Iterator over all the names of all defclasses. You
     * can use each name to look up the corresponding template using
     * findDeftemplate, or the corresponding Java class using
     * javaClassForDefclass.
     *
     * @return the iterator
     * @see #findDeftemplate
     * @see #javaClassForDefclass
     */
    public Iterator<String> listDefclasses() {
        synchronized (m_javaClasses) {
            return new ArrayList<String>(m_javaClasses.keySet()).iterator();
        }
    }


    /**
     * Return an Iterator over all the defglobals in this engine.
     *
     * @return the iterator
     */
    public Iterator<Defglobal> listDefglobals() {
        synchronized (m_defglobals) {
            return new ArrayList<Defglobal>(m_defglobals).iterator();
        }
    }

    /**
     * Return an Iterator over all special functions defined in this engine,
     * including user-defined Java functions, deffunctions, functions defined in the Jess script library,
     * imported static functions, but not including the built-in (<i>intrinsic</i>) functions. All the objects
     * returned by the iterator implement the {@link Userfunction} interface.
     *
     * @return the iterator
     */
    public Iterator<Userfunction> listFunctions() {

        // Strip advice and FunctionHolders here.
        ArrayList<Userfunction> v = new ArrayList<Userfunction>();
        synchronized (m_functions) {
            for (Iterator<String> e = m_functions.keySet().iterator(); e.hasNext();)
                v.add(findUserfunction(e.next()));
        }

        return v.iterator();
    }

    /**
     * Return an Iterator over all deffunctions defined in this engine, including the ones defined in the script library.
     * The Iterator returns {@link Deffunction} objects.
     *
     * @return the iterator
     */
    public Iterator listDeffunctions() {
        return new FilteringIterator(listFunctions(), new Filter() {
            public boolean accept(Object o) {
                return o instanceof Deffunction;
            }
        });
    }

    int countFunctions() {
        return m_functions.size();
    }

    /**
     * Find a defrule or defquery object with a certain name.
     *
     * @param name the name
     * @return the found rule or query, or null.
     */
    public final HasLHS findDefrule(String name) {
        return m_rules.get(resolveName(name));
    }

    /**
     * Return the Java Class corresponding to a given Defclass name,
     * or null if the name was not found.
     *
     * @param name the name of a defclass
     * @return the Class object used to define that defclass
     */

    public Class javaClassForDefclass(String name) {
        try {
            String clazz = jessNameToJavaName(name);
            if (clazz == null)
                return null;
            else
                return findClass(clazz);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    String jessNameToJavaName(String jessName) {
        synchronized(m_javaClasses) {
            String clazz = m_javaClasses.get(jessName);
            if (clazz == null)
                clazz = m_javaClasses.get(resolveName(jessName));
            if (clazz == null && jessName.indexOf("::") == -1)
                clazz = m_javaClasses.get("MAIN::" + jessName);
            return clazz;
        }
    }

    /**
     * Returns the name of a deftemplate that's been created for this class, or null if none
     * @param clazz
     * @return
     */
    public String existingTemplateForClass(Class<? extends Object> clazz) {
        synchronized(m_templatesByClass) {
            String template = m_templatesByClass.get(clazz.getName());
            return template;
        }
    }

    /**
     * Find a deftemplate object with a certain name.
     *
     * @param name the name
     * @return the deftemplate, or null
     */
    public Deftemplate findDeftemplate(String name) throws JessException {
        if (Deftemplate.isSpecialName(name))
            return Deftemplate.getSpecialTemplate(name);

        String fullName = resolveName(name);
        String moduleName = fullName.substring(0, fullName.indexOf("::"));
        Deftemplate deft = findModule(moduleName).getDeftemplate(name);

        if (deft == null && !fullName.equals(name)) {
            // It may actually be defined in MAIN
            deft = findModule(Defmodule.MAIN).getDeftemplate(name);
        }

        return deft;
    }

    /**
     * Find or create the deftemplate by the given name. If there is
     * no such deftemplate, an ordered template is created.
     *
     * @param name the "head" of the template
     * @return a Deftemplate object
     * @throws JessException if the template name is invalid
     */
    public Deftemplate createDeftemplate(String name)
            throws JessException {

        Deftemplate deft = findDeftemplate(name);

        if (deft == null) {
            // this is OK. Create an implied deftemplate
            deft = addDeftemplate(new Deftemplate(name, "(Implied)", this));
            deft.addMultiSlot(RU.DEFAULT_SLOT_NAME, Funcall.NILLIST, "ANY");

        }

        return deft;
    }

    /**
     * Creates a new deftemplate in this rule engine.  Ensures that
     * every deftemplate has a unique name by rejecting duplicates.
     *
     * @param dt a new Deftemplate
     * @return the argument
     * @throws JessException if the deftemplate is already defined
     */
    public Deftemplate addDeftemplate(Deftemplate dt) throws JessException {
        dt.freeze(this);
        Defmodule module = findModule(dt.getModule());
        return module.addDeftemplate(dt, this);
    }

    /**
     * Removes an unused deftemplate from this engine, and from all peered engines.
     * If the template is not found, return silently.
     * If the template is found but is in use, will throw an exception.
     * @param name the name of the template, optionally including a module
     */
    public void removeDeftemplate(String name) throws JessException {
        Deftemplate template = findDeftemplate(name);
        if (template != null) {
            // We're in use if any facts exist that use this template
            for (Iterator it = listFacts(); it.hasNext();) {
                Fact fact = (Fact) it.next();
                if (template.equals(fact.getDeftemplate()))
                    throw new JessException("Rete.removeDeftemplate", "Template in use by facts", template.getName());
            }
            // ... even in deffacts
            for (Iterator<Deffacts> it = listDeffacts(); it.hasNext();) {
                Deffacts facts = it.next();
                for (Iterator it2 = facts.listFacts(); it.hasNext();) {                
                    Fact fact = (Fact) it2.next();
                    if (template.equals(fact.getDeftemplate()))
                        throw new JessException("Rete.removeDeftemplate", "Template in use by deffacts", facts.getName());
                }
            }

            // We're in use if any templates use us for a parent
            for (Iterator it = listDeftemplates(); it.hasNext();) {
                Deftemplate child = (Deftemplate) it.next();
                if (template.equals(child.getParent())) {
                    throw new JessException("Rete.removeDeftemplate", "Template has child template", child.getName());
                }
            }

            // We're in use if any rules or queries reference us
            NodeRoot root = (NodeRoot) getCompiler().getRoot();
            if (root.isInUse(template))
                throw new JessException("Rete.removeDeftemplate", "Template is in use by rules", template.getName());


            // OK to remove
            broadcastEvent(JessEvent.DEFTEMPLATE | JessEvent.REMOVED, template, m_globalContext);
            Defmodule module = findModule(template.getModule());
            module.removeDeftemplate(template);
        }
    }

    /**
     * Add a deffacts construct to this rule engine.
     *
     * @param df a new Deffacts object
     * @return the argument
     * @throws JessException If an error occurs during event broadcasting
     */
    public Deffacts addDeffacts(Deffacts df) throws JessException {
        broadcastEvent(JessEvent.DEFFACTS, df, m_globalContext);
        m_deffacts.add(df);
        return df;
    }

    /**
     * Remove a deffacts construct from this engine and any peers. The deffacts' facts are not removed
     * from working memory until the engine is reset.
     * @param name the name of the deffacts
     * @t
     */
    public void removeDeffacts(String name) throws JessException {
        synchronized (m_deffacts) {
            Deffacts facts = findDeffacts(name);
            if (facts != null) {
                broadcastEvent(JessEvent.DEFFACTS | JessEvent.REMOVED, facts, m_globalContext);
                m_deffacts.remove(facts);
            }
        }
    }

    /**
     * Creates a new Defglobal in this rule engine. This defglobal
     * will be reset immediately, regardless of the setting of
     * resetGlobals.
     *
     * @param dg a new Defglobal object
     * @return the argument
     * @throws JessException if an error occurs
     */
    public Defglobal addDefglobal(Defglobal dg) throws JessException {
        broadcastEvent(JessEvent.DEFGLOBAL, dg, m_globalContext);

        dg.reset(this);
        m_defglobals.add(dg);
        return dg;
    }

    /**
     * Adds a list of Defglobals to this object, as if by addDefglobal().
     *
     * @param dg a List of Defglobals
     * @return the argument
     * @throws JessException if an error occurs
     */
    public List addDefglobals(List dg) throws JessException {
        for (Iterator it = dg.iterator(); it.hasNext();)
            addDefglobal((Defglobal) it.next());
        return dg;
    }

    /**
     * Look up a defglobal by name. The name should include the
     * leading and trailing asterisk required for defglobals.
     *
     * @param name the name of the defglobal
     * @return the Defglobal, if found, or null
     */
    public Defglobal findDefglobal(String name) {
        for (Iterator<Defglobal> e = listDefglobals(); e.hasNext();) {
            Defglobal dg = e.next();
            if (dg.getName().equals(name))
                return dg;
        }
        return null;
    }

    /**
     * Removes the given defglobal from this engine. The name sbould include the leading and trailing asterisks.
     * This method returns silently if the defglobal is not found. Note that this method should not be used with
     * peered engines; attempting to undefine a defglobal in a peered engine will result in an exception.
     *
     * @param name the name of the defglobal
     * @throws JessException if anything goes wrong
     */
    public void removeDefglobal(String name) throws JessException {
        if (m_peered)
            throw new JessException("Rete.removeDefglobal", "Cannot remove defglobal from peered engines", name);

        Defglobal dg = findDefglobal(name);
        if (dg != null) {
            broadcastEvent(JessEvent.DEFGLOBAL | JessEvent.REMOVED, dg, m_globalContext);
            m_defglobals.remove(dg);
            m_globalContext.removeVariable(name);
        }
    }

    /**
     * Removes all user-defined functions from this engine.  Only the
     * built-in functions and static methods from java.lang will
     * remain.
     */

    public void removeUserDefinedFunctions() {
        synchronized(m_functions) {
            Set<String> keys = new HashSet<String>(m_functions.keySet());
            for (Iterator<String> it = keys.iterator(); it.hasNext();)
                removeUserfunction(it.next());
        }
        importPackage("java.lang.");
    }

    /**
     * Creates a new function in this rule engine. Any existing
     * function by the same name will be replaced. Built-in functions
     * can be redefined this way.
     *
     * @param uf a new Userfunction
     * @return the parameter, or null if call rejected by event handler
     */
    public Userfunction addUserfunction(Userfunction uf) {
        try {
            broadcastEvent(JessEvent.USERFUNCTION, uf, m_globalContext);
        } catch (JessException je) {
            return null;
        }

        synchronized(m_functions) {
            FunctionHolder fh;
            if ((fh = m_functions.get(uf.getName())) != null)
                fh.setFunction(uf);
            else
                fh = new FunctionHolder(uf);
            m_functions.put(uf.getName(), fh);
        }
        return uf;
    }

    /**
     * Remove any currently defined function by the given
     * name. Even built-in functions can be removed this way.
     *
     * @param name the name of the function to remove
     */
    public void removeUserfunction(String name) {
        synchronized(m_functions) {
            FunctionHolder fh = m_functions.get(name);
            if (fh != null) {
                Userfunction removed = fh.getFunction();
                if (removed != null) {
                    try {
                        broadcastEvent(JessEvent.USERFUNCTION | JessEvent.REMOVED, removed, m_globalContext);
                    } catch (JessException je) {
                        return;
                    }
                    fh.setFunction(null);
                    m_functions.remove(name);
                }
            }
        }
    }


    /**
     * Add a Userpackage to this engine. A Userpackage is a collection
     * of Userfunctions. A package generally calls addUserfunction
     * lots of times.
     *
     * @param up the package object
     * @return the  package object, or null if  call rejected by event
     *         handler
     */
    public Userpackage addUserpackage(Userpackage up) {
        try {
            broadcastEvent(JessEvent.USERPACKAGE, up, m_globalContext);
        } catch (JessException je) {
            return null;
        }

        up.add(this);
        return up;
    }

    /**
     * Find a userfunction, if there is one by the given name.
     *
     * @param name the name of the function
     * @return the Userfunction object, if there is one, or null.
     */
    public final Userfunction findUserfunction(String name) {
        FunctionHolder fh = m_functions.get(name);
        if (fh != null) {
            return fh.getFunction();
        } else
            return Funcall.getIntrinsic(name);
    }

    /**
     * Find a userfunction, if there is one.
     *
     * @param name The name of the function
     * @return The Userfunction object, if there is one.
     */
    final FunctionHolder findFunctionHolder(String name) {
        synchronized(m_functions) {
            FunctionHolder fh = m_functions.get(name);
            if (fh == null) {
                Userfunction uf = Funcall.getIntrinsic(name);
                if (uf != null)
                    addUserfunction(uf);
                fh = m_functions.get(name);
            }
            return fh;
        }
    }

    /**
     * Add a rule or query to this engine. The rule is compiled and
     * added to the Rete network. Any existing rule or query by the
     * same name is removed first.
     *
     * @param dr a Defrule or Defquery
     * @return the added object
     * @throws JessException if anything goes wrong
     */
    public final HasLHS addDefrule(HasLHS dr) throws JessException {
        if (m_passiveMode)
            return dr;

        synchronized (m_compiler) {
            JessException exception = null;
            removeDefrule(dr.getName());
            HasLHS hlhs = dr;
            try {
                while (hlhs != null) {
                    m_compiler.addRule(hlhs, this);
                    hlhs = hlhs.getNext();
                }

            } catch (RuleCompilerException rce) {
                dr.remove(m_compiler.getRoot());
                throw rce;

            } catch (JessException je) {
                exception = je;
            }

            m_rules.put(dr.getName(), dr);
            broadcastEvent(JessEvent.DEFRULE, dr, m_globalContext);

            if (exception != null)
                throw exception;
            else
                return dr;
        }
    }

    /**
     * Remove a deffacts from the engine. If the given name is "*", remove all deffacts.
     *
     * @param name the name of an existing deffacts construct, or "*"
     * @return the symbol TRUE
     */

    public final Value unDeffacts(String name) {
        if (name.equals("*"))
            m_deffacts.clear();
        else {
            name = resolveName(name);
            synchronized(m_deffacts) {
                for (Iterator<Deffacts> it = m_deffacts.iterator(); it.hasNext();) {
                    Deffacts deffacts = it.next();
                    if (deffacts.getName().equals(name)) {
                        it.remove();
                        return Funcall.TRUE;
                    }
                }
            }
        }
        return Funcall.FALSE;
    }


    /**
     * Remove a rule or query from this Rete object. Removes all
     * subrules of the named rule as well.
     *
     * @deprecated As of Jess 7.1, use removeDefrule(String)
     * @param name the name of the rule or query
     * @return the symbol TRUE
     * @throws JessException if anything goes wrong
     */
    public final Value unDefrule(String name) throws JessException {
        synchronized (m_compiler) {
            HasLHS odr = findDefrule(name);
            removeDefrule(name);
            return (odr == null) ? Funcall.FALSE : Funcall.TRUE;
        }
    }


    /**
     * Remove a rule or query from this Rete object. Removes all
     * subrules of the named rule as well.
     *
     * @param name the name of the rule or query
     * @throws JessException if anything goes wrong
     */
    public void removeDefrule(String name) throws JessException {
        synchronized (m_compiler) {
            HasLHS odr = findDefrule(name);
            if (odr != null) {
                commitActivations();
                m_rules.remove(resolveName(name));
                odr.remove(m_compiler.getRoot());
                if (odr instanceof Defrule) {
                    m_agenda.removeActivationsOfRule((Defrule) odr, this);
                }
                broadcastEvent(JessEvent.DEFRULE | JessEvent.REMOVED, odr, m_globalContext);
            }
        }
    }

    // **********************************************************************
    // Modules
    // **********************************************************************

    /**
     * Define a new module, which becomes current. The current module
     * is the one to which new constructs will be added.
     *
     * @param module a new Defmodule object
     */
    public void addDefmodule(Defmodule module)
            throws JessException {
        String name = module.getName();
        synchronized(m_modules) {
            if (m_modules.get(name) != null)
                throw new JessException("Rete.addDefmodule", "Attempt to redefine defmodule", name);
            m_modules.put(name, module);
        }
        setCurrentModule(name);
        broadcastEvent(JessEvent.DEFMODULE, module, m_globalContext);        
    }

    /**
     * Define a new module, which becomes current. The current module
     * is the one to which new constructs will be added.
     *
     * @deprecated As of Jess 7, use addDefmodule(Defmodule).
     */
    public void addDefmodule(String name)
            throws JessException {
        addDefmodule(new Defmodule(name, ""));
    }

    /**
     * Define a new module, which becomes current. The current module
     * is the one to which new constructs will be added.
     *
     * @deprecated As of Jess 7, use addDefmodule(Defmodule).
     */
    public void addDefmodule(String name, String comment)
            throws JessException {
        addDefmodule(new Defmodule(name, comment));
    }

    /**
     * Remove the given defmodule from this engine and all peered engines.
     * If the module is not found, return silently. Only empty modules can be removed; you must
     * remove all the rules and templates defined in the module first, or an exception will be thrown.
     * @param name
     * @throws JessException
     */
    public void removeDefmodule(String name) throws JessException {
        if (name.equals(Defmodule.MAIN))
            throw new JessException("Rete.removeDefmodule", "Cannot remove module", name);
        Defmodule module = findModule(name);
        if (module != null) {
            if (module.listDeftemplates().hasNext())
                throw new JessException("Rete.removeDefmodule", "Module still defines templates", "");
            for (Iterator<HasLHS> it = listDefrules(); it.hasNext();) {
                if (it.next().getModule().equals(module.getName()))
                    throw new JessException("Rete.removeDefmodule", "Module still defines rules", "");
            }
            broadcastEvent(JessEvent.DEFMODULE | JessEvent.REMOVED, module, m_globalContext);
            m_modules.remove(name);
        }
    }



    /**
     * Return the name of the current module. The current module is
     * the one to which new constructs will be added.
     */
    public String getCurrentModule() {
        return m_currentModule[0];
    }

    /**
     * Change the current module. The current module is the one to
     * which new constructs will be added.
     *
     * @param name the name of the new current module
     * @return the old current module
     * @throws JessException if the name is not a module
     */
    public String setCurrentModule(String name) throws JessException {
        verifyModule(name);
        String orig = m_currentModule[0];
        m_currentModule[0] = name;
        return orig;
    }

    /**
     * List all modules.
     *
     * @return an iterator over the names of all modules
     */
    public Iterator<String> listModules() {
        return m_modules.keySet().iterator();
    }

    /**
     * Query the focus module. The focus module is the one from which
     * rules are being fired.
     *
     * @return the name of the current focus module
     */
    public String getFocus() {
        return m_agenda.getFocus();
    }

    /**
     * Change the focus module. The focus module is the one from which
     * rules are being fired.
     *
     * @param name the module that should get focus
     */
    public void setFocus(String name) throws JessException {
        m_agenda.setFocus(name, this);
    }

    /**
     * Iterate over the module focus stack, from bottom to top. The
     * current focus module is returned last. The focus module is the
     * one from which rules are being fired.
     *
     * @return the iterator
     */
    public Iterator listFocusStack() throws JessException {
        return m_agenda.listFocusStack();
    }

    /**
     * Empty the module focus stack. The focus module is the one from
     * which rules are being fired.
     */
    public void clearFocusStack() {
        m_agenda.clearFocusStack();
    }

    /**
     * Remove the top module from the focus stack, and return it. If
     * expected in non-null, then this is a no-op unless expected
     * names the top module on the stack. The focus module is the one
     * from which rules are being fired.
     *
     * @param expected the expected module, or null
     * @return the name of the module on top the focus stack
     */
    public String popFocus(String expected) throws JessException {
        return m_agenda.popFocus(this, expected);
    }

    /**
     * Throw an exception if the argument isn't the name of a module.
     *
     * @param name a proposed module name
     */

    public void verifyModule(String name) throws JessException {
        if (m_modules.get(name) == null) {
            throw new JessException("Rete.findModule", "No such module", name);
        }

    }

    /**
     * Decorate the name with the current module name, if it doesn't
     * already contain a module name.
     *
     * @param name a module name
     */

    public String resolveName(String name) {
        if (name.indexOf("::") == -1)
            name = RU.scopeName(m_currentModule[0], name);
        return name;
    }

    /**
     * Return the Defmodule object corresponding to the Jess module with the given name.
     * @param name
     * @return
     * @throws JessException
     */
    public Defmodule findModule(String name) throws JessException {
        Defmodule module = m_modules.get(name);
        if (module == null) {
            throw new JessException("Rete.findModule", "No such module", name);
        }
        return module;
    }

    // **********************************************************************
    // Dealing with the agenda: running, stopping, salience, etc.
    // **********************************************************************


    /**
     * Present all the facts on the agenda to a single Node.
     */
    void updateNodes(Set n) throws JessException {
        m_factList.updateNodes(this, n);
    }

    /**
     * Info about a rule to fire.
     */
    void addActivation(Activation a) throws JessException {
        broadcastEvent(JessEvent.ACTIVATION, a, m_globalContext);
        m_agenda.addActivation(a);
    }

    /**
     * An activation has been cancelled or fired; forget it
     */

    void removeActivation(Activation a) throws JessException {
        broadcastEvent(JessEvent.ACTIVATION | JessEvent.REMOVED, a, m_globalContext);
        m_agenda.removeActivation(a);
    }

    void commitActivations() throws JessException {
        m_agenda.commitActivations(this);
    }

    /**
     * Check whether the current module includes any activations to fire.
     * @return true if there are activations in the current module
     * @throws JessException if something goes wrong
     */
    public boolean hasActivations() throws JessException {
        return m_agenda.hasActivations(this);
    }

    /**
     * Check whether there are any activations to fire in the named module. "*" is not accepted.
     * @param moduleName a valid module name
     * @return true if there are activations in the named module
     * @throws JessException if the module name is invalid
     */
    public boolean hasActivations(String moduleName) throws JessException {
        return m_agenda.hasActivations(this, moduleName);
    }


    /**
     * Return an Iterator over all the activations for the current
     * module. Note that some of the activations may be cancelled or
     * already fired; check the return value of "isInactive()"for each
     * one to be sure. This is an expensive operation.
     *
     * @return an iterator over the agenda for the current module
     */
    public Iterator listActivations() throws JessException {
        return m_agenda.listActivationsInCurrentModule(this);
    }

    /**
     * Return an Iterator over all the activations for the named
     * module. Asterisk ("*") is not accepted, in firing order. This is an
     * expensive operation.
     *
     * @param moduleName the name of a module
     * @return an iterator over the agenda for the named module
     * @throws JessException if the module name is invalid
     */
    public Iterator listActivations(String moduleName) throws JessException {
        return m_agenda.listActivations(this, moduleName);
    }

    /**
     * Return the next activation record from the agenda. It's
     * possible that this rule will be wrong if something happens to
     * change the agenda before this rule fires.
     *
     * @return the next activation that will fire
     * @throws JessException if anything goes wrong
     */
    public Activation peekNextActivation() throws JessException {
        return m_agenda.peekNextActivation(this);
    }

    /**
     * The monitor of the object returned from this method will be signalled
     * whenever an activation appears. Thus a run-loop could wait on
     * this monitor when idle.
     *
     * @return the activation lock
     * @see #waitForActivations
     */

    public Object getActivationSemaphore() {
        return m_agenda.getActivationSemaphore();
    }

    /**
     * Waits on the activation lock as long as the agenda is
     * empty. Will return as soon as a rule is actvated. Can be called
     * in a run-loop to wait for more rules to fire.
     *
     * @see #getActivationSemaphore
     */

    public void waitForActivations() {
        m_agenda.waitForActivations(this);
    }

    /**
     * Tell this engine to use the given Strategy object to order the
     * rules on the agenda. You can implement your own strategies.
     *
     * @param s the new conflict resolution strategy
     * @return the name of the previous conflict resolution strategy
     * @throws JessException
     */
    public String setStrategy(Strategy s) throws JessException {
        return m_agenda.setStrategy(s);
    }


    /**
     * Retrieve the Strategy object this engine is using to order activations
     * on the agenda.
     *
     * @return the current conflict resolution strategy
     */
    public Strategy getStrategy() {
        return m_agenda.getStrategy();
    }

    /**
     * Set the salience evaluation behaviour. The behaviour can be one
     * of INSTALL, ACTIVATE, or EVERY_TIME; the default is INSTALL. When
     * the behaviour is INSTALL, a rule's salience is evulated once when
     * the rule is compiled. If it is ACTIVATE, it is computed each time
     * the rule is activated. If it is EVERY_TIME, salience evaluations
     * are done for all rules each time the next rule on the agenda is
     * to be chosen.
     *
     * @param method One of the acceptable values
     * @throws JessException If something goes wrong
     */

    public final void setEvalSalience(int method) throws JessException {
        m_agenda.setEvalSalience(method);
    }

    /**
     * Fetch the salience evaluation behaviour, which helps determine
     * the priority of rule firing. Returns one of the constants
     * INSTALL, ACTIVATE, or EVERY_TIME.
     *
     * @return the salience evaluation behaviour
     * @see #setEvalSalience
     */
    final public int getEvalSalience() {
        return m_agenda.getEvalSalience();
    }

    /**
     * Run the rule engine. When the engine is running, rules are
     * continuously removed from the agenda and fired in priority
     * order; the order is determined by the current conflict
     * resolution strategy.
     *
     * @return the actual number of rules fired
     * @throws JessException if anything goes wrong
     */
    public int run() throws JessException {
        return run(getGlobalContext());
    }

    int run(Context context) throws JessException {
        broadcastEvent(JessEvent.RUN, this, context);
        return m_agenda.run(this, context);
    }

    /**
     * Allows a subclass to be notified immediately before a rule
     * fires. Subclasses can override this method to receive this
     * notification.
     *
     * @param a the activation record for the about-to-fire rule
     */
    protected void aboutToFire(Activation a) {
    }

    /**
     * Allows a subclass to be notified immediately after a rule
     * fires. Subclasses can override this method to receive this
     * notification.
     *
     * @param a the activation record for the just-fired rule
     */
    protected void justFired(Activation a) {
    }

    /**
     * Run the rule engine. If the number of rules fired becomes
     * greater than the argument, the engine stops. When the engine is
     * running, rules are continuously removed from the agenda and
     * fired in priority order; the order is determined by the current
     * conflict resolution strategy.
     *
     * @param max the maximum number of rules to fire
     * @return the number of rules that fired
     * @throws JessException if anything goes wrong
     */
    public int run(int max) throws JessException {
        return run(max, getGlobalContext());
    }

    int run(int max, Context context) throws JessException {
        broadcastEvent(JessEvent.RUN, this, m_globalContext);
        return m_agenda.run(max, this, context);
    }

    /**
     * Run the rule engine until halt() is called. When no rules are
     * active, the calling Thread will be waiting on the activation
     * semaphore.  When the engine is running, rules are continuously
     * removed from the agenda and fired in priority order; the order
     * is determined by the current conflict resolution strategy.
     *
     * @return The number of rules that fired.
     * @throws JessException if an error occurs
     */

    public int runUntilHalt() throws JessException {
        return runUntilHalt(getGlobalContext());
    }

    int runUntilHalt(Context context) throws JessException {
        broadcastEvent(JessEvent.RUN, this, m_globalContext);
        return m_agenda.runUntilHalt(this, context);
    }

    /**
     * Stop the engine from firing rules.
     *
     * @throws JessException if an error occurs during event propagation
     */

    public void halt() throws JessException {
        broadcastEvent(JessEvent.HALT, this, m_globalContext);
        m_agenda.halt();
    }

    /**
     * Returns true if halt has been called since the last run() call.
     *
     * @return true if the engine is halted
     */

    public boolean isHalted() {
        return m_agenda.isHalted();
    }


    /**
     * Find out the name of the currently firing rule.
     *
     * @return The name of the rule that is currently firing, if this is
     *         called while a rule is firing; otherwise, returns null.
     * @see #getThisActivation
     */
    public String getThisRuleName() {
        Activation a = getThisActivation();
        if (a != null)
            return a.getRule().getName();
        else
            return null;
    }

    /**
     * Get the activation record for the currently firing rule. An
     * activation record contains a Defrule and the list of facts that
     * made the rule active.
     *
     * @return the activation record for the rule that is currently
     *         firing, if this is called while a rule is firing; otherwise,
     *         returns null.
     * @see #getThisRuleName
     */
    public Activation getThisActivation() {
        return m_agenda.getThisActivation();
    }

    /**
     * If this engine is currently firing a rule, returns the Thread
     * that the rule is firing on; otherwise, returns null.
     *
     * @return the thread
     * @see #getThisActivation
     */
    public Thread getRunThread() {
        return m_agenda.getRunThread();
    }

    // **********************************************************************
    // Events and event listener support
    // **********************************************************************

    /**
     * Register an event listener with this rule engine. The listener will be notified of various significant events according
     * to the value of the event mask for this engine. Events are generally delivered immediately before an action occurs, so that
     * the listener can veto the event by throwing an exception.
     * @see JessEvent
     * @see #setEventMask(int)
     * @param jel a callback object that implements JessListener
     */

    public void addJessListener(JessListener jel) {
        m_jes.addJessListener(jel);
    }

    /**
     * Remove an event listener from this rule engine. The listener will no longer be notified of events.
     * @see #addJessListener(JessListener)
     * @param jel a previously-registered callback object
     */
    public void removeJessListener(JessListener jel) {
        m_jes.removeJessListener(jel);
    }

    /**
     * Returns an iterator over all previously registered event listeners.
     * @see #addJessListener(JessListener)
     * @return the Iterator
     */
    public Iterator listJessListeners() {
        return m_jes.listJessListeners();
    }

    /**
     * Query the current value of the event mask.
     *
     * @return the event mask
     * @see #setEventMask
     */
    public int getEventMask() {
        return m_jes.getEventMask();
    }

    /**
     * You can control which events this object will fire
     * using this method. The argument is the result
     * of logical-OR-ing together some of the constants in the
     * {@link JessEvent} class. By default, the event mask is 0 and no
     * events are sent.
     *
     * @param mask the new event mask
     * @see #getEventMask
     */
    public void setEventMask(int mask) {
        m_jes.setEventMask(mask);
    }

    final void broadcastEvent(int type, Object data, Context context) throws JessException {
        m_jes.broadcastDebugEvent(this, type, data, context);
        m_jes.broadcastEvent(this, type, data, context);
    }

    // **********************************************************************
    // Debug mode
    // **********************************************************************

    /**
     * Return whether the engine is in passive mode. In passive mode,
     * the engine won't send added rules to the compiler. This is used
     * by the JessDE implementation, but isn't useful to the vast
     * majority of clients.
     *
     * @param passiveMode the desired value of this property
     */
    public void setPassiveMode(boolean passiveMode) {
        m_passiveMode = passiveMode;
    }


    /**
     * Return whether the engine is in debug mode. In debug mode, the
     * engine can single-step. Special JessEvents are used for this
     * purpose.  This is used by the JessDE implementation, but isn't
     * useful to the vast majority of clients.
     *
     * @return whether the engine is in debug mode
     */
    public boolean isDebug() {
        return m_jes.isDebug();
    }

    /**
     * Specify whether the engine should be in debug mode. In debug
     * mode, the engine can single-step. Special JessEvents are used
     * for this purpose.  This is used by the JessDE implementation,
     * but isn't useful to the vast majority of clients.
     *
     * @param debug the desired state
     */
    public void setDebug(boolean debug) {
        m_jes.setDebug(debug);
    }

    /**
     * Store a debug symbol for a function call. This is used by the
     * JessDE implementation, but isn't useful to the vast majority of
     * clients.
     *
     * @param fc       the funcall being invoked
     * @param fileName the source file name
     * @param lineno   the source file line number
     */
    public static void recordFunction(Funcall fc, String fileName, int lineno) {
        recordFunction(fc, new LineNumberRecord(fileName, lineno));
    }

    /**
     * Store a debug symbol for a function call. This is used by the
     * JessDE implementation, but isn't useful to the vast majority of
     * clients.
     *
     * @param fc  the funcall being invoked
     * @param lnr the source file name and line number
     */
    public static void recordFunction(Funcall fc, LineNumberRecord lnr) {
        if (s_lineNumberTable == null)
            s_lineNumberTable = Collections.synchronizedMap(new IdentityHashMap<Funcall, LineNumberRecord>());
        s_lineNumberTable.put(fc, lnr);
    }

    /**
     * Return information about the source location of a given
     * function call, if known. This method only works when the engine
     * is in debug mode. This is used by the JessDE implementation,
     * but isn't useful to the vast majority of clients.
     *
     * @param fc the funcall
     * @return source file and line number information, if known, or null
     */
    public static LineNumberRecord lookupFunction(Funcall fc) {
        if (s_lineNumberTable != null)
            return s_lineNumberTable.get(fc);
        else
            return null;
    }

    /**
     * Remove a listener so that it will no longer receive debug
     * events. This is used by the JessDE implementation, but isn't
     * useful to the vast majority of clients.
     *
     * @param jel the listener to remove
     */
    public void removeDebugListener(JessListener jel) {
        m_jes.removeDebugListener(jel);
    }

    /**
     * Add a listener so that it will receive debug events. This is
     * used by the JessDE implementation, but isn't useful to the vast
     * majority of clients.
     *
     * @param jel the listener to remove
     */
    public void addDebugListener(JessListener jel) {
        m_jes.addDebugListener(jel);
    }

    /**
     * Return an iterator over all listeners that will receive debug
     * events. This is used by the JessDE implementation, but isn't
     * useful to the vast majority of clients.
     *
     * @return the iterator
     */

    public Iterator listDebugListeners() {
        return m_jes.listDebugListeners();
    }

    // **********************************************************************
    // Bloading and serialization
    // **********************************************************************

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        m_routers = new Routers();
        m_tis = new TextReader(true);     
        m_jesp = initInternalParser();
        m_jes = new JessEventSupport(this);
        m_definstanceList.setEngine(this);
        m_globalContext.setEngine(this);
        m_classSource.setEngine(this);
        m_classResearcher = getDefaultClassResearcherInstance();
    }

    /**
     * Read this object's state from the given stream. Similar to the
     * readObject() method used in Serialization, except it loads new
     * state into an existing object. The data on the InputStream must
     * have been written by a call to bsave().
     *
     * @param is an InputStream
     */

    public void bload(InputStream is)
            throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);

        m_globalContext = (Context) ois.readObject();
        m_globalContext.setEngine(this);
        m_resetGlobals = ois.readBoolean();
        m_deffacts = (java.util.List<Deffacts>) ois.readObject();
        m_defglobals = (java.util.List<Defglobal>) ois.readObject();
        m_functions = (Map<String, FunctionHolder>) ois.readObject();
        m_factList = (FactList) ois.readObject();
        m_definstanceList = (DefinstanceList) ois.readObject();
        m_definstanceList.setEngine(this);
        m_rules = (Map<String, HasLHS>) ois.readObject();
        m_compiler = (ReteCompiler) ois.readObject();
        m_storage = (Map<String, Value>) ois.readObject();
        m_agenda = (Agenda) ois.readObject();
        m_classSource = (ClassSource) ois.readObject();
        m_classSource.setEngine(this);
        m_watchInfo = (boolean[]) ois.readObject();
        m_features = (HashSet<String>) ois.readObject();
        m_modules = (Map<String, Defmodule>) ois.readObject();
        m_currentModule = (String[]) ois.readObject();
        m_javaClasses = (Map<String, String>) ois.readObject();
        m_templatesByClass = (Map<String, String>) ois.readObject();
        m_dynamicChecking = ois.readBoolean();
        m_keyedStorage = (Map<Object, Object>) ois.readObject();        
    }

    /**
     * Save this object's state out to the given stream. Similar to
     * the writeObject() method used in serialization, but only writes
     * the state of this object, not the object itself. You can read
     * this into another Rete object using the bload() method.
     *
     * @param os an OutputStream
     */

    public void bsave(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);

        synchronized (getWorkingMemoryLock()) {
            synchronized (getActivationSemaphore()) {
                oos.writeObject(m_globalContext);
                oos.writeBoolean(m_resetGlobals);
                oos.writeObject(m_deffacts);
                oos.writeObject(m_defglobals);
                oos.writeObject(m_functions);
                oos.writeObject(m_factList);
                oos.writeObject(m_definstanceList);
                oos.writeObject(m_rules);
                oos.writeObject(m_compiler);
                oos.writeObject(m_storage);
                oos.writeObject(m_agenda);
                oos.writeObject(m_classSource);
                oos.writeObject(m_watchInfo);
                oos.writeObject(m_features);
                oos.writeObject(m_modules);
                oos.writeObject(m_currentModule);
                oos.writeObject(m_javaClasses);
                oos.writeObject(m_templatesByClass);
                oos.writeBoolean(m_dynamicChecking);
                oos.writeObject(m_keyedStorage);
            }
        }
        oos.flush();
    }

    // **********************************************************************
    // Defclass and definstance support
    // **********************************************************************

    /**
     * Make a shadow fact for the given object and add it to working
     * memory. The boolean argument indicates whether or not to
     * install a PropertyChangeListener to listen for change
     * events. Uses the global context, which is usually what you
     * want.
     *
     * @param jessTypename the name of a defclass
     * @param object       a Java object of the defclass's type
     * @param dynamic      true if PropertyChangeListeners should be used
     * @return a FactIdValue containing the shadow fact for the object
     * @throws JessException if anything goes wrong
     */
    public Value definstance(String jessTypename, Object object,
                             boolean dynamic) throws JessException {
        return definstance(jessTypename, object, dynamic, getGlobalContext());
    }

    /**
     * Make a shadow fact for the given object and add it to working
     * memory. The boolean argument indicates whether or not to
     * install a PropertyChangeListener to listen for change events.
     *
     * @param jessTypename the name of a defclass
     * @param object       a Java object of the defclass's type
     * @param dynamic      true if PropertyChangeListeners should be used
     * @param context      used for asserting the fact
     * @return a FactIdValue containing the shadow fact for the object
     * @throws JessException if anything goes wrong
     */
    public Value definstance(String jessTypename, Object object,
                             boolean dynamic, Context context)
            throws JessException {
        broadcastEvent(JessEvent.DEFINSTANCE, object, context);
        synchronized(getWorkingMemoryLock()) {
            jessTypename = resolveTemplateNameInFocusModule(jessTypename);
            return m_definstanceList.definstance(this, jessTypename, object, dynamic, context);
        }
    }

    /**
     * Returns the "shadow fact" that represents the given object in working memory.
     *
     * @param o an object in working memory
     * @return the shadow fact representing that object
     * @throws JessException if the given object is not in working memory
     */
    public Fact getShadowFactForObject(Object o) throws JessException {
        return m_definstanceList.getShadowFactForObject(this, o);
    }

    String resolveTemplateNameInFocusModule(String jessTypename) throws JessException {
        if (jessTypename.indexOf("::") != -1)
            return jessTypename;
        else if (getFocus().equals(getCurrentModule()))
            return jessTypename;

        String focusName = getFocus() + "::" + jessTypename;
        if (findDeftemplate(focusName) != null)
            return focusName;
        else
            return jessTypename;
    }

    /**
     * Remove the given object from working memory. Tell this engine
     * to stop pattern matching on the given object, and retract the
     * shadow fact.
     *
     * @param object an object currently represented in working memory.
     * @return the shadow fact for the object
     * @throws JessException if object isn't a definstanced object, or on error
     * @see Rete#add(java.lang.Object)
     * @see Rete#undefinstance(Object)
     */

    public Fact remove(Object object) throws JessException {

        Fact f = m_definstanceList.undefinstance(this, object);
        broadcastEvent(JessEvent.DEFINSTANCE | JessEvent.REMOVED, object, m_globalContext);
        return f;
    }


    /**
     * <p>Remove the given object from working memory. Tell this engine
     * to stop pattern matching on the given object, and retract the
     * shadow fact.</p>
     * <p>A synonym for "remove".</p>
     *
     * @param object an object currently represented in working memory.
     * @return the shadow fact for the object
     * @throws JessException if object isn't a definstanced object, or on error
     * @see Rete#add(Object)
     * @see Rete#remove(Object)
     */

    public Fact undefinstance(Object object) throws JessException {
        return remove(object);
    }

    void undefinstanceNoRetract(Object object) throws JessException {

        m_definstanceList.undefinstanceNoRetract(this, object);
        broadcastEvent(JessEvent.DEFINSTANCE | JessEvent.REMOVED, object, m_globalContext);
    }

    /**
     * Bring a shadow fact up to date. If the properties of the given
     * object, assumed to be a definstanced object, have changed, its
     * corresponding shadow fact will be updated.
     *
     * @param object a previously definstanced object
     * @return the shadow fact, as a FactIDValue
     * @throws JessException if object isn't a definstanced object, or on error
     */
    public Value updateObject(Object object) throws JessException {
        return updateObject(object, getGlobalContext());
    }

    public Value updateObject(Object object, Context context) throws JessException {
        return m_definstanceList.updateObject(object, context);
    }

    /**
     * Bring one slot of a shadow fact up to date. If the named
     * property of the given object, assumed to be a definstanced
     * object, has changed, its corresponding shadow fact will be
     * updated.
     *
     * @param object   a previously definstanced object
     * @param slotName the name of a property of that object
     * @return the shadow fact, as a FactIDValue
     * @throws JessException if object isn't a definstanced object, or on error
     */
    public Value updateObject(Object object, String slotName) throws JessException {
        return m_definstanceList.updateObject(object, slotName, getGlobalContext());
    }

    /**
     * Add an object to working memory using various defaults. In this
     * implementation, Jess assumes you want to use an existing
     * deftemplate with the same name as the class of this object,
     * minus the package names. If no such deftemplate exists, one is
     * created. The object is added with automatic support for
     * PropertyChangeEvents if they are generated by the object.
     *
     * @param o an object to add to working memory
     * @return the shadow fact, as a FactIdValue
     * @throws JessException if anything goes wrong
     * @see Rete#remove(Object)
     * @see Rete#undefinstance(Object)
     */
    public Value add(Object o) throws JessException {
        return add(o, getGlobalContext());
    }

    Value add(Object o, Context context) throws JessException {
        String name = o.getClass().getName();
        // TODO Smart search through defclasses
        // TODO Synchronization
        String templateName = existingTemplateForClass(o.getClass());
        if (templateName == null) {
            String shortName = ClassSource.classNameOnly(name);
            if (findDeftemplate(shortName) == null)
                defclass(shortName, name, null);
            templateName = shortName;
        } 
        boolean dynamic = Definstance.acceptsPropertyChangeListeners(o, this);
        return definstance(templateName, o, dynamic, context);
    }

    /**
     * Add a collection of objects to working memory. Calls add(Object) on each item in the collection.
     *
     * @param c a collection of objects to be added to working memory
     * @throws JessException if anything goes wrong
     * @see Rete#add(Object)
     * @see Rete#removeAll(java.util.Collection)
     */

    public void addAll(Collection c) throws JessException {
        addAll(c.iterator());
    }


    /**
     * Remove a collection of objects from working memory. Calls remove(Object) on each item in the collection.
     *
     * @param c a collection of objects to be removed from working memory
     * @throws JessException if anything goes wrong
     * @see Rete#remove(Object)
     * @see Rete#add(Object)
     */

    public void removeAll(Collection c) throws JessException {
        Iterator it = c.iterator();
        while (it.hasNext())
            remove(it.next());
    }

    /**
     * Add a collection of objects to working memory. Calls add(Object) on each item in the collection.
     *
     * @param it Iterator over a collection of objects to be added to working memory
     * @throws JessException if anything goes wrong
     * @see Rete#add(Object)
     */

    public void addAll(Iterator it) throws JessException {
        while (it.hasNext())
            add(it.next());
    }

    /**
     * Add a defclass definition (a deftemplate) to this engine. One
     * slot is included for each JavaBeans property of the class. In
     * addition, slots representing public member variables can be
     * included.
     *
     * @param jessName               the name Jess should use for this defclass
     * @param clazz                  the name of the Java class
     * @param parent                 if non-null, a parent deftemplate or defclass name
     * @param includeMemberVariables if true, slots corresponding to
     *                               public member variables are created.
     */

    public Value defclass(String jessName, String clazz, String parent, boolean includeMemberVariables)
            throws JessException {
        broadcastEvent(JessEvent.DEFCLASS, jessName, m_globalContext);
        try {
            synchronized (getWorkingMemoryLock()) {
                ClassTemplateMaker dtc = new ClassTemplateMaker(this, clazz);

                // Link the Jess name to the Java class name
                mapDefclassName(resolveName(jessName), dtc.getClassName());

                Deftemplate template = dtc.createDeftemplate(jessName, parent, includeMemberVariables);

                // Install our synthetic deftemplate
                addDeftemplate(template);

                // Return the real class name
                return m_valueFactory.get(dtc.getClassName(), RU.SYMBOL);
            }
        } catch (ClassNotFoundException cnfe) {
            throw new JessException("defclass", "Class not found:", cnfe);
        }
    }

    private void mapDefclassName(String jessName, String className) {
        m_javaClasses.put(jessName, className);
        m_templatesByClass.put(className, jessName);
    }


    /**
     * Add a defclass definition (a deftemplate) to this engine. One
     * slot will be created for each JavaBeans property of the class.
     *
     * @param jessName The name Jess should use for this defclass
     * @param clazz    The name of the Java class
     * @param parent   If non-null, a parent deftemplate or defclass name
     */

    public Value defclass(String jessName, String clazz, String parent)
            throws JessException {
        return defclass(jessName, clazz, parent, false);
    }


    DefinstanceList getDefinstanceList() {
        return m_definstanceList;
    }

    // **********************************************************************
    // Miscellaneous
    // **********************************************************************

    /**
     * Fetch the global execution context. This gives you access to
     * defglobals and variables defined at the global scope.
     *
     * @return The global execution context.
     */
    public final Context getGlobalContext() {
        return m_globalContext;
    }

    /**
     * Evaluate a Jess expression in this engine's global context. The
     * result is returned as a jess.Value; you can use the methods of
     * that class to extract the data. For example, here we define a
     * deffunction and then call it, storing the result in an int variable:
     * <p/>
     * <pre>
     * Rete r = new Rete();
     * r.eval("(deffunction square (?n) (return (* ?n ?n)))");
     * Value v = r.eval("(square 3)");
     * int nine = v.intValue(r.getGlobalContext()));
     * </pre>
     * <p/>
     * <p> Note that you may only pass one function call or construct at a
     * time to <tt>eval().</tt></p>
     *
     * @param cmd a string containing a Jess expression
     * @return the result of evaluating the expression
     * @throws JessException if anything goes wrong
     */

    public Value eval(String cmd) throws JessException {
        return eval(cmd, m_globalContext);
    }


    /**
     * Evaluate a Jess expression in this engine's global context. The
     * result is returned as a jess.Value; you can use the methods of
     * that class to extract the data.
     *
     * @param cmd a string containing a Jess expression
     * @return the result of evaluating the expression
     * @throws JessException if anything goes wrong
     * @see Rete#eval(String)
     * @deprecated Use eval() instead.
     */
    public Value executeCommand(String cmd) throws JessException {
        return eval(cmd, m_globalContext);
    }

    /**
     * Evaluate a Jess expression in the given execution context. The
     * result is returned as a jess.Value; you can use the methods of
     * that class to extract the data. For example, here we define a
     * deffunction and then call it, storing the result in an int variable:
     * <p/>
     * <pre>
     * Rete r = new Rete();
     * r.eval("(deffunction square (?n) (return (* ?n ?n)))");
     * Value v = r.eval("(square 3)");
     * int nine = v.intValue(r.getGlobalContext()));
     * </pre>
     * <p/>
     * <p> Note that you may only pass one function call or construct at a
     * time to <tt>eval().</tt></p>
     *
     * @param cmd     a string containing a Jess expression
     * @param context the evaluation context
     * @return the result of evaluating the expression
     * @throws JessException if anything goes wrong
     */
    public Value eval(String cmd, Context context) throws JessException {
        synchronized (m_tis) {
            m_tis.clear();
            m_jesp.clearStack();
            m_tis.appendText(cmd);
            return m_jesp.parse(false, context);
        }
    }

    /**
     * Evaluate a Jess expression in the given context. The result is
     * returned as a jess.Value; you can use the methods of that class
     * to extract the data.
     *
     * @param cmd     a string containing a Jess expression
     * @param context the evaluation context
     * @return the result of evaluating the expression
     * @throws JessException if anything goes wrong
     * @see Rete#eval(String)
     * @deprecated Use eval() instead.
     */
    public Value executeCommand(String cmd, Context context) throws JessException {
        return eval(cmd, context);
    }

    private Jesp initInternalParser() {
        Jesp jesp = new Jesp(m_tis, this);
        jesp.setFileName("<eval pipe>");
        return jesp;
    }

    /**
     * Set the resetGlobals property. When resetGlobals is true, the
     * initializers of defglobals are evaluated when (reset) is
     * executed. When it is false, (reset) doesn't reinitialize
     * defglobals.
     *
     * @param reset The value of this property
     */
    public final void setResetGlobals(boolean reset) {
        m_resetGlobals = reset;
    }

    /**
     * Return the resetGlobals property. When resetGlobals is true,
     * the initializers of defglobals are evaluated when (reset) is
     * executed. When it is false, (reset) doesn't reinitialize
     * defglobals.
     *
     * @return The value of this property
     */
    final public boolean getResetGlobals() {
        return m_resetGlobals;
    }

    /**
     * Fetch the ReteCompiler object used by the engine. You
     * probabably shouldn't use this for anything!
     *
     * @return the Compiler object
     */

    final ReteCompiler getCompiler() {
        return m_compiler;
    }

    /**
     * Return the object that concurrent access to working memory should synchonize on.
     * The engine's rule compiler is no longer used for this purpose.
     */
    
    final Object getWorkingMemoryLock() {
        return m_workingMemoryLock;
    }

    /**
     * Store a value in the engine under a given name for later
     * retrieval. The store and fetch methods provide a simple way to
     * exchange data between Jess and Java. They give you easy access
     * to a single HashMap from both languages, so you can use the
     * HashMap as a "mailbox". Use store() or the Jess function
     * (store) to save a value in this map; use fetch() or the Jess
     * function (fetch) to retrieve the value.
     *
     * @param name a key under which to save the value
     * @param val  the value to store
     * @return any old value stored under this name, or null.
     * @see Rete#fetch
     */

    public Value store(String name, Value val) {
        if (val == null)
            return m_storage.remove(name);
        else
            return m_storage.put(name, val);
    }

    /**
     * Store a value in the engine under a given name for later
     * retrieval by fetch. The Object is first wrapped in a new
     * jess.Value object. The store and fetch methods provide a simple
     * way to exchange data between Jess and Java. They give you easy
     * access to a single HashMap from both languages, so you can use
     * the HashMap as a "mailbox". Use store() or the Jess function
     * (store) to save a value in this map; use fetch() or the Jess
     * function (fetch) to retrieve the value.
     *
     * @param name a key under which to file the value
     * @param val  the value to store
     * @return any old value stored under this name, or null
     * @see Rete#fetch
     */

    public Value store(String name, Object val) {
        if (val == null)
            return m_storage.remove(name);
        else
            return m_storage.put(name, new Value(val));
    }

    /**
     * Retrieve an object previously stored with store(). The store
     * and fetch methods provide a simple way to exchange data between
     * Jess and Java. They give you easy access to a single HashMap
     * from both languages, so you can use the HashMap as a
     * "mailbox". Use store() or the Jess function (store) to save a
     * value in this map; use fetch() or the Jess function (fetch) to
     * retrieve the value.<p/>
     * <p/>
     * The result is returned as a jess.Value * object; you must use
     * the methods of that class to extract the * underlying data.
     *
     * @param name the key under which to find an object
     * @return the object, or null if not found.
     * @see Rete#store
     */

    public Value fetch(String name) {
        return m_storage.get(name);
    }

    /**
     * Clear the storage used by store() and fetch().
     */

    public void clearStorage() {
        m_storage.clear();
    }


    int getTime() {
        return m_factList.getTime();
    }


    /**
     * Return the Factory Jess will use to create Rete Tokens. This
     * method allows extensions like FuzzyJess to use special
     * augmented tokens to store additional information. The vast
     * majority of clients will never use this method.
     *
     * @return the current Token Factory implementation
     */
    public static Factory getFactory() {
        return s_factory;
    }

    /**
     * Set the Factory Jess will use to create Rete Tokens. This
     * method allows extensions like FuzzyJess to use special
     * augmented tokens to store additional information. The vast
     * majority of clients will never use this method.
     *
     * @param f a jess.Factory implementation
     */
    public static void setFactory(Factory f) {
        s_factory = f;
    }

    private void loadScriptlib() {
        try {
            batch(getLibraryPath());                        
        } catch (JessException je) {
            // TODO Explain the problem!
            je.printStackTrace();
        }
    }

    static String getLibraryPath() {        
        try {
            String path = System.getProperty(LIBRARY_PATH_ID);
            if (path != null)
                return new File(path, LIBRARY_NAME).getCanonicalPath();

            
        } catch (Exception ignore) {
            // FALL THROUGH
        }
        return LIBRARY_NAME;
    }

    /**
     * Execute a file of Jess language code or JessML code. Jess will
     * look on the file system relative to the current directory, and
     * also along the CLASSPATH, to find the named file. If Jess is
     * running in an applet, the document base will also be checked.
     *
     * @param filename the name of a file or resource
     * @return the value of the last item parsed from the file, or FALSE
     * @throws JessException if there are I/O problems, or syntax errors in the file
     * @see Batch#batch
     */
    public Value batch(String filename) throws JessException {
        return Batch.batch(filename, this);
    }

    /**
     * Invoke the named query and return the results. The QueryResult
     * has an API similar to java.sql.ResultSet. The global context is
     * used to resolve variables.
     *
     * @param name   the name of the query
     * @param params the query's parameters
     * @return a QueryResult containing all the matches
     * @throws JessException if anything goes wrong
     */

    public QueryResult runQueryStar(String name, ValueVector params)
            throws JessException {
        return runQueryStar(name, params, getGlobalContext());
    }


    /**
     * Invoke the named query and return the results. The QueryResult
     * has an API similar to java.sql.ResultSet. The given context is
     * used to resolve variables.
     *
     * @param name   the name of the query
     * @param params the query's parameters
     * @return a QueryResult containing all the matches
     * @throws JessException if anything goes wrong
     */

    public QueryResult runQueryStar(String name, ValueVector params, Context context)
            throws JessException {
        Iterator it = doRunQuery(name, params, context);
        return new QueryResult(it, context);
    }

    /**
     * Invoke the named query and return an iterator over the matching
     * results. Each match is a jess.Token object. The global context
     * is used to resolve variables.
     *
     * @param name   the name of the query
     * @param params the query's parameters
     * @return an iterator over all results
     * @throws JessException if anything goes wrong
     * @deprecated Since Jess 7.0, superceded by runQueryStar()
     */

    public Iterator runQuery(String name, ValueVector params)
            throws JessException {
        //noinspection deprecation
        return runQuery(name, params, getGlobalContext());
    }

    /**
     * Invoke the named query and return an iterator over the matching
     * results. Each match is a jess.Token object. The global context
     * is used to resolve variables.
     *
     * @param name   the name of the query
     * @param params the query's parameters
     * @return an iterator over all results
     * @throws JessException if anything goes wrong
     * @deprecated Since Jess 7.0, superceded by runQueryStar()
     */

    public Iterator runQuery(String name, ValueVector params, Context context)
            throws JessException {
        return strippedQueryIterator(name, params, context);
    }

    Iterator strippedQueryIterator(String name, ValueVector params, Context context) throws JessException {
        final Iterator iteratorOverQueryResultRows = doRunQuery(name, params, context);
        return new Iterator() {

            public void remove() {
                iteratorOverQueryResultRows.remove();
            }

            public boolean hasNext() {
                return iteratorOverQueryResultRows.hasNext();
            }

            public Object next() {
                return ((QueryResultRow) iteratorOverQueryResultRows.next()).getToken();
            }
        };
    }

    private Iterator doRunQuery(String name, ValueVector params, Context context) throws JessException {
        HasLHS lhs = findDefrule(name);
        if (!(lhs instanceof Defquery))
            throw new JessException("runQuery", "No such query:", name);

        Defquery query = (Defquery) lhs;

        if ((params.size()) != query.getNVariables())
            throw new JessException("runQuery", "Wrong number of variables for query", name);

        // Create the query-trigger fact
        Fact f = new Fact(query.getQueryTriggerName(), this);
        f.setSlotValue(RU.DEFAULT_SLOT_NAME, new Value(params, RU.LIST));

        // Assert the fact, blocking access to other queries; then return the
        // results, which clears the query
        synchronized (getWorkingMemoryLock()) {
            synchronized (query) {
                query.clearResults(this);
                assertFact(f, context);
                // Allow backwards chaining to occur
                if (query.getMaxBackgroundRules() > 0)
                    run(query.getMaxBackgroundRules());
                Iterator it = query.getResults(this);

                query.clearResults(this);
                retract(f);
                return it;
            }
        }
    }

    /**
     * Invoke the named query and return the count of matching
     * results. The global context is used to resolve variables.
     *
     * @param name   the name of the query
     * @param params the query's parameters
     * @return the count of matching results
     * @throws JessException if anything goes wrong
     */

    public int countQueryResults(String name, ValueVector params)
            throws JessException {
        return countQueryResults(name, params, getGlobalContext());
    }

    /**
     * Invoke the named query and return the count of matching
     * results. The given context is used to resolve variables.
     *
     * @param name    the name of the query
     * @param params  the query's parameters
     * @param context an execution context
     * @return the count of matching results
     * @throws JessException if anything goes wrong
     */

    public int countQueryResults(String name, ValueVector params, Context context) throws JessException {
        HasLHS lhs = findDefrule(name);
        if (lhs == null || !(lhs instanceof Defquery))
            throw new JessException("countQueryResults", "No such query:", name);

        Defquery query = (Defquery) lhs;

        if ((params.size()) != query.getNVariables())
            throw new JessException("countQueryResults", "Wrong number of variables for query", name);

        // Create the query-trigger fact
        Fact f = new Fact(query.getQueryTriggerName(), this);
        f.setSlotValue(RU.DEFAULT_SLOT_NAME, new Value(params, RU.LIST));

        // Assert the fact, blocking access to other queries; then return the
        // results, which clears the query
        synchronized (getWorkingMemoryLock()) {
            synchronized (query) {
                query.clearResults(this);
                assertFact(f, context);
                // Allow backwards chaining to occur
                if (query.getMaxBackgroundRules() > 0)
                    run(query.getMaxBackgroundRules());
                int count = query.countResults(this);

                query.clearResults(this);
                retract(f);
                return count;
            }
        }
    }

    // **********************************************************************
    // The watch facility
    // **********************************************************************

    /**
     * Produce some debugging information. The argument specifies
     * which kind of event will be reported. The output goes to the
     * <i>watch router</i>, which is initially "WSTDOUT". You can set
     * the watch router to anything you'd like.
     *
     * @param which one of the constants in WatchConstants
     * @throws JessException if the argument is invalid
     * @see jess.WatchConstants
     * @see jess.Rete#setWatchRouter(java.lang.String)
     */

    public void watch(int which) throws JessException {
        m_watchInfo[which] = true;
        int mask;
        switch (which) {
            case WatchConstants.RULES:
                mask = JessEvent.DEFRULE_FIRED;
                break;

            case WatchConstants.FACTS:
                mask = JessEvent.FACT;
                break;

            case WatchConstants.ACTIVATIONS:
                mask = JessEvent.ACTIVATION;
                break;

            case WatchConstants.COMPILATIONS:
                mask = JessEvent.DEFRULE;
                break;

            case WatchConstants.FOCUS:
                mask = JessEvent.FOCUS;
                break;

            default:
                throw new JessException("watch", "Bad argument ", which);
        }
        m_watchInfo[which] = true;
        mask = getEventMask() | mask | JessEvent.CLEAR;
        setEventMask(mask);
    }

    /**
     * Cancel some debugging information. The argument specifies which
     * kind of event will no longer be reported.
     *
     * @param which one of the constants in WatchConstants
     * @throws JessException if the argument is invalid
     * @see jess.WatchConstants
     */
    public void unwatch(int which) throws JessException {
        int mask;
        switch (which) {
            case WatchConstants.RULES:
                mask = JessEvent.DEFRULE_FIRED;
                break;

            case WatchConstants.FACTS:
                mask = JessEvent.FACT;
                break;

            case WatchConstants.ACTIVATIONS:
                mask = JessEvent.ACTIVATION;
                break;

            case WatchConstants.COMPILATIONS:
                mask = JessEvent.DEFRULE;
                break;

            case WatchConstants.FOCUS:
                mask = JessEvent.FOCUS;
                break;

            default:
                throw new JessException("unwatch", "Bad argument ", which);
        }
        m_watchInfo[which] = false;
        mask = getEventMask() & ~mask;
        setEventMask(mask);
    }

    /**
     * Produce all possible debugging info. Equivalent to calling watch()
     * multiple times using each legal argument in succession.
     */
    public void watchAll() {
        for (int i = 0; i < m_watchInfo.length; ++i)
            m_watchInfo[i] = true;

        int mask = JessEvent.DEFRULE |
                JessEvent.DEFRULE_FIRED |
                JessEvent.FACT |
                JessEvent.FOCUS |
                JessEvent.ACTIVATION;
        mask = getEventMask() | JessEvent.CLEAR | mask;
        setEventMask(mask);
    }

    /**
     * Cancel all debugging info. Equivalent to calling unwatch()
     * using each legal argument in succession.
     */
    public void unwatchAll() {
        for (int i = 0; i < m_watchInfo.length; ++i)
            m_watchInfo[i] = false;

        int mask = JessEvent.DEFRULE |
                JessEvent.DEFRULE_FIRED |
                JessEvent.FACT |
                JessEvent.FOCUS |
                JessEvent.ACTIVATION;
        mask = getEventMask() & ~mask;
        setEventMask(mask);
    }

    // TODO This is probably a bottleneck
    private boolean watchingAny() {
        for (int i = 0; i < m_watchInfo.length; ++i)
            if (m_watchInfo[i])
                return true;
        return false;
    }

    private boolean watching(int which) {
        return m_watchInfo[which];
    }

    /**
     * Responds to a JessEvent by emitting "watch" messages. This
     * method is just an implementation detail, part of the "watch"
     * facility. Each Rete object is registered with itself as a
     * listener.
     *
     * @param je An event object
     */
    public void eventHappened(JessEvent je) {
        if (!watchingAny())
            return;
        int type = je.getType();
        boolean remove = (type & JessEvent.REMOVED) != 0;
        boolean modified = (type & JessEvent.MODIFIED) != 0;

        PrintWriter pw = getWatchRouter();
        if (pw == null)
            return;

        switch (type & ~JessEvent.REMOVED & ~JessEvent.MODIFIED) {

            case JessEvent.FACT: {
                if (watching(WatchConstants.FACTS)) {
                    Fact f = (Fact) je.getObject();
                    pw.print(remove ? " <== " : modified ? " <=> " : " ==> ");
                    pw.print("f-");
                    pw.print(f.getFactId());
                    pw.print(" ");
                    pw.println(f);
                    pw.flush();
                }
                break;
            }

            case JessEvent.FOCUS: {
                if (watching(WatchConstants.FOCUS)) {
                    pw.print(remove ? " <== " : " ==> ");
                    pw.print("Focus ");
                    pw.println(je.getObject());
                    pw.flush();
		}
                break;
            }

            case JessEvent.DEFRULE_FIRED: {
                if (watching(WatchConstants.RULES))
                    ((Activation) je.getObject()).debugPrint(pw);
                break;
            }

            case JessEvent.ACTIVATION: {
                if (watching(WatchConstants.ACTIVATIONS)) {
                    Activation a = (Activation) je.getObject();
                    pw.print(remove ? "<== " : "==> ");
                    pw.print("Activation: ");
                    pw.print(a.getRule().getDisplayName());
                    pw.print(" : ");
                    pw.println(a.getToken().factList());
                    pw.flush();
                }
                break;
            }

            case JessEvent.DEFRULE: {
                if (watching(WatchConstants.COMPILATIONS) && !remove) {
                    pw.println(((HasLHS) je.getObject()).getCompilationTrace());
                    pw.flush();
                }
                break;
            }

            default:
                break;
        }
    }

    /**
     * Sets the router the "watch" facility will use for output. The
     * default is WSTDOUT. The named router must exist.
     *
     * @param s the name of the router to use
     * @return the name of the previous watch router
     * @throws JessException if the router doesn't exist
     */
    public String setWatchRouter(String s) throws JessException {
        if (m_routers.getOutputRouter(s) == null)
            throw new JessException("Rete.setWatchRouter", "Invalid router name", s);
        String old = m_watchRouter;
        m_watchRouter = s;
        return old;
    }

    private PrintWriter getWatchRouter() {
        return m_routers.getOutputRouterAsPrintWriter(m_watchRouter);
    }

    // ****************************************************************
    // Require/provide
    // ****************************************************************

    /**
     * Register a named feature, as if by the "provide"
     * function. After calling this method, calling isFeatureDefined
     * with the same argument will always return true.
     *
     * @param name the name of the feature
     */

    public void defineFeature(String name) {
        m_features.add(name);
    }

    /**
     * Return whether a feature name has been registered by the
     * "provide" function.
     *
     * @param name the name of the feature
     * @return true if the feature is defined
     */

    public boolean isFeatureDefined(String name) {
        return m_features.contains(name);
    }

    // ****************************************************************
    // Logical support facility
    // ****************************************************************

    void removeLogicalSupportFrom(Token token, Fact fact) {
        m_factList.removeLogicalSupportFrom(token, fact);
    }

    /**
     * Returns a list of one or more jess.Token objects that provide
     * logical support for this fact.  This method returns null if
     * there is no specific logical support. You can use the
     * Token.size() method to check how many supporting Facts are in a
     * Token, and the Token.fact() method to check each supporting
     * Fact in turn. This is a fast operation, taking O(ln N) time,
     * where N is the number of facts that have logical support.
     *
     * @param fact a fact of interest
     * @return a list of supporting Token objects, or null if there is
     *         unconditional support
     * @see jess.Token
     * @see jess.Token#fact
     * @see jess.Token#size
     */

    public List getSupportingTokens(Fact fact) {
        return m_factList.getSupportingTokens(fact);
    }

    /**
     * Returns a list of Fact objects that receive logical support
     * from the argument.  This method is potentially expensive, as it
     * takes time proportional to O(N), where N is the number of facts
     * currently receiving logical support of any kind.
     *
     * @param supporter a fact of interest
     * @return a list of zero or more Fact objects
     */

    public List getSupportedFacts(Fact supporter) {
        return m_factList.getSupportedFacts(supporter);
    }

    // *************************************************
    // Class and resource loading
    // *************************************************   

    /**
     * Return the Class that represents the "app object" for this
     * Rete. This class's ClassLoader plays a special role: it is used
     * to load classes named in Jess language code.
     *
     * @return the Class object whose ClassLoader will be used
     */
    public Class getAppObjectClass() {
        return m_classSource.getAppObjectClass();
    }

    /**
     * Specify the "app object" for this Rete. The app object's
     * ClassLoader will be used to load classes named in Jess code. By
     * default, a Rete object is its own app object.
     *
     * @param appObject the "app object".
     */
    public void setAppObject(Object appObject) {
        m_classSource.setAppObject(appObject);
    }

    /**
     * Specify the ClassLoader that will be used to load classes named
     * in Jess code. By default, the context ClassLoader is used.
     *
     * @param loader the ClassLoader to use
     */
    public void setClassLoader(ClassLoader loader) {
        m_classSource.setClassLoader(loader);
    }

    /**
     * Return the ClassLoader that will be used to find classes named
     * in Jess ocde. By default, the context ClassLoader is used.
     *
     * @return the ClassLoader used to load user classes
     */

    public ClassLoader getClassLoader() {
        return m_classSource.getClassLoader();
    }

    /**
     * Load a class using the active ClassLoader. Previously loaded
     * classes are cached. The import tables are used, so the given
     * name doesn't have to be fully qualified.
     *
     * @param className the name of the class
     * @return the Class object
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public Class findClass(String className) throws ClassNotFoundException {
        return m_classSource.findClass(className);
    }

    /**
     * Load a resource using the current ClassLoader's getResource() method.
     *
     * @param name the name of the resource
     * @return a URL for the resource
     */
    public URL getResource(String name) {
        return m_classSource.getResource(name);
    }

    /**
     * Make all the classes in a package available for use in
     * unqualified form from Jess code. The package name should
     * include a trailing "." -- for example, "java.lang." .
     *
     * @param pack the name of a Java package
     */

    public void importPackage(String pack) {
        m_classSource.importPackage(pack);
    }

    /**
     * Make a single class name available for use in unqualified form
     * in Jess code. Also for each public static method and variable,
     * create a Jess function with name of the form "Class.member"
     * which, when called, defers to the given static member, either
     * calling the method or reading the value of the variable.
     *
     * @param clazz the fully-qualified name of the class
     * @throws JessException if the class is not found
     */
    public void importClass(String clazz) throws JessException {
        m_classSource.importClass(clazz);
    }

    /**
     * Return the ClassResearcher this engine will use to learn about Java classes mentioned in Jess code.
     *
     * @return the current class researcher
     */
    public ClassResearcher getClassResearcher() {
        return m_classResearcher;
    }

    /**
     * Set the ClassResearcher this engine will use to learn about Java classes mentioned in Jess code.
     * @param researcher
     */

    public void setClassResearcher(ClassResearcher researcher) {
        m_classResearcher = researcher;
    }

    /**
     * Returns "[Rete]".
     *
     * @return the String "[Rete]".
     */
    public String toString() {
        return "[Rete]";
    }


    /**
     * <p>Fetch the value factory for this engine. The value factory should be used as a source of jess.Value objects, because
     * they are immutable and can be cached and shared. Using the ValueFactory will generally be more efficient than allocating your
     * own objects.</p>
     *
     * <p>Peered engines share a ValueFactory by default; otherwise each engine creates its own.</p>
     *
     * @return the value factory used by this engine.
     * @see ValueFactory
     * @see #setValueFactory(ValueFactory)
     */
    public ValueFactory getValueFactory() {
        return m_valueFactory;
    }

    /**
     * Set the value factory used by this engine. You can create your own custom subclass of ValueFactory to implement your own
     * caching, logging, or tracing scheme.
     *
     * @param factory the new factory
     * @see #getValueFactory()
     */
    public void setValueFactory(ValueFactory factory) {
        m_valueFactory = factory;
    }

    long getNextNodeKey() {
        return m_compiler.getNextNodeKey();
    }

    Object getKeyedStorage(Object key) {
        return m_keyedStorage.get(key);
    }

    void putKeyedStorage(Object key, Object value) {
        m_keyedStorage.put(key, value);
    }

    /**
     * The character used to denote memberhood in "Java patterns."
     * Normally a period, but can be changed to allow dots in slot names (for example.)
     */
    public char getMemberChar() {
        return m_memberChar;
    }

    /**
     * Change the character that denotes a member in "Java patterns."     
     */
    public void setMemberChar(char c) {
        m_memberChar = c;
    }

    /**
     * Returns a URL that resources might be loaded from; useful in applets
     * @return a URL
     * @since 8.0
     */
	public URL getDocumentBase() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns the root of the Rete network. Most users should never need this method; use with caution.
	 */
	public Node getRoot() {
		return m_compiler.getRoot();
	}
}



