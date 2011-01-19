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

package jd.controlling.reconnect.plugins.liveheader.recorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnect;
import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Regex;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class Gui extends AbstractDialog<Object> {

    public class JDRRInfoPopup extends AbstractDialog<Object> {

        public class RRStatus extends JLabel {

            private static final long serialVersionUID = -3280613281656283625L;

            private final ImageIcon   imageProgress;

            private final ImageIcon   imageBad;

            private final ImageIcon   imageGood;

            private final String      strProgress;

            private final String      strBad;

            private final String      strGood;

            public RRStatus() {
                this.imageProgress = JDTheme.II("gui.images.reconnect", 32, 32);
                this.imageBad = JDTheme.II("gui.images.unselected", 32, 32);
                this.imageGood = JDTheme.II("gui.images.selected", 32, 32);
                this.strProgress = JDL.L("jd.router.reconnectrecorder.Gui.icon.progress", "Recording Reconnect ...");
                this.strBad = JDL.L("jd.router.reconnectrecorder.Gui.icon.bad", "Error while recording the Reconnect!");
                this.strGood = JDL.L("jd.router.reconnectrecorder.Gui.icon.good", "Reconnect successfully recorded!");
                this.setStatus(0);
            }

            public void setStatus(final int state) {
                if (state == 0) {
                    this.setIcon(this.imageProgress);
                    this.setText(this.strProgress);
                } else if (state == 1) {
                    this.setIcon(this.imageGood);
                    this.setText(this.strGood);
                } else {
                    this.setIcon(this.imageBad);
                    this.setText(this.strBad);
                }
            }
        }

        private static final long serialVersionUID = 1L;
        private long              reconnect_timer  = 0;

        private RRStatus          statusicon;

        public JDRRInfoPopup() {
            super(UserIO.NO_ICON | UserIO.NO_OK_OPTION, JDL.L("gui.config.jdrr.status.title", "Recording Status"), null, null, JDL.L("gui.btn_abort", "Abort"));

        }

        public void closePopup() {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JDRRInfoPopup.this.cancelButton.setEnabled(false);

                    if (IPController.getInstance().validate()) {
                        if (JDRRInfoPopup.this.reconnect_timer == 0) {
                            /*
                             * Reconnect fand innerhalb des Check-Intervalls
                             * statt
                             */
                            Gui.RECONNECT_DURATION = Gui.CHECK_INTERVAL;
                        } else {
                            Gui.RECONNECT_DURATION = System.currentTimeMillis() - JDRRInfoPopup.this.reconnect_timer;
                        }
                        JDLogger.getLogger().info("dauer: " + Gui.RECONNECT_DURATION);
                        JDRRInfoPopup.this.statusicon.setStatus(1);
                    } else {
                        JDRRInfoPopup.this.statusicon.setStatus(-1);
                    }
                    if (IPController.getInstance().validate()) {
                        Gui.this.save();
                    } else {
                        UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.jdrr.reconnectfaild", "Reconnect failed"));
                    }

                    JDRRInfoPopup.this.dispose();
                }
            });
        }

        @Override
        protected Object createReturnValue() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public JComponent layoutDialogContent() {
            return this.statusicon = new RRStatus();
        }

        @Override
        public void packed() {
            this.setMinimumSize(null);
            this.setResizable(false);
            this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            this.startCheck();
        }

        protected void setReturnmask(final boolean b) {
            ReconnectRecorder.stopServer();
            super.setReturnmask(b);
            if (!b) {
                this.closePopup();
            }
        }

        public void startCheck() {
            new Thread() {
                public void run() {
                    JDRRInfoPopup.this.statusicon.setStatus(0);
                    this.setName(JDL.L("gui.config.jdrr.popup.title", "JDRRPopup"));
                    JDRRInfoPopup.this.reconnect_timer = 0;
                    while (ReconnectRecorder.running) {
                        try {
                            Thread.sleep(Gui.CHECK_INTERVAL);
                        } catch (final Exception e) {
                        }

                        if (!IPController.getInstance().validate() && JDRRInfoPopup.this.reconnect_timer == 0) {
                            JDRRInfoPopup.this.reconnect_timer = System.currentTimeMillis();
                        }
                        if (IPController.getInstance().validate()) {
                            JDRRInfoPopup.this.statusicon.setStatus(1);
                            if (ReconnectRecorder.running == true) {
                                ReconnectRecorder.stopServer();
                                JDRRInfoPopup.this.closePopup();
                            }
                            return;
                        }
                    }
                }
            }.start();
        }
    }

    private static final long   serialVersionUID   = 1L;
    private static final String JDL_PREFIX         = "jd.nrouter.recorder.Gui.";
    private JTextField          routerip;
    private JCheckBox           rawmode;
    public boolean              saved              = false;

    public String               ip                 = null;
    public String               methode            = null;
    public String               user               = null;
    public String               pass               = null;

    private static long         CHECK_INTERVAL     = 5000;

    private static long         RECONNECT_DURATION = 0;

    public Gui(final String ip) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("gui.config.jdrr.title", "Reconnect Recorder"), null, JDL.L("gui.btn_start", "Start"), JDL.L("gui.btn_cancel", "Cancel"));
        this.ip = ip;

    }

    @Override
    protected void addButtons(final JPanel buttonBar) {
        final JButton help = new JButton(JDL.L("gui.btn_help", "Help"));
        help.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                try {
                    JLink.openURL("http://jdownloader.org/knowledge/wiki/reconnect/reconnect-recorder");
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }

        });
        buttonBar.add(help, "tag help, sizegroup confirms");
    }

    @Override
    protected Object createReturnValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        this.routerip = new JTextField(this.ip);

        this.rawmode = new JCheckBox(JDL.L("gui.config.jdrr.rawmode", "RawMode?"));
        this.rawmode.setSelected(false);
        this.rawmode.setHorizontalTextPosition(SwingConstants.LEADING);

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(JDL.L(Gui.JDL_PREFIX + "info1", "Check the IP address of the router and press the Start button"));
        sb.append("<br>");
        sb.append(JDL.L(Gui.JDL_PREFIX + "info2", "Web browser window with the home page of the router opens"));
        sb.append("<br>");
        sb.append(JDL.L(Gui.JDL_PREFIX + "info3", "After the Reconnection hit the Stop button and save"));
        sb.append("</html>");

        final JPanel panel = new JPanel(new MigLayout("wrap 3, ins 5", "[][grow]10[]"));
        panel.add(new JLabel(JDL.L("gui.fengshuiconfig.routerip", "RouterIP") + ":"));
        panel.add(this.routerip, "growx");
        panel.add(this.rawmode);
        panel.add(new JLabel(sb.toString()), "spanx,growx");
        return panel;
    }

    private void save() {
        final int ret = UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.config.jdrr.success", "Success!"), JDL.L("gui.config.jdrr.savereconnect", "Reconnection was successful. Save now?"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
        if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {

            final StringBuilder b = new StringBuilder();
            final String br = System.getProperty("line.separator");
            for (final String element : ReconnectRecorder.steps) {
                b.append(element).append(br);
            }
            this.methode = b.toString().trim();

            if (ReconnectRecorder.AUTH != null) {
                this.user = new Regex(ReconnectRecorder.AUTH, "(.+?):").getMatch(0);
                this.pass = new Regex(ReconnectRecorder.AUTH, ".+?:(.+)").getMatch(0);

                ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setUser(this.user);
                ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setPassword(this.pass);
            }
            // TODO
            // btnCancel.setText(JDL.L("gui.btn_close", "Close"));

            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setRouterIP(this.routerip.getText().trim());
            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setScript(this.methode);
            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setRouterName("Reconnect Recorder Method");

            ReconnectPluginController.getInstance().setActivePlugin(LiveHeaderReconnect.ID);
            if (Gui.RECONNECT_DURATION <= 2000) {
                Gui.RECONNECT_DURATION = 2000;
                /* minimum von 2 seks */
            }
            int aa = (int) (Gui.RECONNECT_DURATION / 1000 * 2);
            if (aa < 30) {
                aa = 30;
            }
            int ab = (int) (Gui.RECONNECT_DURATION / 1000 / 2);
            if (ab < 30) {
                ab = 5;
            }

            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_IPCHECKWAITTIME, aa);
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_IPCHECKWAITTIME, ab);
            JDUtilities.getConfiguration().save();
            this.saved = true;
            this.dispose();
        }
    }

    protected void setReturnmask(final boolean b) {

        super.setReturnmask(b);
        if (b && !ReconnectRecorder.running) {
            if (this.routerip.getText() != null && !this.routerip.getText().matches("\\s*")) {
                String host = this.routerip.getText().trim();
                boolean startwithhttps = false;
                if (host.contains("https")) {
                    startwithhttps = true;
                }
                host = host.replaceAll("http://", "").replaceAll("https://", "");
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, host);

                IPController.getInstance().invalidate();
                ReconnectRecorder.startServer(host, this.rawmode.isSelected());

                try {
                    if (startwithhttps) {
                        JLink.openURL("http://localhost:" + (SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + 1));
                    } else {
                        JLink.openURL("http://localhost:" + SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972));
                    }
                } catch (final Exception e1) {
                    JDLogger.exception(e1);
                }
                try {
                    Dialog.getInstance().showDialog(new JDRRInfoPopup());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

    }
}