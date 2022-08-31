package jess;

/** 
 * Interface for a collection of functions, user-defined or
 * otherwise. This is just a convenient way to collect a group of
 * functions together -- its use is optional.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public interface Userpackage
{

  /**
   * Add this package of functions to the given engine by calling
   * {@link jess.Rete#addUserfunction(Userfunction)} some number of times.
   * @see jess.Rete#addUserfunction
   * @see jess.Rete#addUserpackage
   * @param engine */
  void add(Rete engine);
}
