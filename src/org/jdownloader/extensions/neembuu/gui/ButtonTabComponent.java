/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

import org.jdownloader.extensions.neembuu.DownloadSession;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 */
public class ButtonTabComponent extends JPanel {
    /**
	 * 
	 */
    private static final long       serialVersionUID = 1672135537070393545L;
    private final JTabbedPane       pane;
    private final DownloadSession jdds;
    private final NeembuuGui        neembuuGui;

    public ButtonTabComponent(final JTabbedPane pane, DownloadSession jdds, NeembuuGui neembuuGui) {
        // unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.jdds = jdds;
        this.neembuuGui = neembuuGui;
        if (pane == null) { throw new NullPointerException("TabbedPane is null"); }
        this.pane = pane;
        setOpaque(false);

        // make JLabel read titles from JTabbedPane
        JLabel label = new JLabel() {
            /**
			 * 
			 */
            private static final long serialVersionUID = -946007321758384598L;

            public String getText() {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) { return pane.getTitleAt(i); }
                return null;
            }
        };

        add(/* jdds.getWatchAsYouDownloadSession().getFilePanel() */label);
        // add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        // tab button
        JButton button = new TabButton();
        add(button);
        // add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    private class TabButton extends JButton implements ActionListener {
        /**
		 * 
		 */
        private static final long serialVersionUID = 3088354333032139734L;

        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Close and unmount");
            // Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            // Make it transparent
            setContentAreaFilled(false);
            // No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            // Making nice rollover effect
            // we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            // Close the proper tab by clicking the button
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int ret = JOptionPane.showConfirmDialog(null, jdds.toString(), "Are you sure you want to unmount?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                try {
                    jdds.getWatchAsYouDownloadSession().unMount();
                } catch (Exception a) {

                }
                try {
                    neembuuGui.removeSession(jdds);
                } catch (Exception a) {

                }
            }
            /*
             * int i = pane.indexOfTabComponent(ButtonTabComponent.this); if (i
             * != -1) { pane.remove(i);
             * 
             * }
             */
        }

        // we don't want to update UI for this button
        public void updateUI() {
        }

        // paint the cross
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
                                                               public void mouseEntered(MouseEvent e) {
                                                                   Component component = e.getComponent();
                                                                   if (component instanceof AbstractButton) {
                                                                       AbstractButton button = (AbstractButton) component;
                                                                       button.setBorderPainted(true);
                                                                   }
                                                               }

                                                               public void mouseExited(MouseEvent e) {
                                                                   Component component = e.getComponent();
                                                                   if (component instanceof AbstractButton) {
                                                                       AbstractButton button = (AbstractButton) component;
                                                                       button.setBorderPainted(false);
                                                                   }
                                                               }
                                                           };
}
