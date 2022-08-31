package jess;

/**
 * <p>An Accelerator generates Java versions of rule LHSs, compiles them
 * and returns new TestBase objects to execute them. Jess does not ship with any
 * Accelerator implementations at this time but will in the near future.
 * </P>
 * (C) 2007 Sandia National Laboratories<br>
 */


public interface Accelerator {

  /**
   * Given the function call, return a TestBase object that performs equivalently to the Funcall,
   * or null if this Accelerator can't translate the function.
   * @param f A function call to translate
   * @return A jess.TestBase object
   * @exception JessException if the translation fails unexpectedly.
   * @param engine the active Rete instance
   */

  TestBase speedup(Funcall f, Rete engine) throws JessException;

}











