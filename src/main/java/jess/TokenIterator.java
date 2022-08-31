package jess;

import java.util.Iterator;
import java.io.Serializable;

/**
 * (C) 2007 Sandia National Laboratories
 */

interface TokenIterator extends Serializable {
    boolean hasNext();
    Token next();
}
