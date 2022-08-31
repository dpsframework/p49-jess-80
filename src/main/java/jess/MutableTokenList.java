package jess;

/**
 * A list of Tokens that can be changed.
 * <P>
 * (C) 2013 Sandia Corporation<BR>
 */
interface MutableTokenList extends TokenList {

    /**
     * Removes all tokens from this list.
     */
    void clear();

    /**
     * Add the given token to the end of the list.
     * @param val the token to add
     */
    void add(Token val);

    /**
     * Remove the token at the given index from the list.
     * @param i the index of a token to remove
     * @return the token that was removed
     */
    Token remove(int i);
}
