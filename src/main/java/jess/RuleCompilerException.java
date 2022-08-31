package jess;


/**
 * An error has occurred while a rule is being compiled. The rule was
 * syntactically correct, but there was a semantic error such as a
 * variable being negated at its first use.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


public class RuleCompilerException extends JessException {

  /**
   * Constructs a RuleCompilerException containing a descriptive message.
   * @param routine the name of the routine this exception occurred in.
   * @param msg an informational message.
   */

  public RuleCompilerException(String routine, String msg) {
      super(routine, msg, "");
  }
}
