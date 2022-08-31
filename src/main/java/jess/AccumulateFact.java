package jess;

/**
 * A special class of fact used in the implementation of "accumulate;" all instances claim to be equal.
 * <P>
 * (C) 2007 Sandia National Laboratories<br>
 */

class AccumulateFact extends Fact {
    public AccumulateFact() throws JessException {
        super(Deftemplate.getAccumTemplate());
    }

    public boolean equals(Object o) {
        return (o.getClass() == getClass());
    }

    public int hashCode() {
        return 0;
    }
}
