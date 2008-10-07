package jd.plugins.optional.jdreconnectrecorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import jd.config.Configuration;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDRRGui extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnStartStop;

    private JButton btnSave;

    private JTextArea script;

    private JLabel ipbefore;
    private JLabel ipafter;

    private String ip_before;
    private String ip_after;

    public JDRRGui(Frame owner) {
        super(owner, "Reconnect Recorder");
        this.setModal(true);
        int n = 10;

        ipbefore = new JLabel("IP Before Reconnect:");
        ipafter = new JLabel("IP After Reconnect:");

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        btnSave = new JButton(JDLocale.L("gui.btn_save", "Speichern"));
        btnSave.addActionListener(this);

        btnStartStop = new JButton("Start");
        btnStartStop.addActionListener(this);

        script = new JTextArea();
        script.setEditable(true);

        script.setCaretPosition(0);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnSave);
        bpanel.add(btnCancel);
        bpanel.add(btnStartStop);

        JPanel spanel = new JPanel(new BorderLayout(n, n));
        spanel.add(ipbefore, BorderLayout.NORTH);
        spanel.add(ipafter, BorderLayout.SOUTH);

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
        if (e.getSource() == btnStartStop && btnStartStop.getText().contains("Stop")) {
            ip_after = JDUtilities.getIPAddress();
            ipafter.setText("IP After Reconnect: " + ip_after);
            JDRR.running = false;
            JDRR.stopServer();
            for (String element : JDRR.steps) {
                script.append(element + System.getProperty("line.separator"));
            }
            btnStartStop.setText("Start");
            if (ip_after.equalsIgnoreCase(ip_before)) {
                ipbefore.setText("Reconnect failed");
                ipafter.setText("Reconnect failed");
                return;
            }
            return;
        } else if (e.getSource() == btnStartStop && btnStartStop.getText().contains("Start")) {
            ip_before = JDUtilities.getIPAddress();
            ipbefore.setText("IP Before Reconnect: " + ip_before);
            ipafter.setText("IP After Reconnect: ");
            script.setText("");
            JDRR.startServer(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null));
            btnStartStop.setText("Stop");
            try {
                JLinkButton.openURL("http://localhost:12345");
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
            configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, temp);

        }
        JDRR.gui = false;
        dispose();
    }

}