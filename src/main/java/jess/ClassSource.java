package jess;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Loads user classes and resources. Jess uses this to manage "import"
 * function calls and to cache loaded class objects.
 *
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

class ClassSource implements Serializable {
    private transient Object m_appObject;
    private transient ClassLoader m_classLoader;
    private Map m_classImports = Collections.synchronizedMap(new HashMap());
    private ArrayList m_packageImports = new ArrayList();
    private transient HashMap m_loadedClasses = new HashMap();
    private transient Rete m_engine;

    ClassSource(Object appObject, Rete engine) {
        m_appObject = appObject;
        m_engine = engine;
        importPackage("java.lang.");
    }

    /**
     * Returns the "application object" for this Rete instance
     *
     * @see Rete#Rete
     */

    Class getAppObjectClass() {
        if (m_appObject != null)
            return m_appObject.getClass();
        else
            return Rete.class;
    }

    /**
     * Associates this Rete with an object so that, for instance, the
     * (batch) commands will look for scripts using the object's
     * class loader.
     *
     * @param appObject The app object
     */
    void setAppObject(Object appObject) {
        m_appObject = appObject;
    }

    /**
     * Associates this Rete with a specific class loader; the loader
     * will be used to find batch files and load classes.
     *
     * @param loader The class loader
     */
    void setClassLoader(ClassLoader loader) {
        m_classLoader = loader;
    }

    /**
     * Loading classes and resources
     */

    Class classForName(String name) throws ClassNotFoundException {

        Class clazz = (Class) m_loadedClasses.get(name);
        if (clazz != null)
            return clazz;

        ClassLoader appLoader = getAppObjectClass().getClassLoader();
        if (appLoader != null) {
            try {
                clazz = Class.forName(name, true, appLoader);
                m_loadedClasses.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException silentlyIgnore) {
                /* Nothing */
            }
        }

        if (m_classLoader != null) {
            try {
                clazz = Class.forName(name, true, m_classLoader);
                m_loadedClasses.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException silentlyIgnore) {
                /* Nothing */
            }
        }


        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                try {
                    clazz = Class.forName(name, true, contextLoader);
                    m_loadedClasses.put(name, clazz);
                    return clazz;
                } catch (ClassNotFoundException silentlyIgnore) {
                    /* Nothing */
                }
            }
        } catch (SecurityException silentlyIgnore) {
            /* Nothing */
        }

        clazz = Class.forName(name);
        m_loadedClasses.put(name, clazz);
        return clazz;
    }

    Class findClass(String className) throws ClassNotFoundException {
        Class clazz = (Class) m_loadedClasses.get(className);
        if (clazz != null)
            return clazz;

        if (className.indexOf(".") == -1) {
            String s = (String) m_classImports.get(className);
            if (s != null)
                className = s;

            else {
                for (Iterator e = m_packageImports.iterator(); e.hasNext();) {
                    s = e.next() + className;
                    try {
                        Class c = classForName(s);
                        m_classImports.put(className, s);
                        return c;
                    } catch (ClassNotFoundException ex) {
                        /* Just try again */
                    }
                }
            }
        }
        return classForName(className);
    }

    URL getResource(String name) {

        if (m_appObject != null) {
            URL u = m_appObject.getClass().getResource(name);
            if (u != null)
                return u;
        } else if (m_classLoader != null) {
            URL u = m_classLoader.getResource(name);
            if (u != null)
                return u;
        }

        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                URL u = contextLoader.getResource(name);
                if (u != null)
                    return u;
            }
        } catch (SecurityException silentlyIgnore) {
        }

        return Rete.class.getResource(name);
    }

    void importPackage(String pack) {
        m_packageImports.add(pack);
    }

    void importClass(String clazz) throws JessException {
        m_classImports.put(clazz.substring(clazz.lastIndexOf(".") + 1,
                clazz.length()),
                clazz);
        try {
            Class aClass = findClass(clazz);
            StaticMemberImporter importer = new StaticMemberImporter(aClass);
            importer.addAllStaticFields(m_engine);
            importer.addAllStaticMethods(m_engine);
        } catch (ClassNotFoundException e) {
            throw new JessException("import", "Class not found", e);
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        m_loadedClasses = new HashMap();
    }

    void clear() {
        m_packageImports.clear();
        m_classImports.clear();
        m_loadedClasses.clear();
        importPackage("java.lang.");
    }

    ClassLoader getClassLoader() {
        return m_classLoader;
    }

    static String classNameOnly(String name) {
        if (name.indexOf('.') > -1)
            name = name.substring(name.lastIndexOf('.') + 1);
        return name;
    }

    void setEngine(Rete engine) {
        m_engine = engine;
    }
}
