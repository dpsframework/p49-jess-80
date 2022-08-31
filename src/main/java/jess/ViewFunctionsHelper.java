package jess;

public class ViewFunctionsHelper {
	public static int getCallType(Node n) {
		int calltype = (n instanceof NodeJoin) ? 0 : 1;
		if (n instanceof Node1RTL)
            calltype = 0;
		return calltype;
	}
	
	public static StringBuffer describeNode(Rete engine, Node node) {
		StringBuffer sb = new StringBuffer();
        if (node instanceof Node1) {
            sb.append(node);
        } else if (node instanceof Node2) {
            try {
                sb.append(node);
                sb.append("\n");
                sb.append(((Node2) node).displayMemory(engine).toString());
                sb.append("\n");
                sb.append(((Node2) node).getIndexingInfo(engine));
            } catch (JessException silentlyIgnore) {
            }
        } else if (node instanceof NodeJoin) {
            sb.append(node);
        } else if (node instanceof HasLHS) {
            sb.append(new PrettyPrinter((HasLHS) node).toString());
        }
		return sb;
	}
}
