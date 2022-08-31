package jess.jsr94;

import javax.rules.*;
import java.rmi.RemoteException;

abstract class RuleSessionImpl implements RuleSession {

    protected RuleExecutionSetImpl m_res;

    public RuleSessionImpl(RuleExecutionSetImpl res) throws RuleSessionCreateException {
        try {
            m_res = (RuleExecutionSetImpl) res.clone();
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof Exception)
                throw new RuleSessionCreateException("Problem creating rule session", (Exception) ex.getCause());                
        }
    }

    public void release() throws RemoteException, InvalidRuleSessionException {
        m_res.release();
        m_res = null;
    }

    public RuleExecutionSetMetadata getRuleExecutionSetMetadata() throws InvalidRuleSessionException, RemoteException {
        return new RuleExecutionSetMetadataImpl(m_res);
    }
}
