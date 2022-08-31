package jess;

import java.util.HashMap;
import java.io.Serializable;

/**
 * (C) 2007 Sandia National Laboratories<BR>
 * $Id: FactFunctions.java,v 1.6 2008-05-05 22:06:24 ejfried Exp $
 */
class FactFunctions extends IntrinsicPackageImpl {

    public void add(HashMap ht) {
        addFunction(new Assert(), ht);
        addFunction(new Retract(), ht);
        addFunction(new Update(), ht);
        addFunction(new RetractString(), ht);
        addFunction(new Duplicate(), ht);
        addFunction(new Modify(), ht);
        addFunction(new AssertString(), ht);
        addFunction(new Add(), ht);
    }

    class Add implements Userfunction, Serializable {

        public String getName() {
            return "add";
        }

        public Value call(ValueVector vv, Context context) throws JessException {
            Rete engine = context.getEngine();
            Object o = vv.get(1).javaObjectValue(context);            
            return engine.add(o, context);
        }
    }

    /**
     * *** assert  ***
     */
    class Assert implements Userfunction, Serializable {
        public String getName() {
            return "assert";
        }

        public Value call(ValueVector vvec, Context context)
                throws JessException {
            Fact result = null;
            Rete engine = context.getEngine();
            for (int i = 1; i < vvec.size(); i++) {
                Fact fact = vvec.get(i).factValue(context).expand(context);
                result = engine.assertFact(fact, context);
            }
            if (result != null)
                return new FactIDValue(result);
            else
                return Funcall.FALSE;
        }
    }

    /**
     * *** update  ***
     */
    class Update implements Userfunction, Serializable {
        public String getName() {
            return "update";
        }

        public Value call(ValueVector vvec, Context context)
                throws JessException {
            Value result = null;
            Rete engine = context.getEngine();
            for (int i = 1; i < vvec.size(); i++) {
                Object o = vvec.get(i).javaObjectValue(context);
                result = engine.updateObject(o);
            }
            if (result != null)
                return result;
            else
                return Funcall.FALSE;
        }
    }

    /**
     * *** retract ***
     */

    class Retract implements Userfunction, Serializable {
        public String getName() {
            return "retract";
        }

        public Value call(ValueVector vv, Context context) throws JessException {
            Value v = vv.get(1);
            if (v.type() == RU.SYMBOL && v.stringValue(context).equals("*")) {
                context.getEngine().removeAllFacts();

            } else {
                Rete engine = context.getEngine();
                for (int i = 1; i < vv.size(); i++) {
                    Fact fact = vv.get(i).factValue(context);

                    if (fact != null)
                        engine.retract(fact);
                    else
                        return Funcall.FALSE;
                }
            }
            return Funcall.TRUE;
        }
    }

    class AssertString implements Userfunction, Serializable {
        public String getName() {
            return "assert-string";
        }

        public Value call(ValueVector vv, Context context)
                throws JessException {
            String factString = vv.get(1).stringValue(context);
            Fact fact = context.getEngine().assertString(factString, context);
            if (fact != null)
                return new FactIDValue(fact);
            else
                return Funcall.FALSE;
        }
    }

    /**
     * Karl Mueller NASA/GSFC Code 522.2
     * (Karl.R.Mueller@gsfc.nasa.gov)
     * 26.January.1998
     * <p/>
     * *** retract-string ***
     * Added function to retract fact as a string
     */

    class RetractString implements Userfunction, Serializable {
        public String getName() {
            return "retract-string";
        }

        public Value call(ValueVector vv, Context context) throws JessException {
            for (int i = 1; i < vv.size(); i++) {
                context.getEngine().retractString(vv.get(i).stringValue(context));
            }
            return Funcall.TRUE;
        }
    }



}
