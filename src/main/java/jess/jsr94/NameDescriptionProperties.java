package jess.jsr94;

import java.util.HashMap;
import java.util.Map;

abstract class NameDescriptionProperties {
    private String m_name;
    private String m_description;
    private Map m_properties;
    private static final String DESCRIPTION = "description";
    private static final String NAME = "name";

    public NameDescriptionProperties(Map map) {
        if (map != null) {
            m_description = (String) map.get(DESCRIPTION);
            m_name = (String) map.get(NAME);
            m_properties = new HashMap(map);
        }
        if (m_description == null)
            m_description = "";
        if (m_name == null)
            m_name = "Untitled";
    }

    public NameDescriptionProperties(String name, String description) {
        m_description = description;
        m_name = name;
    }


    public String getName() {
        return m_name;
    }

    public String getDescription() {
        return m_description;
    }

    public Object getProperty(Object o) {
        createMap();
        return m_properties.get(o);
    }

    public void setProperty(Object o, Object o1) {
        createMap();
        m_properties.put(o, o1);
    }

    private void createMap() {
        if (m_properties == null)
            m_properties = new HashMap();
    }
}
