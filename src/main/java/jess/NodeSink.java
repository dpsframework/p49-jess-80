package jess;

/**
 * A NodeSink is an object that contains a collection of Rete
 * nodes. Defrules and Defqueries implement this interface.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

interface NodeSink {
    /**
     * Return a string (useful for debugging) describing all the Rete network
     * nodes connected to this construct.
     * @return A textual description of all the nodes used by this construct
     */

    String listNodes();

    /**
     * Add a node to this sink. This addition should be reflected by subsequent calls to "listNodes()".
     * @param n a Rete network node
     * @throws JessException if n is null
     */

    void addNode(Node n) throws JessException;

}
