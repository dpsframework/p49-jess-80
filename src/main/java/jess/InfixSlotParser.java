package jess;

import java.util.*;

/**
 * Utility class for parsing infix slots. Pulled out of Jesp for testing.
 * (C) 2013 Sandia Corporation
 */
class InfixSlotParser {
    private Jesp jesp;
    static final String[] INFIX_OPERATORS = {"==", "<=", ">=", "<", ">", "!=", "<>"};
    static final String[] LOGICAL_OPERATORS = {"&&", "||"};

    static {
        Arrays.sort(INFIX_OPERATORS);        
    }                       

    public InfixSlotParser(Jesp jesp) {
        this.jesp = jesp;
    }

    void parse(Pattern p, JessTokenStream jts, Rete engine) throws JessException {
        JessToken tok;
        Stack stack = new Stack();

        tok = parseLoop(jts, p, engine, stack);

        if (stack.size() != 1)
            jesp.error("Jesp.parseInfixExpression", "Syntax error", ParseException.SYNTAX_ERROR, tok);
        Expr expr = (Expr) stack.pop();
        boolean hasOr = expr.hasOr();
        if (hasOr)
            combineTestsOnHighestIndexedSlot(expr, p, engine);
        else
            addEachTestToItsOwnSlot(expr, p);
    }

    private JessToken parseLoop(JessTokenStream jts, Pattern p, Rete engine, Stack stack) throws JessException {
        JessToken tok;
        while (true) {
            tok = jesp.nextToken(jts);
            if (tok.m_ttype == '}' || tok.m_ttype == ')')
                break;
            else
                jts.pushBack(tok);
            parseExpr(jts, p, engine, stack);
        }
        return tok;
    }

    private void parseExpr(JessTokenStream jts, Pattern p, Rete engine, Stack stack) throws JessException {
        JessToken tok;
        tok = jesp.nextToken(jts);
        if (tok.m_ttype == '(') {    
            Stack subStack = new Stack();
            tok = parseLoop(jts, p, engine, subStack);
            if (subStack.isEmpty())
                jesp.error("Jesp.parseInfixExpression", "Unexpected token", ParseException.SYNTAX_ERROR, tok);
            else if (subStack.size() > 1)
                jesp.error("Jesp.parseInfixExpression", "Unexpected token", ParseException.SYNTAX_ERROR, tok);
            else {
                Expr expr = (Expr) subStack.pop();
                combineExprOnStack(stack, expr, tok);
            }
        } else if (tok.m_ttype == ')') {
            return;

        } else if (isRelationalOperator(tok)) {
            if (stack.isEmpty())
                jesp.error("Jesp.parseInfixExpression", "Unexpected token", ParseException.SYNTAX_ERROR, tok);
            else if (stack.peek() instanceof OperatorExpr)
                jesp.error("Jesp.parseInfixExpression", "Unexpected token", ParseException.SYNTAX_ERROR, tok);
            else {
                OperatorExpr expr = new OperatorExpr(tok.m_sval);
                stack.push(expr);
            }
        } else {
            jts.pushBack(tok);
            Expr expr = parseOneTest(p, jts, engine);
            combineExprOnStack(stack, expr, tok);
        }
    }

    private void combineExprOnStack(Stack stack, Expr expr, JessToken tok) throws JessException {
        if (!stack.empty()) {
            if (stack.peek() instanceof OperatorExpr) {
                String op = ((OperatorExpr) stack.pop()).operator;
                Expr left = (Expr) stack.pop();
                RelationExpr rel = new RelationExpr(left, op, expr);
                stack.push(rel);
            } else {
                jesp.error("Jesp.parseInfixExpression", "Logical operator expected",
                        LOGICAL_OPERATORS, ParseException.SYNTAX_ERROR, tok);
            }
        } else {
            stack.push(expr);
        }
    }

