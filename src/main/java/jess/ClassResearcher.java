package jess;

import java.lang.reflect.Method;

/**
 * <p>The ClassResearcher is used to learn about a Java class mentioned in Jess source. By abstracting
 * the notion of reflection into a separate class, we can use different reflection mechanisms in different
 * situations. For example, in the JessDE, we can use Eclipse's own machinery instead of Java reflection, so that
 * we can learn about classes without actually loading them.</p>
 *
 * (C) 2007 Sandia National Laboratories
 */
public interface ClassResearcher {

    public class Property {
        private String name;
        private String type;
        private boolean isArray;
		private Method writeMethod;
		private Method readMethod;

        public Property(String name, String type, boolean isArray,
        		Method readMethod, Method writeMethod) {
            this.name = name;
            this.type = type;
            this.isArray = isArray;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
        }
        
        public String getName() {
        	return name;
        }
        
        public String getType() {
        	return type;
        }
        
        public boolean isArray() {
        	return isArray;
        }
        
        public Method getWriteMethod() {
        	return writeMethod;
        }
        
        public Method getReadMethod() {
        	return readMethod;
        }

		void setWriteMethod(Method m) {
			writeMethod = m;
		}
		
		@Override
		public String toString() {
			return "[Property " + name + "]";
		}
    }

    /**
     * Return a list of the JavaBeans properties of a class. The definition should be the same as
     * used by the java.beans.Introspector class.
     * @param clazz the name of the class to look at
     * @return a list of Property objects describing the JavaBeans properties of the class
     * @throws ClassNotFoundException if the class can't be found
     * @throws JessException if anything else goes wrong
     */
    Property[] getBeanProperties(String clazz) throws ClassNotFoundException, JessException;

    /**
     * Return a list of the public instance fields of a class.
     * @param clazz the name of the class to look at
     * @return a list of Property objects describing the public instance fields of the class
     * @throws ClassNotFoundException if the class can't be found
     * @throws JessException if anything else goes wrong
     */
    Property[] getPublicInstanceFields(String clazz) throws ClassNotFoundException, JessException;

    /**
     * Return the fully-qualified name of a class, based on Jess's current import tables.
     * @param clazz the name of a class, either just the class part, or the fully-qualified name
     * @return the full name of the class
     * @throws ClassNotFoundException if the class can't be found
     * @throws JessException if anything else goes wrong
     */
    String resolveClassName(String clazz) throws ClassNotFoundException, JessException;
}
