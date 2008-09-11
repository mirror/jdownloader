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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.HostPluginWrapper;
import jd.JDInit;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.BatchReconnect;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.Progressor;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.http.Encoding;
import jd.plugins.PluginForHost;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;
import net.miginfocom.swing.MigLayout;

public class FengShuiConfigPanel extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1715405893428812995L;
    private static final String GAPLEFT = "gapleft 15!, ";
    private static final String GAPBOTTOM = ", gapbottom :10:push";
    private static final String SPAN = ", spanx" + GAPBOTTOM;

    public static void main(String[] args) {
        new FengShuiConfigPanel();
    }

    private JButton more, apply, cancel, premium, btnAutoConfig, btnSelectRouter, btnTestReconnect;
    private JComboBox languages;
    private SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
    private Configuration config = JDUtilities.getConfiguration();
    private BrowseFile downloadDirectory;
    private String ddir = null;
    private JTextField username, password, ip;
    public JTextField routername;
    private String routerIp = null;
    private Progressor prog;
    private JPanel panel;
    private JProgressBar progress = null;
    public String Reconnectmethode = null;

    private String wikiurl = JDLocale.L("gui.fengshuiconfig.wikiurl", "http://wiki.jdownloader.org/index.php?title=DE:fengshui:");

    public FengShuiConfigPanel() {
        super();
        this.setName("FENGSHUICONFIG");
        this.setTitle("Feng Shui Config");
        this.addWindowListener(new LocationListener());

        JPanel panel = getPanel();
        Dimension minSize = panel.getMinimumSize();
        this.setContentPane(new JScrollPane(panel));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.pack();
        Dimension ps = this.getPreferredSize();
        this.setPreferredSize(new Dimension(Math.min(800, ps.width), Math.min(500, ps.height)));
        this.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.configuration")));
        this.pack();
        panel.setPreferredSize(minSize);
        if (SimpleGUI.CURRENTGUI != null) this.setLocation(SimpleGUI.getLastLocation(SimpleGUI.CURRENTGUI.getFrame(), null, this));
        this.setResizable(false);
        this.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancel)
            dispose();
        else if (e.getSource() == more) {
            save();
            dispose();
            UIInterface ui = JDUtilities.getGUI();
            ConfigurationDialog.showConfig(((SimpleGUI) ui).getFrame(), ui);
            // ConfigurationDialog.showConfig(new JFrame(), ui);

        } else if (e.getSource() == apply) {
            save();
            dispose();
        } else if (e.getSource() == premium) {
            final JDInit init = new JDInit(null);
            init.initPlugins();
            JPopupMenu popup = new JPopupMenu(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"));

            for (Iterator<HostPluginWrapper> it = JDUtilities.getPluginsForHost().iterator(); it.hasNext();) {
                HostPluginWrapper wrapper = it.next();

                if (wrapper.isLoaded()) {
                    final PluginForHost helpplugin = wrapper.getPlugin();
                    if (helpplugin.createMenuitems() != null) {
                        JMenu item;
                        popup.add(item = new JMenu(helpplugin.getHost()));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);

                        // m.setItems(helpplugin.createMenuitems());

                        if (item != null) {
                            popup.add(item);

                            item.addMenuListener(new MenuListener() {
                                public void menuCanceled(MenuEvent e) {
                                }

                                public void menuDeselected(MenuEvent e) {
                                }

                                public void menuSelected(MenuEvent e) {
                                    JMenu m = (JMenu) e.getSource();
                                    JMenuItem c;
                                    m.removeAll();
                                    for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                        c = SimpleGUI.getJMenuItem(menuItem);
                                        if (c == null) {
                                            m.addSeparator();
                                        } else {
                                            m.add(c);
                                        }

                                    }

                                }

                            });
                        } else {
                            popup.addSeparator();
                        }
                    }
                }
            }

            popup.show(((JButton) e.getSource()), 100, 25);
        } else if (e.getSource() == btnTestReconnect) {
            save();

            final MiniLogDialog mld = new MiniLogDialog("Reconnect");
            mld.getBtnOK().setEnabled(false);
            mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"));
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);

            JDUtilities.getLogger().info("Start Reconnect");
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
            JDUtilities.getSubConfig("BATCHRECONNECT").setProperty(BatchReconnect.PARAM_RETRIES, 0);
            JDUtilities.getConfiguration().setProperty(ExternReconnect.PARAM_RETRIES, 0);

            new Thread() {
                @Override
                public void run() {
                    boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    if (Reconnecter.waitForNewIP(1)) {
                        mld.setText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull") + "\r\n\r\n\r\n" + mld.getText());
                    } else {

                        mld.setText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!") + "\r\n\r\n\r\n" + mld.getText());
                        if (JDUtilities.getController().getRunningDownloadNum() > 0) {
                            mld.setText(JDLocale.L("gui.warning.reconnectFailedRunningDownloads", "Please stop all running Downloads first!") + "\r\n\r\n\r\n" + mld.getText());
                        }
                    }
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
                    mld.getProgress().setValue(100);
                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.close", "Fenster schließen"));
                }
            }.start();
        } else if (e.getSource() == btnAutoConfig) {
            GetRouterInfo.autoConfig(password, username, ip, this);
        }
        if (e.getSource() == btnSelectRouter) {
            Vector<String[]> scripts = new HTTPLiveHeader().getLHScripts();

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
                boolean onInit = true;

                @Override
                public void focusGained(FocusEvent e) {
                    if (onInit) {
                        onInit = !onInit;
                        return;
                    }
                    searchField.setForeground(Color.black);
                    if (searchField.getText().equals(text)) {
                        searchField.setText("");
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
                routername.setText(data[1]);
                Reconnectmethode = data[2];
                String user = (String) username.getText();
                if (user == null || user.matches("[\\s]*")) {
                    username.setText(data[4]);
                }
                String pw = (String) password.getText();
                if (pw == null || pw.matches("[\\s]*")) {
                    password.setText(data[5]);
                }

            }
        }
    }

    private void addComponents(JPanel panel, String label, JComponent... components) {

        panel.add(new JLabel("<html><b color=\"#4169E1\">" + label), "gapleft 22, gaptop 5" + GAPBOTTOM);
        for (int i = 0; i < components.length; i++) {
            if (i == components.length - 1) {
                panel.add(components[i], GAPLEFT + SPAN + ", gapright 5");
            } else {
                panel.add(components[i], GAPLEFT);
            }
        }
    }

    private void addSeparator(JPanel panel, String title, Icon icon, String help) {
        try {
            JLinkButton label = new JLinkButton("<html><u><b  color=\"#006400\">" + title, icon, new URL(wikiurl + (title.replaceAll("\\s", "_"))));
            label.setIconTextGap(8);
            panel.add(label, "align left, split 2");
            panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
            panel.add(new JSeparator(), "span 3, pushx, growx");

            JLabel tip = new JLabel(JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.config.tip"), 16, 16));
            tip.setToolTipText(help);
            panel.add(tip, GAPLEFT + "w pref!, wrap");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public String getDownloadDirectory() {
        if (ddir == null) {
            ddir = config.getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
            if (ddir == null) ddir = JDUtilities.getResourceFile("downloads").getAbsolutePath();
        }
        return ddir;
    }

    private JPanel getPanel() {
        panel = new JPanel(new MigLayout("ins 32 22 15 22", "[right, pref!]0[right,grow,fill]0[]"));
        routerIp = config.getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        Reconnectmethode = config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        addSeparator(panel, JDLocale.L("gui.config.general.name", "Allgemein"), JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.configuration"), 32, 32), JDLocale.L("gui.fengshuiconfig.general.tooltip", "<html>You can set the Downloadpath and the language here"));

        languages = new JComboBox(JDLocale.getLocaleIDs().toArray(new String[] {}));
        languages.setSelectedItem(guiConfig.getProperty(SimpleGUI.PARAM_LOCALE, Locale.getDefault()));
        addComponents(panel, JDLocale.L("gui.config.gui.language", "Sprache"), languages);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                downloadDirectory.requestFocus();
            }
        });
        downloadDirectory = new BrowseFile();
        downloadDirectory.setEditable(true);
        downloadDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        downloadDirectory.setText(getDownloadDirectory());
        addComponents(panel, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis"), downloadDirectory);

        addSeparator(panel, JDLocale.L("gui.config.plugin.host.name", "Host Plugins"), JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.config.host"), 32, 32), JDLocale.L("gui.fengshuiconfig.plugin.host.tooltip", "<html>If you have a Premium Account for a hoster you can enter you login<br> password here and JD will use them automatically henceforth<br> if you download files with that hoster"));

        panel.add(premium = new JButton(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster")), GAPLEFT + "align leading, wmax pref" + SPAN);
        premium.addActionListener(this);

        JLinkButton label;
        if (routerIp == null || routerIp.matches("\\s*")) progress = new JProgressBar();
        try {
            String titl = JDLocale.L("gui.config.reconnect.name", "Reconnect");
            label = new JLinkButton("<html><u><b  color=\"#006400\">" + titl, JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.reconnect"), 32, 32), new URL(wikiurl + (titl.replaceAll("\\s", "_"))));

            label.setIconTextGap(8);
            panel.add(label, "align left, split 2");
            panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
            if (routerIp == null)
                panel.add(progress, "span 3, pushx, growx");
            else
                panel.add(new JSeparator(), "span 3, pushx, growx", 15);
            JLabel tip = new JLabel(JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.config.tip"), 16, 16));
            tip.setToolTipText(JDLocale.L("gui.fengshuiconfig.reconnect.tooltip", "<html>Somtimes you need to change your ip via reconnect, to skip the waittime!"));
            panel.add(tip, GAPLEFT + "w pref!, wrap");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        JPanel reconnectPanel = new JPanel(new MigLayout());
        reconnectPanel.add(btnAutoConfig = new JButton(JDLocale.L("gui.config.liveHeader.autoConfig", "Router automatisch setzten")), "pushx");
        btnAutoConfig.addActionListener(this);
        reconnectPanel.add(btnSelectRouter = new JButton(JDLocale.L("gui.config.liveHeader.selectRouter", "Router auswählen")), "w pref!");
        btnSelectRouter.addActionListener(this);
        reconnectPanel.add(btnTestReconnect = new JButton(JDLocale.L("modules.reconnect.testreconnect", "Test reconnect")), "w pref!, wrap");
        btnTestReconnect.addActionListener(this);

        panel.add(reconnectPanel, "spanx, pushx, growx");
        reconnectPanel = new JPanel(new MigLayout());

        reconnectPanel.add(new JLabel(JDLocale.L("gui.config.fengshui.routerip", "RouterIP:")));
        reconnectPanel.add(ip = new JTextField(12));
        ip.setText(routerIp);
        reconnectPanel.add(new JLabel(JDLocale.L("gui.config.fengshui.routername", "Routername:")));
        reconnectPanel.add(routername = new JTextField(12), "wrap");
        routername.setEnabled(false);
        routername.setEditable(false);
        routername.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, ""));
        reconnectPanel.add(new JLabel(JDLocale.L("gui.config.fengshui.user", "Username:")));
        reconnectPanel.add(username = new JTextField(12));
        username.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_USER, ""));
        reconnectPanel.add(new JLabel(JDLocale.L("gui.config.fengshui.password", "Password:")));
        reconnectPanel.add(password = new JTextField(12));
        password.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_PASS, ""));
        panel.add(reconnectPanel, "spanx, pushx, growx, gapbottom :10:push");

        prog = new Progressor() {
            private static final long serialVersionUID = 1L;

            public int getMaximum() {
                return progress.getMaximum();
            }

            public String getMessage() {
                return null;
            }

            public int getMinimum() {
                return progress.getMinimum();
            }

            public String getString() {
                return progress.getString();
            }

            public int getValue() {
                return progress.getValue();
            }

            public void setMaximum(int value) {
                progress.setMaximum(value);

            }

            public void setMessage(String txt) {

            }

            public void setMinimum(int value) {
                progress.setMinimum(value);
            }

            public void setString(String txt) {

                progress.setString(txt);

            }

            public void setStringPainted(boolean v) {
                progress.setStringPainted(v);
            }

            public void setThread(Thread th) {

            }

            public void setValue(int value) {
                progress.setValue(value);
                if (value == 100) {
                    progress.removeAll();

                    panel.remove(15);
                    progress = null;
                    panel.invalidate();
                    panel.add(new JSeparator(), "span 3, pushx, growx", 15);
                    panel.validate();
                    panel.repaint();
                }
            }
        };

        getRouterIp();

        JPanel bpanel = new JPanel(new MigLayout());
        bpanel.add(new JSeparator(), "growy,spanx, pushx, growx, gapbottom :100:push");
        bpanel.add(more = new JButton(JDLocale.L("gui.config.fengshui.expertview", "expert view")), "tag help2");
        more.addActionListener(this);
        bpanel.add(apply = new JButton(JDLocale.L("gui.config.btn_save", "save")), "w pref!, tag apply");
        apply.addActionListener(this);
        bpanel.add(cancel = new JButton(JDLocale.L("gui.config.btn_cancel", "cancel")), "w pref!, tag cancel, wrap");
        cancel.addActionListener(this);
        panel.add(bpanel, "dock south, spanx, pushx, growx");

        return panel;
    }

    public void getRouterIp() {

        new Thread(new Runnable() {

            public void run() {
                if (routerIp == null || routerIp.matches("[\\s]*")) {
                    // System.out.println(routerIp);
                    ip.setText(new GetRouterInfo(prog).getAdress());

                }
                if (Reconnectmethode == null || Reconnectmethode.matches("[\\s]*")) {
                    if (GetRouterInfo.isFritzbox(ip.getText())) {
                        String tit = JDLocale.L("gui.config.fengshui.fritzbox.title", "Fritz!Box erkannt");
                        if (GetRouterInfo.isUpnp(ip.getText(), "49000")) {
                            JDUtilities.getGUI().showHTMLDialog(tit, JDLocale.L("gui.config.fengshui.fritzbox.upnpactive", "Sie haben eine Fritz!Box, der Reconnect läuft über Upnp.<br> Sie brauchen keinen Reconnecteinstellungen zu tätigen."));
                            Reconnectmethode = "[[[HSRC]]]\r\n" + "[[[STEP]]]\r\n" + "[[[REQUEST]]]\r\n" + "POST /upnp/control/WANIPConn1 HTTP/1.1\r\n" + "Host: %%%routerip%%%:49000\r\n" + "Content-Type: text/xml; charset=\"utf-8\"\r\n" + "SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\r\n" +

                            "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>\r\n" + "[[[/REQUEST]]]\r\n" + "[[[/STEP]]]\r\n" + "[[[/HSRC]]]\r\n";
                            routername.setText("!FRITZ BOX (All via UPNP)");
                        } else {
                            JDUtilities.getGUI().showHTMLDialog(tit, JDLocale.LF("gui.config.fengshui.fritzbox.upnpinactive", "Bitte aktivieren sie Upnp bei ihrer Fritz!Box <br><a href=\"http://%s\">zur Fritz!Box</a><br><a href=\"http://wiki.jdownloader.org/index.php?title=Fritz!Box_Upnp\">Wikiartikel: Fritz!Box Upnp</a>", ip.getText()));
                        }
                    }
                }
            }
        }).start();

    }

    public void save() {
        boolean saveit = false;
        boolean restart = false;
        if (!guiConfig.getProperty(SimpleGUI.PARAM_LOCALE, Locale.getDefault()).equals(languages.getSelectedItem())) {
            guiConfig.setProperty(SimpleGUI.PARAM_LOCALE, languages.getSelectedItem());
            guiConfig.save();
            restart = JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.fengshuiconfig.languages.restartwarning", "you have to restart jDownloader to change the language, restart jDownloader now?"));

        }
        if (!downloadDirectory.getText().equals(getDownloadDirectory())) {
            config.setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, downloadDirectory.getText());
            saveit = true;
        }
        if (!ip.getText().matches("[\\s]*") && !ip.getText().equals(routerIp)) {
            config.setProperty(Configuration.PARAM_HTTPSEND_IP, ip.getText());
            saveit = true;
        }
        if (!username.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_USER, ""))) {
            config.setProperty(Configuration.PARAM_HTTPSEND_USER, username.getText());
            saveit = true;
        }
        if (!password.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_PASS, ""))) {
            config.setProperty(Configuration.PARAM_HTTPSEND_PASS, password.getText());
            saveit = true;
        }
        if (Reconnectmethode != null && !Reconnectmethode.equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null))) {
            config.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, Reconnectmethode);
            saveit = true;
        }
        if (routername.getText() != null && !routername.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, ""))) {
            config.setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, routername.getText());
            saveit = true;
        }
        if (saveit) JDUtilities.saveConfig();
        if (restart) JDUtilities.restartJD();

    }
}
