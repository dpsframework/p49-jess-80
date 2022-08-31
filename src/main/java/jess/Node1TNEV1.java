package jess;

/** 
 * Test that two slots in the same fact DO NOT have the same type and value.
 * Fails if either type or value differ.
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class Node1TNEV1 extends Node1
{

    private final int m_idx1;
    private final int m_idx2;
    private final int m_subidx1;
    private final int m_subidx2;

  Node1TNEV1(int idx1, int subidx1, int idx2, int subidx2)
  {
    m_idx1 = idx1;
    m_subidx1 = subidx1;
    m_idx2 = idx2;
    m_subidx2 = subidx2;
  }

  void callNodeRight(int tag, Token t, Context context) throws JessException
  {
    if (processClearCommand(tag, t, context))
      return;

    try
      {
        boolean result;

        Value v1, v2;
        Fact fact = t.topFact();
        if (m_subidx1 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(m_idx1).listValue(null).get(m_subidx1);
        else
          v1 = fact.get(m_idx1);

        if (m_subidx2 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(m_idx2).listValue(null).get(m_subidx2);
        else
          v2 = fact.get(m_idx2);

        result = ! (v1.equals(v2));

        if (result)
          // Pass this token on to all successors.
          passAlong(tag, t, context);
        //debugPrint(token, callType, fact, result);
      }
    catch (JessException re)
      {
        re.addContext("rule LHS (TNEV1)", context);
        throw re;
      }
    catch (Exception e)
      {
        JessException re = new JessException("Node1TNEV1.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (TNEV1)", context);
        throw re;

      }
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1TNEV1)
      {
        Node1TNEV1 n = (Node1TNEV1) o;
        return (m_idx1 == n.m_idx1 && m_idx2 == n.m_idx2 &&
                m_subidx1 == n.m_subidx1 && m_subidx2 == n.m_subidx2);

      }
    else
      return false;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer("[Test that slot #");
    sb.append(m_idx1);
    if (m_subidx1 != -1)
      {
        sb.append(", subindex ");
        sb.append(m_subidx1);
      }
    sb.append(" != slot#");
    sb.append(m_idx2);
    if (m_subidx2 != -1)
      {
        sb.append(", subindex ");
        sb.append(m_subidx2);
      }
    sb.append("]");
    return sb.toString();
  }

}

