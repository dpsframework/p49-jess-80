

package jess;

/**
 * Interface for all functions, user-defined or otherwise, callable
 * from the Jess language. For every Jess function, a class
 * implementing this interface is defined. You can implement this
 * interface and then call {@link Rete#addUserfunction} to add your function
 * to Jess.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public interface Userfunction {
  /**
   * Return the name of this function. Called once when the function is
   * installed into a Rete engine.
   * @return The name of this function, as seen by the Jess language
   */

  String getName();


  /**
   * Call this function with the given argument list. The arguments will be unresolved -
   * do not pass them to other functions or return them as a result without calling
   * resolveValue() on them!
   *
   * @see jess.Value#resolveValue
   * @param vv The argument list. The function name will be the 0th element.
   * @param context The execution context for resolving arguments.
   * @exception JessException If anything goes wrong.
   * @return The result of executing this function.
   */
   Value call(ValueVector vv, Context context) throws JessException;

}
