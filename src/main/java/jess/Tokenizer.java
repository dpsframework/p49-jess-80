package jess;

/**
 * The Jess parser reads its input from a Tokenizer. In general, the
 * supplied ReaderTokenizer implementation is sufficient for most needs.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 * @see ReaderTokenizer
 */

public interface Tokenizer {
    String BLANK_PREFIX = "_blank_";
    String BLANK_MULTI = "_blank_mf";


    /**
     * Specify whether newlines should be reported as tokens
     * @param b true if newlines should be reported
     */
    void reportNewlines(boolean b);

    /**
     * Return characters from the current location to the end of the current line, as a String
     * @return the rest of the current line
     * @throws JessException if anything goes wrong
     */
    String readLine() throws JessException;

    /**
     * Return the next parsed token, skipping over all whitespace.
     * @return the token
     * @throws JessException if anything goes wrong
     */

    JessToken nextToken() throws JessException;

    /**
     * Discard characters from the current stream position to the end of the current line.
     * @return the parsed characters
     * @throws JessException if anything goes wrong
     */
    String discardToEOL() throws JessException;

    /**
     * Return the current character offset. The meaning depends on the underlying data source.
     * @return the character position
     */
    int getStreamPos();

    /**
     * Consume all characters up to, but not including, the next non-whitespace character
     * @throws JessException
     */
    void eatWhitespace() throws JessException;
}
