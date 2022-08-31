package jess;

import java.util.Arrays;

/**
 * A MutableTokenList backed by an array. The default implementation of MutableTokenList used in
 * Rete join memories.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

class ArrayTokenList implements MutableTokenList, java.io.Serializable {
    private Token m_v[];
    private int m_ptr = 0;

    ArrayTokenList() {
    }

    public final int size() {
        return m_ptr;
    }

    public final Token get(int i) {
        return m_v[i];
    }

    public final void clear() {
        if (m_ptr > 0) {
            Arrays.fill(m_v, 0, m_ptr, null);
            m_ptr = 0;
        }
    }

    public final void add(Token val) {
        if (m_v == null) {
            m_v = new Token[3];
        } else if (m_ptr >= m_v.length) {
            Token[] nv = new Token[m_v.length * 2];
            System.arraycopy(m_v, 0, nv, 0, m_v.length);
            m_v = nv;
        }
        m_v[m_ptr++] = val;
    }

    public final Token remove(int i) {
        Token t = m_v[i];
        m_v[i] = m_v[m_ptr - 1];
        m_v[--m_ptr] = null;
        return t;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < m_ptr; ++i) {
            sb.append(m_v[i]);
            sb.append(" ");
        }
        return sb.toString();
    }

}

