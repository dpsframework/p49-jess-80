package jess;

import java.io.Serializable;


/**
 * Tell Jess to match on properties of a specific Java object
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Definstance implements Userfunction, Serializable {

    /**
     * Return the name of this command
     *
     * @return The command name
     */
    public String getName() {
        return "definstance";
    }

    /**
     * SYNTAX: (definstance <jess-classname> <Java-object>
     * [static | dynamic | auto])
     */
    public Value call(ValueVector vv, Context context) throws JessException {
        // ###
        Value v = vv.get(2).resolveValue(context);
        if (v.equals(Funcall.NIL))
            throw new JessException("definstance",
                                    "Argument is nil:", v.toString());

        Rete engine = context.getEngine();
        String jessTypename = vv.get(1).stringValue(context);       
        Object object = v.javaObjectValue(context);

        boolean dynamic;
        if (vv.size() == 4) {
            String shadowType = vv.get(3).symbolValue(context);
            if (shadowType.equals("dynamic"))
                dynamic = true;
            else if (shadowType.equals("static"))
                dynamic = false;
            else if (shadowType.equals("auto"))
                dynamic = acceptsPropertyChangeListeners(object, engine);
            else
                throw new JessException("definstance", "invalid shadow type", shadowType);
        } else {
            dynamic = acceptsPropertyChangeListeners(object, engine);
        }


        return engine.definstance(jessTypename, object, dynamic, context);
    }

    static boolean acceptsPropertyChangeListeners(Object object, Rete engine) throws JessException {
        try {
            if (Call.hasMethodOfName(object.getClass(), "addPropertyChangeListener")) {
                Class pcl = engine.findClass("java.beans.PropertyChangeListener");
                Class[] args = new Class[]{pcl};
                object.getClass().getMethod("addPropertyChangeListener", args);
                object.getClass().getMethod("removePropertyChangeListener", args);
                return true;
            } else
                return false;
        } catch (ClassNotFoundException e) {
            throw new JessException("definstance", "Class not found", e);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
