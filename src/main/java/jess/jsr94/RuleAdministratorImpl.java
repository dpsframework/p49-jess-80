package jess.jsr94;

import javax.rules.admin.*;
import java.rmi.RemoteException;
import java.util.*;

class RuleAdministratorImpl implements RuleAdministrator {
    private final Map m_registrations = Collections.synchronizedMap(new HashMap());

    public RuleExecutionSetProvider getRuleExecutionSetProvider(Map map) throws RemoteException {
        return new RuleExecutionSetProviderImpl();
    }

    public LocalRuleExecutionSetProvider getLocalRuleExecutionSetProvider(Map map) throws RemoteException {
        return new LocalRuleExecutionSetProviderImpl(map);
    }

    public void registerRuleExecutionSet(String name, RuleExecutionSet ruleExecutionSet, Map map) throws RuleExecutionSetRegisterException, RemoteException {
        m_registrations.put(name, ruleExecutionSet);
        ((RuleExecutionSetImpl) ruleExecutionSet).setURI(name);

    }

    public void deregisterRuleExecutionSet(String name, Map map) throws RuleExecutionSetDeregistrationException, RemoteException {
        RuleExecutionSetImpl res = getRuleExecutionSet(name);
        res.setURI(null);
        res.release();
        m_registrations.remove(name);
    }

    public List getRegistrationList() {
        synchronized(m_registrations) {return new ArrayList(m_registrations.keySet());}
    }

    RuleExecutionSetImpl getRuleExecutionSet(String uri) {
        RuleExecutionSetImpl res = (RuleExecutionSetImpl) m_registrations.get(uri);
        res.setURI(uri);
        return res;
    }
}
