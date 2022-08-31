
package jess;

/** 
 * A JessEventAdapter that lets you write JessEvent handlers in the
 * Jess language. The adapter serves as "glue" between a source of
 * JessEvents and a Userfunction that will be invoked when an event is
 * received.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

public class JessEventAdapter implements JessListener
{
  private Funcall m_fc;

  // This is OK because this class isn't serializable.
  private final Rete m_engine;

  /**
   * Create an adapter. Normally you'll call this from Jess code using
   * reflection.  The first argument is the name of a function to call
   * when a JessEvent occurs; The second is the engine to attach to
   * @param uf The name of a Jess function
   * @param engine The engine to field events from
   * @exception JessException If anything goes wrong.
   */
  public JessEventAdapter(String uf, Rete engine) throws JessException
  {
    m_engine = engine;
    m_fc = new Funcall(uf, engine);
    m_fc.setLength(2);
  }

  /**
   * Called when a JessEvent occurs. The function specified in the
   * constructor is called, with the event object as the only
   * argument. The function can examine the event using reflection.
   * @param e The event
   */
  public final void eventHappened(JessEvent e)
  {
    try
      {
        m_fc.set(new Value(e), 1);
        m_fc.execute(m_engine.getGlobalContext());
      }
    catch (JessException re)
      {
        m_engine.getErrStream().println(re);
        m_engine.getErrStream().flush();
      }
  }
}
