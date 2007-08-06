import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;


import de.lagcity.TLH.DEBUG;
import de.lagcity.TLH.Locale;
import de.lagcity.TLH.SWING;



public class BasicWindow extends JFrame {
	public boolean exitSystem = true;
	private Object owner;
	public BasicWindow(Object owner) {
	this.owner=owner;
	initWindow();
	}
	public BasicWindow() {
		initWindow();
	}

	protected GridBagConstraints getGBC(int x, int y, int width, int height) {

		GridBagConstraints gbc = SWING.getGBC(x, y, width, height);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty=1;
		gbc.weightx=1;
		
		return gbc;
	}
	

	private void initWindow() {
		final BasicWindow _this = this;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				Window window = event.getWindow();
				_this.setVisible(true);
				window.setVisible(false);
				window.dispose();
				if (_this.exitSystem) {
					System.exit(0);
				}
			}

		});

		resizeWindow(100);
		setLocationByScreenPercent(50, 50);
		setBackground(Color.LIGHT_GRAY);
	}

	public void resizeWindow(int percent) {
		Dimension screenSize = getToolkit().getScreenSize();
		setSize((screenSize.width * percent) / 100,
				(screenSize.height * percent) / 100);
	}
	public void repack() {
		SwingUtilities.updateComponentTreeUI(this);
	}
	public void setLocationByScreenPercent(int width, int height) {
		Dimension screenSize = getToolkit().getScreenSize();

		setLocation(((screenSize.width - getSize().width) * width) / 100,
				((screenSize.height - getSize().height) * height) / 100);
	}
	public static BasicWindow showImage(File file,String title){
		
		Image img=SWING.loadImage(file);
		BasicWindow w= new BasicWindow();
		ImageComponent ic=new ImageComponent(img);
		
		w.setSize(ic.getImageWidth()+10, ic.getImageHeight()+20);
		w.setLocationByScreenPercent(50, 50);
		w.setTitle(title);
		w.setLayout(new GridBagLayout());
		w.add(ic, SWING.getGBC(0,0,1, 1));
		w.setVisible(true);
		w.pack();
		w.repack();
		w.setAlwaysOnTop(true);
		return w;
		
	}
	
	
	public static BasicWindow showImage(Image img,String title){
		
	
		BasicWindow w= new BasicWindow();
		ImageComponent ic=new ImageComponent(img);
		
		w.setSize(ic.getImageWidth()+10, ic.getImageHeight()+20);
		w.setLocationByScreenPercent(50, 50);
		w.setTitle(title);
		w.setLayout(new GridBagLayout());
		w.add(ic, SWING.getGBC(0,0,1, 1));
		w.setVisible(true);
		w.pack();
		w.repack();
		w.setAlwaysOnTop(true);
		return w;
		
	}
}