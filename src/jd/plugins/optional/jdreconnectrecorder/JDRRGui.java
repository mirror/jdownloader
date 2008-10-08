package jd.plugins.optional.jdreconnectrecorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import jd.config.Configuration;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDRRGui extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnStartStop;

    private JButton btnSave;

    private JTextArea script;

    private JTextField routerip;

    private JButton btnFindIP;

    private String ip_before;
    private String ip_after;

    private JButton btnStop;

    private JDRRInfoPopup infopopup;

    public JDRRGui(Frame owner) {
        this.setModal(false);
        int n = 10;

        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));
        btnFindIP.addActionListener(this);

        routerip = new JTextField("");
        routerip.setHorizontalAlignment(SwingConstants.RIGHT);
        routerip.setText(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null));

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        btnSave = new JButton(JDLocale.L("gui.btn_save", "Speichern"));
        btnSave.addActionListener(this);
        btnSave.setEnabled(false);

        btnStartStop = new JButton("Start");
        btnStartStop.addActionListener(this);

        script = new JTextArea();
        script.setEditable(true);

        script.setCaretPosition(0);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnSave);
        bpanel.add(btnCancel);
        bpanel.add(btnStartStop);
        bpanel.add(btnFindIP);

        JPanel spanel = new JPanel(new BorderLayout(n, n));
        spanel.add(routerip, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(spanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(script), BorderLayout.CENTER);
        panel.add(bpanel, BorderLayout.SOUTH);

        this.getRootPane().setDefaultButton(btnSave);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setContentPane(panel);
        this.setPreferredSize(new Dimension(400, 300));
        this.pack();
        this.setLocation(JDUtilities.getCenterOfComponent(null, this));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnFindIP) {
            new Thread() {
                @Override
                public void run() {
                    GetRouterInfo rinfo = new GetRouterInfo(null);
                    if (rinfo.getAdress() != null) routerip.setText(rinfo.getAdress());
                }
            }.start();
            return;
        } else if (e.getSource() == btnStop || e.getSource() == btnStartStop && btnStartStop.getText().contains("Stop")) {
            ip_after = JDUtilities.getIPAddress();
            JDRR.running = false;
            JDRR.stopServer();
            if (infopopup != null) infopopup.dispose();
            if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                script.setText("");
                for (String element : JDRR.steps) {
                    script.append(element + System.getProperty("line.separator"));
                }
                btnSave.setEnabled(true);
            } else {
                script.setText("Reconnect failed");
                btnSave.setEnabled(false);
            }
            btnStartStop.setText("Start");
            return;
        } else if (e.getSource() == btnStartStop && btnStartStop.getText().contains("Start")) {
            ip_before = JDUtilities.getIPAddress();
            script.setText("");
            JDRR.startServer(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null));
            infopopup = new JDRRInfoPopup(ip_before);
            infopopup.setVisible(true);
            btnStartStop.setText("Stop");
            btnSave.setEnabled(false);
            try {
                JLinkButton.openURL("http://localhost:" + JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972));
            } catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return;
        } else if (e.getSource() == btnSave) {
            if (ip_after.equalsIgnoreCase(ip_before)) { return; }
            Configuration configuration = JDUtilities.getConfiguration();
            String temp = "";
            for (String element : JDRR.steps) {
                temp = temp + (element + System.getProperty("line.separator"));
            }
            if (JDRR.auth != null) {
                configuration.setProperty(Configuration.PARAM_HTTPSEND_USER, new Regex(JDRR.auth, "(.+?):").getMatch(0));
                configuration.setProperty(Configuration.PARAM_HTTPSEND_PASS, new Regex(JDRR.auth, ".+?:(.+)").getMatch(0));
            }
            configuration.setProperty(Configuration.PARAM_HTTPSEND_IP, routerip.getText());
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, temp);
            configuration.setProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));

        }
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
            btnStop = new JButton("Stop");
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
                    } catch (InterruptedException e) {
                    }
                    if (infopopup != null) infopopup.dispose();
                    if (!ip_after.contains("offline") && !ip_after.equalsIgnoreCase(ip_before)) {
                        script.setText("");
                        for (String element : JDRR.steps) {
                            script.append(element + System.getProperty("line.separator"));
                        }
                        btnSave.setEnabled(true);
                    } else {
                        script.setText("Reconnect failed");
                        btnSave.setEnabled(false);
                    }
                    btnStartStop.setText("Start");
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
}