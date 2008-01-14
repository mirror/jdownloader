package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

public class LocationListener implements ComponentListener, WindowListener {

    // private SubConfiguration guiConfig;
    private Logger    logger;

    private Component src;

    public LocationListener() {

        // this.guiConfig=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        this.logger = JDUtilities.getLogger();
    }

    public void componentHidden(ComponentEvent e) {

        src = e.getComponent();
    }

    public void componentMoved(ComponentEvent e) {

        src = e.getComponent();

    }

    public void componentResized(ComponentEvent e) {

        src = e.getComponent();
    }

    public void componentShown(ComponentEvent e) {

        src = e.getComponent();

    }

    public void windowActivated(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowClosed(WindowEvent e) {

        src = e.getComponent();
        saveAll();

    }

    public void saveAll() {
        if (src != null) {

            SimpleGUI.saveLastLocation(src, null);
            SimpleGUI.saveLastDimension(src, null);
            JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
        }
    }

    public void windowClosing(WindowEvent e) {

        src = e.getComponent();
        saveAll();
    }

    public void windowDeactivated(WindowEvent e) {

        src = e.getComponent();
        saveAll();
    }

    public void windowDeiconified(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowIconified(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowOpened(WindowEvent e) {

        src = e.getComponent();

    }

}
