package jd.utils;

import jd.controlling.JDController;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class MacOSQuitController extends Application {

	
	public MacOSQuitController() {
		addApplicationListener(new QuitHandler());
	}
	
	class QuitHandler extends ApplicationAdapter {
		
		public void handleQuit(ApplicationEvent e) {
			JDUtilities.getController().exit();		
		}
		
	}
}
