package jess;

import java.io.Serializable;
import java.util.*;


class LogicalSupport implements Serializable {
    private Multimap m_supportForFacts;
    private List m_toRetract;

    LogicalSupport(List toRetract) {
        m_toRetract = toRetract;
    }

    private boolean logicalSupportInUse() {
        return m_supportForFacts != null;
    }

    private void addLogicalSupportFor(Token token, Fact fact, boolean alreadyExisted) {
        initialize();
        synchronized (m_supportForFacts) {
            Object support = m_supportForFacts.get(fact.getIcon());
            if (support == null && alreadyExisted) {
                // This fact already existed, with unconditional support
                return;
            }
            m_supportForFacts.put(fact.getIcon(), token);
        }
    }

    private void initialize() {
        if (!logicalSupportInUse())
            m_supportForFacts = new Multimap();
    }

    /*
     * Removing all explicit support makes a fact unconditionally supported
     * TODO What about records in the LogicalNodes?
     */
    void addUnconditionalSupportFor(Fact fact) {
        if (!logicalSupportInUse())
            return;

        synchronized (m_supportForFacts) {
            m_supportForFacts.remove(fact.getIcon());
        }
    }

    /**
     * If all the support for a fact disappears, remove it.
     */

    void removeLogicalSupportFrom(Token token, Fact fact) {
        if (!logicalSupportInUse())
            return;
        Fact realFact = fact.getIcon();
        Object support = m_supportForFacts.get(realFact);
        if (support == null)
            return;
        synchronized (m_supportForFacts) {
            if (m_supportForFacts.remove(realFact, token))
                if (m_supportForFacts.get(realFact) == null)
                    m_toRetract.add(realFact);
        }
    }

    void removeAllLogicalSupportFor(Fact f) {
        if (logicalSupportInUse())
            m_supportForFacts.remove(f.getIcon());
    }

    void factAsserted(Context context, Fact f, boolean alreadyExisted) {
        Fact realFact = f.getIcon();
        if (context.getLogicalSupportNode() != null) {
            Token token = context.getToken();
            context.getLogicalSupportNode().dependsOn(realFact, context.getToken(), context.getEngine());
            addLogicalSupportFor(token, realFact, alreadyExisted);
        } else {
            addUnconditionalSupportFor(realFact);
        }
    }

    List getSupportingTokens(Fact fact) {
        if (logicalSupportInUse()) {
            Object support = m_supportForFacts.get(fact.getIcon());
            return (List) support;
        } else
            return null;
    }

    // TODO This should be a fast operation, not a slow one!
    List getSupportedFacts(Fact supporter) {
        ArrayList list = new ArrayList();
        if (logicalSupportInUse()) {
            synchronized (m_supportForFacts) {
                for (Iterator it = m_supportForFacts.entrySet().iterator(); it.hasNext();) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Object support = entry.getValue();
                    if (support == null)
                        //noinspection UnnecessaryContinue
                        continue;
                    else if (support instanceof Token) {
                        for (Token t = (Token) support; t != null; t = t.getParent()) {
                            if (t.topFact().getFactId() == supporter.getFactId()) {
                                list.add(entry.getKey());
                                break;
                            }
                        }
                    } else {
                        ArrayList tokens = (ArrayList) entry.getValue();

                        for (int i = 0; i < tokens.size(); ++i) {
                            Token token = (Token) tokens.get(i);
                            for (Token t = token; t != null; t = t.getParent()) {
                                if (t.topFact().getFactId() == supporter.getFactId()) {
                                    list.add(entry.getKey());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public void clear() {
        if (logicalSupportInUse())
            m_supportForFacts.clear();
    }
}
