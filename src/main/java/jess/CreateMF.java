package jess;

import java.io.Serializable;

class CreateMF implements Userfunction, Serializable {
    private final String m_name;

    public CreateMF(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public Value call(ValueVector vv, Context context) throws JessException {
        ValueVector mf = new ValueVector();

        for (int i = 1; i < vv.size(); i++) {
            Value v = vv.get(i).resolveValue(context);
            switch (v.type()) {
                case RU.LIST:
                    ValueVector list = v.listValue(context);
                    for (int k = 0; k < list.size(); k++) {
                        mf.add(list.get(k).resolveValue(context));
                    }
                    break;
                default:
                    mf.add(v);
                    break;
            }
        }
        return new Value(mf, RU.LIST);
    }
}