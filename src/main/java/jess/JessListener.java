package jess;
import java.util.EventListener;

/**
 <p>JessListener is a notification interface for Jess events. Objects
 that wish to be notified of significant happenings in a Jess engine
 should implement this interface, then register themselves with a
 Rete object using {@link Rete#addJessListener}. <tt>Rete</tt>
 (potentially) fires events at all critical
 junctures during its execution: when rules fire, when a
 {@link Rete#reset} or {@link Rete#clear} call is made, when a fact is
 asserted or retracted, etc. {@link JessEvent} has a
 {@link JessEvent#getType} method to tell you what sort of event you have been
 notified of; the type will be one of the constants in the
 <tt>JessEvent</tt> class.
 </P>

 <p>As an example, let's suppose you'd like your program's graphical
 interface to display a running count of the number of facts on the
 fact-list, and the name of the last executed rule.  You've provided a
 static method, <tt>MyGUI.displayCurrentRule(String ruleName),</tt>
 which you would like to have called when a rule fires. You've got a
 pair of methods <tt>MyGUI.incrementFactCount()</tt> and
 <tt>MyGUI.decrementFactCount()</tt> to keep track of facts. And you've got
 one more static method, <tt>MyGUI.clearDisplay()</tt>, to call when Jess is
 cleared or reset. To accomplish this, you simply need to write an
 event handler, install it, and set the event mask properly. Your event
 handler class might look like this.</p>

 <pre>
 import jess.*;

 public class ExMyEventHandler implements JessListener {
     public void eventHappened(JessEvent je) {
         int defaultMask = JessEvent.DEFRULE_FIRED | JessEvent.FACT | JessEvent.RESET;
         int type = je.getType();
         switch (type) {
              case JessEvent.RESET:
               MyGUI.clearDisplay();
               break;

             case JessEvent.DEFRULE_FIRED:
               MyGUI.displayCurrentRule( ((Activation) je.getObject()).getRule().getName());
               break;

             case JessEvent.FACT | JessEvent.REMOVED:
               MyGUI.decrementFactCount();
               break;

             case JessEvent.FACT:
               MyGUI.incrementFactCount();
               break;

             default:
               // ignore
         }
     }
 }
 </pre>

 <p>Note how the event type constant for fact retracting is composed from
 <tt>FACT | REMOVED.</tt> In general, constants like <tt>DEFRULE,
 DEFTEMPLATE,</tt>  etc,
 refer to the addition of a new construct, while composing these with <tt>REMOVE</tt>
 signifies the removal of the same construct.
 </p>

 <p>To install this listener, you would simply create an instance and call
 <tt>jess.Rete.addEventListener()</tt>, then set the event mask:</p>

 <pre>
 Rete engine = new Rete();
 engine.addJessListener(new ExMyEventHandler());
 engine.setEventMask(engine.getEventMask() | JessEvent.DEFRULE_FIRED | JessEvent.FACT | JessEvent.RESET );
 </pre>

 <p>When {@link Rete#clear} is called, the event mask is
 typically reset to the default. When event handlers are called, they
 have the opportunity to alter the mask to re-install
 themselves. Alternatively, they can call {@link Rete#removeJessListener}
 to unregister themselves.</p>

 <b>Working with events from the Jess language</b>

 <p>It's possible to work with the event classes from Jess language code
 as well. To write an event listener, you can use the
 {@link JessEventAdapter} class. This class works rather like the
 <tt>jess.awt</tt> adapter classes do. Usage is best illustrated with
 an example. Let's say you want to print a message each time a new
 template is defined, and you want to do it from Jess code. Here it
 is:</p>

 <pre>
 (import jess.*)
 ;; Here is the event-handling deffunction
 ;; It accepts one argument, a JessEvent
 (deffunction display-deftemplate-from-event (?evt)
     (if (eq (JessEvent.DEFTEMPLATE) (get ?evt type)) then
     (printout t "New deftemplate: " (call (call ?evt getObject) getName) crlf)))

 ;; Here we install the above function using a JessEventAdapter
 (call (engine) addJessListener
     (new JessEventAdapter display-deftemplate-from-event (engine)))

 ;; Now we add DEFTEMPLATE to the event mask
 (set (engine) eventMask
 (bit-or (get (engine) eventMask) (JessEvent.DEFTEMPLATE)))
 </pre>

 <p>Now whenever a new template is defined, a message will be
 displayed.</p>

 (C) 2013 Sandia Corporation<br>

 @see JessEvent
 @see Rete#addJessListener
 @see Rete#removeJessListener
 */

public interface JessListener extends EventListener
{
  /**
   * Called by a JessEvent source when something interesting happens.
   * The typical implementation of eventHappened will switch on the
   * return value of je.getType().
   *
   * @param je an event object describing the event.
   * @exception JessException if any problem occurs during event handling.  */

  void eventHappened(JessEvent je) throws JessException;
}
