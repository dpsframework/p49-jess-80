package jess;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jess.ClassResearcher.Property;

public class AndroidClassResearcher implements ClassResearcher, Serializable {

	private Rete m_engine;

	public AndroidClassResearcher(Rete engine) {
		m_engine = engine;
	}

	public Property[] getBeanProperties(String clazz)
			throws ClassNotFoundException, JessException {

		Map<String, Property> properties = new HashMap<String, Property>();
		Class<?> c = m_engine.findClass(clazz);
		Method[] methods = c.getMethods();
		for (Method m: methods) {
			createPropertyIfGetter(m, properties);        	
		}

		for (Method m: methods) {
			addToPropertyIfSetter(m, properties);        	
		}

		return properties.values().toArray(new Property[properties.size()]);
	}

	private void addToPropertyIfSetter(Method m, Map<String, Property> properties) {
		String name = m.getName();
		if (m.getParameterTypes().length != 1)
			return;
		else if (Modifier.isStatic(m.getModifiers()))
			return;

		
		// TODO Check argument type
		if (name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3))) {
			String propname = getPropertyName(name, "set");
			Property p = properties.get(propname);
			if (p != null) {
				p.setWriteMethod(m);
			}
		}
	}

	private void createPropertyIfGetter(Method m, Map<String, Property> properties) {
		if (m.getParameterTypes().length != 0)
			return;
		
		else if (Modifier.isStatic(m.getModifiers()))
			return;

		String name = m.getName();
		if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3))) {
			String propName = getPropertyName(name, "get");
			Property p = new Property(propName, getSlotType(m.getReturnType()), false, m, null);
			properties.put(propName, p);
		} else if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2))) {
			String propName = getPropertyName(name, "is");
			Property p = new Property(propName, getSlotType(m.getReturnType()), false, m, null);
			properties.put(propName, p);
		}			
	}

	// TODO: Needs to be a little fancier
	String getPropertyName(String name, String prefix) {
		if (name.length() - prefix.length() > 1) {
			if (Character.isLowerCase(name.charAt(prefix.length() + 1))) {
				// Names like "getFoo"
				return Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
			} else {
				// Names like "getURL"
				return name.substring(prefix.length());
			}
			
		} else {
			// Names like "getA"
			return name.substring(prefix.length()).toLowerCase();
		}
	}

	public Property[] getPublicInstanceFields(String clazz)
			throws ClassNotFoundException, JessException {
		Class<?> c = m_engine.findClass(clazz);
		Field[] fields = c.getFields();
		ArrayList<Property> properties = new ArrayList<Property>();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			if (Modifier.isStatic(f.getModifiers()))
				continue;

			String name = f.getName();
			Class<?> type = f.getType();
			boolean isArray = type.isArray();
			if (isArray)
				type = type.getComponentType();
			String jessType = getSlotType(type);

			properties.add(new Property(name, jessType, isArray, null, null));
		}
		return (Property[]) properties.toArray(new Property[properties.size()]);
	}

	static String getSlotType(Class<?> type) {
		if (type == int.class || type == Integer.class ||
				type == short.class || type == Short.class ||
				type == byte.class || type == Byte.class)
			return "INTEGER";
		else if (type == long.class || type == Long.class)
			return "LONG";
		else if (type == boolean.class || type == Boolean.class)
			return "SYMBOL";
		else if (type == float.class || type == Float.class ||
				type == double.class || type == Double.class)
			return "FLOAT";
		else if (type == String.class)
			return "STRING";
		else
			return "ANY";
	}

	public String resolveClassName(String clazz) throws ClassNotFoundException,
	JessException {
		Class<?> theClass = m_engine.findClass(clazz);
		return theClass.getName();
	}

}
