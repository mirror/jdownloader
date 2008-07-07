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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist das Hauptfemster der Konfiguration. Sie verwaltet die
 * Tabpane.
 * 
 * @author JD-Team/astaldo
 * 
 */
public class ConfigurationDialog extends JFrame implements ActionListener, ChangeListener {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4046836223202290819L;

    private Configuration configuration;

    private JTabbedPane tabbedPane;

    private static ConfigurationDialog CURRENTDIALOG = null;

    private JButton btnSave;

    private JButton btnCancel;

    private UIInterface uiinterface;

    private Vector<ConfigPanel> configPanels = new Vector<ConfigPanel>();

    public static ConfigurationDialog DIALOG;

    public static Frame PARENTFRAME = null;

    private JCheckBox chbExpert;

    @SuppressWarnings("unchecked")
    private Vector<Class> configClasses = new Vector<Class>();

    private Vector<JPanel> containerPanels = new Vector<JPanel>();

    private SubConfiguration guiConfig;

    private JButton btnRestart;

    /**
     * @param parent
     * @param uiinterface
     */
    private ConfigurationDialog(JFrame parent, UIInterface uiinterface) {
        // super(parent);
        DIALOG = this;
        this.guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        PARENTFRAME = parent;
        this.uiinterface = uiinterface;
        setTitle(JDLocale.L("gui.config.title", "Konfiguration"));
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.configuration")));
        this.setName("CONFIGDIALOG");

        configuration = JDUtilities.getConfiguration();
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(null);
        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        String laf = UIManager.getLookAndFeel().getName().toLowerCase();
        if (laf.contains("nimbus") || laf.contains("gtk")) tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        if (System.getProperty("os.name").toLowerCase().contains("mac") && UIManager.getLookAndFeel().getClass().getName().equals(UIManager.getSystemLookAndFeelClassName())) {
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        }

        this.addConfigPanel(ConfigPanelGeneral.class, JDTheme.V("gui.images.config.home"), JDLocale.L("gui.config.tabLables.general", "General settings"));
        this.addConfigPanel(ConfigPanelDownload.class, JDTheme.V("gui.images.config.network_local"), JDLocale.L("gui.config.tabLables.download", "Download/Network settings"));
        this.addConfigPanel(ConfigPanelGUI.class, JDTheme.V("gui.images.config.gui"), JDLocale.L("gui.config.tabLables.gui", "Benutzeroberfläche"));
        this.addConfigPanel(ConfigPanelReconnect.class, JDTheme.V("gui.images.config.reconnect"), JDLocale.L("gui.config.tabLables.reconnect", "Reconnect settings"));
        this.addConfigPanel(ConfigPanelUnrar.class, JDTheme.V("gui.images.config.unrar"), JDLocale.L("gui.config.tabLables.unrar", "Archiv extract settings"));
        // this.addConfigPanel(ConfigPanelTweak.class,
        // JDTheme.I("gui.images.config.tip"),
        // JDLocale.L("gui.config.tabLables.tweak", "Leistung optimieren"));
        
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) {
            this.addConfigPanel(ConfigPanelCaptcha.class, JDTheme.V("gui.images.config.ocr", "ocr"), JDLocale.L("gui.config.tabLables.jac", "OCR Captcha settings"));
            this.addConfigPanel(ConfigPanelInfoFileWriter.class, JDTheme.V("gui.images.config.infoFile", "infoFile"), JDLocale.L("gui.config.tabLables.infoFileWriter", "'Info File Writer' settings"));
            this.addConfigPanel(ConfigPanelEventmanager.class, JDTheme.V("gui.images.config.eventmanager", "eventmanager"), JDLocale.L("gui.config.tabLables.eventManager", "Eventmanager"));
            this.addConfigPanel(ConfigPanelUpdater.class, JDTheme.V("gui.images.config.updater"), JDLocale.L("gui.config.tabLables.updater", "Update"));
            this.addConfigPanel(ConfigPanelPluginsOptional.class, JDTheme.V("gui.images.config.addons"), JDLocale.L("gui.config.tabLables.optionalPlugin", "Optional Plugin settings"));
            this.addConfigPanel(ConfigPanelPluginForContainer.class, JDTheme.V("gui.images.config.container"), JDLocale.L("gui.config.tabLables.containerPlugin", "Link-Container settings"));
        }
        this.addConfigPanel(ConfigPanelPluginForHost.class, JDTheme.V("gui.images.config.host"), JDLocale.L("gui.config.tabLables.hostPlugin", "Host Plugin settings"));
        this.addConfigPanel(ConfigPanelPluginForDecrypt.class, JDTheme.V("gui.images.config.decrypt"), JDLocale.L("gui.config.tabLables.decryptPlugin", "Decrypter Plugin settings"));
        this.addConfigPanel(ConfigPanelRessources.class, JDTheme.V("gui.images.config.packagemanager"), JDLocale.L("gui.config.tabLables.ressources", "Paketmanager"));

