package jess;

import java.io.Serializable;

/**
 * A sort of Hash table of Tokens kept by sortcode
 *
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class TokenTree implements Serializable {
    private int m_hash;
    private MutableTokenList[] m_tokens;
    private boolean m_useSortcode;
    private int m_fact, m_slot, m_subSlot;
    private int m_size;
    private static final double THRESHOLD = 1;

    TokenTree(int hash, boolean useSortCode, int tokenIdx, int factIdx, int subIdx) {
        m_hash = hash;
        m_useSortcode = useSortCode;
        m_slot = factIdx;
        m_subSlot = subIdx;
        m_fact = tokenIdx;
        m_tokens = new MutableTokenList[m_hash];
    }

    int loadFactor() {
        return m_size / m_hash;
    }

    final void clear() {
        for (int i=0; i< m_hash; i++)
            if (m_tokens[i] != null)
                m_tokens[i].clear();
        m_size = 0;
    }

    synchronized boolean add(Token t, boolean update) throws JessException {
        boolean result = doAdd(t, update);
        if (loadFactor() > THRESHOLD)
            rehash();
        return result;
    }

    synchronized Token remove(Token t) throws JessException {

        int code = codeForToken(t);

        MutableTokenList v = findCodeInTree(code, false);

        if (v == null)
            return null;

        int size = v.size();

        for (int i=0; i< size; i++) {
            Token tt = v.get(i);
            if (t.fastDataEquals(tt)) {
                // Might be a different multislot permutation
                if (t.dataEquals(tt)) {
                    v.remove(i);
                    --m_size;
                    return tt;
                }
            }
        }
        return null;
    }


    synchronized TokenList findListForToken(Token token, boolean create) throws JessException {
        int code = codeForToken(token);
        return findCodeInTree(code, create);
    }

    TokenList getTestableTokens(Value key) throws JessException {
        int code = key.hashCode();
        code = conditionHash(code);
        return findCodeInTree(code, false);
    }

    Value extractKey(Token token) throws JessException {
        Value value;

        if (m_subSlot == -1)
            value = token.fact(m_fact).get(m_slot);
        else
            value = token.fact(m_fact).get(m_slot).
                    listValue(null).get(m_subSlot);

        return value;
    }

    private Token subsetToken(Token t) {
        Token parent = t;
        while (parent.size() > m_fact)
            parent = parent.getParent();
        return parent;
    }

    private void rehash() throws JessException {
        //System.out.print(".");
        TokenList[] vecs = m_tokens;
        m_hash = (int) (m_hash * 1.7 + 1);
        m_tokens = new MutableTokenList[m_hash];
        m_size = 0;
        for (int i=0; i<vecs.length; ++i) {
            TokenList vec = vecs[i];
            if (vec == null)
                continue;
            int count = vec.size();
            for (int j=0; j<count; ++j) {
                doAdd(vec.get(j), false);
            }
        }
    }

    private synchronized MutableTokenList findCodeInTree(int code, boolean create) {
        if (create && m_tokens[code] == null)
            return m_tokens[code] = new ArrayTokenList();
        else
            return m_tokens[code];
    }

    private int conditionHash(int code) {
        code += code >> 9;
        /*code ^=  (code >>> 14);
        code +=  (code << 4);
        code ^=  (code >>> 10); */
        if (code < 0)
            code = -code;
        code %= m_hash;
        return code;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<m_hash; ++i) {
            if (m_tokens[i] != null) {
                sb.append(i);
                sb.append(": ");
                sb.append(m_tokens[i]);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    void dumpMemory(StringBuffer sb) {
        for (int i = 0; i < m_hash; i++) {
            TokenList tv = m_tokens[i];
            if (tv == null)
                continue;
            for (int j = 0; j < tv.size(); j++) {
                sb.append(tv.get(j));
                sb.append("\n");
            }
        }
    }

    int getHash() {
        return m_hash;
    }

    TokenList getTokenList(int i) {
        return m_tokens[i];
    }

    // EJFH TODO Fold this into "dumpMemory".
    String getIndexingInfo() throws JessException {
        if (m_useSortcode)
            return "token sort code.";

        Token token = findAnyToken();
        String result;
        if (token == null) {
            result = "slot " + m_slot + " of fact " + m_fact + " in each token.";

        } else {
            Fact fact = token.fact(m_fact);
            Deftemplate template = fact.getDeftemplate();
            result = "the \"" + template.getSlotName(m_slot) + "\" slot of a \"" + template.getName() + "\" fact.";
        }

        if (m_subSlot != -1)
            result =  "subslot " + m_subSlot + " of " + result;

        return result;
    }

    private synchronized boolean doAdd(Token t, boolean update) throws JessException {
        int code = codeForToken(t);
        MutableTokenList v = findCodeInTree(code, true);
        if (update) {
            int size = v.size();
            for (int i=0; i< size; i++) {
                Token tt = v.get(i);
                if (t.dataEquals(tt)) {
                    return false;
                }
            }
        }
        v.add(t);
        ++m_size;
        return true;
    }

    private Token findAnyToken() {
        for (int i=0; i<m_hash; ++i) {
            TokenList v = m_tokens[i];
            if (v != null && v.size() > 0)
                return v.get(0);
        }
        return null;
    }

    private int codeForToken(Token t) throws JessException {
        int code;

        if (m_useSortcode) {
            if (m_fact == 0)
                code = t.m_sortcode;
            else
                code = subsetToken(t).m_sortcode;

        } else if (m_slot == -1)
            code = t.fact(m_fact).getFactId();

        else if (m_subSlot == -1) {

            Fact fact = t.fact(m_fact);
            code = fact.m_v[m_slot].hashCode();
        }

        else
            code = t.fact(m_fact).
                    m_v[m_slot].listValue(null).m_v[m_subSlot].hashCode();

        return conditionHash(code);
    }

    static class Spy {
        boolean m_useSortcode;
        int m_fact, m_slot, m_subSlot, m_size;

        Spy(TokenTree t) {
            m_useSortcode = t.m_useSortcode;
            m_fact = t.m_fact;
            m_slot = t.m_slot;
            m_subSlot = t.m_subSlot;
            m_size = t.m_size;
        }
    }


}



