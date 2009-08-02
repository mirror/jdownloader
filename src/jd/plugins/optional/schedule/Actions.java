package jd.plugins.optional.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Actions implements Serializable {
    private static final long serialVersionUID = 5218836057229345533L;
    private String name;
    private boolean enabled = true;
    private Date date;
    private int repeat = 0; 
    private ArrayList<Executions> executions = new ArrayList<Executions>();
    
    public Actions(String name) {
        this.name = name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void addExecutions(Executions e) {
        executions.add(e);
    }
    
    public void removeExecution(int row) {
        executions.remove(row);
    }
    
    public ArrayList<Executions> getExecutions() {
        return executions;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getRepeat() {
        return repeat;
    } 
}
