package jess.tools;

import java.util.*;

/**
 * A poor man's profiler. You can call "inc" and "dec" to increment and decrement a named counter.
 * This class automatically produces a report at shutdown if anything is ever counted. There's
 * nothing Jess-specific about this class; it's just a useful utility. <P>
 * (C) Sandia National Laboratories
 */
public class Profiler {
    private static Map m_data;

    /**
     * Increment the count associated with the given name. If the name hasn't been used before, a new
     * counter is created and initialized to zero before incrementing it.
     * @param name any string
     */
    public static void inc(String name) {
        int[] data = get(name);
        ++data[0];
    }

    /**
     * Decrement the count associated with the given name. If the name hasn't been used before, a new
     * counter is created and initialized to zero before decrementing it.
     * @param name any string
     */
    public static void dec(String name) {
        int[] data = get(name);
        --data[0];
    }

    private static int[] get(String name) {
        int[] data = (int[]) map().get(name);
        if (data == null) {
            data = new int[1];
            map().put(name, data);
        }
        return data;
    }

    private static Map map() {
        if (m_data == null) {
            m_data = new HashMap();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    for (Iterator it = m_data.entrySet().iterator(); it.hasNext();) {
                        Map.Entry entry = (Map.Entry) it.next();
                        System.out.println(entry.getKey() + "=" + ((int[]) entry.getValue())[0]);
                    }
                }
            });
        }
        return m_data;
    }
}