    private Expr parseOneTest(Pattern p, JessTokenStream jts, Rete engine) throws JessException {
        String slotName = parseSlotForInfixPattern(jts, p);
        Variable variable = getVariableBoundToSlot(p, slotName);
        String operator = parseOperatorForInfixPattern(jts);
        Value literal = parseOperandForInfixPattern(jts, engine, p);

        if (operator.equals("=="))
            operator = "eq";
        else if (operator.equals("!="))
            operator = "neq";
        InfixTest1 aTest = buildFunctionForInfixPattern(p, slotName, operator, engine, literal, variable);
        /*}*/
        return new TestExpr(aTest);
    }

    private Variable getVariableBoundToSlot(Pattern p, String slotName) throws JessException {
        // If this slot isn't already bound to a variable, bind it to one
        // named after this slot
        int index = p.getDeftemplate().getSlotIndex(slotName);
        Variable variable = p.getVariable(index, -1);
        return variable;
    }

    private void addEachTestToItsOwnSlot(Expr expr, Pattern p) throws JessException {
        for (Iterator it = allTests(expr); it.hasNext();) {
            InfixTest1 test = (InfixTest1) it.next();
            p.addTest(test);
        }
    }

    private Iterator allTests(Expr expr) {
        ArrayList list = new ArrayList();
        addTestsToList(expr, list);
        return list.iterator();
    }

    private void addTestsToList(Expr expr, ArrayList list) {
        if (expr instanceof TestExpr) {
            InfixTest1 test = ((TestExpr) expr).test;
            list.add(test);
        } else {
            RelationExpr rel = (RelationExpr) expr;
            addTestsToList(rel.left, list);
            addTestsToList(rel.right, list);
        }
    }

    private void combineTestsOnHighestIndexedSlot(Expr expr, Pattern p, Rete engine) throws JessException {
        String slotToUse = highestIndexedSlot(allTests(expr), p.getDeftemplate());
        FuncallValue v = new FuncallValue(expr.getFuncall(engine));
        Test1 test = new Test1(Test1.EQ, slotToUse, v, RU.AND);
        p.addTest(test);
    }

    private String highestIndexedSlot(Iterator tests, Deftemplate deft) throws JessException {
        int max = 0;
        while (tests.hasNext()) {
            String slot = ((InfixTest1) tests.next()).m_slotName;
            int index = deft.getSlotIndex(slot);
            if (index > max)
                max = index;
        }
        return deft.getSlotName(max);
    }

    private InfixTest1 buildFunctionForInfixPattern(Pattern p, String slotName, String operator,
                                                    Rete engine, Value literal, Variable var) throws JessException {
        Funcall f = new Funcall(operator, engine);
        f.arg(var);
        f.arg(literal);

        return new InfixTest1(Test1.EQ, -1, new FuncallValue(f), slotName, var);
    }

    private boolean isInfixOperator(JessToken tok) {
        String value = tok.toString();
        return Arrays.binarySearch(INFIX_OPERATORS, value) > -1;
    }

    private boolean isRelationalOperator(JessToken tok) {
        String value = tok.toString();
        return value.equals("||") || value.equals("&&");
    }

    private String parseOperatorForInfixPattern(JessTokenStream jts) throws JessException {
        JessToken tok;
        if (!isInfixOperator(tok = jesp.nextToken(jts)))
            jesp.error("parseInfixSlot", "Expected operator", INFIX_OPERATORS, ParseException.SYNTAX_ERROR, tok);
        return tok.m_sval;
    }

    private String parseSlotForInfixPattern(JessTokenStream jts, Pattern p) throws JessException {
        Deftemplate deft = p.getDeftemplate();
        String slotName = jesp.checkForValidSlotName(jts, deft);
        if (deft.getSlotType(slotName) == RU.MULTISLOT)
            jesp.error("parseInfixSlot", "Can't use infix expressions with multislots",
                    deft.getSlotNames(), ParseException.SEMANTIC_ERROR, jts.getLastToken());

        return slotName;
    }

