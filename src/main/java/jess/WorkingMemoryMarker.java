package jess;

/**
 * <p>A WorkingMemoryMarker is a "memento" that records the state of working memory.
 * Calling {@link jess.Rete#mark()} returns an object that implements this interface, which you must save.
 * By calling {@link Rete#resetToMark} and passing back the same object, you can retract all facts asserted since the marker was
 * created.
 * </p>
 * (C) 2013 Sandia Corporation<BR>
 * @see Rete#mark
 * @see Rete#resetToMark
 */
public interface WorkingMemoryMarker {
    /**
     * This method is responsible for restoring working memory state. This method is public only as an implementation detail --
     * there is no expectation that users will call this method or implement this interface themselves
     * @param engine the rule engine
     * @throws JessException if anything goes wrong
     */
    void restore(Rete engine) throws JessException;
}
