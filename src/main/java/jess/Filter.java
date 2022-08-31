package jess;

/**
 * <p>An interface representing a generic boolean single-argument operation.
 * A Filter can be passed to the {@link Rete#getObjects(Filter)} method, where it is used
 * to choose relevant objects from working memory.</p>
 * (C) 2013 Sandia Corporation<BR>
 */
public interface Filter {
    /**
     * Returns true if the given object should be included in the filtered set.
     * @param o the object to test
     * @return true if the object should be included
     */
    boolean accept(Object o);

    /**
     * A Filter implementation that passes objects that are instances
     * of a given class.
     */
    public static class ByClass implements Filter {
        private Class m_clazz;

        /**
         * Constructor. The given class object's "isInstance()" method
         * is used to filter objects.
         * @param clazz the class to filter by
         */
        public ByClass(Class clazz) {
            m_clazz = clazz;
        }

        /**
         * Returns true if the given object is an instance of the
         * Class provided to the constructor.
         * @param o the object to test
         * @return true if the object is an instance of this filter's class
         */
        public boolean accept(Object o) {
            return m_clazz.isInstance(o);
        }
    }

    /**
     * A Filter implementation that passes Jess objects defined in a
     * given module.
     */
    public static class ByModule implements Filter {
        private String m_module;

        /**
         * Constructor. The given module name will be used to filter objects
         * @param module the module name to filter by
         */
        public ByModule(String module) {
            m_module = module;
        }

        /**
         * Returns true if the given object implements the jess.Modular
         * interface and is defined in the filter's named module
         * @param o the object to test
         * @return true if the object is a 'Modular' in this filter's module
         * @see jess.Modular
         */
        public boolean accept(Object o) {
            if (! (o instanceof Modular))
                return false;
            return ((Modular) o).getModule().equals(m_module);
        }
    }
}
