package jess;

import java.io.Serializable;
import java.util.Stack;

/**
 * A smart lexer for the Jess language.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class JessTokenStream implements Serializable {
    private final Stack m_stack;
    private final Tokenizer m_tokenizer;
    private int m_lineno;
    private final StringBuffer m_string = new StringBuffer();
    private JessToken m_lastToken;

    /**
     * Construct a JessTokenStream.
     * Tell the tokenizer how to separate Jess tokens
     * @param tokenizer
     */
    public JessTokenStream(Tokenizer tokenizer) {
        m_tokenizer = tokenizer;
        m_stack = new Stack();
    }

    public JessToken getLastToken() {
        return m_lastToken;
    }


    public int getLineNumber() {
        return m_lineno;
    }

    public JessToken nextToken() throws JessException {
        JessToken tok;
        if (m_stack.empty())
            tok = m_tokenizer.nextToken();
        else
            tok = (JessToken) m_stack.pop();

        switch (tok.m_ttype) {
        case JessToken.NONE_TOK:
        case JessToken.COMMENT_TOK:
        case JessToken.MULTILINE_COMMENT_TOK:
            break;
        default:
            // TODO m_string should contain exact actual input 
            m_string.append(tok.toString());
            m_string.append(" ");
        }
        m_lineno = tok.m_lineno;
        return m_lastToken = tok;
    }


    public void pushBack(JessToken tok) {
        switch (tok.m_ttype) {
        case JessToken.NONE_TOK:
        case JessToken.COMMENT_TOK:
        case JessToken.MULTILINE_COMMENT_TOK:
            return;
        default:
            m_lineno = tok.m_lineno;
            m_stack.push(tok);
            m_string.setLength(Math.max(0, m_string.length() - (tok.toString().length() + 1)));
        }
    }

    void clear() {
        m_string.setLength(0);
    }

    void clearStack() {
        clear();
        m_stack.clear();
    }

    public String toString() {
        return m_string.toString();
    }

    public int getStreamPos() {
        return m_tokenizer.getStreamPos();
    }

    void eatWhitespace() throws JessException {
        m_tokenizer.eatWhitespace();
    }

    public JessToken nextNonCommentToken() throws JessException {
        JessToken token = nextToken();
        while (token.isComment())
            token = nextToken();
        return token;
    }
}












