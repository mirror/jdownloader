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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledSeparator;

/**
 * Diese Klasse ist das Hauptfemster der Konfiguration. Sie verwaltet die
 * Tabpane.
 * 
 * @author JD-Team/astaldo
 * 
 */
public class ConfigurationDialog extends JFrame implements ActionListener, ChangeListener {
    private static ConfigurationDialog CURRENTDIALOG = null;

    public static ConfigurationDialog DIALOG;

    public static Frame PARENTFRAME = null;

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4046836223202290819L;

    /**
     * Zeigt die Konfiguration an
     * 
     * @param frame
     * @return
     */
    public static boolean showConfig(final JFrame frame) {
        if (CURRENTDIALOG != null && CURRENTDIALOG.isVisible()) return false;
        CURRENTDIALOG = new ConfigurationDialog(frame);
        return true;
    }

    private JButton btnFengShuiConfig;

    private JButton btnCancel;

    private JButton btnRestart;

    private JButton btnSave;

    private Vector<Class<?>> configClasses = new Vector<Class<?>>();

    private Vector<ConfigPanel> configPanels = new Vector<ConfigPanel>();

    private Configuration configuration;

    private SubConfiguration subConfig;

    private Vector<JPanel> containerPanels = new Vector<JPanel>();

    private JTabbedPane tabbedPane;

