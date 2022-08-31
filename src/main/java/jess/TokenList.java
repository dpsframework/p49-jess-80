package jess;

/**
 * An iterable, nonmutable list of jess.Token objects, used to store lists of Tokens in Jess's join
 * node Rete memories.
 * <P>
 * (C) 2013 Sandia Corporation<BR>
  */
interface TokenList {

    int size();

    Token get(int i);
}
