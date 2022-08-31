package jess;

/**
 * A generic Rete network test. Different implementations of this
 * represent pattern-network tests, join-network tests, etc.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public interface TestBase
{
  /** Used by TestBase constructors to indicate this test is for equality */
  public int EQ  = 0;
  /** Used by TestBase constructors to indicate this test is for inequality */
  public int NEQ = 1;

  /**
   * Perform the actual test. The context argument contains all relevant information
   * needed to resolve variables, etc.
   * @param context The execution context in which to evaluate the test
   * @exception JessException  If anything goes wrong
   * @return The result of the test
   */

  boolean doTest(Context context) throws JessException;

}





