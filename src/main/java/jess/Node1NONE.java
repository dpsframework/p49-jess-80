package jess;

/** 
 * A test that always fails
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */ 

class Node1NONE extends Node1 {
    void callNodeRight(int tag, Token t, Context context) throws JessException {
    }

    public int getNodeType() {
        return TYPE_NONE;
    }

    public boolean equals(Object o) {
        return (o instanceof Node1NONE);
    }
}

