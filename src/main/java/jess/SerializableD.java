package jess;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * A serializable description of a reflected Java method or member variable.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

interface SerializableD extends Serializable {
    String getName();

    Class getPropertyType(Rete engine) throws JessException;

    Object getPropertyValue(Rete engine, Object o) throws JessException, IllegalAccessException, InvocationTargetException;

    void setPropertyValue(Rete engine, Object om, Object value) throws JessException, IllegalAccessException, InvocationTargetException;
}