        this.addConfigPanel(ConfigPanelLinks.class, JDTheme.V("gui.images.config.tip"), JDLocale.L("gui.config.tabLables.links", "Wichtige Links"));
        
        try {
            tabbedPane.setFont(new Font("Courier", Font.PLAIN, 12));
            int maxLength = 0;
            int tabs = tabbedPane.getTabCount();
            for (int i = 0; i < tabs; i++) {maxLength = Math.max(maxLength, tabbedPane.getTitleAt(i).length());}
            for (int i = 0; i < tabs; i++) {tabbedPane.setTitleAt(i, fill(tabbedPane.getTitleAt(i), maxLength+1));}
        } catch (Exception e) {}
        
        btnSave = new JButton(JDLocale.L("gui.config.btn_save", "Speichern"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.config.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);
        btnRestart = new JButton(JDLocale.L("gui.config.btn_restart", "Speichern und neu starten"));
        btnRestart.addActionListener(this);
        chbExpert = new JCheckBox(JDLocale.L("gui.config.cbo_expert", "Experten Ansicht"));
        chbExpert.setSelected(guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false));
        chbExpert.addActionListener(this);
        chbExpert.setOpaque(false);

        setLayout(new GridBagLayout());
        // JDUtilities.addToGridBag(this, tabbedPane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        int n = 5;
        JXPanel cp = new JXPanel(new BorderLayout(n,n));
        int b = 12;
        cp.setBorder(new EmptyBorder(b,b,b,b));
        setContentPane(cp);
        JPanel sp = new JPanel(new BorderLayout(n,n));
        sp.setOpaque(false);
        JPanel btPanel = new JPanel(new FlowLayout(n,n, FlowLayout.RIGHT));
        btPanel.setOpaque(false);
        btPanel.add(btnRestart);
        btPanel.add(btnSave);
        btPanel.add(btnCancel);
        sp.add(chbExpert, BorderLayout.WEST);
        sp.add(btPanel, BorderLayout.EAST);
        // JDUtilities.addToGridBag(this, chbExpert, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(this, btPanel, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        cp.add(tabbedPane, BorderLayout.CENTER);
        cp.add(sp, BorderLayout.SOUTH);

