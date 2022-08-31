package jess;

import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Return a list of all defined functions.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


class ListFunctions implements Userfunction, Serializable {
    public String getName() {
        return "list-function$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        String[] names = listAllFunctions(context.getEngine());
        ValueVector rv = toValueVector(names);
        return new Value(rv, RU.LIST);
    }

    static ValueVector toValueVector(String[] names) throws JessException {
        ValueVector rv = new ValueVector(names.length);
        for (int i = 0; i < names.length; ++i)
            rv.add(new Value(names[i], RU.SYMBOL));
        return rv;
    }

    static String[] listAllFunctions(Rete engine) {
        TreeSet set = new TreeSet();

        Iterator e = engine.listFunctions();
        while (e.hasNext()) {
            String name = ((Userfunction) e.next()).getName();
            if (name.indexOf(".") == -1)
                set.add(name);
        }

        e = Funcall.listIntrinsics();
        while (e.hasNext()) {
            String name = ((Userfunction) e.next()).getName();
            set.add(name);
        }

        return (String[]) set.toArray(new String[set.size()]);
    }
}
