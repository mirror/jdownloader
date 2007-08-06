import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import de.lagcity.TLH.DEBUG;
import de.lagcity.TLH.GLOBALS;
import de.lagcity.TLH.Locale;
import de.lagcity.TLH.PROPERTY;
import de.lagcity.TLH.SYSTEM;

public class CAntiCaptchaGUI extends BasicWindow {

	public static void main(String[] args) {

		CAntiCaptchaGUI owner = new CAntiCaptchaGUI();

	}

	public CAntiCaptchaGUI() {
		Locale.lc.setLanguage("deutsch");
		initWindow();
	
	
	
		 repack();
		 Locale.printMissing();

	}
	private void initWindow(){
		 setSize(400, 300);
		 setLocationByScreenPercent(50, 50);
		 setTitle(Locale.lc.get("MAINTITLE", "cAntiCaptcha (coalado)"));				
		 setLayout(new GridBagLayout());
		 setVisible(true);
	}
}