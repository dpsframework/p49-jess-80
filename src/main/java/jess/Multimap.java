package jess;

import java.util.*;

class Multimap implements Map, java.io.Serializable {
    private HashMap map = new HashMap();

    public int size() {
        return 0;
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    public synchronized boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public synchronized Collection values() {
        return map.values();
    }

    public synchronized void putAll(Map t) {
        map.putAll(t);
    }

    public synchronized Set entrySet() {
        return map.entrySet();
    }

    public synchronized Set keySet() {
        return map.keySet();
    }

    public Object get(Object key) {
        Object current = map.get(key);
        if (current == null)
            return null;
        else if (current instanceof List)
            return current;
        else {
            ArrayList list = new ArrayList();
            list.add(current);
            return list;
        }
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public synchronized Object put(Object key, Object value) {
        Object current = map.get(key);
        if (current == null)
            return map.put(key, value);
        else if (current instanceof List) {
            List list = (List) current;
            list.add(value);
            return list;
        } else {
            List list = new ArrayList();
            list.add(current);
            list.add(value);
            return map.put(key, list);
        }
    }

    public synchronized boolean remove(Object key, Object value) {
        Object current = map.get(key);
        if (current == null)
            return false;
        if (current.equals(value)) {
            map.remove(key);
            return true;
        } else if (current instanceof List)  {
            List list = (List) current;
            boolean result = list.remove(value);
            if (list.isEmpty())
                map.remove(key);
            return result;
        } else {
            return false;
        } 
    }

}
