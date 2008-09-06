//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.components;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ClickPositionDialog extends JDialog implements ActionListener, HyperlinkListener, MouseListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    public static void main(String[] args) {
        ClickPositionDialog d = new ClickPositionDialog(new JFrame(), JDUtilities.getResourceFile("captchas\\xup.in\\05.09.2008_19.40.19.39.jpg"), "Imageclick", "klick any item", 20, new Point( -1, -1 ));

    }

    private JButton btnBAD;
    private JButton btnCnTh;
    /**
     * Bestätigungsknopf
     */

    private Thread countdownThread;

    private Component htmlArea;
    public Point result =new Point(-1,-1);
    private JScrollPane scrollPane;

    private String titleText;
    private JLabel button;

    public ClickPositionDialog(final Frame owner, final File image, final String title, final String msg, final int countdown, final Point defaultResult) {
        super(owner);
        setModal(true);

        setLayout(new GridBagLayout());

        countdownThread = new Thread() {

            @Override
            public void run() {

                while (!isVisible() && isDisplayable()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) { return; }
                    if (titleText != null) {

                        setTitle(JDUtilities.formatSeconds(c) + " mm:ss  >> " + titleText);
                    } else {
                        setTitle(JDUtilities.formatSeconds(c) + " mm:ss");
                    }

                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) {

                    return; }

                }
                result = defaultResult;
                dispose();

            }

        };
        this.titleText = title;

        if (title != null) {
            this.setTitle(title);
        }

        ImageIcon imageIcon = new ImageIcon(image.getAbsolutePath());

        button = new JLabel(imageIcon);
    button.addMouseListener(this);
    button.setToolTipText(msg);
  //      button.setToolTipText(msg);
        JDUtilities.addToGridBag(this, button, 0, 0, 3, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);

        if (msg != null) {
            htmlArea = new JTextPane();
            ((JTextPane) htmlArea).setEditable(false);
            ((JTextPane) htmlArea).setContentType("text/html");
            ((JTextPane) htmlArea).setText(msg);
            ((JTextPane) htmlArea).requestFocusInWindow();
            ((JTextPane) htmlArea).addHyperlinkListener(this);

            scrollPane = new JScrollPane(htmlArea);
            JDUtilities.addToGridBag(this, scrollPane, 0, 1, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        }

        int d = 0;

        btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
        btnCnTh.addActionListener(this);
        JDUtilities.addToGridBag(this, btnCnTh, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnBAD.addActionListener(this);
        JDUtilities.addToGridBag(this, btnBAD, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
this.setAlwaysOnTop(true);
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        countdownThread.start();
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {

            countdownThread = null;

        } else if (e.getSource() == btnBAD) {
            setVisible(false);
            dispose();
        }
if(e.getSource()==button){
    
   
}
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
        countdownThread = null;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

            JLinkButton.openURL(e.getURL());

        }

    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseReleased(MouseEvent e) {
     
    this.result= e.getPoint();
    setVisible(false);
    dispose();
    
        
    }
}
