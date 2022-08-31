package jess;

/**
 * A means of communication information about pattern matching
 * changes. Used in Jess's implementation of the "logical" conditional
 * element.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

interface MatchInfoSource {
    boolean isRelevantChange(int factIndex, Token token, Context context);
}
