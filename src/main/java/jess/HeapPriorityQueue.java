package jess;

import java.io.*;
import java.util.*;

/**
 * A heap-based priority queue used to implement the agenda.
 * <p/>
 * See Sedgewick's "Algorithms in C++", Third Ed., page 386.  We don't
 * use element 0 in the array because that makes the implementation
 * cleaner.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

class HeapPriorityQueue implements Serializable {
    private Activation [] m_queue;
    private Strategy m_strategy;
    private int m_size;

    public HeapPriorityQueue(Strategy s) {
        m_queue = new Activation[10];
        m_strategy = s;
    }

    private HeapPriorityQueue(Rete engine, HeapPriorityQueue hpq) throws JessException {
        m_queue = (Activation[]) hpq.m_queue.clone();
        for (int i = 1; i <= hpq.m_size; i++) {
            m_queue[i] = new Activation(engine, m_queue[i]);
        }
        m_size = hpq.m_size;
        m_strategy = hpq.m_strategy;
    }

    public Strategy setStrategy(Strategy s) {
        Strategy temp = m_strategy;
        m_strategy = s;

        Activation[] old = m_queue;
        m_queue = new Activation[old.length];
        m_size = 0;
        for (int i=0; i<old.length; ++i) {
            if (old[i] != null)
                push(old[i]);
        }                
        return temp;
    }

    public Strategy getStrategy() {
        return m_strategy;
    }

    public boolean isEmpty() {
        return m_size == 0;
    }

    public synchronized void remove(Activation c) {
        if (m_size > 0) {
            Activation[] queue = m_queue;
            int i = c.getIndex();
            c.setIndex(-1);
            queue[i] = queue[m_size];
            queue[i].setIndex(i);
            queue[m_size] = null;
            --m_size;
            if (i <= m_size) {
                if (i > 1 && m_strategy.compare(queue[i], queue[i/2]) < 0) {
                    fixUp(i);
                } else {
                    fixDown(i);
                }
            }
            // checkHeap();
        }
    }

    public synchronized void push(Activation c) {
        if (m_size == m_queue.length - 1) {
            Activation[] temp = new Activation[m_size * 2];
            System.arraycopy(m_queue, 1, temp, 1, m_size);
            m_queue = temp;
        }
        ++m_size;
        m_queue[m_size] = c;
        c.setIndex(m_size);
        fixUp(m_size);
        notify();
        // checkHeap();
    }

    public synchronized Activation pop() {
        if (isEmpty())
            return null;
        Activation c = m_queue[1];
        remove(c);
        return c;
    }

    public synchronized Activation peek() {
        if (isEmpty()) {
            return null;
        } else
            return m_queue[1];
    }

    public synchronized void clear() {
        m_queue = new Activation[10];
        m_size = 0;
    }

    public Iterator iterator(Rete engine) throws JessException {
        synchronized(this) {
            final HeapPriorityQueue hpq = new HeapPriorityQueue(engine, this);
            return new Iterator() {
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public boolean hasNext() {
                    return !hpq.isEmpty();
                }

                public Object next() {
                    return hpq.pop();
                }
            };
        }
    }

    // Push an element down to reform the heap.
    private void fixDown(int k) {
        Activation[] queue = m_queue;
        Strategy strategy = m_strategy;
        int size = m_size;
        while (2 * k <= size) {
            int j = 2 * k;
            if (j < size && strategy.compare(queue[j], queue[j + 1]) > 0)
                j++;
            if (! (strategy.compare(queue[k], queue[j]) > 0))
                break;
            exch(queue, k, j);
            k = j;
        }
    }

    // Push an element up to reform the heap.
    private void fixUp(int k) {
        Activation[] queue = m_queue;
        Strategy strategy = m_strategy;
        while (k > 1 && strategy.compare(queue[k / 2], queue[k]) > 0) {
            int j = k / 2;
            exch(queue, k, j);
            k = j;
        }
    }

    private void exch(Activation[] queue, int i, int j) {
        Activation c = queue[i];
        queue[i] = queue[j];
        queue[j] = c;
        queue[i].setIndex(i);
        queue[j].setIndex(j);
    }

    public int size() {
        return m_size;
    }

    void checkHeap() {
        for (int i=1; i<=m_size; ++i) {
            if (m_queue[i].getIndex() != i)
                new RuntimeException("checkHeap: wrong index at " + i + ": " + m_queue[i]).printStackTrace();
            else if (i > 1 && m_strategy.compare(m_queue[i / 2], m_queue[i]) > 0) {
                new RuntimeException("checkHeap: bad heap at " + i + ": " + m_queue[i] + "/" + m_queue[i/2]).printStackTrace();
                dumpHeap();
            }
        }
    }

    void dumpHeap() {
        for (int j = 1; j <= m_size; j++) {
            System.out.println(m_queue[j]);
        }
    }

    synchronized void removeActivationsOfRule(Defrule rule) {
        Activation[] copy = (Activation[]) m_queue.clone();
        int originalSize = m_size;
        for (int i = 1; i <= originalSize; i++) {
            Activation activation = copy[i];
            if (activation.getRule() == rule) {
                remove(activation);
            }
        }
    }
}

