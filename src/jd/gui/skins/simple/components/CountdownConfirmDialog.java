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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class CountdownConfirmDialog extends JDialog implements ActionListener {

    /**
     * Bestätigungsknopf
     */
    private JButton btnOK;

    private JButton btnBAD;

    private Thread countdownThread;

    private JTextPane htmlArea;

    private JScrollPane scrollPane;

    private boolean result = false;

    private JButton btnCnTh;

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    public CountdownConfirmDialog(final Frame owner, final String msg, final int countdown) {
        super(owner);

        setModal(true);

        setLayout(new GridBagLayout());

        this.countdownThread = new Thread() {

            public void run() {
                int c = countdown;

                while (--c >= 0) {
                    if(countdownThread==null)return;
                    setTitle(JDUtilities.formatSeconds(c)+" mm:ss");
                
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) return;

                }

                dispose();

            }

        };
        this.countdownThread.start();

        htmlArea = new JTextPane();
        scrollPane = new JScrollPane(htmlArea);
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(msg);
        htmlArea.requestFocusInWindow();

        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnCnTh= new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
        btnOK.addActionListener(this);
        btnBAD.addActionListener(this);
        btnCnTh.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        //setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

         setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JDUtilities.addToGridBag(this, scrollPane, 0, 0, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        
        JDUtilities.addToGridBag(this, btnCnTh, 0, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JDUtilities.addToGridBag(this, btnOK, 1, 2, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnBAD, 2, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==btnCnTh){
          
                countdownThread=null;
          
        }
        
        if (e.getSource() == btnOK) {
            this.result = true;
            setVisible(false);
            dispose();
        }
        if (e.getSource() == btnBAD) {

            setVisible(false);
            dispose();
        }

        if (countdownThread != null && countdownThread.isAlive()) this.countdownThread.interrupt();
        countdownThread=null;
    }

    public static boolean showCountdownConfirmDialog(Frame owner, String msg, int countdown) {
        CountdownConfirmDialog d = new CountdownConfirmDialog(owner, msg, countdown);
    
        return d.result;
    }

    public static void main(String[] args) {
        showCountdownConfirmDialog(new JFrame(), "<h2>test</h2>", 10);

    }
}
