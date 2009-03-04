//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.controlling.reconnect.ReconnectMethod;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDRRGui extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnStart;

    private JTextField routerip;
    public boolean saved = false;
    private String ip_before;
    private String ip_after;
    public String RouterIP = null;
    private JButton btnStop;
    public String methode = null, user = null, pass = null;
    private static long check_intervall = 3000;
    private static long reconnect_duration = 0;
    private JFrame frame;

    public JDRRGui(JFrame frame, String ip) {
        super(frame);
        this.frame = frame;
        RouterIP = ip;
        int n = 10;
        this.setTitle(JDLocale.L("gui.config.jdrr.title", "Reconnect Recorder"));
        routerip = new JTextField(RouterIP);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        btnStart = new JButton(JDLocale.L("gui.btn_start", "Start"));
        btnStart.addActionListener(this);
        JTextPane infolable = new JTextPane();
        infolable.setEditable(false);
        infolable.setContentType("text/html");
        infolable.addHyperlinkListener(JLinkButton.getHyperlinkListener());
        infolable.setText(JDLocale.L("gui.config.jdrr.infolable", "<span color=\"#4682B4\">Überprüfe die IP-Adresse des Routers und drück auf Start,<br>ein Browserfenster mit der Startseite des Routers öffnet sich,<br>nach dem Reconnect drückst du auf Stop und speicherst.<br>Mehr Informationen gibt es </span><a href=\"http://wiki.jdownloader.org/index.php?title=Recorder\">hier</a>"));
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnCancel);
        bpanel.add(btnStart);

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
        if (new CountdownConfirmDialog(this.frame, JDLocale.L("gui.config.jdrr.savereconnect", "Der Reconnect war erfolgreich möchten sie jetzt speichern?"), 10, true, CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO).result) {

            Configuration configuration = JDUtilities.getConfiguration();

            StringBuilder b = new StringBuilder();
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
            configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText().trim());
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, methode);
            configuration.setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, "Reconnect Recorder Methode");
            configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
            if (reconnect_duration <= 2000) {
                reconnect_duration = 2000;
                /* minimum von 2 seks */
            }
            configuration.setProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, ((reconnect_duration / 1000) * 2) + 10);
            configuration.setProperty(ReconnectMethod.PARAM_IPCHECKWAITTIME, ((reconnect_duration / 1000) / 2) + 2);
            configuration.save();
            saved = true;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnStart && JDRR.running == false) {
            if (routerip.getText() != null && !routerip.getText().matches("\\s*")) JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText().trim());

            ip_before = JDUtilities.getIPAddress(null);
            JDRR.startServer(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null));

            try {
                JLinkButton.openURL("http://localhost:" + (JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972)));

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            JDRRInfoPopup popup = new JDRRInfoPopup(ip_before);
            popup.start_check();
            popup.setVisible(true);
            return;
        }
        dispose();
    }

    public class JDRRInfoPopup extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;
        private long reconnect_timer = 0;
        private RRStatus statusicon;

        public JDRRInfoPopup(String ipbefore) {
            super();
            setModal(true);
            setLayout(new GridBagLayout());
            JPanel p = new JPanel(new GridBagLayout());
            btnStop = new JButton(JDLocale.L("gui.btn_abort", "Abort"));
            btnStop.addActionListener(this);
            statusicon = new RRStatus();
            JDUtilities.addToGridBag(p, statusicon, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.NORTH);
            JDUtilities.addToGridBag(p, btnStop, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.SOUTH);
            JDUtilities.addToGridBag(this, p, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            setResizable(false);
            setUndecorated(false);
            setTitle("RRStatus");
            setLocation(20, 20);
            setAlwaysOnTop(true);
            pack();
        }

        public void start_check() {
            new Thread() {
                public void run() {
                    statusicon.setStatus(0);
                    this.setName(JDLocale.L("gui.config.jdrr.popup.title", "JDRRPopup"));
                    reconnect_timer = 0;
                    while (JDRR.running) {
                        try {
                            Thread.sleep(check_intervall);
                        } catch (Exception e) {
                        }
                        ip_after = JDUtilities.getIPAddress(null);
                        if (ip_after.contains("offline") && reconnect_timer == 0) {
                            reconnect_timer = System.currentTimeMillis();
                        }
                        if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                            statusicon.setStatus(1);
                            if (JDRR.running == true) closePopup();
                            return;
                        }
                    }
                }
            }.start();
        }

        public class RRStatus extends JComponent {

            private static final long serialVersionUID = -3280613281656283625L;

            private int status = 0;

            private Image imageProgress;

            private Image imageBad;

            private Image imageGood;

            public RRStatus() {
                status = 0;
                imageProgress = JDUtilities.getImage(JDTheme.V("gui.images.reconnect"));
                imageBad = JDUtilities.getImage(JDTheme.V("gui.images.reconnect_bad"));
                imageGood = JDUtilities.getImage(JDTheme.V("gui.images.reconnect_ok"));
                setPreferredSize(new Dimension(32, 32));
            }

            public int getImageHeight() {
                return imageGood.getHeight(this);
            }

            public int getImageWidth() {
                return imageGood.getWidth(this);
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
            JDRR.stopServer();
            btnStop.setEnabled(false);
            ip_after = JDUtilities.getIPAddress(null);
            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                if (reconnect_timer == 0) {
                    /*
                     * Reconnect fand innerhalb des Check-Intervalls statt
                     */
                    reconnect_duration = check_intervall;
                } else {
                    reconnect_duration = System.currentTimeMillis() - reconnect_timer;
                }
                JDUtilities.getLogger().info("dauer: " + reconnect_duration + "");
                statusicon.setStatus(1);
            } else {
                statusicon.setStatus(-1);
            }
            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                save();
            } else {
                // save(); /*zu debugzwecken*/
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
            }
            dispose();
        }

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource() == btnStop) {
                closePopup();
            }
        }
    }

}