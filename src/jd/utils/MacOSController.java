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
		addApplicationListener(new QuitHandler());
		addApplicationListener(new AboutHandler());
		addApplicationListener(new PrefHandler());
	}
	
	class QuitHandler extends ApplicationAdapter {
		
		public void handleQuit(ApplicationEvent e) {
			JDUtilities.getController().exit();		
		}
	}
	
	class AboutHandler extends ApplicationAdapter {
	    
	    public void handleAbout(ApplicationEvent e) {
	        JDAboutDialog.getDialog().setVisible(true);
	    }
	}
	
	class PrefHandler extends ApplicationAdapter {
	    
	    public void handlePreferences(ApplicationEvent e) {
	        SimpleGUI.CURRENTGUI.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
	    }
	}
}
