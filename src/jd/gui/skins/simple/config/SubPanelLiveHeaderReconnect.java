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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.http.Encoding;
import jd.router.FindRouterIP;
import jd.router.GetRouterInfo;
import jd.router.RouterInfoCollector;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnAutoConfig;

    private JButton btnFindIP;

    private JButton btnSelectRouter;

    private GUIConfigEntry ip;

    private HTTPLiveHeader lh;

    private GUIConfigEntry pass;

    private GUIConfigEntry routerScript;

    private GUIConfigEntry user;

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
        } else if (e.getSource() == btnSelectRouter) {
            Vector<String[]> scripts = lh.getLHScripts();

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

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            final DefaultListModel defaultListModel = new DefaultListModel();
            final String text = "Search Router Model";
            final JTextField searchField = new JTextField();
            searchField.setForeground(Color.lightGray);
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

                @Override
                public void focusGained(FocusEvent e) {
                    searchField.setForeground(Color.black);
                    if (searchField.getText().equals(text)) {
                        searchField.setText("");
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (searchField.getText().equals("")) {
                        searchField.setForeground(Color.lightGray);
                        searchField.setText(text);
                        for (String element : d) {
                            defaultListModel.addElement(element);
                        }
                    }
                }
            });

            // !!! Eclipse Clear Console Icon
            ImageIcon imageIcon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.exit")));
            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(16, -1, Image.SCALE_SMOOTH));
            JButton reset = new JButton(imageIcon);

            reset.setBorder(null);
            reset.setOpaque(false);
            reset.setContentAreaFilled(false);
            reset.setBorderPainted(false);
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    searchField.setForeground(Color.lightGray);
                    searchField.setText(text);
                    for (String element : d) {
                        defaultListModel.addElement(element);
                    }
                }
            });
            searchField.setText(text);
            // !!! Lupen-Icon
            Icon icon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.update_manager")));
            JPanel p = new JPanel(new BorderLayout(5, 5));
            p.add(searchField, BorderLayout.CENTER);
            p.add(reset, BorderLayout.EAST);
            JLabel example = new JLabel("Example: 3Com ADSL");
            example.setForeground(Color.gray);
            p.add(example, BorderLayout.SOUTH);
            for (String element : d) {
                defaultListModel.addElement(element);
            }
            JScrollPane scrollPane = new JScrollPane(list);
            panel.add(p, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(400, 500));
            int n = 10;
            panel.setBorder(new EmptyBorder(n, n, n, n));
            JOptionPane op = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
            JDialog dialog = op.createDialog(this, JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
            dialog.add(op);
            dialog.setModal(true);
            dialog.setPreferredSize(new Dimension(400, 500));
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            int answer = ((Integer) op.getValue()).intValue();
            if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
                String selected = (String) list.getSelectedValue();
                int id = Integer.parseInt(selected.split("\\.")[0]);
                String[] data = scripts.get(id);
                if (data[2].toLowerCase().indexOf("curl") >= 0) {
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
                }
                routerScript.setData(data[2]);
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
            GetRouterInfo.autoConfig(pass, user, ip, routerScript);
        }

    }

    @Override
    public void initPanel() {

        btnSelectRouter = new JButton(JDLocale.L("gui.config.liveHeader.selectRouter", "Router auswählen"));
        btnSelectRouter.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnSelectRouter, 0, 0, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));
        btnFindIP.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnFindIP, 1, 0, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnAutoConfig = new JButton(JDLocale.L("gui.config.liveHeader.autoConfig", "Router automatisch setzten"));
        btnAutoConfig.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnAutoConfig, 2, 0, GridBagConstraints.REMAINDER, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(pass);
        ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")));
        addGUIConfigEntry(ip);
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, JDLocale.L("gui.config.liveHeader.waitTimeForIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_HTTPSEND_RETRIES, JDLocale.L("gui.config.liveHeader.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, JDLocale.L("gui.config.liveHeader.waitForIP", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, RouterInfoCollector.PROPERTY_SHOW_ROUTERINFO_DIALOG, JDLocale.L("gui.config.reconnect.showupload", "Show ReconnectInfo Upload Window")).setDefaultValue(JDUtilities.getConfiguration().getBooleanProperty(RouterInfoCollector.PROPERTY_SHOW_ROUTERINFO_DIALOG, true))));

        routerScript = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS, JDLocale.L("gui.config.liveHeader.script", "HTTP Script")));
        this.entries.add(routerScript);

        add(panel, BorderLayout.NORTH);
        add(routerScript, BorderLayout.CENTER);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

}