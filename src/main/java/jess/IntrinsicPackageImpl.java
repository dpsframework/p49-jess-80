package jess;

import java.util.HashMap;
import java.io.Serializable;

/**
 * (C) 2013 Sandia Corporation<BR>
 * $Id: IntrinsicPackageImpl.java,v 1.1 2006-02-03 22:22:31 ejfried Exp $
 */
abstract class IntrinsicPackageImpl implements IntrinsicPackage, Serializable {
    protected void addFunction(Userfunction uf, HashMap ht) {
        ht.put(uf.getName(), uf);
    }
}
