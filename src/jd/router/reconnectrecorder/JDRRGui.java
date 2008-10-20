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

package jd.router.reconnectrecorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.MalformedURLException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.router.RouterInfoCollector;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDRRGui extends JDialog implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnStartStop;

    private String script;

    private void setScript(String script) {
        this.script = script;
    }

    private void appendScript(String script) {
        this.script += script;
    }

    private JTextField routerip;
    public boolean saved = false;
    private String ip_before;
    private String ip_after;
    public String RouterIP = null;
    private JButton btnStop;
    private JDRRInfoPopup infopopup;
    public String methode = null, user = null, pass = null;
    private boolean isSaveMSG =false;
    public JDRRGui(JFrame frame, String ip) {
        super(frame);
        RouterIP = ip;
        int n = 10;
        this.setTitle(JDLocale.L("gui.config.jdrr.title", "Reconnect Recorder"));
        addWindowListener(this);
        if (RouterIP == null) RouterIP = RouterInfoCollector.getRouterIP();

        routerip = new JTextField(RouterIP);
        // routerip.setHorizontalAlignment(SwingConstants.RIGHT);
        // routerip.setText(RouterIP);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);
        btnCancel.setEnabled(false);

        btnStartStop = new JButton(JDLocale.L("gui.btn_start", "Start"));
        btnStartStop.addActionListener(this);
        JTextPane infolable = new JTextPane();
        infolable.setEditable(false);
        infolable.setContentType("text/html");
        infolable.addHyperlinkListener(JLinkButton.getHyperlinkListener());
        infolable.setText(JDLocale.L("gui.config.jdrr.infolable", "<span color=\"#4682B4\">Überprüfe die IP-Adresse des Routers und drück auf Start,<br>ein Browserfenster mit der Startseite des Routers öffnet sich,<br>nach dem Reconnect drückst du auf Stop und speicherst.<br>Mehr Informationen gibt es </span><a href=\"http://wiki.jdownloader.org/index.php?title=Recorder\">hier</a>"));
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnCancel);
        bpanel.add(btnStartStop);

        JPanel spanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        spanel.add(new JLabel(JDLocale.L("gui.fengshuiconfig.routerip", "RouterIP") + ":"));

        c.weightx = 100;

        c.fill = GridBagConstraints.BOTH;
        spanel.add(routerip, c);

        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(spanel, BorderLayout.NORTH);
        panel.add(infolable, BorderLayout.CENTER);
        panel.add(bpanel, BorderLayout.SOUTH);
        this.setContentPane(panel);
        this.pack();
        this.setLocation(JDUtilities.getCenterOfComponent(null, this));
    }

    private void save() {
        if (ip_after.equalsIgnoreCase(ip_before) || isSaveMSG) { return; }
        isSaveMSG=true;
        if (new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.L("gui.config.jdrr.savereconnect", "Der Reconnect war erfolgreich möchten sie jetzt speichern?"), 10, true, CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO).result) {

            saved = true;
            Configuration configuration = JDUtilities.getConfiguration();

            StringBuffer b = new StringBuffer();
            for (String element : JDRR.steps) {
                b.append(element + System.getProperty("line.separator"));
            }
            methode = b.toString().trim();

            if (JDRR.auth != null) {
                user = new Regex(JDRR.auth, "(.+?):").getMatch(0);
                pass = new Regex(JDRR.auth, ".+?:(.+)").getMatch(0);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, user);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, pass);
            }
            btnCancel.setText(JDLocale.L("gui.config.jdrr.close", "Schließen"));
            configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText());
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, methode);
            configuration.setProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));
            JDUtilities.saveConfig();
        }

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnStop || e.getSource() == btnStartStop && btnStartStop.getText().contains("Stop")) {
            ip_after = JDUtilities.getIPAddress();
            JDRR.running = false;
            JDRR.stopServer();
            if (infopopup != null) {
                infopopup.dispose();
                infopopup = null;
            }

            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                setScript("");
                for (String element : JDRR.steps) {
                    appendScript(element + System.getProperty("line.separator"));
                }
                save();
            } else {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
            }
            btnStartStop.setText(JDLocale.L("gui.btn_start", "Start"));
            return;
        } else if (e.getSource() == btnStartStop && btnStartStop.getText().contains("Start")) {
            if (routerip.getText() != null && !routerip.getText().matches("\\s*")) JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText());
            ip_before = JDUtilities.getIPAddress();
            setScript("");
            JDRR.startServer(RouterInfoCollector.getRouterIP());
            infopopup = new JDRRInfoPopup(ip_before);
            infopopup.setVisible(true);
            btnCancel.setEnabled(true);
            btnStartStop.setText(JDLocale.L("gui.btn_stop", "Stop"));
            try {
                JLinkButton.openURL("http://localhost:" + JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972));
            } catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return;
        }
        close();
    }

    private void close() {
        JDRR.gui = false;
        JDRR.running = false;
        JDRR.stopServer();
        dispose();

    }

    public class JDRRInfoPopup extends JDialog implements ActionListener {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        RRStatus statusicon;

        public JDRRInfoPopup(String ipbefore) {
            super();
            setModal(false);
            setLayout(new GridBagLayout());
            JPanel p = new JPanel(new GridBagLayout());
            btnStop = new JButton(JDLocale.L("gui.btn_stop", "Stop"));
            btnStop.addActionListener(this);
            statusicon = new RRStatus();
            JDUtilities.addToGridBag(p, statusicon, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.NORTH);
            JDUtilities.addToGridBag(p, btnStop, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.SOUTH);
            JDUtilities.addToGridBag(this, p, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setSize(50, 70);
            setResizable(false);
            setUndecorated(false);
            setTitle("RRStatus");
            setLocation(20, 20);
            setAlwaysOnTop(true);

            pack();
        }

        public class RRStatus extends JComponent {
            /**
             * 
             */
            private static final long serialVersionUID = -3280613281656283625L;

            private int status;

            private Image imageProgress;

            private Image imageBad;

            private Image imageGood;

            public RRStatus() {
                status = 0;
                imageProgress = JDUtilities.getImage(JDTheme.V("gui.images.reconnect"));
                imageBad = JDUtilities.getImage(JDTheme.V("gui.images.reconnect_bad"));
                imageGood = JDUtilities.getImage(JDTheme.V("gui.images.reconnect_ok"));

                setPreferredSize(new Dimension(imageGood.getWidth(null), imageGood.getHeight(null)));

                new Thread() {
                    public void run() {
                        this.setName(JDLocale.L("gui.config.jdrr.popup.title", "JDRRPopup"));
                        while (JDRR.running) {
                            try {
                                Thread.sleep(5000);
                            } catch (Exception e) {
                            }
                            ip_after = JDUtilities.getIPAddress();
                            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                                setStatus(1);
                                closePopup();
                                break;
                            }
                        }
                    }
                }.start();
                setStatus(0);
            }

            public int getImageHeight() {
                return imageProgress.getHeight(this);

            }

            public int getImageWidth() {
                return imageProgress.getWidth(this);
            }

            @Override
            public void paintComponent(Graphics g) {
                if (status == 0) {
                    g.drawImage(imageProgress, 0, 0, null);
                } else if (status == 1) {
                    g.drawImage(imageGood, 0, 0, null);
                } else {
                    g.drawImage(imageBad, 0, 0, null);
                }
            }

            public void setStatus(int state) {
                status = state;
                repaint();
            }

        }

        public void closePopup() {
            btnStop.setEnabled(false);
            ip_after = JDUtilities.getIPAddress();
            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                statusicon.setStatus(1);
            } else {
                statusicon.setStatus(-1);
            }
            JDRR.running = false;
            JDRR.stopServer();
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                    }
                    if (infopopup != null) infopopup.dispose();
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        setScript("");
                        for (String element : JDRR.steps) {
                            appendScript(element + System.getProperty("line.separator"));
                        }
                        save();
                    } else {
                        JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
                    }
                    btnStartStop.setText(JDLocale.L("gui.btn_start", "Start"));
                    dispose();
                }
            }.start();
        }

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource() == btnStop) {
                closePopup();
            }
        }
    }

    public void windowActivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowClosed(WindowEvent e) {
        close();

    }

    public void windowClosing(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowDeactivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowDeiconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowIconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub

    }
}