    private Value parseOperandForInfixPattern(JessTokenStream jts, Rete engine, Pattern p) throws JessException {
        JessToken tok;
        tok = jesp.nextToken(jts);
        Value literal = jesp.tokenToValue(tok, engine, jts);

        // If it's a symbol then it might be a slot name
        if (literal.type() == RU.SYMBOL) {
            String slotname = literal.symbolValue(null);
            if (slotname.indexOf(engine.getMemberChar()) < 1)
                literal = possiblyTestForLocalSlot(p, literal);
            else
                literal = possiblyTestForNonlocalSlot(literal, engine);
        }

        return literal;
    }

    private Value possiblyTestForNonlocalSlot(Value literal, Rete engine) throws JessException {
        String symbol = literal.symbolValue(null);
        return new UnresolvedSlotReference(symbol, engine.getMemberChar());
    }

    private Value possiblyTestForLocalSlot(Pattern p, Value literal) throws JessException {
        Deftemplate deft = p.getDeftemplate();
        String slotname = literal.symbolValue(null);
        int index = deft.getSlotIndex(slotname);
        // If this symbol names a slot in this pattern's template
        if (index != -1) {
            // Then (1) Replace the literal with a variable
            //      (2) Add a test that binds that other slot to the variable
            Variable var = p.getVariable(index, -1);
            p.addTest(new Test1(TestBase.EQ, slotname, -1, var));
            return var;
        } else {
            return literal;
        }
    }

    static class UnresolvedSlotReference extends Variable {
        private String m_slotName;
        private String m_patternBinding;

        UnresolvedSlotReference(String variableName, char memberChar) throws JessException {
            super(variableName, RU.VARIABLE);
            int index = variableName.indexOf(memberChar);
            m_patternBinding = variableName.substring(0, index);
            m_slotName = variableName.substring(index+1);
        }

        public String getSlotName() {
            return m_slotName;
        }

        public String getPatternBinding() {
            return m_patternBinding;
        }

    }

    static class InfixTest1 extends Test1 {
        private Variable m_variable;

        public InfixTest1(int test, int sub_idx, Value slot_value, String slotName, Variable variable) {
            super(test, slotName, sub_idx, slot_value);
            m_variable = variable;
        }
    }

    interface Expr {
        boolean hasOr();
        Funcall getFuncall(Rete engine) throws JessException;
    }

    static class TestExpr implements Expr {
        InfixTest1 test;

        public TestExpr(InfixTest1 test) {
            this.test = test;
        }

        public boolean hasOr() {
            return false;
        }

        public Funcall getFuncall(Rete engine) throws JessException {
            if (test.m_slotValue instanceof FuncallValue)
                return test.m_slotValue.funcallValue(null);
            else {
                String functionName = test.m_test == Test1.NEQ ? "neq" : "eq";
                Funcall f = new Funcall(functionName, engine);
                f.arg(test.m_variable);
                f.arg(test.m_slotValue);
                return f;
            }
        }
    }

    static class RelationExpr implements Expr {
        Expr left;
        String operator;
        Expr right;

        public RelationExpr(Expr left, String operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        public boolean hasOr() {
            return "||".equals(operator) || left.hasOr() || right.hasOr();
        }

        public Funcall getFuncall(Rete engine) throws JessException {
            String functor = (operator.equals("&&")) ? "and" : "or";

            Funcall f = new Funcall(functor, engine);
            f.arg(left.getFuncall(engine));
            f.arg(right.getFuncall(engine));
            return f;
        }
    }

    static class OperatorExpr implements Expr {
        String operator;

        public OperatorExpr(String operator) {
            this.operator = operator;
        }

        public boolean hasOr() {
            return operator.equals("||");
        }

        public Funcall getFuncall(Rete engine) {
            throw new RuntimeException("Operation not supported");
        }
    }
}
