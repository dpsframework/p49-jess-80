package jess.jsr94;

import javax.rules.RuleExecutionSetMetadata;

class RuleExecutionSetMetadataImpl implements RuleExecutionSetMetadata {
    private RuleExecutionSetImpl m_res;

    public RuleExecutionSetMetadataImpl(RuleExecutionSetImpl res) {
        m_res = res;
    }

    public String getUri() {
        return m_res.getURI();
    }

    public String getName() {
        return m_res.getName();
    }

    public String getDescription() {
        return m_res.getDescription();
    }
}
