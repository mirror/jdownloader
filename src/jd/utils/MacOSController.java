package jd.utils;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.JDAboutDialog;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.gui.skins.simple.config.FengShuiConfigPanel;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class MacOSController extends Application {

	
	public MacOSController() {
	    setEnabledPreferencesMenu(true);
	    setEnabledAboutMenu(true);
		addApplicationListener(new Handler());
	}
	
	class Handler extends ApplicationAdapter {
		
		public void handleQuit(ApplicationEvent e) {
			JDUtilities.getController().exit();
		}
		
		public void handleAbout(ApplicationEvent e) {
		    e.setHandled(true); 
            JDAboutDialog.getDialog().setVisible(true);
        }
		
		public void handlePreferences(ApplicationEvent e) {
		    SimpleGUI.CURRENTGUI.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
	    }
	}

}