        // JDUtilities.addToGridBag(this, btnSave, GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0,
        // null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        // JDUtilities.addToGridBag(this, btnCancel,
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE,
        // GridBagConstraints.EAST);
        JDUtilities.getLogger().info("" + JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.SELECTED_CONFIG_TAB, 0));
        tabbedPane.addChangeListener(this);
        if (configClasses.size()<=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.SELECTED_CONFIG_TAB, 0) ||JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.SELECTED_CONFIG_TAB, 0) == 0) {
            paintPanel(0);
        } else {
            tabbedPane.setSelectedIndex(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.SELECTED_CONFIG_TAB, 0));
        } // paintPanel();

        // there is already a pack below. Can't see how a second pack would improve things?
        // pack();

        LocationListener list = new LocationListener();
        this.addComponentListener(list);
        this.addWindowListener(list);
        // questionable if one needs this call to pack since restoreWindow does
        // a similar job. Only way this may hurt is by increasing the time it takes to 
        // make the dialog visible.
        setPreferredSize(new Dimension(640,640));
        pack();
        setLocationRelativeTo(null);
        // pack already calls validate implicitely. 
        // this.validate();
        SimpleGUI.restoreWindow(parent, null, this);
        // setVisible should be called after restoreWindow. Otherwise we have a
        // strange growing effect since the dialog is first made visible and then
        // assigned a new (possibly different) size by restoreWindow.
        this.setVisible(true);
    }

    private String fill(String s, int maxLength) {
        int add = maxLength - s.length();
        for (int i = 0; i < add; i++) {
            s += " ";
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private void paintPanel(int i) {

        if (i < configPanels.size() && configPanels.get(i) != null) {
           return;
        }
        if(i>containerPanels.size()-1)i=containerPanels.size()-1;
        Class class1 = configClasses.get(i);
        ConfigPanel panel = initSubPanel(class1);
        configPanels.remove(i);
        configPanels.add(i, panel);
        JPanel container = containerPanels.get(i);
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);
        // JDUtilities.addToGridBag(container, scrollPane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTH);
        container.add(panel, BorderLayout.CENTER);
//        container.add(scrollPane, BorderLayout.CENTER);
    }

    @SuppressWarnings("unchecked")
    public ConfigPanel initSubPanel(Class class1) {
        try {

            Class[] classes = new Class[] { Configuration.class, UIInterface.class };
            Constructor con = class1.getConstructor(classes);
            return (ConfigPanel) con.newInstance(new Object[] { configuration, uiinterface });
        }

        catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    /**
     * @author JD-Team Fügt einen neuen ConfigTab hinzu
     * @param configPanel
     */
    @SuppressWarnings("unchecked")
    private void addConfigPanel(Class configPanelClass, String img, String title) {

        this.configClasses.add(configPanelClass);

//        JPanel p = new JPanel(new GridBagLayout());
//        this.containerPanels.add(p);
//        configPanels.add(null);
//        JDUtilities.addToGridBag(p, new JLabel(title, new ImageIcon(JDUtilities.getImage(img)), SwingConstants.LEFT), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.FIRST_LINE_START);
//        JDUtilities.addToGridBag(p, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);

        int n = 10;
        JPanel p = new JPanel(new BorderLayout(n,n));
        p.setBorder(new EmptyBorder(n,n,n,n));
        this.containerPanels.add(p);
        configPanels.add(null);
        
        int m = 2;
        JPanel headerPanel = new JPanel(new BorderLayout(m,m));
        ImageIcon icon = new ImageIcon(JDUtilities.getImage(img));
//        headerPanel.add(new JLabel(title, icon, SwingConstants.LEFT), BorderLayout.NORTH);
        try {
            JLinkButton linkButton = new JLinkButton(title, icon, new URL(JDLocale.L("gui.configdialog.wikilink.theconfigurationmenu", "http://jdownloader.org/wiki/index.php?title=Konfig:")+title.replaceAll("\\s", "_")));
            linkButton.setBorder(null);
            headerPanel.add(linkButton, BorderLayout.WEST);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        headerPanel.add(new JXTitledSeparator(""), BorderLayout.SOUTH);
        p.add(headerPanel , BorderLayout.NORTH);
        
        
        tabbedPane.addTab(title, JDUtilities.getscaledImageIcon(icon, 20, -1), p);       
    }

    /**
     * Zeigt die Konfiguration an
     * 
     * @param frame
     * @param uiinterface
     * @return
     */
    public static boolean showConfig(final JFrame frame, final UIInterface uiinterface) {
        if (CURRENTDIALOG != null && CURRENTDIALOG.isVisible()) return false;

        CURRENTDIALOG = new ConfigurationDialog(frame, uiinterface);

        return true;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRestart) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) configPanels.elementAt(i).save();
            }
            JDUtilities.setConfiguration(configuration);
            JDUtilities.saveConfig();
            JDUtilities.restartJD();

        }

        if (e.getSource() == btnSave) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) configPanels.elementAt(i).save();
            }
            JDUtilities.setConfiguration(configuration);
            JDUtilities.saveConfig();

        }
        if (e.getSource() == this.chbExpert) {
            guiConfig.setProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, chbExpert.isSelected());
            JDUtilities.saveConfig();

        }

        this.dispose();

        setVisible(false);
    }

    public void stateChanged(ChangeEvent e) {

        int index = tabbedPane.getSelectedIndex();
        JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).setProperty(SimpleGUI.SELECTED_CONFIG_TAB, index);
        paintPanel(index);
        JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
        validate();
        // pack();

    }

}
