package jess;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;

/**
 * Functions to deal with multifields. Many of these functions were
 * contributed by Win Carus (Win_Carus@inso.com)
 * <P>
 * (C) 2013 Sandia Corporation<br>
 */

class MultiFunctions extends IntrinsicPackageImpl {

    public void add(HashMap table) {
        addFunction(new CreateMF("create$"), table);
        addFunction(new DeleteMF(), table);
        addFunction(new ExplodeMF(), table);
        addFunction(new FirstMF(), table);
        // implode$ added by Win Carus (9.17.97)
        addFunction(new ImplodeMF(), table);
        // insert$ added by Win Carus (9.17.97)
        addFunction(new InsertMF(), table);
        addFunction(new LengthMF(), table);
        addFunction(new MemberMF(), table);
        addFunction(new NthMF(), table);
        // replace$ added by Win Carus (9.17.97)
        addFunction(new ReplaceMF(), table);
        addFunction(new RestMF(), table);
        // subseq$ added by Win Carus (9.17.97)
        addFunction(new SubseqMF(), table);
        // subsetp added by Win Carus (9.17.97); revised (10.2.97)
        addFunction(new SubsetP(), table);
        // union added by Win Carus (10.2.97)
        addFunction(new Union(), table);
        // intersection added by Win Carus (10.2.97)
        addFunction(new Intersection(), table);
        // complement added by Win Carus (10.2.97)
        addFunction(new Complement(), table);
    }
}



class DeleteMF implements Userfunction, Serializable {
    public String getName() {
        return "delete$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector newmf = new ValueVector();

        ValueVector mf = vv.get(1).listValue(context);
        int begin = (int) vv.get(2).numericValue(context);
        int end = (int) vv.get(3).numericValue(context);

        if (end < begin || begin < 1 || end > mf.size())
            throw new JessException("delete$",
                    "invalid range",
                    "(" + begin + "," + end + ")");
        for (int i = 0; i < mf.size(); i++) {
            if (i >= (begin - 1) && i <= (end - 1)) {
                continue;
            }
            newmf.add(mf.get(i).resolveValue(context));
        }

        return new Value(newmf, RU.LIST);
    }
}

class FirstMF implements Userfunction, Serializable {
    public String getName() {
        return "first$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = vv.get(1).listValue(context);
        ValueVector newmf = new ValueVector(1);
        if (mf.size() > 0)
            newmf.add(mf.get(0).resolveValue(context));
        return new Value(newmf, RU.LIST);
    }
}

class ImplodeMF implements Userfunction, Serializable {
    public String getName() {
        return "implode$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = vv.get(1).listValue(context);

        StringBuffer buf = new StringBuffer("");

        for (int i = 0; i < mf.size(); i++) {
            buf.append(mf.get(i).resolveValue(context)).append(" ");
        }

        String result = buf.toString();
        int len = result.length();

        if (len == 0)
            return new Value(result, RU.STRING);
        else
            return new Value(result.substring(0, len - 1), RU.STRING);
    }
}

class InsertMF implements Userfunction, Serializable {
    public String getName() {
        return "insert$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = vv.get(1).listValue(context);
        int idx = (int) vv.get(2).numericValue(context);

        if (idx < 1 || idx > mf.size() + 1) {
            throw new JessException("insert$",
                    "index must be >= 1 and <= " + (mf.size() + 1), ": " + idx);
        }

        ValueVector newmf = new ValueVector();
        // adjust for zero indexing
        --idx;
        for (int i = 0; i < idx; i++) {
            newmf.add(mf.get(i).resolveValue(context));
        }

        for (int j = 3; j < vv.size(); j++) {
            Value v = vv.get(j).resolveValue(context);
            if (v.type() != RU.LIST)
                newmf.add(v);
            else {
                ValueVector insertedmf = v.listValue(context);
                for (int k = 0; k < insertedmf.size(); k++)
                    newmf.add(insertedmf.get(k).resolveValue(context));
            }
        }

        for (int i = idx; i < mf.size(); i++) {
            newmf.add(mf.get(i).resolveValue(context));
        }

        return new Value(newmf, RU.LIST);
    }
}

class NthMF implements Userfunction, Serializable {
    public String getName() {
        return "nth$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        int idx = (int) vv.get(1).numericValue(context);

        if (idx < 1) {
            return Funcall.NIL;
        }
        ValueVector mf = vv.get(2).listValue(context);

        if (idx > mf.size()) {
            return Funcall.NIL;
        }

        return mf.get(idx - 1).resolveValue(context);
    }
}

class LengthMF implements Userfunction, Serializable {
    public String getName() {
        return "length$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = vv.get(1).listValue(context);
        return new Value(mf.size(), RU.INTEGER);
    }
}

class ReplaceMF implements Userfunction, Serializable {
    public String getName() {
        return "replace$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {

        ValueVector mf = vv.get(1).listValue(context);
        int startIdx = (int) vv.get(2).numericValue(context);
        int endIdx = (int) vv.get(3).numericValue(context);

        if (startIdx < 1 || startIdx > mf.size() + 1 ||
                endIdx < 1 || endIdx > mf.size() + 1 || startIdx > endIdx) {
            throw new JessException("replace$", "index must be >= 1 and <= " +
                    (mf.size() + 1),
                    ": " + startIdx + " " + endIdx);
        }

        ValueVector newmf = new ValueVector();

        // adjust for 0-based
        --startIdx;
        --endIdx;

        for (int i = 0; i <= startIdx - 1; i++) {
            newmf.add(mf.get(i).resolveValue(context));
        }

        for (int j = 4; j < vv.size(); j++) {
            Value v = vv.get(j).resolveValue(context);
            if (v.type() != RU.LIST)
                newmf.add(v);
            else {
                ValueVector insertedmf = v.listValue(context);
                for (int k = 0; k < insertedmf.size(); k++)
                    newmf.add(insertedmf.get(k).resolveValue(context));
            }
        }

        for (int i = endIdx + 1; i < mf.size(); i++) {
            newmf.add(mf.get(i).resolveValue(context));
        }

        return new Value(newmf, RU.LIST);
    }
}

