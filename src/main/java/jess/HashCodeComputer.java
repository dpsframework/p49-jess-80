package jess;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The HashCodeComputer allows hash code values for objects to be computed depending
 * on their status as instances of "value" or "non-value" classes.
 *
 * @see #isValueObject(Object)
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class HashCodeComputer {

    private static final AbstractCollection<Class> m_nonValueClasses = new ConcurrentLinkedQueue<Class>();
    static {
        m_nonValueClasses.add(Map.class);
        m_nonValueClasses.add(Collection.class);
    }

    /**
     * Return a hashCode value for an object. If the argument is a "value object", the object's hashCode() is called.
     * If the argument is not, then a constant value is returned. This effectively means that hashing on non-value objects
     * degenerates to linear search.
     * @see #isValueObject(Object)
     * @param o the object
     * @return a constant hashcode for the object
     */
    public static int hashCode(Object o) {
        if (isValueObject(o))
            return o.hashCode();
        else
            return 0;
    }

    /**
     * Report whether or not Jess considers an object to be a "value object."
     * As far as Jess is concerned, an object can be considered  a "value object" if the return
     * value of its hashCode() method will never change during the time Jess is working with the object. By default,
     * Map and Collection instances are known to be non-value objects, while all others are assumed to be value objects.
     * @see #setIsValueClass(Rete, String, boolean) 
     * @param o the object
     * @return true if the object is a value object
     */
    public static boolean isValueObject(Object o) {
        for (Class clazz: m_nonValueClasses) {
            if (clazz.isAssignableFrom(o.getClass()))
                return false;
        }
        return true;
    }

    /**
     * Record whether the class by the given name is a value object class. Jess assumes all classes are value classes by
     * default except for Maps and Collections. It's important that you tell Jess when a class is not a value object class;
     * failure to do so can cause corruption of working memory and undefined results.
     *
     * @param engine the rule engine used to resolve the class name
     * @param name the name of a class
     * @param status true if class is a value class
     * @throws ClassNotFoundException if there's no class by this name
     */
    public static void setIsValueClass(Rete engine, String name, boolean status) throws ClassNotFoundException {
        final Class aClass = engine.findClass(name);
        synchronized(m_nonValueClasses) {
            if (status) {
                m_nonValueClasses.remove(aClass);
            } else {
                if (!m_nonValueClasses.contains(aClass))
                    m_nonValueClasses.add(aClass);
            }
        }
    }
}
