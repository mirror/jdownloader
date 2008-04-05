package org.jdesktop.swingx;



/**
 * Used with the setEnumerationValues method to specify enumerated values for
 * properties
 */
public final class EnumerationValue {
    private String name;
    private Object value;
    private String javaInitializationString;
    
    public EnumerationValue(String name, Object value, String javaInitString) {
        this.name = name;
        this.value = value;
        this.javaInitializationString = javaInitString;
    }
    
    public String getName() {
        return name;
    }
    
    public String toString() {
        return name;
    }
    
    public Object getValue() {
        return value;
    }
    
    public String getJavaInitializationString() {
        return javaInitializationString;
    }
}