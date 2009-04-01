package jd.gui.skins.simple;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.Timer;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.javaone.ZoomyPanel;

public class Tunes4 {
	
	private static final Tunes4 INSTANCE = new Tunes4();
	public static Tunes4 getINSTANCE() {return INSTANCE;}
	
	private CardLayout viewCardLayout;
	private JXCollapsiblePane cardPane;
	private ZoomyPanel zoomyPanel;
	private JButton viewButton;
	private JButton switchViewButton;
	
	protected boolean collapse;
	
	
	public void showLastPanel() {
		viewCardLayout.last(cardPane.getContentPane());
	}

	private Tunes4() {
		viewCardLayout = new CardLayout();
		cardPane = new JXCollapsiblePane();
		
		
		// ABSOLUT NOTWENDIG - nicht in new JXCollapsiblePane(viewCardLayout) setzen !!!
		cardPane.setLayout(viewCardLayout); 
		
		zoomyPanel = createZoomyPanel();
		cardPane.add(zoomyPanel, "Zoomy");

		collapse = false;
		cardPane.setCollapsed(collapse);
		
		// Buttons can be accessed by GETTERS
		createControlButtons();
	}

	private void createControlButtons() {
		viewButton = new JButton("Zoom!");
		viewButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				collapse = !collapse;
				cardPane.setCollapsed(collapse);
			}
		});
		
		switchViewButton = new JButton("Chill");
		switchViewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (!cardPane.isCollapsed()) {
					viewCardLayout.next(cardPane.getContentPane());
				}
			}
		});
	}
	
    private ZoomyPanel createZoomyPanel() {
        zoomyPanel = new ZoomyPanel();
        zoomyPanel.addComponentListener(new ComponentListener() {
            private Timer timer = null;

            public void componentResized(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
                if (timer != null) {
                    timer.stop();
                }
                timer = new Timer(33, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        zoomyPanel.repaint();
                    }
                });
                zoomyPanel.init();
                timer.start();
            }

            public void componentHidden(ComponentEvent e) {
                if (timer != null) {
                    timer.stop();
                    timer = null;
                }
            }
        });
        //musicContents.setAlpha(.5f);
        return zoomyPanel;
    }

	public JXCollapsiblePane getCardPane() {
		return cardPane;
	}

	public JButton getSwitchViewButton() {
		return switchViewButton;
	}

	public JButton getViewButton() {
		return viewButton;
	}

	public CardLayout getViewCardLayout() {
		return viewCardLayout;
	}

	public ZoomyPanel getZoomyPanel() {
		return zoomyPanel;
	}

}
