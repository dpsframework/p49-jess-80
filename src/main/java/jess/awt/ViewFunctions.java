package jess.awt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.util.*;

import jess.Context;
import jess.Funcall;
import jess.HasLHS;
import jess.JessEvent;
import jess.JessException;
import jess.JessListener;
import jess.Node;
import jess.Rete;
import jess.Token;
import jess.Userfunction;
import jess.Value;
import jess.ValueVector;
import jess.ViewFunctionsHelper;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;



/**
 * A nifty graphical Rete Network viewer for Jess.
 * <p/>
 * (C) 2013 Sandia Corporation<br>
 */

public class ViewFunctions implements jess.Userpackage {

    /**
     * Actual Userfunction class: now just a trivial shell around Graph
     */

    static class View implements Userfunction, Serializable {
        public String getName() {
            return "view";
        }

        public Value call(final ValueVector vv, final Context context) throws JessException {
            final JessException[] holder = new JessException[1];
            new Runnable() {
                public void run() {
                    try {
                        HasLHS r = null;
                        if (vv.size() > 1) {
                            r = context.getEngine().
                                    findDefrule(vv.get(1).stringValue(context));
                            if (r == null)
                                throw new JessException("view", "No such rule or query",
                                        vv.get(1).stringValue(context));
                        }

                        // Main view frame and panel
                        final Rete engine = context.getEngine();
                        final JFrame f = new JFrame("Network View");
                        final Graph g = new Graph(context.getEngine(), r);
                        engine.setEventMask(engine.getEventMask() | JessEvent.DEFRULE);
                        engine.addJessListener(g);

                        f.getContentPane().add(g, "Center");
                        f.setSize(500, 500);

                        JPanel p = new JPanel();
                        JButton b = new JButton("Home");
                        b.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                g.init();
                            }
                        });
                        p.add(b);

                        f.getContentPane().add("South", p);
                        f.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent we) {
                                f.dispose();
                                engine.removeJessListener(g);
                            }
                        });

                        // Show frame
                        f.validate();
                        f.setVisible(true);
                    } catch (JessException ex) {
                        holder[0] = ex;
                    }
                }
            }.run();
            if (holder[0] != null)
                throw holder[0];
            else
                return Funcall.TRUE;
        }
    }


    /**
     * One node in the Rete Network view
     */

    static class VNode {
        int m_x, m_y;
        final Node m_node;
        final Color m_c;

        /**
         * @param x
         * @param y
         * @param c
         * @param node
         */
        VNode(int x, int y, Color c, Node node) {
            m_x = x;
            m_y = y;
            m_node = node;
            m_c = c;
        }
    }

    /**
     * One inter-node link in the Rete Network view
     */
    static class VEdge {
        final int m_from;
        final int m_to;
        final Color m_c;

        /**
         * @param from
         * @param to
         * @param c
         */
        VEdge(int from, int to, Color c) {
            m_from = from;
            m_to = to;
            m_c = c;
        }

        public boolean equals(Object o) {
            VEdge edge = (VEdge) o;
            return m_from == edge.m_from &&
                    m_to == edge.m_to &&
                    m_c ==  edge.m_c;
        }

        public int hashCode() {
            return m_from ^ m_to ^ m_c.hashCode();
        }
    }

    /**
     * The display panel itself
     */
    static class Graph extends JPanel
            implements MouseListener, MouseMotionListener, JessListener {

        private int m_nVNodes;
        private VNode m_VNodes[];

        private int m_nVEdges;
        private VEdge m_VEdges[];

        private VNode m_pick;
        private Node m_show;

        // Innocuous because the viewer window's contents aren't
        // serializable anyway. Make this transient to be doubly sure.
        private final transient Rete m_engine;

        static final Color m_selectColor = Color.pink;
        static final int NODE_WIDTH = 10, HW = NODE_WIDTH / 2, NODE_HEIGHT = 10, HH = NODE_HEIGHT / 2;

        private long m_lastMD;

        // 100 rows of nodes ... that'd be some rulebase!
        private final int[] m_nextSlot = new int[100];

        private final Color[] m_edgeColors = {Color.green, Color.blue};

        private HasLHS m_haslhs;

        public int getnVEdges() {
            return m_nVEdges;
        }

        public int getnVNodes() {
            return m_nVNodes;
        }

        Graph(Rete r, HasLHS dr) {
            m_engine = r;
            addMouseListener(this);
            addMouseMotionListener(this);
            setSize(500, 500);
            m_haslhs = dr;
            init();
        }

        int findVNode(Node n, int depth) {
            for (int i = 0; i < m_nVNodes; i++) {
                if (m_VNodes[i].m_node == n) {
                    return i;
                }
            }
            return addVNode(n, depth);
        }

        private Color getNodeColor(Node n) {
            switch (n.getNodeType()) {
                case Node.TYPE_NODE1:
                    return Color.RED;
                case Node.TYPE_NODE2:
                    return Color.GREEN;
                case Node.TYPE_NODENOT2:
                    return Color.YELLOW;
                case Node.TYPE_TERMINAL:
                    return Color.CYAN;
                case Node.TYPE_ADAPTER:
                    return Color.ORANGE;
                case Node.TYPE_NONE:
                default:
                    return Color.BLACK;
            }
        }

        private Color getEdgeColor(Node n) {
            int calltype = ViewFunctionsHelper.getCallType(n);
            
            if (calltype < 0 || calltype > m_edgeColors.length)
                return Color.black;
            else
                return m_edgeColors[calltype];
        }

        int addVNode(Node node, int depth) {
            VNode n = new VNode(++m_nextSlot[depth] * (NODE_WIDTH + HW),
                    depth * (NODE_HEIGHT + HH),
                    getNodeColor(node), node);

            if (m_nVNodes == m_VNodes.length) {
                VNode[] temp = new VNode[m_nVNodes * 2];
                System.arraycopy(m_VNodes, 0, temp, 0, m_nVNodes);
                m_VNodes = temp;
            }

            m_VNodes[m_nVNodes] = n;
            return m_nVNodes++;
        }

        void addVEdge(Node from, Node to, int depth, Color c) {
            VEdge e = new VEdge(findVNode(from, depth), findVNode(to, depth + 1), c);
            for (int i = 0; i < m_nVEdges; ++i)
                if (e.equals(m_VEdges[i]))
                    return;
            if (m_nVEdges == m_VEdges.length) {
                VEdge[] temp = new VEdge[m_nVEdges * 2];
                System.arraycopy(m_VEdges, 0, temp, 0, m_nVEdges);
                m_VEdges = temp;
            }
            m_VEdges[m_nVEdges++] = e;
        }

        public void paintVNode(Graphics g, VNode n) {
            int x = n.m_x;
            int y = n.m_y;
            g.setColor((n == m_pick) ? m_selectColor : n.m_c);
            int w = NODE_WIDTH;
            int h = NODE_HEIGHT;
            g.fillRect(x - w / 2, y - h / 2, w, h);
            g.setColor(Color.black);
            g.drawRect(x - w / 2, y - h / 2, w - 1, h - 1);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension d = getSize();

            g.setColor(getBackground());
            g.fillRect(0, 0, d.width, d.height);
            for (int i = 0; i < m_nVEdges; i++) {
                VEdge e = m_VEdges[i];
                int x1 = m_VNodes[e.m_from].m_x;
                int y1 = m_VNodes[e.m_from].m_y;
                int x2 = m_VNodes[e.m_to].m_x;
                int y2 = m_VNodes[e.m_to].m_y;
                g.setColor(e.m_c);
                g.drawLine(x1, y1, x2, y2);
            }

            for (int i = 0; i < m_nVNodes; i++) {
                paintVNode(g, m_VNodes[i]);
            }

            FontMetrics fm = g.getFontMetrics();

            if (m_show != null) {
                g.setColor(Color.black);
                String s = m_show.toString();
                int h = fm.getHeight();
                g.drawString(s, 10, (d.height - h) + fm.getAscent());
            }

        }

        public void mousePressed(MouseEvent e) {
            int bestdist = Integer.MAX_VALUE;
            int x = e.getX();
            int y = e.getY();
            for (int i = 0; i < m_nVNodes; i++) {
                VNode n = m_VNodes[i];
                int dist = (n.m_x - x) * (n.m_x - x) + (n.m_y - y) * (n.m_y - y);
                if (dist < bestdist) {
                    m_pick = n;
                    bestdist = dist;
                }
            }

            if (bestdist > 200)
                m_pick = null;
            else {
                m_pick.m_x = x;
                m_pick.m_y = y;
            }
            repaint();
            e.consume();
        }

        public void mouseReleased(MouseEvent e) {
            try {
                long interval = System.currentTimeMillis() - m_lastMD;
                if (interval < 500) {
                    new NodeViewer(m_pick.m_node, m_engine);
                    m_lastMD = 0;
                } else if (m_pick != null) {
                    m_pick.m_x = e.getX();
                    m_pick.m_y = e.getY();
                    m_lastMD = System.currentTimeMillis();
                }
            } finally {
                m_pick = null;
                repaint();
                e.consume();
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (m_pick != null) {
                m_pick.m_x = e.getX();
                m_pick.m_y = e.getY();
                repaint();
            }
            e.consume();
        }

        public void mouseMoved(MouseEvent e) {
            int bestdist = Integer.MAX_VALUE;
            int x = e.getX();
            int y = e.getY();
            Node over = null;
            for (int i = 0; i < m_nVNodes; i++) {
                VNode n = m_VNodes[i];
                int dist = (n.m_x - x) * (n.m_x - x) + (n.m_y - y) * (n.m_y - y);
                if (dist < bestdist) {
                    over = n.m_node;
                    bestdist = dist;
                }
            }

            if (bestdist > 200)
                m_show = null;

            else
                m_show = over;

            repaint();
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        private void buildNetwork(Node n, int depth) {
            for (Iterator it = n.successors(); it.hasNext();) {
                Node s = (Node) it.next();
                if (m_haslhs != null && !ruleContains(s))
                    continue;
                addVEdge(n, s, depth, getEdgeColor(n));
                buildNetwork(s, depth + 1);
            }
        }

        private boolean ruleContains(Node n) {
            for (Iterator<Node> it = m_haslhs.getNodes(); it.hasNext();)
                if (it.next() == n)
                    return true;
            return false;
        }


        public void init() {
            m_VNodes = new VNode[10];
            m_VEdges = new VEdge[10];
            m_nVNodes = m_nVEdges = 0;
            m_pick = null;
            m_show = null;

            for (int i = 0; i < m_nextSlot.length; i++)
                m_nextSlot[i] = 0;

            buildNetwork(m_engine.getRoot(), 1);

            repaint();
        }

        public void eventHappened(JessEvent je) {
            if ((je.getType() & JessEvent.DEFRULE) != 0
                    || je.getType() == JessEvent.CLEAR) {
                if (m_haslhs != null)
                    m_haslhs = m_engine.findDefrule(m_haslhs.getName());
                init();
            }
        }

    }

    /**
     * The detail viewer
     */
    static class NodeViewer extends JFrame implements JessListener {
        private final Node m_node;
        private final TextArea m_view;
        private final TextArea m_events;

        NodeViewer(Node n, final Rete engine) {
            super(n.toString());
            m_node = n;
            engine.store("NODE", m_node);

            m_view = new TextArea(40, 20);

            m_events = new TextArea(40, 20);

            m_view.setEditable(false);
            m_events.setEditable(false);

            JPanel p = new JPanel();
            p.setLayout(new GridLayout(2, 1));
            p.add(m_view);
            p.add(m_events);


            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    dispose();
                    m_node.removeJessListener(NodeViewer.this);
                    engine.removeJessListener(NodeViewer.this);
                }
            });

            getContentPane().add(p, "Center");
            describeNode(engine);
            setSize(600, 600);
            validate();
            setVisible(true);
            m_node.addJessListener(this);
            engine.addJessListener(this);
            engine.setEventMask(engine.getEventMask() |
                    JessEvent.DEFRULE |
                    JessEvent.CLEAR);
        }


        void describeNode(Rete engine) {
            StringBuffer sb = ViewFunctionsHelper.describeNode(engine, m_node);
            m_view.setText(sb.toString());
        }

        public void eventHappened(JessEvent je) throws JessException {
            Object o = je.getSource();
            if (o == m_node) {
                Token t = (Token) je.getObject();
                int type = je.getType();
                if (m_view != null)
                    m_events.append(type + ": " + t + "\n");

            }
            describeNode(je.getContext().getEngine());
        }
    }

	public void add(Rete engine) {
		engine.addUserfunction(new View());
	}

}
