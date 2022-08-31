package jess;

import java.io.Serializable;


/**
 * A conditional element is either a Pattern or a Group of
 * Patterns. It's a way to treat the elements of a Defrule's left hand
 * side generically.
 *
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

public interface ConditionalElement extends Serializable {

    String getName();

    String getBoundName();

    int getGroupSize();

    boolean isGroup();

    ConditionalElement getConditionalElement(int i);

}
