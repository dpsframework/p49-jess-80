package jess;

import java.util.HashMap;

/** 
 * An IntrinsicPackage is a collection of built-in functions.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

interface IntrinsicPackage {

    /**
     * Add this package of functions to the given Map by name.
     */
    void add(HashMap ht);
}
