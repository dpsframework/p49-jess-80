package jess;

import java.io.Serializable;
import java.util.ArrayList;

class If implements Userfunction, Serializable {
    private static final int[] ONE = new int[] {0};

    public String getName() {
        return "if";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        int[] blocks;

        if (vv instanceof Funcall) {
            blocks = (int[]) ((Funcall) vv).getScratchPad();
            if (blocks == null) {
                blocks = findBlocks(vv);
                ((Funcall) vv).setScratchPad(blocks);
            }
        } else
            blocks = findBlocks(vv);


        for (int i=0; i<blocks.length; ++i) {
            boolean isElse = vv.get(blocks[i]).equals(Funcall.s_else);

            Value test = isElse ?
                    Funcall.TRUE :
                    vv.get(blocks[i]+1).resolveValue(context);

            if (!(test.equals(Funcall.FALSE))) {
                int start = isElse ? blocks[i] + 1 : blocks[i] + 3;
                int limit = (i == blocks.length - 1 ? vv.size() : blocks[i+1]);
                Value result = Funcall.FALSE;

                for (int expr = start; expr < limit; ++expr) {
                    result = vv.get(expr).resolveValue(context);

                    if (context.returning()) {
                        return context.getReturnValue();
                    }
                }
                return result;
            }
        }
        return Funcall.FALSE;
    }

    static int[] findBlocks(ValueVector f) {
        if (f.size() < 5)
            return ONE;

        // Find start offsets of blocks
        ArrayList list = new ArrayList();
        list.add(new Integer(0));
        for (int i=3; i<f.m_ptr; ++i) {
            if (isAlternative(f.m_v[i]))
                list.add(new Integer(i));
        }
        if (list.size() == 1)
            return ONE;

        // Turn then into an array of int
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Integer) list.get(i)).intValue();
        }
        return result;
    }

    private static boolean isAlternative(Value value) {
        return value.equals(Funcall.s_else) || value.equals(Funcall.s_elif);

    }

}