class RestMF implements Userfunction, Serializable {
    public String getName() {
        return "rest$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector newmf = new ValueVector();

        ValueVector mf = vv.get(1).listValue(context);

        for (int i = 1; i < mf.size(); i++) {
            newmf.add(mf.get(i).resolveValue(context));
        }

        return new Value(newmf, RU.LIST);
    }
}

class MemberMF implements Userfunction, Serializable {
    public String getName() {
        return "member$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        Value target = vv.get(1).resolveValue(context);
        ValueVector list = vv.get(2).listValue(context);

        for (int i = 0; i < list.size(); i++) {
            if (target.equals(list.get(i).resolveValue(context))) {
                return new Value(i + 1, RU.INTEGER);
            }
        }
        return Funcall.FALSE;
    }
}

class SubseqMF implements Userfunction, Serializable {

    public String getName() {
        return "subseq$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = vv.get(1).listValue(context);
        int startIdx = (int) vv.get(2).numericValue(context);
        int endIdx = (int) vv.get(3).numericValue(context);

        // Note: multifield indices are 1-based, not 0-based.

        if (startIdx < 1)
            startIdx = 1;

        if (endIdx > mf.size())
            endIdx = mf.size();

        ValueVector newmf = new ValueVector();

        if (startIdx <= mf.size() &&
                endIdx <= mf.size() &&
                startIdx <= endIdx) {
            if (startIdx == endIdx) {
                newmf.add(mf.get(startIdx - 1).resolveValue(context));
            } else {
                for (int i = startIdx; i <= endIdx; ++i) {
                    newmf.add(mf.get(i - 1).resolveValue(context));
                }
            }
        }
        return new Value(newmf, RU.LIST);
    }
}

class SubsetP implements Userfunction, Serializable {
    public String getName() {
        return "subsetp";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector firstmf = vv.get(1).listValue(context);
        ValueVector secondmf = vv.get(2).listValue(context);

        // if (one member in the first multifield is not in the second multifield)
        // then return FALSE else return TRUE

        HashMap ht = new HashMap();

        for (int i = 0; i < secondmf.size(); ++i) {
            Value v = secondmf.get(i).resolveValue(context);
            ht.put(v, v);
        }

        for (int i = 0; i < firstmf.size(); ++i) {
            Value v = firstmf.get(i).resolveValue(context);
            if (ht.get(v) == null)
                return Funcall.FALSE;
        }

        return Funcall.TRUE;
    }
}

class Union implements Userfunction, Serializable {
    public String getName() {
        return "union$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector firstmf = vv.get(1).listValue(context);

        ValueVector newmf = new ValueVector();

        HashMap ht = new HashMap();

        for (int i = 0; i < firstmf.size(); ++i) {
            Value v = firstmf.get(i).resolveValue(context);
            if (ht.put(v, v) == null)
                newmf.add(v);
        }

        for (int j = 2; j < vv.size(); j++) {
            ValueVector secondmf = vv.get(j).listValue(context);
            for (int i = 0; i < secondmf.size(); ++i) {
                Value v = secondmf.get(i).resolveValue(context);
                if (ht.put(v, v) == null)
                    newmf.add(v);
            }
        }
        return new Value(newmf, RU.LIST);
    }
}

class Intersection implements Userfunction, Serializable {
    public String getName() {
        return "intersection$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector firstmf = vv.get(1).listValue(context);
        ValueVector secondmf = vv.get(2).listValue(context);

        ValueVector newmf = new ValueVector();

        HashMap ht = new HashMap();

        for (int i = 0; i < firstmf.size(); ++i) {
            Value v = firstmf.get(i).resolveValue(context);
            ht.put(v, v);
        }

        for (int i = 0; i < secondmf.size(); ++i) {
            Value v = secondmf.get(i).resolveValue(context);
            if (ht.get(v) != null) {
                newmf.add(v);
            }
        }
        return new Value(newmf, RU.LIST);
    }
}

class Complement implements Userfunction, Serializable {
    public String getName() {
        return "complement$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector firstmf = vv.get(1).listValue(context);
        ValueVector secondmf = vv.get(2).listValue(context);

        ValueVector newmf = new ValueVector();

        HashMap ht = new HashMap();

        for (int i = 0; i < firstmf.size(); ++i) {
            Value v = firstmf.get(i).resolveValue(context);
            ht.put(v, v);
        }

        for (int i = 0; i < secondmf.size(); ++i) {
            Value v = secondmf.get(i).resolveValue(context);
            if (ht.get(v) == null) {
                newmf.add(v);
            }
        }

        return new Value(newmf, RU.LIST);
    }
}

class ExplodeMF implements Userfunction, Serializable {
    public String getName() {
        return "explode$";
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector retval = new ValueVector();

        // Parenthesis is to placate parser; a spot fix because the parser
        // thinks there should be a whole sexp if there is more than one token.
        StringReader sbis
                = new StringReader("(" + vv.get(1).stringValue(context));
        JessTokenStream jts = new JessTokenStream(new ReaderTokenizer(sbis, false));

        jts.nextToken(); // discard "(", first token
        JessToken jt = jts.nextToken();

        while (jt.m_ttype != JessToken.NONE_TOK) {
            // Turn the token into a value.
            retval.add(jt.rawValueOf(context));
            jt = jts.nextToken();
        }
        return new Value(retval, RU.LIST);

    }
}


