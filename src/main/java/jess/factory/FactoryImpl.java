package jess.factory;

import jess.*;

import java.io.Serializable;


/**
 * The default token factory implementation. This class just forwards
 * each method to the equivalent Token constructor.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 * @see jess.Rete#setFactory(Factory)
 */

public class FactoryImpl implements Factory, Serializable {

    
    public Token newToken(Fact firstFact) throws JessException {
        return new Token(firstFact);
    }

   
    public Token newToken(Token token, Fact newFact) throws JessException {
        return new Token(token, newFact);
    }

    
    public Token newToken(Token lt, Token rt) throws JessException {
        return new Token(lt, rt);
    }

    
    public Token newToken(Token token) throws JessException {
        return new Token(token);
    }
}
