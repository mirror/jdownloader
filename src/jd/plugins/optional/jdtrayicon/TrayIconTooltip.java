package jd.plugins.optional.jdtrayicon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import jd.controlling.DownloadController;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class TrayIconTooltip {
    private JWindow toolParent;
    private JPanel toolPanel;
    private TrayInfo trayInfo;
    
    private JLabel lblSpeed;
    private JLabel lblDlRunning;
    private JLabel lblDlFinished;
    private JLabel lblDlTotal;
    private JDProgressBar prgTotal;
    private JLabel lblETA;
    private JLabel lblProgress;
    
    private int counter = 0;
    private boolean inside = false;
    
    public TrayIconTooltip() {
    	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				toolPanel = new JPanel();
				toolPanel.setLayout(new MigLayout("wrap 2", "[fill, grow][fill, grow]"));
				toolPanel.setVisible(true);
				toolPanel.setOpaque(true);
				toolPanel.setBackground(new Color(0xb9cee9));
				toolPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, toolPanel.getBackground().darker()));
				
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.downloads", "Downloads:")), "spanx 2");
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.dl.running", "Running:")), "gapleft 10");
				toolPanel.add(lblDlRunning = new JLabel(""));
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.dl.finished", "Finished:")), "gapleft 10");
				toolPanel.add(lblDlFinished = new JLabel(""));
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.dl.total", "Total:")), "gapleft 10");
				toolPanel.add(lblDlTotal = new JLabel(""));
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.speed", "Speed:")));
				toolPanel.add(lblSpeed = new JLabel(""));
				toolPanel.add(lblProgress = new JLabel(""), "newline, spanx 2");
				toolPanel.add(prgTotal = new JDProgressBar(), "spanx 2");
				toolPanel.add(new JLabel(JDLocale.L("plugins.optional.trayIcon.eta", "ETA:")));
				toolPanel.add(lblETA = new JLabel(""));
				toolPanel.addMouseListener(new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent arg0) {}

					@Override
					public void mouseEntered(MouseEvent arg0) {
						inside = true;
					}

					@Override
					public void mouseExited(MouseEvent arg0) {
						inside = false;
					}

					@Override
					public void mousePressed(MouseEvent arg0) {}

					@Override
					public void mouseReleased(MouseEvent arg0) {}
				});

		        toolParent = new JWindow();
		        toolParent.setAlwaysOnTop(true);
		        toolParent.add(toolPanel);
		        toolParent.pack();
		        toolParent.setVisible(false);
			}
		});
    }
    
    public void show(MouseEvent e) {
    	if (counter > 0) {
            counter = 2;
            return;
        }
        counter = 2;
        if(trayInfo != null) trayInfo.interrupt();
        trayInfo = new TrayInfo(e.getPoint());
        trayInfo.start();
    }
	
	public void hide() {
        toolParent.setVisible(false);
        counter = 0;
        inside = false;
    }

	 private void calcLocation(final JWindow window, final Point p) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;

                if (p.x <= limitX) {
                    if (p.y <= limitY)
                        window.setLocation(p.x, p.y);
                    else
                        window.setLocation(p.x, p.y - window.getHeight());
                } else {
                    if (p.y <= limitY)
                        window.setLocation(p.x - window.getWidth(), p.y);
                    else
                        window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                }
            }
        });
    }
	 
	 private class TrayInfo extends Thread {
        private Point p;

        public TrayInfo(Point p) {
            this.p = p;
        }

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                interrupt();
            }
            
            toolParent.pack();
            calcLocation(toolParent, p);
            toolParent.setVisible(true);
            toolParent.toFront();
            
            final DownloadController dlc = JDUtilities.getDownloadController();

            while ((inside || counter > 0) && toolParent.isVisible()) {
            	SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						
		            	long tot = 0;
		                long loaded = 0;
		                int finished = 0;
		                for (DownloadLink l : dlc.getAllDownloadLinks()) {
		                    tot += l.getDownloadSize();
		                    loaded += l.getDownloadCurrent();
		                    
		                    if(tot == loaded) finished++;
		                }
		                
		                lblDlRunning.setText(String.valueOf(JDUtilities.getController().getRunningDownloadNum()));
		                lblDlFinished.setText(String.valueOf(finished));
		                lblDlTotal.setText(String.valueOf(dlc.getAllDownloadLinks().size()));
		            	lblSpeed.setText(JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s");
		                
		                lblProgress.setText(JDLocale.L("plugins.optional.trayIcon.progress", "Progress: ") +  JDUtilities.formatFilesize(loaded, 0) + " / " + JDUtilities.formatFilesize(tot, 0));
		                prgTotal.setMaximum(tot);
		                prgTotal.setValue(loaded);
		                
		                long etanum = 0;
		                if(JDUtilities.getController().getSpeedMeter() > 1024)
		                	etanum = (tot - loaded) / JDUtilities.getController().getSpeedMeter();
		                
		                lblETA.setText(JDUtilities.formatSeconds(etanum));
		                
		                toolParent.pack();
					}});
            	
            	if(!inside)
            		counter--;
            	
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hide();
        }
    }
}
