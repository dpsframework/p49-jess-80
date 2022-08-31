package jess;

import java.util.Map;

/**
 * A Node that can give logical support to a fact.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */


interface LogicalNode {
    public void dependsOn(Fact f, Token t, Rete engine);
    public int getTokenSize();
    public Map getLogicalDependencies(Rete engine);
    public void setMatchInfoSource(MatchInfoSource source);
}
