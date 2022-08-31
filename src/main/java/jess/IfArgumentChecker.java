package jess;

/**
 * (C) 2013 Sandia Corporation
 */
class IfArgumentChecker implements ArgumentChecker {
    public boolean check(Funcall f, JessToken tok, ErrorSink errorSink) throws JessException {
        //if (f.size() == 2 && !"then".equals(tok.m_sval)) {
        //    errorSink.error("Jesp.parseFuncall", "Expected 'then'", ParseException.SYNTAX_ERROR, tok);
        //}

        int[] blocks = If.findBlocks(f);
        int lastBlock = blocks[blocks.length - 1];
        if (f.size() == lastBlock + 2 && !f.get(lastBlock).equals(Funcall.s_else) && !"then".equals(tok.m_sval))
            errorSink.error("Jesp.parseFuncall", "Expected 'then'", ParseException.SYNTAX_ERROR, tok);

        if (hasElse(f, blocks)) {
            if ("else".equals(tok.m_sval) || "elif".equals(tok.m_sval)) {
                errorSink.error("Jesp.parseFuncall", "No new blocks after 'else' block in 'if'", ParseException.SYNTAX_ERROR, tok);
            }
        }

        return true;
    }

    private boolean hasElse(Funcall f, int[] blocks) throws JessException {
        for (int i = 0; i < blocks.length; i++) {
            if (f.get(blocks[i]).equals(Funcall.s_else))
                return true;
        }
        return false;
    }
}
