package org.jdownloader.settings.advanced;

public interface AdvancedConfigEntry {

    public String getKey();

    public Object getValue();

    public Class<?> getType();

    public String getDescription();

    public Validator getValidator();

    public Object getDefault();

    public void setValue(Object value);

    public String getTypeString();

}