    private ConfigurationDialog(JFrame parent) {

        DIALOG = this;
        PARENTFRAME = parent;

        setTitle(JDLocale.L("gui.config.title", "Konfiguration"));
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.configuration")));
        setName("CONFIGDIALOG");

        configuration = JDUtilities.getConfiguration();
        subConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(null);

        String laf = UIManager.getLookAndFeel().getName().toLowerCase();
        if (laf.contains("nimbus") || laf.contains("gtk")) {
            tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        }

        if (System.getProperty("os.name").toLowerCase().contains("mac") && UIManager.getLookAndFeel().getClass().getName().equals(UIManager.getSystemLookAndFeelClassName())) {
            tabbedPane.setTabPlacement(SwingConstants.TOP);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        } else {
            tabbedPane.setTabPlacement(SwingConstants.LEFT);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }

        addConfigPanel(ConfigPanelGeneral.class, JDTheme.V("gui.images.config.home"), JDLocale.L("gui.config.tabLables.general", "General settings"));
        addConfigPanel(ConfigPanelDownload.class, JDTheme.V("gui.images.config.network_local"), JDLocale.L("gui.config.tabLables.download", "Download/Network settings"));
        addConfigPanel(ConfigPanelGUI.class, JDTheme.V("gui.images.config.gui"), JDLocale.L("gui.config.tabLables.gui", "Benutzeroberfläche"));
        addConfigPanel(ConfigPanelReconnect.class, JDTheme.V("gui.images.config.reconnect"), JDLocale.L("gui.config.tabLables.reconnect", "Reconnect settings"));
        addConfigPanel(ConfigPanelCaptcha.class, JDTheme.V("gui.images.config.ocr"), JDLocale.L("gui.config.tabLables.jac", "OCR Captcha settings"));
        addConfigPanel(ConfigPanelPluginForHost.class, JDTheme.V("gui.images.config.host"), JDLocale.L("gui.config.tabLables.hostPlugin", "Host Plugin settings"));
        addConfigPanel(ConfigPanelPluginForDecrypt.class, JDTheme.V("gui.images.config.decrypt"), JDLocale.L("gui.config.tabLables.decryptPlugin", "Decrypter Plugin settings"));
        addConfigPanel(ConfigPanelAddons.class, JDTheme.V("gui.images.config.packagemanager"), JDLocale.L("gui.config.tabLables.addons", "Addon manager"));
        addConfigPanel(ConfigPanelPluginForContainer.class, JDTheme.V("gui.images.config.container"), JDLocale.L("gui.config.tabLables.containerPlugin", "Link-Container settings"));
        addConfigPanel(ConfigPanelEventmanager.class, JDTheme.V("gui.images.config.eventmanager"), JDLocale.L("gui.config.tabLables.eventManager", "Eventmanager"));

        try {
            tabbedPane.setFont(new Font("Courier", Font.PLAIN, 12));
            int maxLength = 0;
            int tabs = tabbedPane.getTabCount();
            for (int i = 0; i < tabs; i++) {
                maxLength = Math.max(maxLength, tabbedPane.getTitleAt(i).length());
            }
            for (int i = 0; i < tabs; i++) {
                tabbedPane.setTitleAt(i, fill(tabbedPane.getTitleAt(i), maxLength + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnSave = new JButton(JDLocale.L("gui.btn_save", "Speichern"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);
        btnRestart = new JButton(JDLocale.L("gui.config.btn_restart", "Speichern und neu starten"));
        btnRestart.addActionListener(this);
        btnFengShuiConfig = new JButton(JDLocale.L("gui.config.btn_fengshui", "Vereinfachte Ansicht"));
        btnFengShuiConfig.addActionListener(this);

        setLayout(new GridBagLayout());

        int n = 5;

        JPanel btPanel = new JPanel(new FlowLayout(n, n, FlowLayout.RIGHT));
        btPanel.add(btnRestart);
        btPanel.add(btnSave);
        btPanel.add(btnCancel);

        JPanel btPanelLeft = new JPanel(new FlowLayout(n, n, FlowLayout.RIGHT));
        btPanelLeft.add(btnFengShuiConfig);

        JPanel sp = new JPanel(new BorderLayout(n, n));
        sp.add(btPanel, BorderLayout.EAST);
        sp.add(btPanelLeft, BorderLayout.WEST);

        JXPanel cp = new JXPanel(new BorderLayout(n, n));
        cp.setBorder(new EmptyBorder(n, n, n, n));
        cp.add(tabbedPane, BorderLayout.CENTER);
        cp.add(sp, BorderLayout.SOUTH);
        setContentPane(cp);

        tabbedPane.addChangeListener(this);
        int lastTab = subConfig.getIntegerProperty(SimpleGUI.SELECTED_CONFIG_TAB, 0);
        if (configClasses.size() <= lastTab || lastTab == 0) {
            paintPanel(0);
        } else {
            tabbedPane.setSelectedIndex(lastTab);
        }

        addWindowListener(new LocationListener());
        setLocationRelativeTo(null);
        SimpleGUI.restoreWindow(parent, this);
        setPreferredSize(new Dimension(Math.max(getWidth(), 800), Math.max(getHeight(), 640)));
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRestart) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) {
                    configPanels.elementAt(i).save();
                }
            }
            configuration.save();
            JDUtilities.setConfiguration(configuration);
            JDUtilities.restartJD();
        } else if (e.getSource() == btnSave || e.getSource() == btnFengShuiConfig) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) {
                    configPanels.elementAt(i).save();
                }
            }
            configuration.save();
            JDUtilities.setConfiguration(configuration);
        }

        setVisible(false);
        dispose();

        if (e.getSource() == btnFengShuiConfig) {
            SimpleGUI.CURRENTGUI.getGuiConfig().setProperty(SimpleGUI.PARAM_SHOW_FENGSHUI, true);
            SimpleGUI.CURRENTGUI.showConfig();
        }
    }

    /**
     * Fügt einen neuen ConfigTab hinzu
     * 
     * @param configPanelClass
     * @param img
     * @param title
     * @author JD-Team
     */
    private void addConfigPanel(Class<?> configPanelClass, String img, String title) {
        configClasses.add(configPanelClass);

        int n = 10;
        JPanel p = new JPanel(new BorderLayout(n, n));
        p.setBorder(new EmptyBorder(n, n, n, n));
        containerPanels.add(p);
        configPanels.add(null);

        int m = 2;
        JPanel headerPanel = new JPanel(new BorderLayout(m, m));
        ImageIcon icon = new ImageIcon(JDUtilities.getImage(img));

        try {
            JLinkButton linkButton = new JLinkButton(title, icon, new URL(JDLocale.L("gui.configdialog.wikilink.theconfigurationmenu", "http://jdownloader.org/wiki/index.php?title=Konfig:") + title.replaceAll("\\s", "_")));
            linkButton.setBorder(null);
            headerPanel.add(linkButton, BorderLayout.WEST);
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }
        headerPanel.add(new JXTitledSeparator(""), BorderLayout.SOUTH);
        p.add(headerPanel, BorderLayout.NORTH);

        tabbedPane.addTab(title, JDUtilities.getScaledImageIcon(icon, 20, -1), p);
    }

    private String fill(String s, int maxLength) {
        int add = maxLength - s.length();
        StringBuilder st = new StringBuilder(s);
        for (int i = 0; i < add; i++) {
            st.append(" ");
        }
        return st.toString();
    }

    public ConfigPanel initSubPanel(Class<?> class1) {
        try {
            Constructor<?> con = class1.getConstructor(new Class[] { Configuration.class });
            return (ConfigPanel) con.newInstance(new Object[] { configuration });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void paintPanel(int i) {

        if (i < configPanels.size() && configPanels.get(i) != null) return;
        if (i > containerPanels.size() - 1) {
            i = containerPanels.size() - 1;
        }

        ConfigPanel panel = initSubPanel(configClasses.get(i));
        configPanels.remove(i);
        configPanels.add(i, panel);
        containerPanels.get(i).add(panel, BorderLayout.CENTER);
    }

    public void stateChanged(ChangeEvent e) {

        int index = tabbedPane.getSelectedIndex();
        subConfig.setProperty(SimpleGUI.SELECTED_CONFIG_TAB, index);
        subConfig.save();
        paintPanel(index);
        validate();

    }

}