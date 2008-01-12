package jd.gui.skins.simple;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

import jd.config.SubConfiguration;
import jd.event.UIEvent;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LocationListener implements ComponentListener {


    private SubConfiguration guiConfig;
    private Logger logger;

    public LocationListener() {
       
        this.guiConfig=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        this.logger=JDUtilities.getLogger();
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
