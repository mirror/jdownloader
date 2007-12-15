package jd.controlling;

import jd.event.ControlEvent;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann dazu verwendet werden einen Fortschritt in der GUI
 * anzuzeigen. Sie bildet dabei die schnittstelle zwischen Interactionen,
 * plugins etc und der GUI
 * 
 * @author coalado
 * 
 */
public class ProgressController {



    private int    max;
    private static int idCounter=0;
    private int    currentValue;

    private String statusText;

    private Object source;

    private boolean finished;
    private int id;

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
       // JDUtilities.getLogger().info(this.toString());
        fireChanges();
    }
    public void setSource(Object src){
        this.source=src;
        fireChanges();
    }
    public Object getSource(){
        return source;
    }
    public ProgressController(String name, int max) {
     this.id=idCounter++;
        this.max = max;
        this.statusText="init "+name;
        currentValue = 0;
        finished=false;
        fireChanges();
    }
 public String toString(){
     return id+": "+super.toString();
 }
    public ProgressController(String name) {
       this(name,100);
    }

    public void setRange(int max){
        
        this.max = max;
       // JDUtilities.getLogger().info(this.toString());
        setStatus(currentValue);
    }
    public void setStatus(int value) {
        if (value < 0) value = 0;
        if (value > max) value = max;
        this.currentValue = value;
       // JDUtilities.getLogger().info(this.toString());
        fireChanges();
    }

    public double getPercent() {
        int range = max;
        int current = currentValue;
        return (double) current / (double) range;

    }
    public void fireChanges(){
       // JDUtilities.getLogger().info("FIRE "+this);
        if(!this.isFinished())
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, this.source));
    }

    public void increase(int i) {
       // JDUtilities.getLogger().info(this.toString());
        setStatus(currentValue+i);
        
    }
    public void decrease(int i) {
        setStatus(currentValue-1);
        
    }
    public boolean isFinished(){
        return finished;
    }
    public void finalize(){
        JDUtilities.getLogger().info(this.toString());
        this.finished=true;
        this.currentValue=this.max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, this.source));
        
    }

    public int getMax() {
       return this.max;
      
    }

    public int getValue() {
      return this.currentValue;
    }

    public void addToMax(int length) {
      this.setRange(max+length);
    
    }
}
