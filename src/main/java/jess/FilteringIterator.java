package jess;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>A wrapper around an Iterator that applies a jess.Filter to each element, rejecting the ones that don't
 * pass the filter.</p>
 * (C) 2013 Sandia Corporation
 */
public class FilteringIterator implements Iterator {
    private Iterator m_iterator;
    private Filter m_filter;
    private Object m_next;

    public FilteringIterator(Iterator iterator, Filter filter) {
        m_iterator = iterator;
        m_filter = filter;
        findNextElement();
    }

    private void findNextElement() {
        m_next = null;
        while (m_iterator.hasNext()) {
            Object next = m_iterator.next();
            if (m_filter.accept(next)) {
                m_next = next;
                break;
            }
        }
    }

    public void remove() {
        m_iterator.remove();
    }

    public boolean hasNext() {
        return m_next != null;
    }

    public Object next() {
        if (m_next == null)
            throw new NoSuchElementException();
        Object result = m_next;
        findNextElement();
        return result;
    }
}
