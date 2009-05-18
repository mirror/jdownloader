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

package jd.gui.skins.simple.config.subpanels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.http.Encoding;
import jd.router.FindRouterIP;
import jd.router.GetRouterInfo;
import jd.router.reconnectrecorder.JDRRGui;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnAutoConfig;

    private JButton btnFindIP;

    private JButton btnSelectRouter;

    private GUIConfigEntry ip;

    private HTTPLiveHeader lh;

    private GUIConfigEntry pass;

    private GUIConfigEntry user;

    private JButton btnRouterRecorder;

    private GUIConfigEntry script;

    public SubPanelLiveHeaderReconnect(Configuration configuration, HTTPLiveHeader interaction) {
        super();
        this.configuration = configuration;
        initPanel();
        lh = interaction;
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnFindIP) {
            new FindRouterIP(ip);
        } else if (e.getSource() == this.btnRouterRecorder) {
            new Thread() {
                public void run() {
                    if (((JTextField) ip.getInput()[0]).getText() == null || ((JTextField) ip.getInput()[0]).getText().trim().equals("")) {
                        Thread th = new Thread() {
                            public void run() {
                                FindRouterIP.findIP(ip);
                            }
                        };
                        th.start();
                        while (th.isAlive()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                    new GuiRunnable<Object>() {

                        //@Override
                        public Object runSave() {
                            JDRRGui jd = new JDRRGui(SimpleGUI.CURRENTGUI, ((JTextField) ip.getInput()[0]).getText());
                            jd.setModal(true);
                            jd.setVisible(true);
                            if (jd.saved) {
                                ((JTextField) ip.getInput()[0]).setText(jd.RouterIP);
                                if (jd.user != null) ((JTextField) user.getInput()[0]).setText(jd.user);
                                if (jd.pass != null) ((JTextField) pass.getInput()[0]).setText(jd.pass);
                                ((JTextArea) script.getInput()[0]).setText(jd.methode);
                            }
                            return null;
                        }

                    }.start();

                }
            }.start();

        } else if (e.getSource() == btnSelectRouter) {
            final Vector<String[]> scripts = lh.getLHScripts();

            Collections.sort(scripts, new Comparator<String[]>() {
                public int compare(String[] a, String[] b) {
                    return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
                }

            });

            HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
            for (int i = scripts.size() - 1; i >= 0; i--) {
                if (ch.containsKey(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2])) {
                    scripts.remove(i);
                } else {

                    ch.put(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2], true);
                }
            }
            ch.clear();
            final String[] d = new String[scripts.size()];
            for (int i = 0; i < d.length; i++) {
                d[i] = i + ". " + Encoding.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
            }

            JPanel panel = new JPanel(new MigLayout("ins 10,wrap 3", "[grow 30,fill]5[grow 0,fill]10[grow,fill,300!]", "[fill]5[]5[fill,grow]"));
            final DefaultListModel defaultListModel = new DefaultListModel();
            final String text = JDLocale.L("gui.config.reconnect.selectrouter", "Search Router Model");
            final JTextField searchField = new JTextField();

            final JList list = new JList(defaultListModel);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                }

                public void insertUpdate(DocumentEvent e) {
                    refreshList();
                }

                private void refreshList() {
                    String search = searchField.getText().toLowerCase();
                    String[] hits = search.split(" ");
                    defaultListModel.removeAllElements();
                    for (int i = 0; i < d.length; i++) {
                        for (int j = 0; j < hits.length; j++) {
                            if (!d[i].toLowerCase().contains(hits[j])) {
                                break;
                            }
                            if (j == hits.length - 1) {
                                defaultListModel.addElement(d[i]);
                            }
                        }
                    }
                    list.setModel(defaultListModel);
                }

                public void removeUpdate(DocumentEvent e) {
                    refreshList();
                }
            });
            searchField.addFocusListener(new FocusAdapter() {

                //@Override
                public void focusGained(FocusEvent e) {

                    if (searchField.getText().equals(text)) {
                        searchField.setText("");
                    }
                }

                //@Override
                public void focusLost(FocusEvent e) {
                    if (searchField.getText().equals("")) {

                        searchField.setText(text);
                        for (String element : d) {
                            defaultListModel.addElement(element);
                        }
                    }
                }
            });
            final JTextArea preview = new JTextArea();
            preview.setFocusable(true);
            // !!! Eclipse Clear Console Icon

            JButton reset = new JButton(JDTheme.II("gui.images.undo", 16, 16));

            reset.setBorder(null);
            reset.setOpaque(false);
            reset.setContentAreaFilled(false);
            reset.setBorderPainted(false);
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    searchField.setForeground(Color.lightGray);
                    searchField.setText(text);
                    preview.setText("");
                    for (String element : d) {
                        defaultListModel.addElement(element);
                    }
                }
            });
            searchField.setText(text);
            // !!! Lupen-Icon

            list.addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    String selected = (String) list.getSelectedValue();
                    int id = Integer.parseInt(selected.split("\\.")[0]);
                    String[] data = scripts.get(id);

                    preview.setText(data[2]);
                }

            });
            JLabel example = new JLabel(JDLocale.L("gui.config.reconnect.selectrouter.example", "Example: 3Com ADSL"));

            for (String element : d) {
                defaultListModel.addElement(element);
            }
            JScrollPane scrollPane = new JScrollPane(list);

            panel.add(searchField);
            panel.add(reset);
            panel.add(new JScrollPane(preview), "spany");

            panel.add(example, "spanx 2");
            panel.add(scrollPane, "spanx 2");

            // panel.setPreferredSize(new Dimension(650, 500));

            JOptionPane op = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, JDTheme.II("gui.images.search"));
            JDialog dialog = op.createDialog(this, JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
            dialog.add(op);
            dialog.setModal(true);
            dialog.setPreferredSize(new Dimension(700, 500));
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            if (op.getValue() == null) return;
            int answer = ((Integer) op.getValue()).intValue();
            if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
                String selected = (String) list.getSelectedValue();
                int id = Integer.parseInt(selected.split("\\.")[0]);
                String[] data = scripts.get(id);
                if (data[2].toLowerCase().indexOf("curl") >= 0) {
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
                }
                script.setData(data[2]);
                String username = (String) user.getText();
                if (username == null || username.matches("[\\s]*")) {
                    user.setData(data[4]);
                }
                String pw = (String) pass.getText();
                if (pw == null || pw.matches("[\\s]*")) {
                    pass.setData(data[5]);
                }

            }
        } else if (e.getSource() == btnAutoConfig) {
            GetRouterInfo.autoConfig(pass, user, ip, script);
        }

    }

    //@Override
    public void initPanel() {

        this.setLayout(new MigLayout("ins 0 20 0 20, wrap 2", "[grow 20,fill][grow,fill]", "[]5[]5[]"));
        btnSelectRouter = new JButton(JDLocale.L("gui.config.liveHeader.selectRouter", "Routerauswahl"));
        btnSelectRouter.addActionListener(this);
        add(btnSelectRouter, "gaptop 10");
        add(panel, "spany 3,gapbottom 20");

        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));
        btnFindIP.addActionListener(this);
        add(btnFindIP);
        // JDUtilities.addToGridBag(panel, btnFindIP, 1, 0, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnAutoConfig = new JButton(JDLocale.L("gui.config.liveHeader.autoConfig", "Router automatisch setzten"));
        btnAutoConfig.addActionListener(this);

        // add(btnAutoConfig,"aligny top");
        // JDUtilities.addToGridBag(panel, btnAutoConfig, 2, 0,
        // GridBagConstraints.REMAINDER, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        btnRouterRecorder = new JButton(JDLocale.L("gui.config.liveHeader.recorder", "Create Reconnect Script"));
        btnRouterRecorder.addActionListener(this);
        add(btnRouterRecorder, "aligny top");
        panel.setLayout(new MigLayout("ins 10 10 10 0,wrap 2", "[fill,grow 10]10[fill,grow]"));
        user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.httpliveheader.user", "User")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.httpliveheader.password", "Password")));
        addGUIConfigEntry(pass);
        ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.httpliveheader.routerIP", "Router's ip")));
        addGUIConfigEntry(ip);

        JScrollPane sp;
        add(sp=new JScrollPane((script=new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS, JDLocale.L("gui.config.httpliveheader.script", "Reconnect Script")))).getInput()[0]), "gaptop 10,spanx,gapright 20,pushy, growy");
//       sp.setBorder(null);
        // routerScript = new GUIConfigEntry();
        // this.entries.add(routerScript);

        // add(routerScript);
    }

    //@Override
    public void load() {
        loadConfigEntries();
    }

    //@Override
    public void save() {
        saveConfigEntries();

    }

}