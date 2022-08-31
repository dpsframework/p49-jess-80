package jess;

import java.io.Serializable;
import java.util.*;

class NodeLogicalDependencyHandler implements Serializable {
    private final HashMap m_logicalDepends = new HashMap();
    private MatchInfoSource m_matchInfoSource;
    private final int m_tokenSize;

    public NodeLogicalDependencyHandler(int tokenSize) {
        m_tokenSize = tokenSize;
    }

    public void setMatchInfoSource(MatchInfoSource matchInfoSource) {
        m_matchInfoSource = matchInfoSource;
    }

    void removeLogicalSupportFrom(Token token, Context context) {
        ArrayList list = (ArrayList) m_logicalDepends.remove(token);
        if (list != null) {
            Rete engine = context.getEngine();
            for (int i = 0; i < list.size(); ++i) {
                Fact f = (Fact) list.get(i);
                engine.removeLogicalSupportFrom(token, f.getIcon());
            }
        }
    }

    void dependsOn(Fact f, Token t) {

        ArrayList list = (ArrayList) m_logicalDepends.get(t);
        if (list == null) {
            list = new ArrayList();
            m_logicalDepends.put(t, list);
        }

        synchronized (list) {
            list.add(f.getIcon());
        }
    }

    Map getMap() {
        return m_logicalDepends;
    }

    void clear() {
        m_logicalDepends.clear();
    }

    public void tokenMatched(int tag, Token t, Context context) {
        switch (tag) {
            case RU.CLEAR:
                m_logicalDepends.clear();
                break;
            case RU.UPDATE:
                break;
            case RU.MODIFY_REMOVE:
            case RU.MODIFY_ADD:

                if (m_matchInfoSource.isRelevantChange(m_tokenSize, t, context))
                    removeLogicalSupportFrom(t, context);
                break;
            default:
                // If we've seen this token before, then its state has changed, and any
                // support has become void. This therefore handles both normal and negated
                // logical support.
                removeLogicalSupportFrom(t, context);
                break;
        }
    }
}
