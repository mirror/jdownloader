package jd.gui.skins.simple;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
public class LocationListener implements ComponentListener {


    //private SubConfiguration guiConfig;
    //private Logger logger;

    public LocationListener() {
       
        //this.guiConfig=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
       // this.logger=JDUtilities.getLogger();
    }

    public void componentHidden(ComponentEvent e) {
    // TODO Auto-generated method stub

    }

    public void componentMoved(ComponentEvent e) {

        

     SimpleGUI.saveLastLocation(e.getComponent(), null);
          
     
     

    }

    public void componentResized(ComponentEvent e) {
          SimpleGUI.saveLastDimension(e.getComponent(), null);
    }

    public void componentShown(ComponentEvent e) {
    // TODO Auto-generated method stub

    }


}
