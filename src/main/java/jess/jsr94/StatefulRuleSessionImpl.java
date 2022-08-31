package jess.jsr94;

import javax.rules.*;
import java.rmi.RemoteException;
import java.util.*;

class StatefulRuleSessionImpl extends RuleSessionImpl implements StatefulRuleSession {

    StatefulRuleSessionImpl(RuleExecutionSetImpl res) throws RuleSessionCreateException {
        super(res);
    }

    public boolean containsObject(Handle handle) throws RemoteException, InvalidRuleSessionException, InvalidHandleException {
        return m_res.containsObject(handle);
    }

    public Handle addObject(Object o) throws RemoteException, InvalidRuleSessionException {
        return m_res.addObject(o);
    }

    public List addObjects(List list) throws RemoteException, InvalidRuleSessionException {
        List handles = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Object o = it.next();
            handles.add(addObject(o));
        }
        return handles;
    }

    /*
     * Removes the object contained in the handle, then adds a new object.
     */
    public void updateObject(Handle handle, Object o) throws RemoteException, InvalidRuleSessionException, InvalidHandleException {
        m_res.removeObject(handle);
        m_res.addObject(o);
        ((HandleImpl) handle).setObject(o);
    }

    public void removeObject(Handle handle) throws RemoteException, InvalidHandleException, InvalidRuleSessionException {
        m_res.removeObject(handle);
    }

    public List getObjects() throws RemoteException, InvalidRuleSessionException {
        return m_res.getObjects();
    }

    public List getHandles() throws RemoteException, InvalidRuleSessionException {
        List handles = new ArrayList();
        List objects = getObjects();
        for (Iterator it = objects.iterator(); it.hasNext();) {
            handles.add(addObject(it.next()));
        }
        return handles;
    }

    public List getObjects(ObjectFilter objectFilter) throws RemoteException, InvalidRuleSessionException {
        return m_res.getObjects(objectFilter);
    }

    public void executeRules() throws RemoteException, InvalidRuleSessionException {
        m_res.run();
    }

    public void reset() throws RemoteException, InvalidRuleSessionException {
        m_res.reset();
    }

    public Object getObject(Handle handle) throws RemoteException, InvalidHandleException, InvalidRuleSessionException {
        return ((HandleImpl) handle).getObject();
    }

    public int getType() throws RemoteException, InvalidRuleSessionException {
        return RuleRuntime.STATEFUL_SESSION_TYPE;
    }
}
