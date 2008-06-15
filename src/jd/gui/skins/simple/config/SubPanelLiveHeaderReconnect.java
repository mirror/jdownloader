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
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.LogRecord;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.graphics.ColorUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.ProgressDialog;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener,ControlListener {
    /**
     * 
     */
    private static final long serialVersionUID = 6710420298517566329L;

    // private Configuration configuration;

    private HTTPLiveHeader lh;

    private GUIConfigEntry routerScript;

    private GUIConfigEntry ip;

    private GUIConfigEntry user;

    private GUIConfigEntry pass;

    private JButton btnAutoConfig;

    private JButton btnSelectRouter;

    private JButton btnFindIP;

    private MiniLogDialog mld;

    public SubPanelLiveHeaderReconnect(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        // this.configuration = configuration;
        initPanel();
        this.lh = (HTTPLiveHeader) interaction;
        load();
       
    }

    public void save() {
        this.saveConfigEntries();

    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        // ConfigEntry cfg;
        btnSelectRouter = new JButton(JDLocale.L("gui.config.liveHeader.selectRouter", "Router auswählen"));
        btnAutoConfig = new JButton(JDLocale.L("gui.config.liveHeader.autoConfig", "Router automatisch setzten"));
        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));

        btnSelectRouter.addActionListener(this);
        btnAutoConfig.addActionListener(this);
        btnFindIP.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnSelectRouter, 0, 0, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnFindIP, 1, 0, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
        JDUtilities.addToGridBag(panel, btnAutoConfig, 2, 0, GridBagConstraints.REMAINDER, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
        user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(pass);
        String routerip = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);

        ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")).setDefaultValue(routerip));
        addGUIConfigEntry(ip);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, JDLocale.L("gui.config.liveHeader.waitTimeForIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, JDLocale.L("gui.config.liveHeader.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, JDLocale.L("gui.config.liveHeader.waitForIP", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20).setExpertEntry(true));
        addGUIConfigEntry(ce);

        routerScript = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, JDLocale.L("gui.config.liveHeader.script", "HTTP Script")));
        addGUIConfigEntry(routerScript);

        add(panel);

    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.liveHeader.name", "Reconnect via LiveHeader");
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnFindIP) {
            JDUtilities.getController().addControlListener(this);
          mld = new MiniLogDialog(JDLocale.L("gui.config.routeripfinder", "Router IPsearch"));
            mld.getBtnOK().setEnabled(false);
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);
           //mld.setEnabled(true);
            new Thread() {
                public void run() {
                    ip.setData(JDLocale.L("gui.config.routeripfinder.featchIP", "Suche nach RouterIP..."));
                    mld.getProgress().setValue(60);
                    GetRouterInfo rinfo = new GetRouterInfo(null);
                    mld.getProgress().setValue(80);
                    ip.setData(rinfo.getAdress());
                    mld.getProgress().setValue(100);

                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.config.routeripfinder.close", "Fenster schließen"));
                    //mld.setEnabled(true);
                   
                    JDUtilities.getController().addControlListener(SubPanelLiveHeaderReconnect.this);
                }
            }.start();

        }
        if (e.getSource() == btnSelectRouter) {
            Vector<String[]> scripts = lh.getLHScripts();

            Collections.sort(scripts, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    String[] aa = (String[]) a;
                    String[] bb = (String[]) b;

                    if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) > 0) {
                        return 1;
                    } else if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) < 0) {
                        return -1;
                    } else {
                        return 0;
                    }

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
                d[i] = i + ". " + JDUtilities.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
            }

            // String selected = (String) JOptionPane.showInputDialog(this,
            // JDLocale.L("gui.config.liveHeader.dialog.selectRouter", "Bitte
            // wähle deinen Router aus"),
            // JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router
            // importieren"), JOptionPane.INFORMATION_MESSAGE, null, d, null);
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

                public void removeUpdate(DocumentEvent e) {
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
            });
            searchField.addFocusListener(new FocusAdapter() {
                boolean onInit = true;

                public void focusGained(FocusEvent e) {
                    if (onInit) {
                        onInit = !onInit;
                        return;
                    }
                    searchField.setForeground(Color.black);
                    if (searchField.getText().equals(text)) searchField.setText("");
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
                    for (int i = 0; i < d.length; i++) {
                        defaultListModel.addElement(d[i]);
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
            for (int i = 0; i < d.length; i++) {
                defaultListModel.addElement(d[i]);
            }
            // list.setPreferredSize(new Dimension(400, 500));
            JScrollPane scrollPane = new JScrollPane(list);
            panel.add(p, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(400, 500));
            int n = 10;
            panel.setBorder(new EmptyBorder(n, n, n, n));
            JOptionPane op = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
            // JDialog dialog = new
            // JDialog(SwingUtilities.getWindowAncestor(btnSelectRouter), );
            JDialog dialog = op.createDialog(this, JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
            dialog.add(op);
            dialog.setModal(true);
            dialog.setPreferredSize(new Dimension(400, 500));
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            int answer = ((Integer) op.getValue()).intValue(); // JOptionPane.showConfirmDialog(this,
                                                                // panel,
                                                                // JDLocale.L("gui.config.liveHeader.dialog.importRouter",
                                                                // "Router
                                                                // importieren"),
                                                                // JOptionPane.OK_CANCEL_OPTION,
                                                                // JOptionPane.INFORMATION_MESSAGE);
            if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
                String selected = (String) list.getSelectedValue();
                int id = Integer.parseInt(selected.split("\\.")[0]);
                String[] data = scripts.get(id);
                if (data[2].toLowerCase().indexOf("curl") >= 0) {
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
                }
                routerScript.setData(data[2]);
                String username = (String) user.getText();
                if (username == null || username.matches("[\\s]*")) user.setData(data[4]);
                String pw = (String) pass.getText();
                if (pw == null || pw.matches("[\\s]*")) pass.setData(data[5]);

            }
        }
            if (e.getSource() == this.btnAutoConfig) {

            if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.config.liveHeader.warning.wizard", "Die automatische Suche nach den Einstellungen kann einige Minuten in Anspruch nehmen. Bitte geben Sie vorher Ihre Router Logindaten ein. Jetzt ausführen?"))) {
                Thread th;
                final ProgressDialog progress = new ProgressDialog(ConfigurationDialog.PARENTFRAME, JDLocale.L("gui.config.liveHeader.progress.message", "jDownloader sucht nach Ihren Routereinstellungen"), null, false, false);

                th = new Thread() {
                    public void run() {

                        GetRouterInfo routerInfo = new GetRouterInfo(progress);
                        routerInfo.setLoginPass((String)pass.getText());
                        routerInfo.setLoginUser((String)user.getText());
                        String username = (String) user.getText();
                        if (username != null && !username.matches("[\\s]*")) routerInfo.username = username;
                        String pw = (String) pass.getText();
                        if (pw != null && !pw.matches("[\\s]*")) routerInfo.password = pw;
                        String[] data;
                        if (GetRouterInfo.validateIP(ip.getText() + "")) {
                            data = routerInfo.getRouterData(ip.getText() + "");
                        } else {
                            data = routerInfo.getRouterData(null);
                        }
                        if (data == null) {
                            progress.setVisible(false);
                            progress.dispose();
                            JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.notFound", "jDownloader konnte ihre Routereinstellung nicht automatisch ermitteln."));
                            return;
                        }
                        routerScript.setData(data[2]);
                        if (username == null || username.matches("[\\s]*")) user.setData(data[4]);
                        if (pw == null || pw.matches("[\\s]*")) pass.setData(data[5]);

                        user.setData(data[4]);
                        progress.setVisible(false);
                        progress.dispose();
                        JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.yourRouter", "Sie haben eine") + " " + data[1]);

                    }
                };
                th.start();
                progress.setThread(th);
                progress.setVisible(true);

            }
        }

    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED&&mld!=null&&mld.isEnabled()) {
            LogRecord l = (LogRecord) event.getParameter();

            if (l.getSourceClassName().startsWith("jd.router.GetRouterInfo")) {
                mld.setText(JDUtilities.formatSeconds((int)l.getMillis()/1000)+" : "+ l.getMessage() + "\r\n"+mld.getText());
                mld.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                mld.getProgress().setValue(mld.getProgress().getValue()+1);
            }
        }
        
    }

}
