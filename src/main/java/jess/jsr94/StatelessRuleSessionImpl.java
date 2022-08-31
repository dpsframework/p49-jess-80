package jess.jsr94;

import javax.rules.*;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

class StatelessRuleSessionImpl extends RuleSessionImpl implements StatelessRuleSession {
    StatelessRuleSessionImpl(RuleExecutionSetImpl res) throws RuleSessionCreateException {
        super(res);
    }

    public List executeRules(List list) throws InvalidRuleSessionException, RemoteException {
        synchronized(m_res) {
            m_res.reset();

            for (Iterator it = list.iterator(); it.hasNext();)
                m_res.addObject(it.next());
            m_res.run();
            return m_res.getObjects();
        }
    }

    public List executeRules(List list, ObjectFilter objectFilter) throws InvalidRuleSessionException, RemoteException {
        synchronized(m_res) {
            m_res.reset();
            for (Iterator it = list.iterator(); it.hasNext();)
                m_res.addObject(it.next());
            m_res.run();
            return m_res.getObjects(objectFilter);
        }
    }

    public int getType() throws RemoteException, InvalidRuleSessionException {
        return RuleRuntime.STATELESS_SESSION_TYPE;
    }
}
