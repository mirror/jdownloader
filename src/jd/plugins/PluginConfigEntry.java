package jd.plugins;

import java.awt.event.ActionListener;
import java.util.logging.Logger;

import jd.Property;

public class PluginConfigEntry{
    
    @SuppressWarnings("unused")
    private transient Logger        logger                      = Plugin.getLogger();
    private int type;
    private String label;
    private ActionListener actionListener;
    private String propertyName;
    private Property propertyInstance;
    private Object[] list;
    private Object defaultValue;

    public PluginConfigEntry(int type, ActionListener listener, String label) {
        this.type = type;      
        this.label=label;     
        this.actionListener=listener;
           

       
    }
    public PluginConfigEntry(int type, String label) {
        this.type = type;
        this.label=label;    

    }
    public  PluginConfigEntry(int type) {
        this.type = type;      

    }
    public PluginConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
        this.type = type;
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;  
        this.label=label; 
    

    }
    
    public PluginConfigEntry(int type, Property propertyInstance, String propertyName, Object[] list,String label) {
        this.type = type;
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;    
    
        this.list=list;
        this.label=label; 
    

    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getLabel() {
        
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public ActionListener getActionListener() {
        return actionListener;
    }
    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }
    public String getPropertyName() {
        return propertyName;
    }
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    public Property getPropertyInstance() {
        return propertyInstance;
    }
    public void setPropertyInstance(Property propertyInstance) {
        this.propertyInstance = propertyInstance;
    }
    public Object[] getList() {
        return list;
    }
    public void setList(Object[] list) {
        this.list = list;
    }
    public Object getDefaultValue() {
        return defaultValue;
       
    }
    public void setDefaultValue(Object value) {
        defaultValue=value;
       
    }
    
}