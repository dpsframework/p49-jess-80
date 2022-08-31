package jess;
import java.io.Serializable;

/**
 * Actual function advice classes implement this
 */

interface Advice extends Userfunction, Serializable
{
  Userfunction getFunction();
  void addAction(Value v);

}
