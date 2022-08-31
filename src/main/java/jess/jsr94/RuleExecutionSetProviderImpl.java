package jess.jsr94;

import jess.JessException;
import jess.Rete;
import org.w3c.dom.Element;

import javax.rules.admin.*;
import java.io.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

class RuleExecutionSetProviderImpl implements RuleExecutionSetProvider {
    public RuleExecutionSetProviderImpl() {
    }

    public RuleExecutionSet createRuleExecutionSet(Element elem, Map map) throws RuleExecutionSetCreateException, RemoteException {
        try {
            return new RuleExecutionSetImpl(elem, map);
        } catch (JessException e) {
            throw new RuleExecutionSetCreateException(e.getMessage(), e);
        }
    }

    public RuleExecutionSet createRuleExecutionSet(Serializable serializable, Map map) throws RuleExecutionSetCreateException, RemoteException {
        if (serializable instanceof Rete)
            return new RuleExecutionSetImpl((Rete) serializable, map);
        else
            throw new RuleExecutionSetCreateException("Argument is not a jess.Rete object");
    }

    public RuleExecutionSet createRuleExecutionSet(String name, Map map) throws RuleExecutionSetCreateException, IOException {
        try {
            URL url = new URL(name);
            Reader reader = new InputStreamReader(url.openStream());
            try {
                return new RuleExecutionSetImpl(reader, map);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuleExecutionSetCreateException(e.getMessage(), e);
        } catch (JessException e) {
            throw new RuleExecutionSetCreateException(e.getMessage(), e);
        }
    }
}
