package jess;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

/**
 * Some functions associated with defmodules.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class ModuleFunctions extends IntrinsicPackageImpl {

    public void add(HashMap table) {

        addFunction(new SetFocus(), table);
        addFunction(new GetFocus(), table);
        addFunction(new SetCurrentModule(), table);
        addFunction(new GetCurrentModule(), table);
        addFunction(new ListFocusStack(), table);
        addFunction(new GetFocusStack(), table);
        addFunction(new ClearFocusStack(), table);
        addFunction(new PopFocus(), table);
    }
}

class SetFocus implements Userfunction, Serializable {
    public String getName() {
        return "focus";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete engine = context.getEngine();
        String oldFocus = engine.getFocus();
        for (int i = (vv.size() - 1); i > 0; --i) {
            String module = vv.get(i).stringValue(context);
            engine.setFocus(module);
        }
        return new Value(oldFocus, RU.SYMBOL);
    }
}

class GetFocus implements Userfunction, Serializable {
    public String getName() {
        return "get-focus";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String module = context.getEngine().getFocus();
        return context.getEngine().getValueFactory().get(module, RU.SYMBOL);
    }
}

class PopFocus implements Userfunction, Serializable {
    public String getName() {
        return "pop-focus";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String module = context.getEngine().popFocus(null);
        return context.getEngine().getValueFactory().get(module, RU.SYMBOL);
    }
}

class SetCurrentModule implements Userfunction, Serializable {
    public String getName() {
        return "set-current-module";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String module = vv.get(1).stringValue(context);
        String oldModule = context.getEngine().setCurrentModule(module);
        return context.getEngine().getValueFactory().get(oldModule, RU.SYMBOL);
    }
}

class GetCurrentModule implements Userfunction, Serializable {
    public String getName() {
        return "get-current-module";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String module = context.getEngine().getCurrentModule();
        return context.getEngine().getValueFactory().get(module, RU.SYMBOL);
    }
}


class ClearFocusStack implements Userfunction, Serializable {
    public String getName() {
        return "clear-focus-stack";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        context.getEngine().clearFocusStack();
        return Funcall.NIL;
    }
}

/**
 * Has to "flip" the values as returned by listFocusStack().
 */

class ListFocusStack implements Userfunction, Serializable {
    public String getName() {
        return "list-focus-stack";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete r = context.getEngine();
        PrintWriter outStream = r.getOutStream();
        Stack s = new Stack();
        for (Iterator it = r.listFocusStack(); it.hasNext();)
            s.push(it.next());
        while (!s.isEmpty())
            outStream.println(s.pop());
        outStream.flush();
        return Funcall.NIL;
    }
}

class GetFocusStack implements Userfunction, Serializable {
    public String getName() {
        return "get-focus-stack";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Rete r = context.getEngine();
        ValueVector rv = new ValueVector();
        Stack s = new Stack();
        for (Iterator it = r.listFocusStack(); it.hasNext();)
            s.push(it.next());
        while (!s.isEmpty())
            rv.add(context.getEngine().getValueFactory().get((String) s.pop(), RU.SYMBOL));
        return new Value(rv, RU.LIST);
    }
}



