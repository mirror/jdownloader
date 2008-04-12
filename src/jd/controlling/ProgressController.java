//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.controlling;

import jd.event.ControlEvent;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann dazu verwendet werden einen Fortschritt in der GUI
 * anzuzeigen. Sie bildet dabei die schnittstelle zwischen Interactionen,
 * plugins etc und der GUI
 * 
 * @author JD-Team
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
     return "ProgressController "+id;
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
        //JDUtilities.getLogger().info("FINALIZE "+this.toString()+this.getStatusText());
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

    public int getID() {
       return id;
    }
}
