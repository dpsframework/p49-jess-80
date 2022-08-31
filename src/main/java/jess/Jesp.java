package jess;

import java.io.Reader;
import java.util.*;

import jess.Deffunction.Argument;

/**
 * <p>The Jess language parser.
 * You can parse Jess language code directly with this class.
 * Simply loading the contents of a file (or any
 * other data source that can be supplied as a <tt>java.io.Reader</tt> is very easy:</p>
 * <p/>
 * <p/>
 * <pre>
 * Rete engine = new Rete();
 * FileReader file = new FileReader("myfile.clp");
 * try {
 *     Jesp parser = new Jesp(file, engine);
 *     parser.parse(false);
 * } finally {
 *     file.close();
 * }
 * </pre>
 * <p/>
 * <p>But this class's public interface is much richer than that. If
 * you want to, you can parse the file one expression at a time,
 * and have access to the parsed data. The method
 * {@link #parseExpression} returns <tt>java.lang.Object</tt>,
 * and the returned object can either be a {@link Value} or
 * one of the Jess classes that represent a construct
 * ({@link Defrule}, {@link Deftemplate}, etc.) In
 * addition, you can choose to have the parser execute function
 * calls as it parses them, or simply return them to you
 * unexecuted (this is controlled by the second argument to
 * <tt>parseExpression</tt>.</p>
 * <p/>
 * <pre>
 * Rete engine = new Rete();
 * FileReader file = new FileReader("myfile.clp");
 * Context context = engine.getGlobalContext();
 * try {
 *     Jesp parser = new Jesp(file, engine);
 *     Object result = Funcall.TRUE;
 *     while (!result.equals(Funcall.EOF)) {
 *         result = parser.parseExpression(context, false);
 *         // Here you can use instanceof to determine what sort
 *         // of object "result" is, and process it however you want
 *     }
 * } finally {
 *     file.close();
 * }
 * </pre>
 * <p/>
 * <p/>
 * There are also methods in this class to control whether
 * comments should be returned from <tt>parseExpression</tt>
 * or just skipped, methods to fetch various pieces of
 * information about the parsing process, and a mechanism for
 * controlling whether warnings or just errors should be
 * reported.
 * </p>
 * <p/>
 * Instances of <tt>Jesp</tt> are not serializable, as they hold a
 * reference to a Reader.</p>
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class Jesp implements ErrorSink {
    final static String JAVACALL = "call";
    public final static String PROMPT = "Jess> ";

    /**
     * Stream where input comes from
     */
    private JessTokenStream m_jts;
    private Rete m_engine;
    private boolean m_issueWarnings = false;
    private ArrayList<ParseException> m_warnings = new ArrayList<ParseException>();
    private String m_fileName = "<unknown>";

    private static final String AUTO_FOCUS = "auto-focus";
    private final static String[] RULE_DECLARABLES = {
            "salience",
            "node-index-hash",
            AUTO_FOCUS,
            "no-loop"
    };

    private final static String[] QUERY_DECLARABLES = {
            "node-index-hash",
            "variables",
            "max-background-rules"
    };

    private final static String[] DEFMODULE_DECLARABLES = {
            AUTO_FOCUS,
    };

    static final String ORDERED = "ordered";
    static final String FROM_CLASS = "from-class";
    static final String INCLUDE_VARIABLES = "include-variables";
    static final String BACKCHAIN_REACTIVE = "backchain-reactive";
    static final String SLOT_SPECIFIC = "slot-specific";

    private final static String[] DEFTEMPLATE_DECLARABLES = {
            SLOT_SPECIFIC,
            BACKCHAIN_REACTIVE,
            FROM_CLASS,
            INCLUDE_VARIABLES,
            ORDERED
    };

    private final static String[] SLOT_QUALIFIERS = {
            "type",
            "default",
            "default-dynamic",
            "allowed-values",
    };

    private static final String[] OPEN_PAREN = {"("};
    static final String[] CLOSE_PAREN = {")"};
    private static final String[] PARENS = {"(", ")"};
    private static final String[] SLOT_TYPE = new String[]{"slot", "multislot"};
    private static final String[] DECLARE_OR_SLOT_TYPE = new String[]{"declare", "slot", "multislot"};
    private static final String[] RIGHT_ARROW = new String[]{"=>"};
    private static final String[] CONSTRUCT_NAMES = {
            "defrule", "deffunction", "deftemplate", "deffacts", "defglobal", "defmodule", "defquery"
    };
    private HashMap<String, ArgumentChecker> m_checkers = new HashMap<String, ArgumentChecker>();

    static {
        Arrays.sort(CONSTRUCT_NAMES);
    }

    /**
     * Construct a Jesp object.
     * The reader will be looked up in the Rete object's router tables,
     * and any wrapper found there will be used.
     *
     * @param reader The Reader from which this Jesp should get its input
     * @param engine The engine that the parsed commands go to
     */

    public Jesp(Reader reader, Rete engine) {
        this(engine);
        // ###
        Tokenizer t = engine.getInputWrapper(reader);

        if (t == null) {
            t = new ReaderTokenizer(reader, false);
        }

        m_jts = new JessTokenStream(t);
    }


    /**
     * Construct a Jesp object.
     * The given tokenizer will be used directly
     *
     * @param tokenizer The Tokenizer from which this Jesp should get its input
     * @param engine    The engine that the parsed commands go to
     */
    public Jesp(Tokenizer tokenizer, Rete engine) {
        this(engine);
        // ###
        m_jts = new JessTokenStream(tokenizer);
    }

    private Jesp(Rete engine) {
        m_engine = engine;

        QueryArgumentChecker checker = new QueryArgumentChecker();
        addArgumentChecker("run-query", checker);
        addArgumentChecker("run-query*", checker);
        addArgumentChecker("count-query-results", checker);
        addArgumentChecker("if", new IfArgumentChecker());
    }

    /**
     * Add an ArgumentChecker to this parser. The ArgumentChecker's {@link ArgumentChecker#check}
     * method will be called whenever a matching function call is parsed.
     *
     * @param name    the name of the function for which this ArgumentChecker should be invoked
     * @param checker the ArgumentChecker to use
     */
    public void addArgumentChecker(String name, ArgumentChecker checker) {
        m_checkers.put(name, checker);
    }

    /**
     * Return the rule engine this parser is attached to .
     *
     * @return the Rete instance
     */
    public Rete getEngine() {
        return m_engine;
    }

    /**
     * Turn parser warnings on (true) or off (false).
     *
     * @param required true if warnings should be issued
     */
    public void setIssueWarnings(boolean required) {
        m_issueWarnings = required;
    }

    /**
     * Clear any pending parser warnings.
     */
    public void clearWarnings() {
        m_warnings.clear();
    }

    /**
     * Get the list of currently applicable warnings; each warning is an instance of {@link ParseException}.
     *
     * @return a list of warnings
     */
    public List<ParseException> getWarnings() {
        return m_warnings;
    }

    /**
     * Wrapper for {@link jess.JessTokenStream#getStreamPos}.
     *
     * @return the stream position
     */
    public int getStreamPos() {
        return m_jts.getStreamPos();
    }

    /**
     * Consumes all whitespace up to the next non-whitespace token.
     *
     * @throws JessException if an input error occurs
     */
    public void eatWhitespace() throws JessException {
        m_jts.eatWhitespace();
    }

    /**
     * Consumes all whitespace and comments up to the next non-whitespace, non-comment token.
     *
     * @throws JessException if an input error occurs
     */
    public void eatWhitespaceAndComments() throws JessException {
        JessToken token = nextToken(m_jts);
        m_jts.pushBack(token);
    }

    JessTokenStream getTokenStream() {
        return m_jts;
    }

    /**
     * Parses an input file.
     * Argument is true if a prompt should be printed (to the
     * Rete object's standard output), false for no prompt. Uses the Rete object's global context to resolve variables.
     *
     * @param prompt True if a prompt should be printed.
     * @return The result of the last parsed entity (often TRUE or FALSE).
     * @throws JessException If anything goes wrong.
     */

    public Value parse(boolean prompt) throws JessException {
        return parse(prompt, m_engine.getGlobalContext());
    }

    /**
     * Parses an input file by calling {@link #promptAndParseOneExpression} in a loop.
     * Argument is true if a prompt should be printed (to the
     * Rete object's standard output), false for no prompt. Uses the given context to resolve variables.
     *
     * @param prompt True if a prompt should be printed.
     * @return The result of the last parsed entity (often TRUE or FALSE).
     * @throws JessException If anything goes wrong.
     */
    public synchronized Value parse(boolean prompt, Context context)
            throws JessException {

        Value val = Funcall.TRUE, oldval = val;

        while (!val.equals(Funcall.EOF)) {

            oldval = val;
            val = promptAndParseOneExpression(prompt, context);

        }
        return oldval;
    }

    /**
     * Parse and return a single expression. The expression is always printed to the Rete object's standard output.
     * Argument is true if a prompt should be printed (to the
     * Rete object's standard output), false for no prompt. Uses the given context to resolve variables.
     *
     * @param prompt True if a prompt should be printed.
     * @return The result of the last parsed entity (often TRUE or FALSE).
     * @throws JessException If anything goes wrong.
     */
    public Value promptAndParseOneExpression(boolean prompt, Context context) throws JessException {
        Value val;
        if (prompt) {
            m_engine.getOutStream().print(PROMPT);
            m_engine.getOutStream().flush();
        }
        Object result = parseExpression(context, true, m_jts);
        if (result instanceof Value)
            val = (Value) result;
        else
            val = Funcall.TRUE;

        if (prompt) {
            if (!val.equals(Funcall.NIL)) {
                if (val.type() == RU.LIST)
                    // Add parens to list
                    m_engine.getOutStream().print('(');

                m_engine.getOutStream().print(val);

                if (val.type() == RU.LIST)
                    m_engine.getOutStream().print(')');

                m_engine.getOutStream().println();
            }
        }
        return val;
    }

    /**
     * Flush any partially-parsed information, probably to the next
     * ')'. Useful in error recovery.
     */

    public void clear() {
        m_jts.clear();
    }

    void clearStack() {
        m_jts.clearStack();
    }

    /**
     * Parses an input file containing only facts, asserts each one.
     *
     * @return The symbol TRUE
     * @throws JessException If an error occurs
     */
    public Value loadFacts(Context c) throws JessException {
        JessToken jt = nextToken(m_jts);

        while (jt.m_ttype != JessToken.NONE_TOK) {
            m_jts.pushBack(jt);
            Fact f = parseFact(c.getEngine(), m_jts);
            m_engine.assertFact(f, c);
            jt = nextToken(m_jts);
        }

        return Funcall.TRUE;
    }

    /**
     * Parse and return a single expression. Nothing is printed.
     * Uses the given context to resolve variables. Optionally, executes the expression
     * if it's a function call.
     *
     * @param context         an execution context
     * @param executeFuncalls true if function calls should be executed immediately
     * @return The result of the parsed entity
     * @throws JessException If anything goes wrong.
     */
    public Object parseExpression(Context context, boolean executeFuncalls)
            throws JessException {
        return parseExpression(context, executeFuncalls, m_jts);
    }

    /**
     * Parse and return a single expression from the given token stream. Nothing is printed.
     * Uses the given context to resolve variables. Optionally, executes the expression
     * if it's a function call.
     *
     * @param context         an execution context
     * @param executeFuncalls true if function calls should be executed immediately
     * @param jts             a token stream from which to parse an expression
     * @return The result of the parsed entity
     * @throws JessException If anything goes wrong.
     */
    public Object parseExpression(Context context,
                                  boolean executeFuncalls, final JessTokenStream jts)
            throws JessException {
        try {
            JessToken jt = nextToken(jts);
            switch (jt.m_ttype) {
                case JessToken.SYMBOL_TOK:
                case JessToken.STRING_TOK:
                case JessToken.INTEGER_TOK:
                case JessToken.FLOAT_TOK:
                case JessToken.LONG_TOK:
                case JessToken.VARIABLE_TOK:
                case JessToken.MULTIVARIABLE_TOK:
                    return jt.valueOf(context);
                case JessToken.MULTILINE_COMMENT_TOK:
                case JessToken.COMMENT_TOK:
                    return Funcall.NIL;
                case'(':
                    break;
                case JessToken.NONE_TOK:
                    if ("EOF".equals(jt.m_sval))
                        return Funcall.EOF;
                default:
                    error("parseExpression",
                            "Expected a '(', constant, or global variable", ParseException.SYNTAX_ERROR, jt);
            }

            JessToken headToken = nextToken(jts);
            String head = tokenAsSymbol(headToken);

            jts.pushBack(headToken);
            jts.pushBack(jt);

            if (head.equals("defrule")) {
                HasLHS defrule = parseDefrule(context, m_engine, jts);
                if (executeFuncalls)
                    m_engine.addDefrule(defrule);
                return defrule;
            } else if (head.equals("defquery")) {
                HasLHS defquery = parseDefquery(context, m_engine, jts);
                if (executeFuncalls)
                    m_engine.addDefrule(defquery);
                return defquery;
            } else if (head.equals("deffacts")) {
                Deffacts deffacts = parseDeffacts(m_engine, jts);
                m_engine.addDeffacts(deffacts);
                return deffacts;
            } else if (head.equals("deftemplate")) {
                Deftemplate deftemplate = parseDeftemplate(context, m_engine, jts);
                m_engine.addDeftemplate(deftemplate);
                return deftemplate;
            } else if (head.equals("deffunction")) {
                Deffunction deffunction = parseDeffunction(m_engine, jts);
                m_engine.addUserfunction(deffunction);
                return deffunction;
            } else if (head.equals("defglobal")) {
                List<Defglobal> defglobal = parseDefglobal(m_engine, jts);
                m_engine.addDefglobals(defglobal);
                return defglobal;
            } else if (head.equals("defmodule")) {
                Defmodule defmodule = parseDefmodule(jts, context);
                m_engine.addDefmodule(defmodule);
                return defmodule;
            } else if (head.equals("EOF")) {
                if (m_issueWarnings)
                    warning("Jesp.parse", "Expected construct or function name", listFunctionAndConstructNames(m_engine), ParseException.SEMANTIC_ERROR, headToken);
                return Funcall.EOF;
            } else if (executeFuncalls) {
                jts.eatWhitespace();
                return parseAndExecuteFuncall(null, context, m_engine, jts, true);
            } else {
                return new FuncallValue(parseFuncall(m_engine, jts, true));
            }
        } catch (ParseException pe) {
            pe.setLineNumber(jts.getLineNumber());
            pe.setProgramText(jts.toString());
            pe.setFilename(m_fileName);            
            // jts.discardToEOL();
            throw pe;
        } catch (JessException je) {
            if (je.getLineNumber() == -1) {
                je.setLineNumber(jts.getLineNumber());
                je.setProgramText(jts.toString());
                je.setFilename(m_fileName);
            }
            // jts.discardToEOL();
            throw je;
        } finally {
            jts.clear();
            m_engine.getGlobalContext().clearReturnValue();
        }
    }

    /**
     * Set the filename used for error reporting.
     *
     * @param fileName the filename this parser will report in error messages
     */
    public void setFileName(String fileName) {
        m_fileName = fileName;
    }

    static String tokenAsSymbol(JessToken tok) {
        if (tok.m_ttype != JessToken.SYMBOL_TOK) {
            if (tok.m_ttype == '-')
                return "-";
            else if (tok.m_ttype == '=')
                return "=";
                // This is allowed so we can use shorthand 'JAVACALL' syntax
            else if (tok.m_ttype == JessToken.VARIABLE_TOK)
                return tok.m_sval;
            else
                return tok.toString();
        } else {
            if (tok.m_sval != null)
                return tok.m_sval;
            else
                return tok.toString();
        }
    }

    /**
     * Parse a defmodule construct and return a Defmodule object.
     * <p/>
     * Syntax:<br>
     * (defmodule modulename "Comment")
     *
     * @param jts the token stream
     * @param context
     * @return the parsed Defmodule object
     * @throws JessException if there is a syntax error.
     */
    public Defmodule parseDefmodule(final JessTokenStream jts, Context context) throws JessException {
        /* ****************************************
           '(defmodule'
           **************************************** */

        if ((nextToken(jts).m_ttype != '(') ||
                !(nextToken(jts).m_sval.equals("defmodule")))
            error("parseDefmodule", "Expected (defmodule...", ParseException.SYNTAX_ERROR, jts.getLastToken());

        JessToken name = nextToken(jts);
        if (name.m_ttype != JessToken.SYMBOL_TOK)
            error("parseDefmodule", "Expected module name", ParseException.SYNTAX_ERROR, name);

        JessToken next = nextToken(jts);

        String comment = "";
        if (next.m_ttype == JessToken.STRING_TOK) {
            comment = next.m_sval;
            next = nextToken(jts);
        }

        Defmodule module = new Defmodule(name.m_sval, comment);

        Map<String, ValueVector> declarations = new HashMap<String, ValueVector>();
        if (next.m_ttype == '(') {
            m_jts.pushBack(next);
            Map<String, JessToken> tokens = parseDeclarations(declarations, DEFMODULE_DECLARABLES, m_engine, m_jts);
            for (Iterator<?> it = declarations.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                ValueVector vv = (ValueVector) entry.getValue();

                if (key.equals(AUTO_FOCUS)) {
                    Value val = vv.get(1);
                    module.setAutoFocus(!Funcall.FALSE.equals(val));
                } else {
                    error("parseDefmodule", "Invalid declarand", DEFMODULE_DECLARABLES, ParseException.INVALID_DECLARAND, tokens.get(key));
                }
            }
            next = m_jts.nextToken();
        }

        if (next.m_ttype == ')') {
            return module;

        } else
            error("parseDefmodule", "Expected ')'", ParseException.SYNTAX_ERROR, next);

        // NOT REACHED
        return null;
    }

    /**
     * parseDefglobal
     * <p/>
     * Syntax:
     * (defglobal ?x = 3 ?y = 4 ... )
     *
     * @return
     * @throws JessException
     */
    private List<Defglobal> parseDefglobal(Rete engine, final JessTokenStream jts) throws JessException {
        List<Defglobal> list = new ArrayList<Defglobal>();
        /* ****************************************
           '(defglobal'
           **************************************** */

        if ((nextToken(jts).m_ttype != '(') ||
                !(nextToken(jts).m_sval.equals("defglobal")))
            error("parseDefglobal", "Expected (defglobal...", ParseException.SYNTAX_ERROR, jts.getLastToken());

        /* ****************************************
       varname = value sets
       **************************************** */

        JessToken name, value;
        while ((name = nextTokenNoWarnings(jts)).m_ttype != ')') {
            if (name.m_ttype != JessToken.VARIABLE_TOK)
                error("parseDefglobal", "Expected a variable name", ParseException.SYNTAX_ERROR, name);

            // Defglobal names must start and end with an asterisk!
            if (name.m_sval.charAt(0) != '*' ||
                    name.m_sval.charAt(name.m_sval.length() - 1) != '*')
                error("parseDefglobal", "Defglobal names must start and " +
                        "end with an asterisk", ParseException.SYNTAX_ERROR, name);

            if (nextToken(jts).m_ttype != '=')
                error("parseDefglobal", "Expected =", new String[]{"="}, ParseException.SYNTAX_ERROR, jts.getLastToken());

            value = nextToken(jts);

            switch (value.m_ttype) {

                case JessToken.SYMBOL_TOK:
                case JessToken.STRING_TOK:
                case JessToken.VARIABLE_TOK:
                case JessToken.MULTIVARIABLE_TOK:
                case JessToken.FLOAT_TOK:
                case JessToken.LONG_TOK:
                case JessToken.INTEGER_TOK:
                case'(':
                    list.add(new Defglobal(name.m_sval, tokenToValue(value, engine, jts)));
                    break;

                default:
                    error("parseDefglobal", "Bad value", ParseException.SYNTAX_ERROR, jts.getLastToken());
            }
        }

        return list;
    }

    /**
     * Parses a function call and returns the parsed Funcall object.
     * <p/>
     * Syntax:<br>
     * (functor field2 (nested funcall) (double (nested funcall)))
     * <p/>
     * Trick: If the functor is a variable, we insert the functor 'call'
     * and assume we're going to make an outcall to Java on the object in
     * the variable!
     *
     * @return the parsed function call
     * @throws JessException if there is a syntax error.
     */
    public Funcall parseFuncall(Rete engine, final JessTokenStream jts) throws JessException {
        return parseFuncall(engine, jts, false);
    }

    private Funcall parseFuncall(Rete engine, final JessTokenStream jts, boolean warningIncludesConstructs) throws JessException {
        JessToken tok;
        String name;
        Funcall fc = null;

        if (nextToken(jts).m_ttype != '(')
            error("parseFuncall", "Expected '('", ParseException.SYNTAX_ERROR, jts.getLastToken());

        int lineno = jts.getLineNumber();

        /* ****************************************
           functor
           **************************************** */
        tok = nextToken(jts);
        switch (tok.m_ttype) {

            case JessToken.SYMBOL_TOK:
                if (isAConstructName(tok.m_sval))
                    error("parseFuncall", "Can't use this construct here; function name expected", listFunctionNames(engine), ParseException.SYNTAX_ERROR, tok);
                if (m_issueWarnings && engine.findUserfunction(tok.m_sval) == null) {
                    if (warningIncludesConstructs)
                        warning("parseFuncall", "Undefined function or construct", listFunctionAndConstructNames(engine), ParseException.WARNING_UNDEFINED_FUNCTION, tok);
                    else
                        warning("parseFuncall", "Undefined function", listFunctionNames(engine), ParseException.WARNING_UNDEFINED_FUNCTION, tok);
                }

                fc = new Funcall(tok.m_sval, engine);
                break;

            case'=':
                // special functors
                fc = new Funcall("=".intern(), engine);
                break;

            case JessToken.VARIABLE_TOK:
                // insert implied functor
                fc = new Funcall(JAVACALL, engine);
                fc.add(new Variable(tok.m_sval, RU.VARIABLE));
                break;

            case'(':
                // insert implied functor
                fc = new Funcall(JAVACALL, engine);
                jts.pushBack(tok);
                Funcall fc2 = parseFuncall(engine, jts);
                fc.add(new FuncallValue(fc2));
                break;

            default:
                error("parseFuncall",
                        "\"" + jts.getLastToken().toString() + "\" is not a valid function name",
                        warningIncludesConstructs ? listFunctionAndConstructNames(engine) : listFunctionNames(engine),
                        ParseException.SYNTAX_ERROR, jts.getLastToken());
        }

        name = fc.get(0).stringValue(null);

        /* ****************************************
           arguments
           **************************************** */
        tok = nextToken(jts);
        while (tok.m_ttype != ')') {

            switch (tok.m_ttype) {
                // simple arguments
                case JessToken.SYMBOL_TOK:
                case JessToken.STRING_TOK:
                case JessToken.FLOAT_TOK:
                case JessToken.LONG_TOK:
                case JessToken.INTEGER_TOK:
                case JessToken.VARIABLE_TOK:
                case JessToken.MULTIVARIABLE_TOK:
                    checkFunctionArgument(fc, name, tok);
                    fc.add(tokenToValue(tok, engine, jts));
                    break;

                    // nested funcalls
                case'(':

                    JessToken tok2 = nextToken(jts);
                    if (tok2.m_ttype == ')') {
                        checkFunctionArgument(fc, name, tok);
                        fc.add(Funcall.NILLIST);
                        if (m_issueWarnings) {
                            warning("parseFuncall", "Could be nested function call",
                                    listFunctionNames(engine), ParseException.ADVICE_COULD_BE_FUNCTION, tok2);
                        }
                        break;
                    } else {
                        jts.pushBack(tok2);
                        jts.pushBack(tok);
                    }
                    if (name.equals("assert")) {
                        Fact fact = parseFact(engine, jts);
                        checkFunctionArgument(fc, name, tok);
                        fc.add(new FactIDValue(fact));
                        break;

                    } else if ((name.equals("modify") ||
                            name.equals("duplicate"))
                            && fc.size() > 1) {
                        ValueVector pair = parseValuePair(null, engine, true, jts);
                        checkFunctionArgument(fc, name, tok);
                        fc.add(new Value(pair, RU.LIST));
                        break;

                    } else if ((name.equals("lambda")) && fc.size() == 1) {
                        List<Argument> args = parseArgumentList(jts);
                        ValueVector vv = new ValueVector();
                        for (Iterator<Argument> it = args.iterator(); it.hasNext();) {
                            Deffunction.Argument argument = it.next();
                            vv.add(new Variable(argument.getName(), argument.getType()));
                        }
                        checkFunctionArgument(fc, name, tok);
                        fc.add(new Value(vv, RU.LIST));
                        break;

                    } else {
                        Funcall fc2 = parseFuncall(engine, jts);
                        checkFunctionArgument(fc, name, tok);
                        fc.add(new FuncallValue(fc2));
                        break;
                    }

                case JessToken.NONE_TOK:
                    // EOF during eval
                    error("parseFuncall", "Unexpected EOF", ParseException.SYNTAX_ERROR, tok);
                    break;

                default:
                    checkFunctionArgument(fc, name, tok);                                            
                    fc.add(m_engine.getValueFactory().get(String.valueOf((char) tok.m_ttype), RU.STRING));
                    break;

            } // switch tok.m_ttype
            tok = nextToken(jts);
        } // while tok.m_ttype != ')'

        if (engine.isDebug())
            Rete.recordFunction(fc, m_fileName, lineno);

        return fc;
    }

    private void checkFunctionArgument(Funcall fc, String name, JessToken tok) throws JessException {
        ArgumentChecker checker = m_checkers.get(name);
        if (checker != null)
            checker.check(fc, tok, this);
    }

    /**
     * parseValuePair
     * These are used in (modify) funcalls and salience declarations
     * <p/>
     * Syntax:
     * (SYMBOL_TOK VALUE)
     *
     * @return
     * @throws JessException
     */
    private ValueVector parseValuePair(String[] allowed, Rete engine, boolean allowVariableHead, final JessTokenStream jts) throws JessException {
        ValueVector pair = new ValueVector(2);
        JessToken tok;

        if (allowed != null) {
            Arrays.sort(allowed);
        }

        /* ****************************************
           '(atom'
           **************************************** */

        if ((tok = nextToken(jts)).m_ttype != '(')
            error("parseValuePair", "Expected '('", OPEN_PAREN, ParseException.SYNTAX_ERROR, tok);

        tok = nextToken(jts);
        if (!allowVariableHead && tok.m_ttype != JessToken.SYMBOL_TOK) {
            error("parseValuePair", "Expected '<atom>'", allowed, ParseException.SYNTAX_ERROR, tok);
        }

        if (allowed != null && !tok.isVariable()) {
            if (Arrays.binarySearch(allowed, tok.m_sval) < 0) {
                error("parseValuePair", "Bad value", allowed, ParseException.SYNTAX_ERROR, tok);
            }
        }

        pair.add(tokenToValue(tok, engine, jts));

        /* ****************************************
           value
           **************************************** */
        do {
            switch ((tok = nextToken(jts)).m_ttype) {
                case JessToken.SYMBOL_TOK:
                case JessToken.STRING_TOK:
                case JessToken.VARIABLE_TOK:
                case JessToken.MULTIVARIABLE_TOK:
                case JessToken.LONG_TOK:
                case JessToken.FLOAT_TOK:
                case JessToken.INTEGER_TOK:
                case'(':
                    pair.add(tokenToValue(tok, engine, jts));
                    break;

                case')':
                    break;

                default:
                    error("parseValuePair", "Bad argument", ParseException.SYNTAX_ERROR, tok);
            }
        } while (tok.m_ttype != ')');

        return pair;
    }


    /**
     * parseDeffacts
     * <p/>
     * Syntax:
     * (deffacts <name> ["comment"] (fact) [(fact)...])
     *
     * @return the parsed Deffacts construct
     * @throws JessException if there is a syntax error.
     */
    public Deffacts parseDeffacts(Rete engine, final JessTokenStream jts) throws JessException {
        Deffacts df;
        JessToken tok;

        /* ****************************************
           '(deffacts'
           **************************************** */

        if (nextToken(jts).m_ttype != '(' ||
                (tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK ||
                !tok.m_sval.equals("deffacts")) {
            error("parseDeffacts", "Expected '( deffacts'", ParseException.SYNTAX_ERROR, jts.getLastToken());
        }

        /* ****************************************
           deffacts name
           **************************************** */

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parseDeffacts", "Expected deffacts name", ParseException.SYNTAX_ERROR, tok);
        String name = tok.m_sval;

        tok = nextToken(jts);

        /* ****************************************
           optional comment
           **************************************** */

        String docstring = "";
        if (tok.m_ttype == JessToken.STRING_TOK) {
            docstring = tok.m_sval;
            tok = nextToken(jts);
        }

        df = new Deffacts(name, docstring, engine);
        engine.setCurrentModule(df.getModule());

        /* ****************************************
           list of facts
           **************************************** */

        while (tok.m_ttype == '(') {
            jts.pushBack(tok);
            Fact f = parseFact(engine, jts);
            df.addFact(f);
            tok = nextToken(jts);
        }

        expectCloseParen(tok, "parseDeffacts");

        return df;
    }

    /**
     * parseFact
     * <p/>
     * This is called from the parse routine for Deffacts and from the
     * Funcall parser for 'assert'; because of this latter, it can have
     * variables that need expanding.
     * <p/>
     * Syntax:
     * ordered facts: (atom field1 2 "field3")
     * NOTE: We now turn these into unordered facts with a single slot "__data"
     * unordered facts: (atom (slotname value) (slotname value2))
     *
     * @return
     * @throws JessException
     */
    Fact parseFact(Rete engine, final JessTokenStream jts) throws JessException {
        String name, slot = RU.DEFAULT_SLOT_NAME;
        int slotType;
        Fact f;
        JessToken tok = null;

        /* ****************************************
           '( atom'
           **************************************** */

        if (nextToken(jts).m_ttype != '(')
            error("parseFact", "Expected '('", OPEN_PAREN, ParseException.SYNTAX_ERROR, tok);

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parseFact", "Expected template name", listTemplateNames(engine), ParseException.SYNTAX_ERROR, tok);

        name = tok.m_sval;

        /* ****************************************
           slot data
           What we do next depends on whether we're parsing an
           ordered or unordered fact. We can determine this very easily:
           If there is a deftemplate, use it; if the first slot is named
           "__data", this is ordered, else unordered. If there is no
           deftemplate, assume ordered.
           **************************************** */

        if (m_issueWarnings) {
            if (engine.findDeftemplate(name) == null)
                warning("parseFact", "Creating implied deftemplate", listTemplateNames(engine), ParseException.WARNING_IMPLIED_DEFTEMPLATE, tok);
        }

        // get a deftemplate or create one
        Deftemplate deft = engine.createDeftemplate(name);

        /* ****************************************
           SLOT DATA
           **************************************** */
        f = new Fact(name, engine);
        tok = nextToken(jts);
        boolean parsingSlots = !deft.isOrdered();

        while (tok.m_ttype != ')') {
            JessToken slotName = null;
            Value slotValue = null;
            if (!deft.isOrdered()) {
                expectOpenParen(tok, "parseFact");

                // Slot name
                if ((slotName = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
                    error("parseFact", "Bad slot name", deft.getSlotNames(), ParseException.SYNTAX_ERROR, slotName);
                slot = slotName.m_sval;
                tok = nextToken(jts);
            } else {
                if (tok.m_ttype == '(') {
                    JessToken tok2 = nextToken(jts);
                    if (RU.DEFAULT_SLOT_NAME.equals(tok2.m_sval)) {
                        parsingSlots = true;
                        slotName = tok2;
                        slot = RU.DEFAULT_SLOT_NAME;
                        tok = nextToken(jts);
                    } else {
                        jts.pushBack(tok2);
                    }
                }
            }

            // Is this a slot or a multislot?
            int idx = deft.getSlotIndex(slot);
            if (idx == -1)
                error("parseFact",
                        "No such slot " + slot + " in template " + deft.getName(),
                        deft.getSlotNames(),
                        ParseException.WARNING_NO_SUCH_SLOT, slotName);

            slotType = deft.getSlotType(idx);

            switch (slotType) {

                // Data in normal slot
                case RU.SLOT:
                    switch (tok.m_ttype) {

                        case JessToken.SYMBOL_TOK:
                        case JessToken.STRING_TOK:
                        case JessToken.VARIABLE_TOK:
                        case JessToken.MULTIVARIABLE_TOK:
                        case JessToken.FLOAT_TOK:
                        case JessToken.LONG_TOK:
                        case JessToken.INTEGER_TOK:
                            slotValue = tokenToValue(tok, engine, jts);
                            break;

                        case'=':
                            JessToken previous = tok;
                            tok = nextToken(jts);
                            if (tok.m_ttype != '(')
                                error("parseFact",
                                        "'=' cannot appear as an atom within a fact", ParseException.SYNTAX_ERROR, previous);
                            // FALLTHROUGH
                        case'(': {
                            jts.pushBack(tok);
                            Funcall fc = parseFuncall(engine, jts);
                            slotValue = new FuncallValue(fc);
                            break;
                        }

                        default:
                            error("parseFact", "Bad slot value", ParseException.SYNTAX_ERROR, tok);
                    }

                    tok = expectCloseParen(jts, "parseFact");
                    break;

                case RU.MULTISLOT:
                    // Data in multislot. Code is very similar, but bits of
                    // data are added to a multifield
                    ValueVector slot_vv = new ValueVector();

                    while (tok.m_ttype != ')') {
                        switch (tok.m_ttype) {

                            case JessToken.SYMBOL_TOK:
                            case JessToken.STRING_TOK:
                            case JessToken.VARIABLE_TOK:
                            case JessToken.MULTIVARIABLE_TOK:
                            case JessToken.LONG_TOK:
                            case JessToken.FLOAT_TOK:
                            case JessToken.INTEGER_TOK:
                                slot_vv.add(tokenToValue(tok, engine, jts));
                                break;

                            case'=':
                                JessToken previous = tok;
                                tok = nextToken(jts);
                                if (tok.m_ttype != '(')
                                    error("parseFact", "'=' cannot appear as an atom within a fact", ParseException.SYNTAX_ERROR, previous);
                                // FALLTHROUGH
                            case'(': {
                                jts.pushBack(tok);
                                Funcall fc = parseFuncall(engine, jts);
                                slot_vv.add(new FuncallValue(fc));
                                break;
                            }

                            default:
                                error("parseFact", "Bad slot value", ParseException.SYNTAX_ERROR, tok);
                        }

                        tok = nextToken(jts);

                    }
                    slotValue = new Value(slot_vv, RU.LIST);
                    break;

                default:
                    error("parseFact", "No such slot in deftemplate", ParseException.WARNING_NO_SUCH_SLOT, slotName);
            }

            if (slotValue == null)
                error("parseFact", "Missing value?", ParseException.SYNTAX_ERROR, slotName);

            if (!deft.isAllowedValue(slot, slotValue))
                error("parseFact", "Literal value not in allowed values for slot " + slotName, ParseException.SEMANTIC_ERROR, tok);
            f.setSlotValue(slot, slotValue);

            if (parsingSlots) {
                // hopefully advance to next ')'
                tok = nextToken(jts);
            } else
                break;
        }

        expectCloseParen(tok, "parseFact");

        return f;

    }

    /**
     * Parses a deftemplate construct and returns the parsed Deftemplate object.
     * <p/>
     * Syntax:<br>
     * (deftemplate (slot foo (default <value>)) (multislot bar))
     *
     * @return the parsed Deftemplate object
     * @throws JessException if there is a syntax error.
     */
    public Deftemplate parseDeftemplate(Context context, Rete engine, final JessTokenStream jts) throws JessException {
        Deftemplate dt;
        int slotType;
        Value defaultValue;
        String defaultType;
        ValueVector allowedValues;
        JessToken tok;

        /* ****************************************
           '(deftemplate'
           **************************************** */

        if ((nextToken(jts).m_ttype != '(') ||
                !(nextToken(jts).m_sval.equals("deftemplate")))
            error("parseDeftemplate", "Expected (deftemplate...", ParseException.SYNTAX_ERROR, jts.getLastToken());

        /* ****************************************
           deftemplate name, optional extends clause
           **************************************** */

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parseDeftemplate", "Expected deftemplate name", ParseException.SYNTAX_ERROR, tok);

        String name = tok.m_sval;

        String docstring = "";
        String parent = null;
        JessToken parentToken = null;
        if ((tok = nextToken(jts)).m_ttype == JessToken.SYMBOL_TOK) {
            if (tok.m_sval.equals("extends")) {
                parentToken = nextToken(jts);
                if (parentToken.m_ttype == JessToken.SYMBOL_TOK)
                    parent = parentToken.m_sval;
                else
                    error("parseDeftemplate",
                            "Expected deftemplate name to extend",
                            listTemplateNames(engine),
                            ParseException.SYNTAX_ERROR, parentToken);
            } else
                error("parseDeftemplate", "Expected '(' or 'extends'", new String[]{"(", "extends"}, ParseException.SYNTAX_ERROR, tok);

            tok = nextToken(jts);
        }

        /* ****************************************
           optional comment
           **************************************** */

        if (tok.m_ttype == JessToken.STRING_TOK) {
            docstring = tok.m_sval;
        } else {
            jts.pushBack(tok);
        }

        /* ****************************************
        * declarations
        **************************************** */

        HashMap<String, ValueVector> declarations = new HashMap<String, ValueVector>();
        Map<String, JessToken> declTokens = parseDeclarations(declarations, DEFTEMPLATE_DECLARABLES, engine, jts);
        boolean sawDeclarations = (declTokens.size() > 0);
        String className = null;
        boolean doBackwardChaining = false;
        boolean isSlotSpecific = false;
        boolean includeVariables = false;
        tok = nextToken(jts);

        if (parent == null)
            dt = new Deftemplate(name, docstring, engine);
        else {
            Deftemplate parentTemplate = engine.findDeftemplate(parent);
            if (parentTemplate == null)
                error("parseDeftemplate", "Parent template is undefined",
                        listTemplateNames(engine), ParseException.SYNTAX_ERROR, parentToken);
            dt = new Deftemplate(name, docstring,
                    engine.findDeftemplate(parent), engine);
        }
        boolean ordered = false;
        for (Iterator<String> it = declarations.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            ValueVector vv = declarations.get(key);
            if (key.equals(SLOT_SPECIFIC)) {
                if (!vv.get(1).equals(Funcall.FALSE))
                    isSlotSpecific = true;
            } else if (key.equals(BACKCHAIN_REACTIVE)) {
                if (!vv.get(1).equals(Funcall.FALSE))
                    doBackwardChaining = true;
            } else if (key.equals(INCLUDE_VARIABLES)) {
                if (!vv.get(1).equals(Funcall.FALSE)) {
                    includeVariables = true;
                    if (declarations.get(FROM_CLASS) == null)
                        throw new ParseException("parseDeftemplate", "include-variables requires from-class", m_fileName, declTokens.get(INCLUDE_VARIABLES));
                }
            } else if (key.equals(FROM_CLASS)) {
                className = vv.get(1).symbolValue(context);
            } else if (key.equals(ORDERED)) {
                if (vv.get(1).equals(Funcall.TRUE)) {
                    ordered = true;
                    if (declarations.get(FROM_CLASS) != null)
                        throw new ParseException("parseDeftemplate", "Ordered template can't be generated from class", m_fileName, declTokens.get(FROM_CLASS));
                    else if (declarations.get(INCLUDE_VARIABLES) != null)
                        throw new ParseException("parseDeftemplate", "Ordered template can't include variables", m_fileName, declTokens.get(INCLUDE_VARIABLES));
                    else if (declarations.get(SLOT_SPECIFIC) != null)
                        throw new ParseException("parseDeftemplate", "Ordered template can't be slot-specific", m_fileName, declTokens.get(SLOT_SPECIFIC));
                    else if (parent != null)
                        throw new ParseException("parseDeftemplate", "Ordered templates can't extend other templates", m_fileName, parentToken);
                }
            } else
                error("parseDeftemplate", "Invalid declarand", DEFTEMPLATE_DECLARABLES, ParseException.INVALID_DECLARAND, declTokens.get(key));
        }

        if (ordered) {
            dt.addMultiSlot(RU.DEFAULT_SLOT_NAME, Funcall.NILLIST, "ANY");
            if (doBackwardChaining)
                dt.doBackwardChaining(engine);
            return dt;
        }

        // TODO Extract deftemplate-maker from defclass, and call it here; then make parsing
        // below conditional on defclassness, but use the if... thens afterward, and just return
        // the deftemplate without adding it. Ideally we should be able to do this without mapping
        // the typename or anything.
        if (className != null) {
            if (tok.m_ttype == '(')
                throw new ParseException("parseDeftemplate", "Templates that use from-class can't declare slots", m_fileName, tok);

            if (doBackwardChaining)
                throw new ParseException("Jesp.parseDeftemplate", "Defclass can't be backchain-reactive", m_fileName, declTokens.get(BACKCHAIN_REACTIVE));

            engine.defclass(name, className, parent, includeVariables);

            if (isSlotSpecific)
                engine.findDeftemplate(name).setSlotSpecific(true);

            return engine.findDeftemplate(name);
        } else if (declarations.containsKey(INCLUDE_VARIABLES)) {
            throw new ParseException("Jesp.parseDeftemplate", "include-variables can only be used with from-class", m_fileName, declTokens.get(INCLUDE_VARIABLES));
        }

        /* ****************************************
       individual slot descriptions
       **************************************** */

        // ( <slot category>

        while (tok.m_ttype == '(') { // 'slot'
            if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK ||
                    !(tok.m_sval.equals("slot") || tok.m_sval.equals("multislot"))) {
                String[] alternatives = SLOT_TYPE;
                if (!sawDeclarations && dt.getNSlots() == 0)
                    alternatives = DECLARE_OR_SLOT_TYPE;

                error("parseDeftemplate", "Bad slot type", alternatives, ParseException.SYNTAX_ERROR, tok, dt);
            }

            slotType = tok.m_sval.equals("slot") ? RU.SLOT : RU.MULTISLOT;

            // <slot name>
            if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
                error("parseDeftemplate", "Bad slot name", ParseException.SYNTAX_ERROR, tok, dt);
            name = tok.m_sval;            
            if (name.indexOf(engine.getMemberChar()) > -1)
                error("parseDeftemplate", "Bad character in slot name", ParseException.SYNTAX_ERROR, tok, dt);


            // optional slot qualifiers

            defaultValue = (slotType == RU.SLOT) ? Funcall.NIL : Funcall.NILLIST;
            allowedValues = null;
            defaultType = "ANY";

            tok = nextToken(jts);
            while (tok.m_ttype == '(') { // slot qualifier
                if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
                    error("parseDeftemplate", "Slot qualifier must be a symbol", SLOT_QUALIFIERS, ParseException.SYNTAX_ERROR, tok, dt);

                // default value qualifier

                String option = tok.m_sval;

                if (option.equalsIgnoreCase("default") || option.equalsIgnoreCase("default-dynamic")) {
                    ValueVector list = null;
                    if (slotType == RU.MULTISLOT)
                        list = new ValueVector();
                    Value result = null;
                    tok = nextToken(jts);
                    while (tok.m_ttype != ')') {
                        switch (tok.m_ttype) {

                            case JessToken.SYMBOL_TOK:
                            case JessToken.STRING_TOK:
                            case JessToken.FLOAT_TOK:
                            case JessToken.INTEGER_TOK:
                            case JessToken.LONG_TOK:
                                result = tokenToValue(tok, engine, jts);
                                break;

                            case'(':
                                if (option.equalsIgnoreCase("default-dynamic")) {
                                    jts.pushBack(tok);
                                    Funcall fc = parseFuncall(engine, jts);
                                    result = new FuncallValue(fc);
                                } else
                                    result = parseAndExecuteFuncall(tok, context, engine, jts, false);
                                break;

                            default:
                                error("parseDeftemplate",
                                        "Illegal default slot value", ParseException.SYNTAX_ERROR, tok);
                        }
                        if (slotType == RU.MULTISLOT) {
                            if (result.type() == RU.LIST)
                                list.addAll(result.listValue(engine.getGlobalContext()));
                            else
                                list.add(result);
                            tok = nextToken(jts);
                        } else
                            break;
                    }
                    if (slotType == RU.MULTISLOT)
                        defaultValue = new Value(list, RU.LIST);
                    else
                        defaultValue = result;

                    if (slotType == RU.MULTISLOT && defaultValue.type() != RU.LIST)
                        error("parseDeftemplate", "Default value for multislot " +
                                name + " is not a multifield: " + defaultValue, ParseException.SEMANTIC_ERROR, tok, dt);                    

                    if (tok.m_ttype == ')')
                        jts.pushBack(tok);

                } else if (option.equalsIgnoreCase("type")) {
                    tok = nextToken(jts);
                    defaultType = tok.m_sval;
                    if (tok.m_ttype != JessToken.SYMBOL_TOK || !Deftemplate.isValidSlotType(defaultType))
                        error("parseDeftemplate", "Invalid slot type", Deftemplate.TYPE_NAMES, ParseException.SYNTAX_ERROR, tok, dt);
                } else if (option.equalsIgnoreCase("allowed-values")) {
                    allowedValues = new ValueVector();
                    tok = nextToken(jts);
                    while (tok.m_ttype != ')') {
                        allowedValues.add(tokenToValue(tok, engine, jts).resolveValue(context));
                        tok = nextToken(jts);                    
                    }
                    jts.pushBack(tok);
                    if (allowedValues.size() == 0)
                        error("parseDeftemplate", "Allowed-values cannot have zero members", ParseException.SEMANTIC_ERROR, tok, dt);

                } else
                    error("parseDeftemplate", "Unimplemented slot qualifier", SLOT_QUALIFIERS, ParseException.SYNTAX_ERROR, tok, dt);

                expectCloseParen(jts, "parseDeftemplate");

                tok = nextToken(jts);
            }
            expectCloseParen(tok, "parseDeftemplate");

            // Reconcile allowed-values and default value
            if (allowedValues != null) {
                if (defaultValueSpecified(defaultValue) &&
                        ! defaultIsAllowed(defaultValue, allowedValues, context)) {
                    error("parseDeftemplate", "Default value not compatible with allowed values", ParseException.SEMANTIC_ERROR, tok, dt);
                }
            }

            if (slotType == RU.SLOT)
                dt.addSlot(name, defaultValue, defaultType);
            else {
                dt.addMultiSlot(name, defaultValue, defaultType);
            }

            dt.setSlotAllowedValues(name, allowedValues);

            tok = nextToken(jts);
        }
        expectCloseParen(tok, "parseDeftemplate");


        if (doBackwardChaining)
            dt.doBackwardChaining(engine);

        if (isSlotSpecific)
            dt.setSlotSpecific(true);

        return dt;
    }

    private boolean defaultIsAllowed(Value defaultValue, ValueVector allowedValues, Context context) throws JessException {
        if (defaultValue.type() == RU.LIST) {
            ValueVector list = defaultValue.listValue(context);
            for (int i=0; i<list.size(); ++i)
                if (!allowedValues.contains(list.get(i)))
                    return false;
            return true;
        } else
        return allowedValues.contains(defaultValue);        
    }

    private boolean defaultValueSpecified(Value defaultValue) {
        return !defaultValue.equals(Funcall.NIL) &&
                !defaultValue.equals(Funcall.NILLIST);
    }

    /**
     * Parses a defrule construct and creates a Defrule object.
     * <p/>
     * Syntax:<br>
     * (defrule name<br>
     * [ "docstring...." ]<br>
     * [ (declare [(salience 1)] [(node-index-hash 57)]) ]<br>
     * (pattern 1)<br>
     * ?foo <- (pattern 2)<br>
     * (pattern 3)<br>
     * =><br>
     * (action 1)<br>
     * (action ?foo)<br>
     * )
     *
     * @return the parsed Defrule object
     * @throws JessException if there is a syntax error.
     * @noinspection EqualsBetweenInconvertibleTypes
     */

    public Defrule parseDefrule(Context context, Rete engine, JessTokenStream jts) throws JessException {

        JessToken tok;

        String nameAndDoc[] = parseNameAndDocstring("defrule", jts);

        /* ****************************************
        * check for salience declaration
        **************************************** */

        HashMap<String, ValueVector> declarations = new HashMap<String, ValueVector>();
        Map<String, JessToken> declTokens = parseDeclarations(declarations, RULE_DECLARABLES, engine, jts);

        /* ****************************************
         * Parse all the LHS patterns
         **************************************** */
        String module = RU.getModuleFromName(nameAndDoc[0], engine);
        engine.setCurrentModule(module);
        Group patterns = parseLHS(engine, jts);

        /* ****************************************
         * should be looking at "=>"
         **************************************** */
        tok = nextToken(jts);
        if (tok.m_ttype != '=' ||
                (tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK ||
                !tok.m_sval.equals(">")) {
            error("parseDefrule", "Expected '=>'", RIGHT_ARROW, ParseException.SYNTAX_ERROR, tok);
        }

        /* ****************************************
         * Parse RHS actions
         **************************************** */
        ArrayList<Funcall> actions = parseActions(engine, jts);

        /* ****************************************
         * Should be looking at the closing paren
         **************************************** */
        expect(')', ")", jts);

        /* ****************************************
         * All parsed. Now build the rule
         **************************************** */

        Defrule rule = new Defrule(nameAndDoc[0], nameAndDoc[1], engine);
        rule.setLHS(patterns, engine);

        // install declarations
        for (Iterator<String> it = declarations.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            ValueVector vv = declarations.get(key);
            if (key.equals("salience"))
                rule.setSalience(vv.get(1), engine);

            else if (key.equals("node-index-hash"))
                rule.setNodeIndexHash(vv.get(1).intValue(context));

            else if (key.equals(AUTO_FOCUS)) {
                String val = vv.get(1).symbolValue(context);
                rule.setAutoFocus(!Funcall.FALSE.equals(val));

            } else if (key.equals("no-loop")) {
                String val = vv.get(1).symbolValue(context);
                rule.setNoLoop(!Funcall.FALSE.equals(val));

            } else
                error("parseDefrule", "Invalid declarand", RULE_DECLARABLES, ParseException.INVALID_DECLARAND, declTokens.get(key));
        }

        // Install actions
        for (int j = 0; j < actions.size(); j++)
            rule.addAction(actions.get(j));

        return rule;
    }

    private String[] parseNameAndDocstring(String construct, final JessTokenStream jts)
            throws JessException {
        JessToken tok;

        /* ****************************************
           '(defrule'
           **************************************** */

        if ((nextToken(jts).m_ttype != '(') ||
                !(nextToken(jts).m_sval.equals(construct)))
            error("parseNameAndDocstring", "Expected " + construct, ParseException.SYNTAX_ERROR, jts.getLastToken());

        /* ****************************************
           defrule name, optional comment
           **************************************** */

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parseNameAndDocstring", "Expected name", ParseException.SYNTAX_ERROR, tok);

        String name = tok.m_sval;

        if (m_engine.findDefrule(name) != null)
            warning("parseNameAndDocstring", "Construct redefined", new String[0], ParseException.WARNING_REDEFINITION, tok);


        String docstring = "";
        if ((tok = nextToken(jts)).m_ttype == JessToken.STRING_TOK)
            docstring = tok.m_sval;
        else
            jts.pushBack(tok);


        return new String[]{name, docstring};
    }

    private Map<String, JessToken> parseDeclarations(Map<String, ValueVector> declarations, String[] allowed, Rete engine, final JessTokenStream jts)
            throws JessException {

        HashMap<String, JessToken> tokens = new HashMap<String, JessToken>();

        JessToken tok, tok2;
        // Consume the paren
        tok = nextToken(jts);
        if (tok.m_ttype != '(') {
            jts.pushBack(tok);
            return tokens;
        }

        tok2 = nextToken(jts);

        if (tok2.m_ttype == JessToken.SYMBOL_TOK && tok2.m_sval.equals("declare")) {
            while ((tok2 = nextToken(jts)).m_ttype != ')') {
                JessToken headToken = jts.nextToken();
                jts.pushBack(headToken);
                jts.pushBack(tok2);
                ValueVector vv = parseValuePair(allowed, engine, false, jts);
                String head = vv.get(0).symbolValue(null);
                declarations.put(head, vv);
                tokens.put(head, headToken);
            }
            return tokens;

        } else { // head wasn't 'declare'
            jts.pushBack(tok2);
            jts.pushBack(tok);
            return tokens;
        }
    }


    /**
     * parseActions
     * Parses the RHSs of rules
     */

    private ArrayList<Funcall> parseActions(Rete engine, final JessTokenStream jts) throws JessException {
        JessToken tok = nextToken(jts);
        ArrayList<Funcall> actions = new ArrayList<Funcall>();
        while (tok.m_ttype == '(') {
            jts.pushBack(tok);
            Funcall f = parseFuncall(engine, jts);
            actions.add(f);
            tok = nextToken(jts);
        }
        jts.pushBack(tok);
        return actions;
    }

    /**
     * parseLHS
     * Parses the patterns of a defrule or defquery.
     */

    private Group parseLHS(Rete engine, final JessTokenStream jts) throws JessException {
        // **************************************************
        // We need to keep track of the type of each variable, since
        // CLIPS code lets you omit the second and later '$' before multivars.
        // This only matters when a multivar is actualy matched against, since
        // if the '$' is omitted, a TMF node won't get generated. We'll
        // therefore 'put the $'s back in' as needed. This table is shared
        // across all patterns in a rule.
        // **************************************************

        Map<String, Integer> varnames = new HashMap<String, Integer>();
        Group patterns = new Group(Group.AND);

        // now we're looking for just patterns
        JessToken tok = nextToken(jts);
        while (tok.m_ttype == '(' || tok.m_ttype == JessToken.VARIABLE_TOK) {
            jts.pushBack(tok);

            ConditionalElementX p = parsePattern(varnames, engine, jts);
            patterns.add(p);
            tok = nextToken(jts);
        }
        jts.pushBack(tok);
        return patterns;
    }

    private JessToken expect(int type, String value, final JessTokenStream jts) throws JessException {
        JessToken tok = nextToken(jts);
        if (tok.m_ttype != type ||
                !tok.m_sval.equals(value))
            error("parseLHS", "Expected '" + value + "'", new String[]{value}, ParseException.SYNTAX_ERROR, tok);
        return tok;
    }


    /**
     * parsePattern
     * <p/>
     * parse a Pattern object in a Rule LHS context
     * <p/>
     * Syntax:
     * Like that of a fact, except that values can have complex forms like
     * <p/>
     * ~value       (test for not a value)
     * ?X&~red      (store the value in X; fail match if not red)
     * ?X&:(> ?X 3) (store the value in X; fail match if not greater than 3)
     *
     * @param varnames
     * @param jts
     * @return
     * @throws JessException
     */
    ConditionalElementX parsePattern(Map<String, Integer> varnames, Rete engine, final JessTokenStream jts)
            throws JessException {
        String name;
        String patternBinding = null;
        JessToken tok = nextToken(jts);
        JessToken bindingToken = null;

        if (tok.m_ttype == JessToken.VARIABLE_TOK) {

            // pattern bound to a variable
            // These look like this:
            // ?name <- (pattern 1 2 3)

            patternBinding = tok.m_sval;
            bindingToken = tok;
            expect(JessToken.SYMBOL_TOK, "<-", jts);
            tok = nextToken(jts);
        }

        /* ****************************************
       ' ( <atom> '
       **************************************** */

        expectOpenParen(tok, "parsePattern");
        JessToken parenToken = tok;

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parsePattern", "Expected template name", listTemplateNames(engine), ParseException.SYNTAX_ERROR, tok);

        JessToken nameToken = tok;
        name = tok.m_sval;

        /* ****************************************
           Special handling for grouping CEs
           **************************************** */

        if (name.equals(Group.EXISTS)) {
            // TODO Do this transformation in rule?
            Group inner = new Group(Group.NOT);
            while ((tok = nextToken(jts)).m_ttype != ')') {
                jts.pushBack(tok);
                inner.add(parsePattern(varnames, engine, jts));
            }
            if (inner.getGroupSize() == 0)
                error("parsePattern", "expected pattern", OPEN_PAREN, ParseException.SYNTAX_ERROR, tok);
            Group outer = new Group(Group.NOT);
            outer.add(inner);
            // TODO This shouldn't be legal, should it?
            if (patternBinding != null)
                outer.setBoundName(patternBinding);
            return outer;

        } else if (name.equals(Group.FORALL)) {
            // TODO Do this transformation in rule
            Group outerNot = new Group(Group.NOT);
            Group outerAnd = new Group(Group.AND);
            Group innerNot = new Group(Group.NOT);
            Group innerAnd = new Group(Group.AND);

            outerAnd.add(parsePattern(varnames, engine, jts));

            while ((tok = nextToken(jts)).m_ttype != ')') {
                jts.pushBack(tok);
                innerAnd.add(parsePattern(varnames, engine, jts));
            }
            innerNot.add(innerAnd);
            outerAnd.add(innerNot);
            outerNot.add(outerAnd);
            // TODO This shouldn't be legal, should it?
            if (patternBinding != null)
                outerNot.setBoundName(patternBinding);
            return outerNot;

        } else if (name.equals(Group.UNIQUE)) {
            // TODO DO this transformation in rule
            ConditionalElementX pattern = parsePattern(varnames, engine, jts);
            expectCloseParen(jts, "parsePattern");
            if (patternBinding != null)
                pattern.setBoundName(patternBinding);
            return pattern;

        } else if (name.equals(Group.ACCUMULATE)) {
            Accumulate g = new Accumulate();
            g.setInitializer(tokenToValue(nextToken(jts), engine, jts));
            g.setBody(tokenToValue(nextToken(jts), engine, jts));
            g.setReturn(tokenToValue(nextToken(jts), engine, jts));
            ConditionalElementX pattern = parsePattern(varnames, engine, jts);
            expectCloseParen(jts, "parsePattern");
            g.add(pattern);
            if (patternBinding != null)
                g.setBoundName(patternBinding);
            return g;

        } else if (Group.isGroupName(name)) {
            Group g = new Group(name);
            while ((tok = nextToken(jts)).m_ttype != ')') {
                jts.pushBack(tok);
                g.add(parsePattern(varnames, engine, jts));
            }
            if (g.getGroupSize() == 0)
                error("parsePattern", "expected pattern", OPEN_PAREN, ParseException.SYNTAX_ERROR, tok);

            if (patternBinding != null)
                g.setBoundName(patternBinding);
            return g;

        } else if (name.equals(Group.TEST)) {
            // this is a 'test' pattern. We trick up a fake one-slotted
            // pattern which will get treated specially by the compiler.
            if (patternBinding != null)
                error("parsePattern",
                        "Can't bind a 'test' CE to a variable", ParseException.SYNTAX_ERROR, bindingToken);

            Pattern p = new Pattern(Deftemplate.getTestTemplate());

            Funcall f = parseFuncall(engine, jts);

            p.addTest(new Test1(TestBase.EQ, RU.DEFAULT_SLOT_NAME, new FuncallValue(f), RU.NONE));

            expectCloseParen(jts, "parsePattern");

            return p;

        } else {
            jts.pushBack(nameToken);
            jts.pushBack(parenToken);
            Pattern p = parseRegularPattern(engine, jts, varnames);
            if (patternBinding != null)
                p.setBoundName(patternBinding);
            return p;
        }
    }


    private Pattern parseRegularPattern(Rete engine, JessTokenStream jts, Map<String, Integer> varnames) throws JessException {

        expectOpenParen(jts, "parsePattern");

        JessToken tok = nextToken(jts);
        if (tok.m_ttype != JessToken.SYMBOL_TOK)
            error("parsePattern", "Expected template name", listTemplateNames(engine), ParseException.SYNTAX_ERROR, tok);

        Deftemplate deft = findOrCreateDeftemplate(engine, tok.m_sval, tok);

        Pattern p = new Pattern(deft);
        tok = nextToken(jts);

        if (deft.isOrdered()) {
            // For ordered facts, you can skip the slot name and parentheses
            if (tok.m_ttype != '(') {
                jts.pushBack(tok);
                parseContentsOfOneSlot(p, RU.DEFAULT_SLOT_NAME, jts, varnames, engine);
                return p;
            }
        }

        if (!isParenthesis(tok.m_ttype) && tok.m_ttype != '{')
            error("parsePattern", "Expected slot or end of pattern", PARENS, ParseException.SYNTAX_ERROR, tok);

        while (tok.m_ttype != ')') {

            if (tok.m_ttype == '(') {
                String slot = checkForValidSlotName(jts, deft);
                parseContentsOfOneSlot(p, slot, jts, varnames, engine);
            } else if (tok.m_ttype == '{') {
                InfixSlotParser parser = new InfixSlotParser(this);
                parser.parse(p, jts, engine);
            } else
                error("parsePattern", "Expected '(' or '{'", new String[]{"(", "{"}, ParseException.SYNTAX_ERROR, tok);

            tok = nextToken(jts);
            if (!isParenthesis(tok.m_ttype) && tok.m_ttype != '{')
                error("parsePattern", "Expected ')' , '{', or '('", new String[]{")", "{", "("}, ParseException.SYNTAX_ERROR, tok);
        }

        return p;
    }

    private boolean isParenthesis(int type) {
        return type == '(' || type == ')';
    }

    private Deftemplate findOrCreateDeftemplate(Rete engine, String name, JessToken tok) throws JessException {
        Deftemplate deft = engine.findDeftemplate(name);
        boolean existing = (deft != null);

        if (deft == null) {
            if (m_issueWarnings && !existing)
                warning("Jesp.parsePattern", "Created implied ordered template", listTemplateNames(engine), ParseException.WARNING_IMPLIED_DEFTEMPLATE, tok);
            deft = engine.createDeftemplate(name);
        }
        return deft;
    }

    String checkForValidSlotName(JessTokenStream jts, Deftemplate deft) throws JessException {
        JessToken slotName;
        String slot;
        if ((slotName = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parsePattern",
                    "Bad slot name",
                    deft.getSlotNames(),
                    ParseException.SYNTAX_ERROR, slotName);
        slot = slotName.m_sval;
        int index = deft.getSlotIndex(slot);
        if (index == -1)
            error("parsePattern",
                    "No such slot " + slot + " in template " + deft.getName(),
                    deft.getSlotNames(),
                    ParseException.WARNING_NO_SUCH_SLOT, slotName);
        return slot;
    }

    private void parseContentsOfOneSlot(Pattern p, String slot, JessTokenStream jts, Map<String, Integer> varnames, Rete engine) throws JessException {
        Deftemplate deft = p.getDeftemplate();
        int index = deft.getSlotIndex(slot);

        int subidx = (deft.isMultislot(index) ? 0 : -1);
        int nextConjunction = RU.NONE;
        JessToken tok = nextToken(jts);
        while (tok.m_ttype != ')') {

            Test1[] someTests = parseASingleTest(jts, varnames, subidx, engine, p, slot);
            tok = nextToken(jts);
            // EJFH This isn't quite right. It's possible that parseASingleTest adds a test and then returns another,
            // in which case the second should always have an "and" and nextConjunction applies to the first...
            for (int i = 0; i < someTests.length; i++) {
                Test1 someTest = someTests[i];
                if (i == 0)
                    someTest.m_conjunction = nextConjunction;
                p.addTest(someTest);
            }

            if (tok.m_ttype == '&') {
                tok = nextToken(jts);
                nextConjunction = RU.AND;

            } else if (tok.m_ttype == '|') {
                tok = nextToken(jts);
                nextConjunction = RU.OR;

            } else if (!deft.isMultislot(index) && tok.m_ttype != ')') {
                error("parsePattern", slot + " is not a multislot", ParseException.SYNTAX_ERROR, tok);

            } else {
                ++subidx;
                nextConjunction = RU.NONE;
            }
        }

        if (deft.isMultislot(index))
            p.setSlotLength(slot, subidx);
    }

    private Test1[] parseASingleTest(JessTokenStream jts, Map<String, Integer> varnames, int subidx, Rete engine, Pattern p, String slot) throws JessException {
        JessToken tok = jts.getLastToken();
        // if this is a '~'  test, keep track
        boolean notSlot = false;
        if (tok.m_ttype == '~') {
            notSlot = true;
            tok = nextToken(jts);
        }

        switch (tok.m_ttype) {
            case JessToken.VARIABLE_TOK:
            case JessToken.MULTIVARIABLE_TOK:
                return testForVariable(p, varnames, tok, notSlot, slot, subidx, engine, jts);

            case JessToken.SYMBOL_TOK: {
                if (tok.m_sval.equals(":")) {
                    return testForPredicateConstraint(engine, jts, notSlot, slot, subidx);
                } else {
                    return testForLiteral(notSlot, subidx, tok, slot, engine, jts);
                }
            }
            case JessToken.STRING_TOK:
            case JessToken.FLOAT_TOK:
            case JessToken.LONG_TOK:
            case JessToken.INTEGER_TOK:
                return testForLiteral(notSlot, subidx, tok, slot, engine, jts);

            case'=': {
                return testForReturnValueConstraint(engine, jts, p, slot, subidx, notSlot);
            }

            case JessToken.REGEXP_TOK: {
                return testForRegexp(p, slot, subidx, tok, engine, jts, notSlot);
            }

            case'(':
            default: {
                error("parsePattern", "Bad slot value", ParseException.SYNTAX_ERROR, tok);
            }
        }
        return null;
    }

    private Test1[] testForPredicateConstraint(Rete engine, JessTokenStream jts, boolean notSlot, String slotName, int subidx) throws JessException {
        Test1 aTest;
        Funcall f = parseFuncall(engine, jts);
        aTest = new Test1(testFor(notSlot), slotName, subidx, new FuncallValue(f));
        return new Test1[]{aTest};
    }

    private Test1[] testForLiteral(boolean notSlot, int subidx, JessToken tok, String slotName, Rete engine, JessTokenStream jts) throws JessException {
        Test1 aTest;
        aTest = new Test1(testFor(notSlot), slotName, subidx, tokenToValue(tok, engine, jts));
        return new Test1[]{aTest};
    }

    private Test1[] testForRegexp(Pattern p, String slot, int subidx, JessToken tok, Rete engine, JessTokenStream jts, boolean notSlot) throws JessException {
        ArrayList<Test1> tests = new ArrayList<Test1>();

        // Handle these by transforming into calls to regexp function
        int idx = p.getDeftemplate().getSlotIndex(slot);

        Value var = p.findAnyExistingVariable(idx, subidx);
        if (var == null) {
            var = new Variable(RU.gensym("__jesp"), RU.VARIABLE);
            tests.add(new Test1(TestBase.EQ, slot, subidx, var));
        }

        Funcall f = new Funcall("regexp", m_engine);
        f.arg(m_engine.getValueFactory().get(tok.m_sval, RU.STRING));
        f.arg(var);
        if (engine.isDebug())
            Rete.recordFunction(f, m_fileName, jts.getLineNumber());
        tests.add(new Test1(testFor(notSlot), slot, subidx, new FuncallValue(f)));
        return tests.toArray(new Test1[tests.size()]);
    }

    private Test1[] testForReturnValueConstraint(Rete engine, JessTokenStream jts, Pattern p, String slot, int subidx, boolean notSlot) throws JessException {
        ArrayList<Test1> tests = new ArrayList<Test1>();

        // We're going to handle these by transforming them into
        // predicate constraints.

        Funcall inner = parseFuncall(engine, jts);

        // We're building (eq* <this-slot> <inner>)
        Funcall outer = new Funcall("eq*", engine);

        // We need the variable that refers to this slot
        // Slot name known to be good by now.
        int idx = p.getDeftemplate().getSlotIndex(slot);

        Value var = p.findAnyExistingVariable(idx, subidx);

        if (var == null) {
            var = new Variable(RU.gensym("__jesp"), RU.VARIABLE);
            tests.add(new Test1(TestBase.EQ, slot, subidx, var));
        }

        // Finish up the Funcall
        outer.add(var);
        outer.add(new FuncallValue(inner));
        if (engine.isDebug())
            Rete.recordFunction(outer, m_fileName, jts.getLineNumber());

        tests.add(new Test1(testFor(notSlot), slot, subidx, new FuncallValue(outer)));
        return tests.toArray(new Test1[tests.size()]);

    }

    private Test1[] testForVariable(Pattern p, Map<String, Integer> varnames, JessToken tok, boolean notSlot, String slot, int subidx, Rete engine, JessTokenStream jts) throws JessException {
        // Fix type if necessary - lets you omit the '$' on
        // second and later occurrences of multivars.
        Integer type = varnames.get(tok.m_sval);
        if (type == null)
            varnames.put(tok.m_sval, new Integer(tok.m_ttype));
        else
            tok.m_ttype = type.intValue();

        if (tok.m_sval.indexOf(engine.getMemberChar()) != -1) {
            return getDottedVariableTest(p, engine, tok, notSlot, slot, subidx);
        } else {        
            Test1 aTest = new Test1(testFor(notSlot), slot, subidx, tokenToValue(tok, engine, jts));
            return new Test1[]{aTest};
        }
    }

    private Test1[] getDottedVariableTest(Pattern p, Rete engine, JessToken tok, boolean notSlot, String slot, int subidx) throws JessException {
        if (tok.m_ttype == JessToken.MULTIVARIABLE_TOK)
            error("testForVariable", "Dotted multivariables undefined", ParseException.SYNTAX_ERROR, tok);
        String name = tok.m_sval;
        int index = name.indexOf(engine.getMemberChar());
        String target = name.substring(0, index);
        String property = name.substring(index+1);

        ArrayList<Test1> tests = new ArrayList<Test1>();
        int idx = p.getDeftemplate().getSlotIndex(slot);
        Value var = p.findAnyExistingVariable(idx, subidx);

        if (var == null) {
            var = new Variable(RU.gensym("__jesp"), RU.VARIABLE);
            tests.add(new Test1(TestBase.EQ, slot, subidx, var));
        }

        Funcall get = new Funcall("get", m_engine);
        get.arg(new Variable(target, RU.VARIABLE));
        get.arg(property);

        Funcall eq = new Funcall("eq", m_engine);
        eq.arg(var);
        eq.arg(get);
        Test1 aTest = new Test1(testFor(notSlot), slot, subidx, new FuncallValue(eq));
        tests.add(aTest);
        return tests.toArray(new Test1[tests.size()]);
    }

    private int testFor(boolean not_slot) {
        return not_slot ? TestBase.NEQ : TestBase.EQ;
    }

    private String[] listTemplateNames(Rete engine) {
        ArrayList<String> list = new ArrayList<String>();
        String moduleQualifier = engine.getCurrentModule() + "::";
        for (Iterator<?> it = engine.listDeftemplates(); it.hasNext();) {
            String name = ((Deftemplate) it.next()).getName();
            if (!name.startsWith("MAIN::__")) {
                if (name.startsWith(moduleQualifier))
                    name = name.substring(moduleQualifier.length());
                list.add(name);
            }
        }
        String[] result = list.toArray(new String[list.size()]);
        Arrays.sort(result);
        return result;
    }

    private String[] listFunctionNames(Rete engine) {
        return ListFunctions.listAllFunctions(engine);
    }

    private String[] listFunctionAndConstructNames(Rete engine) {
        List<String> functions = Arrays.asList(ListFunctions.listAllFunctions(engine));
        List<String> constructs = Arrays.asList(CONSTRUCT_NAMES);
        TreeSet<String> set = new TreeSet<String>(functions);
        set.addAll(constructs);
        return set.toArray(new String[set.size()]);
    }

    /**
     * Parses a defquery construct and returns a Defquery object.
     * <p/>
     * Syntax:<br>
     * (defquery name<br>
     * [ "docstring...." ]<br>
     * [(declare (variables ?var1 ?var2 ...))]<br>
     * (pattern))<br>
     *
     * @return the parsed Defquery object
     * @throws JessException if there is a syntax error.
     */

    public synchronized Defquery parseDefquery(Context context, Rete engine, final JessTokenStream jts)
            throws JessException {

        JessToken tok;

        String nameAndDoc[] = parseNameAndDocstring("defquery", jts);

        // Parse variable, node-index-hash declarations
        HashMap<String, ValueVector> declarations = new HashMap<String, ValueVector>();
        Map<String, JessToken> declTokens = parseDeclarations(declarations, QUERY_DECLARABLES, engine, jts);

        //Parse all the LHS patterns
        String module = RU.getModuleFromName(nameAndDoc[0], engine);
        engine.setCurrentModule(module);
        Group patterns = parseLHS(engine, jts);

        tok = nextToken(jts);
        Defquery query = new Defquery(nameAndDoc[0], nameAndDoc[1], engine);

        // Should be looking at closing paren
        if (tok.m_ttype != ')')
            error("parseDefquery", "Expected ')', got " + tok.toString(), CLOSE_PAREN, ParseException.SYNTAX_ERROR, tok, query);

        for (Iterator<String> it = declarations.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            ValueVector vv = declarations.get(key);
            if (key.equals("variables")) {
                for (int j = 1; j < vv.size(); j++) {
                    Value v = vv.get(j);
                    // TODO Not reported well in IDE  !
                    if (v.type() != RU.VARIABLE)
                        error("parseDefquery", "Expected variable, got " + v.toString(), new String[]{v.toString()}, ParseException.SYNTAX_ERROR, tok, query);
                    query.addQueryVariable((Variable) v);
                }
            } else if (key.equals("node-index-hash"))
                query.setNodeIndexHash(vv.get(1).intValue(context));

            else if (key.equals("max-background-rules"))
                query.setMaxBackgroundRules(vv.get(1).intValue(context));

            else
                error("parseDefquery", "Invalid declarand", QUERY_DECLARABLES, ParseException.INVALID_DECLARAND, declTokens.get(key));
        }
        query.setLHS(patterns, engine);

        return query;
    }

    /**
     * Parses a deffunction contstuct to create a Deffunction object.
     * <p/>
     * Syntax:<br>
     * (deffunction name ["doc-comment"] (<arg1><arg2...) ["doc-comment"]<br>
     * (action)<br>
     * value<br>
     * (action))
     *
     * @return the parsed Deffunction construct
     * @throws JessException if there is a syntax error.
     */
    public Deffunction parseDeffunction(Rete engine, final JessTokenStream jts) throws JessException {
        Deffunction df;
        JessToken tok;

        /* ****************************************
           '(deffunction'
           **************************************** */

        if ((nextToken(jts).m_ttype != '(') ||
                !(nextToken(jts).m_sval.equals("deffunction")))
            error("parseDeffunction", "Expected (deffunction...", ParseException.SYNTAX_ERROR, jts.getLastToken());

        /* ****************************************
       deffunction name
       **************************************** */

        if ((tok = nextToken(jts)).m_ttype != JessToken.SYMBOL_TOK)
            error("parseDeffunction", "Expected deffunction name", ParseException.SYNTAX_ERROR, jts.getLastToken());
        String name = tok.m_sval;
        boolean alreadyExisted = engine.findUserfunction(name) != null;
        if (alreadyExisted)
            warning("parseDeffunction", "Function redefined", new String[0], ParseException.WARNING_REDEFINITION, tok);

        /* ****************************************
           optional comment
           **************************************** */

        String docstring = "";
        if ((tok = nextToken(jts)).m_ttype == JessToken.STRING_TOK) {
            docstring = tok.m_sval;
            tok = nextToken(jts);
        }

        df = new Deffunction(name, docstring);

        /* ****************************************
           Argument list
           **************************************** */

        jts.pushBack(tok);
        ArrayList<Argument> arguments = parseArgumentList(jts);

        for (Iterator<Argument> it = arguments.iterator(); it.hasNext();) {
            Deffunction.Argument argument = it.next();
            df.addArgument(argument);
        }

        /* ****************************************
           optional comment
           **************************************** */

        if ((tok = nextToken(jts)).m_ttype == JessToken.STRING_TOK) {
            df.setDocstring(tok.m_sval);
            tok = nextToken(jts);
        }

        /* ****************************************
           function calls and values
           **************************************** */

        if (!alreadyExisted)
            addDummyFunctionSoRecursiveCallsParse(engine, name);

        while (tok.m_ttype != ')') {
            if (tok.m_ttype == '(') {
                jts.pushBack(tok);
                Funcall f = parseFuncall(engine, jts);
                df.addAction(f);

            } else {
                switch (tok.m_ttype) {

                    case JessToken.SYMBOL_TOK:
                    case JessToken.STRING_TOK:
                    case JessToken.VARIABLE_TOK:
                    case JessToken.MULTIVARIABLE_TOK:
                    case JessToken.LONG_TOK:
                    case JessToken.FLOAT_TOK:
                    case JessToken.INTEGER_TOK:
                        df.addValue(tokenToValue(tok, engine, jts));
                        break;

                    default:
                        error("parseDeffunction", "Unexpected character", ParseException.SYNTAX_ERROR, tok, df);
                }
            }
            tok = nextToken(jts);
        }


        return df;
    }

    private ArrayList<Argument> parseArgumentList(JessTokenStream jts) throws JessException {
        expectOpenParen(jts, "parseArgumentList");
        JessToken tok;
        ArrayList<Argument> arguments = new ArrayList<Argument>();
        while ((tok = nextToken(jts)).m_ttype == JessToken.VARIABLE_TOK ||
                tok.m_ttype == JessToken.MULTIVARIABLE_TOK) {
            int type = tok.m_ttype == JessToken.VARIABLE_TOK ? RU.VARIABLE : RU.MULTIVARIABLE;
            arguments.add(new Deffunction.Argument(tok.m_sval, type));
        }

        expectCloseParen(tok, "parseArgumentList");

        return arguments;
    }

    private void addDummyFunctionSoRecursiveCallsParse(Rete engine, String name) {
        engine.addUserfunction(new DummyFunction(name));
    }

    Value parseAndExecuteFuncall(JessToken tok, Context c,
                                 Rete engine, final JessTokenStream jts,
                                 boolean warningsIncludeConstructs) throws JessException {
        if (tok != null)
            jts.pushBack(tok);
        Funcall fc = parseFuncall(engine, jts, warningsIncludeConstructs);
        jts.eatWhitespace();

        return fc.execute(c);

    }

    private JessToken nextTokenNoWarnings(JessTokenStream jts) throws JessException {
        boolean warnings = m_issueWarnings;
        m_issueWarnings = false;
        try {
            return nextToken(jts);
        } finally {
            m_issueWarnings = warnings;
        }
    }

    JessToken nextToken(JessTokenStream jts) throws JessException {
        JessToken token = jts.nextNonCommentToken();
        warnOnBadTokens(token);
        return token;
    }

    void warnOnBadTokens(JessToken token) {
        if (m_issueWarnings && isAnUndefinedDefglobal(token)) {
            warning("Jesp.nextToken", "Undefined defglobal " + token.m_sval, listAllDefglobals(), ParseException.WARNING_UNDEFINED_DEFGLOBAL, token);
        }
    }

    private boolean isAnUndefinedDefglobal(JessToken token) {
        return (token.isVariable() &&
                Defglobal.isADefglobalName(token.m_sval) &&
                !m_engine.getGlobalContext().isVariableDefined(token.m_sval));
    }

    private String[] listAllDefglobals() {
        ArrayList<String> list = new ArrayList<String>();
        for (Iterator<?> it = m_engine.listDefglobals(); it.hasNext();)
            list.add(((Defglobal) it.next()).getName());
        return list.toArray(new String[list.size()]);
    }

    Value tokenToValue(JessToken token, Rete engine, final JessTokenStream jts) throws JessException {
        ValueFactory factory = engine.getValueFactory();
        switch (token.m_ttype) {
            case JessToken.SYMBOL_TOK:
                return factory.get(token.m_sval, RU.SYMBOL);

            case JessToken.STRING_TOK:
                return factory.get(token.m_sval, RU.STRING);

            case JessToken.VARIABLE_TOK:
                return factory.get(token.m_sval, RU.VARIABLE);
            case JessToken.MULTIVARIABLE_TOK:
                return factory.get(token.m_sval, RU.MULTIVARIABLE);

            case JessToken.FLOAT_TOK:
                return factory.get(token.m_nval, RU.FLOAT);
            case JessToken.INTEGER_TOK:
                return factory.get(token.m_nval, RU.INTEGER);
            case JessToken.LONG_TOK:
                return factory.get(token.m_lval);

            case'(':
                jts.pushBack(token);
                Funcall fc = parseFuncall(engine, jts);
                return new FuncallValue(fc);

            default:
                return Funcall.NIL;
        }
    }


    /*
     * Make error reporting a little more compact.
     * Expect line number, program text to be set in "finally" blocks above
     */
    public void error(String routine, String msg, int code, JessToken errorToken) throws JessException {
        ParseException p = new ParseException("Jesp." + routine, msg, m_fileName, errorToken);
        p.setErrorCode(code);
        throw p;
    }

    public void error(String routine, String msg, int code, JessToken errorToken, Named construct) throws JessException {
        ParseException p = new ParseException("Jesp." + routine, msg, m_fileName, errorToken, construct);
        p.setErrorCode(code);
        throw p;
    }

    public void error(String routine, String msg, String[] alternatives, int code, JessToken errorToken) throws JessException {
        ParseException p = new ParseException("Jesp." + routine, msg, m_fileName, alternatives, errorToken);
        p.setErrorCode(code);
        throw p;
    }

    public void error(String routine, String msg, String[] alternatives, int code, JessToken errorToken, Named construct) throws JessException {
        ParseException p = new ParseException("Jesp." + routine, msg, m_fileName, alternatives, errorToken, construct);
        p.setErrorCode(code);
        throw p;
    }

    public void warning(String routine, String msg, String[] alternatives, int code, JessToken errorToken) {
        //noinspection ThrowableInstanceNeverThrown
        ParseException p = new ParseException("Jesp." + routine, msg, m_fileName, alternatives, errorToken);
        p.setErrorCode(code);
        p.setLineNumber(errorToken.m_lineno);
        m_warnings.add(p);
    }

    public static boolean isAConstructName(String name) {
        if (name.startsWith("def")) {
            for (int i = 0; i < CONSTRUCT_NAMES.length; i++) {
                if (name.equals(CONSTRUCT_NAMES[i]))
                    return true;
            }
        }
        return false;
    }

    private JessToken expectCloseParen(JessTokenStream jts, String routine) throws JessException {
        return expectCloseParen(nextToken(jts), routine);
    }

    private JessToken expectCloseParen(JessToken tok, String routine) throws JessException {
        if (tok.m_ttype != ')')
            error(routine, "Expected ')'", CLOSE_PAREN, ParseException.SYNTAX_ERROR, tok);
        return tok;
    }


    private JessToken expectOpenParen(JessTokenStream jts, String routine) throws JessException {
        return expectOpenParen(nextToken(jts), routine);
    }

    private JessToken expectOpenParen(JessToken tok, String routine) throws JessException {
        if (tok.m_ttype != '(')
            error(routine, "Expected '('", CLOSE_PAREN, ParseException.SYNTAX_ERROR, tok);
        return tok;
    }

    private static class DummyFunction implements Userfunction {
        private String m_name;

        public DummyFunction(String name) {
            m_name = name;
        }

        public String getName() {
            return m_name;
        }

        public Value call(ValueVector vv, Context context) throws JessException {
            return Funcall.NIL;
        }
    }

}


