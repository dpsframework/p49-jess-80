package jess;

class QueryArgumentChecker implements ArgumentChecker {
    public boolean check(Funcall f, JessToken tok, ErrorSink errorSink) {
        if (f.size() == 1 && tok.isLexeme())
            tok.m_sval = errorSink.getEngine().resolveName(tok.m_sval);
        return true;
    }
}
