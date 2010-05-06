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

package jd.nrouter.recorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectMethod;
import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AbstractDialog;
import jd.nrouter.IPCheck;
import jd.nutils.JDFlags;
import jd.parser.Regex;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class Gui extends AbstractDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextField routerip;
    private JCheckBox rawmode;
    public boolean saved = false;
    private String ip_before;
    private String ip_after;
    public String ip = null;
    public String methode = null;
    public String user = null;
    public String pass = null;
    private static long check_intervall = 5000;
    private static long reconnect_duration = 0;

    public Gui(final String ip) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("gui.config.jdrr.title", "Reconnect Recorder"), null, JDL.L("gui.btn_start", "Start"), JDL.L("gui.btn_cancel", "Cancel"));
        this.ip = ip;
        init();
    }

    public JComponent contentInit() {
        routerip = new JTextField(ip);

        rawmode = new JCheckBox(JDL.L("gui.config.jdrr.rawmode", "RawMode?"));
        rawmode.setSelected(false);

        final JTextPane infolable = new JTextPane();
        infolable.setEditable(false);
        infolable.setContentType("text/html");
        infolable.addHyperlinkListener(JLink.getHyperlinkListener());
        infolable.setText(JDL.L("gui.config.jdrr.infolabel", "<span color=\"#4682B4\"> Check the IP address of the router and press the Start <br> Web browser window with the home page of the router opens <br> Reconnection after you hit to stop and save. <br> More information is available </span> <a href=\"http://jdownloader.org/knowledge/wiki/reconnect/reconnect-recorder\"> here</a>"));

        final JPanel panel = new JPanel(new MigLayout("wrap 1", "[center]"));
        panel.add(new JLabel(JDL.L("gui.fengshuiconfig.routerip", "RouterIP") + ":"), "split 3");
        panel.add(routerip, "growx");
        panel.add(rawmode);
        panel.add(infolable, "growx");
        return panel;
    }

    private void save() {
        final int ret = UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.config.jdrr.success", "Success!"), JDL.L("gui.config.jdrr.savereconnect", "Reconnection was successful. Save now?"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
        if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {

            final Configuration configuration = JDUtilities.getConfiguration();

            final StringBuilder b = new StringBuilder();
            for (final String element : ReconnectRecorder.steps) {
                b.append(element + System.getProperty("line.separator"));
            }
            methode = b.toString().trim();

            if (ReconnectRecorder.auth != null) {
                user = new Regex(ReconnectRecorder.auth, "(.+?):").getMatch(0);
                pass = new Regex(ReconnectRecorder.auth, ".+?:(.+)").getMatch(0);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, user);
                configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, pass);
            }
            btnCancel.setText(JDL.L("gui.btn_close", "Close"));
            configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText().trim());
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, methode);
            configuration.setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, "Reconnect Recorder Method");
            configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
            if (reconnect_duration <= 2000) {
                reconnect_duration = 2000;
                /* minimum von 2 seks */
            }
            int aa = (int) ((reconnect_duration / 1000) * 2);
            if (aa < 30) {
                aa = 30;
            }
            int ab = (int) ((reconnect_duration / 1000) / 2);
            if (ab < 30) {
                ab = 5;
            }
            configuration.setProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, aa);
            configuration.setProperty(ReconnectMethod.PARAM_IPCHECKWAITTIME, ab);
            configuration.save();
            saved = true;
            dispose();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK && ReconnectRecorder.running == false) {
            if (routerip.getText() != null && !routerip.getText().matches("\\s*")) {
                String host = routerip.getText().trim();
                boolean startwithhttps = false;
                if (host.contains("https")) startwithhttps = true;
                host = host.replaceAll("http://", "").replaceAll("https://", "");
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, host);

                ip_before = IPCheck.getIPAddress();
                ReconnectRecorder.startServer(host, rawmode.isSelected());

                try {
                    if (startwithhttps) {
                        JLink.openURL("http://localhost:" + (SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + 1));
                    } else {
                        JLink.openURL("http://localhost:" + (SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972)));
                    }
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
                new JDRRInfoPopup();
                return;
            }
        }
        dispose();
    }

    public class JDRRInfoPopup extends AbstractDialog implements ActionListener {

        private static final long serialVersionUID = 1L;
        private long reconnect_timer = 0;
        private RRStatus statusicon;

        public JDRRInfoPopup() {
            super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON | UserIO.NO_OK_OPTION, JDL.L("gui.config.jdrr.status.title", "Recording Status"), null, null, JDL.L("gui.btn_abort", "Abort"));

            init();
        }

        public JComponent contentInit() {
            return statusicon = new RRStatus();
        }

        @Override
        public void packed() {
            remove(countDownLabel);
            pack();
            setMinimumSize(null);
            setResizable(false);
            setDefaultCloseOperation(AbstractDialog.DO_NOTHING_ON_CLOSE);

            startCheck();
        }

        public void startCheck() {
            new Thread() {
                public void run() {
                    statusicon.setStatus(0);
                    this.setName(JDL.L("gui.config.jdrr.popup.title", "JDRRPopup"));
                    reconnect_timer = 0;
                    while (ReconnectRecorder.running) {
                        try {
                            Thread.sleep(check_intervall);
                        } catch (Exception e) {
                        }
                        ip_after = IPCheck.getIPAddress();
                        if (ip_after.contains("na") && reconnect_timer == 0) {
                            reconnect_timer = System.currentTimeMillis();
                        }
                        if (!ip_after.contains("na") && !ip_after.equalsIgnoreCase(ip_before)) {
                            statusicon.setStatus(1);
                            if (ReconnectRecorder.running == true) closePopup();
                            return;
                        }
                    }
                }
            }.start();
        }

        public class RRStatus extends JLabel {

            private static final long serialVersionUID = -3280613281656283625L;

            private ImageIcon imageProgress;

            private ImageIcon imageBad;

            private ImageIcon imageGood;

            private String strProgress;

            private String strBad;

            private String strGood;

            public RRStatus() {
                imageProgress = JDTheme.II("gui.images.reconnect", 32, 32);
                imageBad = JDTheme.II("gui.images.unselected", 32, 32);
                imageGood = JDTheme.II("gui.images.selected", 32, 32);
                strProgress = JDL.L("jd.router.reconnectrecorder.Gui.icon.progress", "Recording Reconnect ...");
                strBad = JDL.L("jd.router.reconnectrecorder.Gui.icon.bad", "Error while recording the Reconnect!");
                strGood = JDL.L("jd.router.reconnectrecorder.Gui.icon.good", "Reconnect successfully recorded!");
                setStatus(0);
            }

            public void setStatus(int state) {
                if (state == 0) {
                    this.setIcon(imageProgress);
                    this.setText(strProgress);
                } else if (state == 1) {
                    this.setIcon(imageGood);
                    this.setText(strGood);
                } else {
                    this.setIcon(imageBad);
                    this.setText(strBad);
                }
            }
        }

        public void closePopup() {
            ReconnectRecorder.stopServer();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    btnCancel.setEnabled(false);

                    ip_after = IPCheck.getIPAddress();
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        if (reconnect_timer == 0) {
                            /*
                             * Reconnect fand innerhalb des Check-Intervalls
                             * statt
                             */
                            reconnect_duration = check_intervall;
                        } else {
                            reconnect_duration = System.currentTimeMillis() - reconnect_timer;
                        }
                        JDLogger.getLogger().info("dauer: " + reconnect_duration);
                        statusicon.setStatus(1);
                    } else {
                        statusicon.setStatus(-1);
                    }
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        save();
                    } else {
                        UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
                    }

                    dispose();
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource() == btnCancel) {
                closePopup();
            }
        }
    }

}