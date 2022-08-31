package jess.jsr94;

import javax.rules.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

class RuleRuntimeImpl implements RuleRuntime {
    private RuleAdministratorImpl m_administrator;

    public RuleRuntimeImpl(RuleAdministratorImpl administrator) {
        m_administrator = administrator;
    }

    public RuleSession createRuleSession(String uri, Map map, int type) throws RuleSessionTypeUnsupportedException, RuleSessionCreateException, RuleExecutionSetNotFoundException, RemoteException {
        RuleExecutionSetImpl res = m_administrator.getRuleExecutionSet(uri);
        if (res == null)
            throw new RuleExecutionSetNotFoundException(uri);
        switch (type) {
            case STATEFUL_SESSION_TYPE:
                return new StatefulRuleSessionImpl(res);
            case STATELESS_SESSION_TYPE:
                return new StatelessRuleSessionImpl(res);
            default:
                throw new RuleSessionTypeUnsupportedException(String.valueOf(type));
        }
    }

    public List getRegistrations() throws RemoteException {
        return m_administrator.getRegistrationList();
    }
}
