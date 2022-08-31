package jess;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * A Comparator that can be used to sort an array of
 * java.lang.reflect.Method objects. Can't be genericized without making it considerably harder to use, 
 * as we search for a String in a List of Methods; note conversions below
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class MethodNameComparator {
    private static Comparator<Method> s_sortInstance = new SortComparator();

    public static Comparator<Method> getSortInstance() {
        return s_sortInstance;
    }
    
    private static Comparator s_searchInstance = new SearchComparator();

    public static Comparator getSearchInstance() {
        return s_searchInstance;
    }

    // As a special case, methods with "Object" arguments are less favored than methods without.
    // Since we'll eventually search linearly through the list of methods, sorting them towards the end
    // makes it less likely they'll be called.
    static class SortComparator implements Comparator<Method> {
    	public int compare(Method o1, Method o2) {    		
    		int nameCompare = o1.getName().compareTo(o2.getName());
    		if (nameCompare != 0)
    			return nameCompare;
    		Class<?>[] parameterTypes1 = o1.getParameterTypes();    		
    		Class<?>[] parameterTypes2 = o2.getParameterTypes();
    		int countCompare = parameterTypes1.length - parameterTypes2.length;
    		if (countCompare != 0)
    			return countCompare;
    	
    		for (int i=0; i<parameterTypes1.length; ++i) {
    			if (parameterTypes1[i] == Object.class && parameterTypes2[i] != Object.class)
    				return 1;
    			else if (parameterTypes2[i] == Object.class && parameterTypes1[i] != Object.class)
    				return -1;	
    		}    		
    		return 0;
    	}
    }
    
    static class SearchComparator implements Comparator {
    	public int compare(Object o1, Object o2) {
    		if (o1 instanceof Method)
    			o1 = ((Method)o1).getName();
    		if (o2 instanceof Method)
    			o2 = ((Method)o2).getName();
    		
    		return ((String) o1).compareTo((String) o2);
    	}
    }
}

