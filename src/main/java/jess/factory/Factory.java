package jess.factory;

import jess.*;

/**
 * A factory for Rete network {@link Token} objects. Jess will use these methods
 * rather than creating any <tt>Token</tt> objects directly. This allows Jess
 * extensions to add information to <tt>Token</tt> instances by providing a subclass.
 * <p>
 * (C) 2013 Sandia Corporation<br>
 * @see jess.Rete#setFactory(Factory)
 */

public interface Factory {
  Token newToken(Fact firstFact) throws JessException;
  Token newToken(Token t, Fact newFact) throws JessException;
  Token newToken(Token lt, Token rt) throws JessException;
  Token newToken(Token t) throws JessException;
}
