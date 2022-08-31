package jess;

import java.io.Serializable;
import java.util.*;

class ControlFunctions extends IntrinsicPackageImpl {
    public void add(HashMap ht) {
        addFunction(new Foreach(), ht);
        addFunction(new While(), ht);
        addFunction(new If(), ht);
        addFunction(new TryCatch(), ht);
        addFunction(new Throw(), ht);
        addFunction(new For(), ht);
        addFunction(new Break(), ht);
        addFunction(new Continue(), ht);
    }
}

class Foreach implements Userfunction, Serializable {

    public String getName() {
        return "foreach";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value collection = vv.get(2).resolveValue(context);
        if (collection.type() == RU.LIST)
            return processValueVector(vv, collection, context);
        else if (collection.type() == RU.JAVA_OBJECT) {
            Object object = collection.javaObjectValue(context);
            if (object instanceof Collection)
                return processIterator(vv, ((Collection) object).iterator(), context);
            else if (object instanceof Iterator)
                return processIterator(vv, ((Iterator) object), context);
        }
        throw new JessException("foreach", "Argument must be ValueVector, Collection, or Iterator:", collection.toString());

    }

    private Value processIterator(ValueVector vv, Iterator it, Context context) throws JessException {
        String variable = vv.get(1).variableValue(context);
        Value v = Funcall.NIL;

        try {
            while (it.hasNext()) {
                Object item = it.next();
                Value itemValue;
                if (item instanceof Value)
                    itemValue = (Value) item;
                else
                    itemValue = RU.objectToValue(item.getClass(), item);
                context.setVariable(variable, itemValue.resolveValue(context));
                for (int j = 3; j < vv.size(); j++) {
                    v = vv.get(j).resolveValue(context);
                    if (context.returning()) {
                        v = context.getReturnValue();
                        return v;
                    }
                }
            }
        } catch (BreakException bk) {
            // Fall through
        }
        return v;
    }

    private Value processValueVector(ValueVector vv, Value collection, Context context) throws JessException {
        String variable = vv.get(1).variableValue(context);
        ValueVector items = collection.listValue(context);
        Value v = Funcall.NIL;

        try {
            for (int i = 0; i < items.size(); i++) {
                context.setVariable(variable, items.get(i).resolveValue(context));
               try {
                for (int j = 3; j < vv.size(); j++) {
                    v = vv.get(j).resolveValue(context);
                    if (context.returning()) {
                        v = context.getReturnValue();
                        return v;
                    }
                }
               } catch (ContinueException ce) {
                  // Fall through
               }
            }
        } catch (BreakException bk) {
            // Fall through
        }
        return v;
    }
}

class Throw implements Userfunction, Serializable {

    public String getName() {
        return "throw";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Throwable t = (Throwable) vv.get(1).javaObjectValue(context);
        t.fillInStackTrace();

        if (t instanceof JessException)
            throw (JessException) t;
        else {
            throw new JessException("throw",
                                    "Exception thrown from Jess language code",
                                    t);
        }
    }
}

class TryCatch implements Userfunction, Serializable {

    public String getName() {
        return "try";
    }

    /** @noinspection EqualsBetweenInconvertibleTypes*/
    public Value call(ValueVector vv, Context context) throws JessException {

        // First find catch and/or finally
        int catchKeyword = -1;      // index of catch keyword
        int finallyKeyword = -1;    // index of finally keyword
        int endOfTryBlock = -1;
        int endOfCatchBlock = -1;
        int endOfFinallyBlock = -1;

        for (int j = 1; j < vv.size(); j++) {

            if (vv.get(j).type() == RU.SYMBOL &&
                    vv.get(j).equals("catch") &&
                    catchKeyword < 0) {

                // try/catch + possibly finally
                catchKeyword = j;
                endOfTryBlock = catchKeyword;
                // Set default
                endOfCatchBlock = vv.size();
            }

            if (vv.get(j).type() == RU.SYMBOL &&
                    vv.get(j).equals("finally")) {

                finallyKeyword = j;
                endOfFinallyBlock = vv.size();

                if (catchKeyword > 0) {

                    // try/catch/finally
                    endOfCatchBlock = finallyKeyword;
                } else {
                    // try/finally
                    endOfTryBlock = finallyKeyword;
                }

                break;
            }
        }

        if (catchKeyword == -1 && finallyKeyword == -1)
            throw new JessException("try",
                                    "Neither catch nor finally block in try expression", "");

        Value v = Funcall.NIL;

        try {
            for (int j = 1; j < endOfTryBlock; j++) {
                v = vv.get(j).resolveValue(context);
                if (context.returning()) {
                    v = context.getReturnValue();
                    break;
                }
            }
        } catch (Throwable t) {
            // Rethrow if there is no catch block
            if (catchKeyword == -1) {
                if (t instanceof JessException) {
                    throw (JessException) t;
                } else {
                    throw new JessException("TryCatchThrow.call",
                                            "Unexpected exception in try-block",
                                            t);
                }
            }

            v = Funcall.FALSE; // so we can have empty handlers
            context.setVariable("ERROR", new Value(t));

            for (int j = ++catchKeyword; j < endOfCatchBlock; j++) {
                v = vv.get(j).resolveValue(context);

                if (context.returning()) {
                    v = context.getReturnValue();
                    break;
                }
            }
        } finally {
            boolean wasReturning = context.returning();
            context.clearReturnValue();
            for (int j = ++finallyKeyword; j < endOfFinallyBlock; j++) {
                vv.get(j).resolveValue(context);

                if (context.returning()) {
                    return context.getReturnValue();
                }
            }
            if (wasReturning)
                context.setReturnValue(v);
        }

        // Return a value if there is no uncaught exception
        return v;
    }
}


class While implements Userfunction, Serializable {

    public String getName() {
        return "while";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        // This accepts a superset of the correct syntax...
        Value result = vv.get(1).resolveValue(context);

        // Skip optional do
        int sawDo = 0;
        if (vv.get(2).equals(Funcall.s_do))
            ++sawDo;

        outer_loop:
        try {
            while (!result.equals(Funcall.FALSE)) {
		try {
		    for (int i = 2 + sawDo; i < vv.size(); i++) {
			vv.get(i).resolveValue(context);
			if (context.returning()) {
			    result = context.getReturnValue();
			    break outer_loop;
			}
		    }
		} catch (ContinueException ce) {
		    // Fall through
		}
		
                result = vv.get(1).resolveValue(context);
		
            }
        } catch (BreakException bk) {
            // Fall through
        }
        return result;
    }
}



class For implements Userfunction, Serializable {

    public String getName() {
        return "for";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        Value initializer = vv.get(1);
        Value test = vv.get(2);
        Value increment = vv.get(3);

        initializer.resolveValue(context);
        try {
            while (!test.resolveValue(context).equals(Funcall.FALSE)) {
                try {
                for (int i = 4; i<vv.size(); ++i) {
                    vv.get(i).resolveValue(context);
                    if (context.returning()) {
                        return context.getReturnValue();
                    }
                }
                } catch (ContinueException ce) {
                // Fall through
                }
                increment.resolveValue(context);
            }
        } catch (BreakException bk) {
            // Fall through
        }
        return Funcall.FALSE;
    }
}

class Break implements Userfunction, Serializable {

    public String getName() {
        return "break";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        throw BreakException.INSTANCE;
    }
}

class Continue implements Userfunction, Serializable {
    public String getName() {
        return "continue";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        throw ContinueException.INSTANCE;
    }
}

