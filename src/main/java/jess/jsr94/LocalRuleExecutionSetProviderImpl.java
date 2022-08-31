package jess.jsr94;

import jess.JessException;
import jess.Rete;

import javax.rules.admin.*;
import java.io.*;
import java.util.Map;

class LocalRuleExecutionSetProviderImpl implements LocalRuleExecutionSetProvider {
    public LocalRuleExecutionSetProviderImpl(Map map) {
    }

    public RuleExecutionSet createRuleExecutionSet(InputStream inputStream, Map map) throws RuleExecutionSetCreateException, IOException {
        return createRuleExecutionSet(new InputStreamReader(inputStream), map);
    }

    public RuleExecutionSet createRuleExecutionSet(Reader reader, Map map) throws RuleExecutionSetCreateException, IOException {
        try {
            return new RuleExecutionSetImpl(reader, map);
        } catch (JessException e) {
            throw new RuleExecutionSetCreateException("Failure in parsing rule execution set", e);
        }
    }

    public RuleExecutionSet createRuleExecutionSet(Object o, Map map) throws RuleExecutionSetCreateException {
        if (o instanceof Rete)
            return new RuleExecutionSetImpl((Rete) o, map);
        else
            throw new RuleExecutionSetCreateException("Argument is not a jess.Rete object");
    }
}
