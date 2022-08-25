//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.gui.swing.dialog;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import jd.SecondLevelLaunch;
import jd.controlling.ClipboardMonitoring;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.gui.swing.Factory;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.io.JDIO;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.JavaSSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.os.ContainerRuntime;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.Snap;
import org.appwork.utils.os.hardware.HardwareType;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class AboutDialog extends AbstractDialog<Integer> {
    private int labelHeight = 0;

    public AboutDialog() {
        super(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.BUTTONS_HIDE_OK | Dialog.STYLE_HIDE_ICON, _GUI.T.jd_gui_swing_components_AboutDialog_title(), null, null, null);
    }

    @Override
    protected Integer createReturnValue() {
        return null;
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    public static Thread showNonBlocking() {
        final Thread thread = new Thread("AboutDialog") {
            {
                setDaemon(true);
            }

            @Override
            public void run() {
                final AboutDialog aboutDialog = new EDTHelper<AboutDialog>() {
                    @Override
                    public AboutDialog edtRun() {
                        final AboutDialog ret = new AboutDialog();
                        ret.setModalityType(ModalityType.MODELESS);
                        return ret;
                    }
                }.getReturnValue();
                try {
                    Dialog.getInstance().showDialog(aboutDialog);
                } catch (DialogNoAnswerException e1) {
                }
            }
        };
        thread.start();
        return thread;
    }

    public static void main(String[] args) throws Exception {
        Application.setApplication(".jd_home");
        showNonBlocking().join();
    }

    private static String JNA_VERSION_STRING = null;
    private static Double BC_VERSION         = null;

    @Override
    public JComponent layoutDialogContent() {
        this.labelHeight = new JLabel("HeightTester").getPreferredSize().height;
        final JPanel contentpane = new JPanel();
        JLabel lbl = new JLabel("JDownloader® 2", JLabel.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize() * 2.0f));
        final JPanel links1stRow = new JPanel(new MigLayout("ins 0", "[]push[]push[]push[]"));
        final JPanel links2ndRow = new JPanel(new MigLayout("ins 0", "push[]push[]push"));
        try {
            final File file = Application.getResource("licenses/jdownloader.license");
            if (file.isFile()) {
                final JButton btn = Factory.createButton(_GUI.T.jd_gui_swing_components_AboutDialog_license(), new AbstractIcon(IconKey.ICON_PREMIUM, 16), new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final String license = JDIO.readFileToString(file);
                        try {
                            final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.jd_gui_swing_components_AboutDialog_license_title(), license, null, null, null) {
                                @Override
                                protected boolean isResizable() {
                                    return true;
                                }
                            };
                            d.setPreferredSize(JDGui.getInstance().getMainFrame().getSize());
                            Dialog.getInstance().showDialog(d);
                        } catch (DialogNoAnswerException ignore) {
                        }
                    }
                });
                btn.setBorder(null);
                links2ndRow.add(btn);
            }
            links1stRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_homepage(), new AbstractIcon(IconKey.ICON_HOME, 16), new URL("https://jdownloader.org/home")));
            links1stRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_forum(), new AbstractIcon(IconKey.ICON_BOARD, 16), new URL("https://board.jdownloader.org/")));
            links1stRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_ticket(), new AbstractIcon(IconKey.ICON_BOARD, 16), new URL("https://support.jdownloader.org/")));
            links1stRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_uninstall(), new AbstractIcon(IconKey.ICON_CLEAR, 16), new URL("https://support.jdownloader.org/Knowledgebase/Article/View/how-can-i-uninstall-jdownloader")));
            links2ndRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_contributers(), new AbstractIcon(IconKey.ICON_EDIT, 16), new URL("https://support.jdownloader.org/Knowledgebase/Article/View/setup-ide-eclipse")));
            links2ndRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_imprint(), new AbstractIcon(IconKey.ICON_ABOUT, 16), new URL("https://jdownloader.org/impressum")));
            links2ndRow.add(new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_privacy(), new AbstractIcon(IconKey.ICON_ABOUT, 16), new URL("https://my.jdownloader.org/legal/privacy.html#jdownloader")));
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        contentpane.setLayout(new MigLayout("ins 10, wrap 1", "[grow,fill]"));
        contentpane.add(new JLabel(new AbstractIcon(IconKey.ICON_LOGO_JD_LOGO_64_64, -1)), "aligny center, spany 6");
        contentpane.add(lbl, "");
        MigPanel stats = new MigPanel("ins 0,wrap 2", "[][grow,align right]", "[]");
        contentpane.add(stats, "pushx,growx,spanx");
        Map<String, Object> map = null;
        try {
            stats.add(createLink(_GUI.T.jd_gui_swing_components_AboutDialog_trademark(), "https://jdownloader.org/impressum"), "spanx,alignx center");
            try {
                final File buildJson = Application.getResource("build.json");
                if (buildJson.isFile()) {
                    map = JSonStorage.restoreFromString(IO.readFileToString(buildJson), TypeRef.HASHMAP);
                }
            } catch (Exception e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            if (map != null) {
                stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_builddate()));
                stats.add(createLink(map.get("buildDate")));
            }
            stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_runtime()));
            stats.add(createLink(TimeFormatter.formatMilliSeconds(Time.systemIndependentCurrentJVMTimeMillis() - SecondLevelLaunch.startup, 0)));
            try {
                stats.add(new JLabel("Java:"), "");
                final java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                ExtButton comp;
                stats.add(comp = createLink(System.getProperty("java.vendor") + " - " + System.getProperty("java.runtime.name") + " - " + System.getProperty("java.version") + (Application.is64BitJvm() ? "(64bit/" : "(32bit/") + CrossSystem.getARCHFamily() + ")"));
                comp.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.showInExplorer(new File(CrossSystem.getJavaBinary()));
                        try {
                            final java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                            final List<String> arguments = runtimeMxBean.getInputArguments();
                            final StringBuilder sb = new StringBuilder();
                            for (String s : arguments) {
                                if (sb.length() > 0) {
                                    sb.append(" ");
                                }
                                sb.append(s);
                            }
                            final StringSelection selection = new StringSelection(sb.toString());
                            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(selection, selection);
                        } catch (final Throwable e1) {
                        }
                    }
                });
                try {
                    final java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                    final List<String> arguments = runtimeMxBean.getInputArguments();
                    final StringBuilder sb = new StringBuilder();
                    for (String s : arguments) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append(s);
                    }
                    comp.setToolTipText(sb.toString());
                } catch (final Throwable e1) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                }
                stats.add(new JLabel("OS:"), "");
                stats.add(createLink(CrossSystem.getOSFamily() + "(" + CrossSystem.getOS() + ")" + (CrossSystem.is64BitOperatingSystem() ? "(64bit)" : "(32bit)")));
                stats.add(new JLabel("Memory:"), "");
                stats.add(comp = createLink("Usage: " + SizeFormatter.formatBytes(memory.getUsed()) + " - Allocated: " + SizeFormatter.formatBytes(memory.getCommitted()) + " - Max: " + SizeFormatter.formatBytes(memory.getMax())));
                try {
                    final List<MemoryPoolMXBean> memoryPoolMXBeans = java.lang.management.ManagementFactory.getMemoryPoolMXBeans();
                    final StringBuilder sb = new StringBuilder();
                    for (final MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append("Pool:").append(memoryPoolMXBean.getName()).append("\r\n");
                        sb.append("Type:").append(memoryPoolMXBean.getType()).append("\r\n");
                        sb.append("Managed by:").append(Arrays.toString(memoryPoolMXBean.getMemoryManagerNames())).append("\r\n");
                        final MemoryUsage collectionUsage = memoryPoolMXBean.getCollectionUsage();
                        if (collectionUsage != null) {
                            sb.append("Usage: " + SizeFormatter.formatBytes(collectionUsage.getUsed()) + " - Allocated: " + SizeFormatter.formatBytes(collectionUsage.getCommitted()) + " - Max: " + SizeFormatter.formatBytes(collectionUsage.getMax()));
                        }
                        sb.append("\r\n");
                    }
                    comp.setToolTipText(sb.toString());
                } catch (final Throwable e1) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                }
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_installdir()), "");
            ExtButton bt;
            final File directory = Application.getResource(".");
            final String fullPath = "<html><u>" + directory + "</u></html>";
            final String anonPath = "<html><u>" + _GUI.T.jd_gui_swing_components_AboutDialog_installdir_anon() + "</u></html>";
            stats.add(bt = createLink(anonPath));
            final ExtButton directoryButton = bt;
            bt.addMouseListener(new MouseListener() {
                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    directoryButton.setText(anonPath);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    directoryButton.setText(fullPath);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                }
            });
            bt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(directory);
                }
            });
            if (Snap.isInsideSnap() || ContainerRuntime.isInsideContainer() || HardwareType.getHardware() != null) {
                stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_environment()), "spanx");
                if (HardwareType.getHardware() != null) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_hardware()), "gapleft 10");
                    stats.add(createLink(HardwareType.getHardware().toString()));
                }
                if (Snap.isInsideSnap()) {
                    stats.add(new JLabel("Snap:"), "gapleft 10");
                    stats.add(createLink(Snap.getSnapInstanceName()));
                }
                if (ContainerRuntime.isInsideContainer()) {
                    stats.add(new JLabel(ContainerRuntime.getType() + ":"), "gapleft 10");
                    stats.add(createLink(ContainerRuntime.getID()));
                }
            }
            if (map != null) {
                stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_sourcerevisions()), "spanx");
                if (map.containsKey("JDownloaderRevision")) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_core()), "gapleft 10");
                    stats.add(createLink("#" + map.get("JDownloaderRevision"), "https://svn.jdownloader.org/build.php?check=" + map.get("JDownloaderRevision")));
                }
                if (map.containsKey("JDownloaderUpdaterRevision")) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_launcher()), "gapleft 10");
                    stats.add(createLink("#" + map.get("JDownloaderUpdaterRevision")));
                }
                if (map.containsKey("AppWorkUtilsRevision")) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_appworkutilities()), "gapleft 10");
                    stats.add(createLink("#" + map.get("AppWorkUtilsRevision")));
                }
                if (map.containsKey("JDBrowserRevision")) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_browser()), "gapleft 10");
                    stats.add(createLink("#" + map.get("JDBrowserRevision")));
                }
                if (map.containsKey("UpdateClientV2Revision")) {
                    stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_updater()), "gapleft 10");
                    stats.add(createLink("#" + map.get("UpdateClientV2Revision")));
                }
            }
        } catch (Throwable t) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(t);
        }
        contentpane.add(lbl = new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_mopdules()), "gaptop 5, spanx");
        stats = new MigPanel("ins 0 10 0 0,wrap 2", "[][grow,align right]", "[]");
        contentpane.add(stats, "pushx,growx,spanx");
        stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_core()), "");
        stats.add(createLink("Copyright \u00A9 2009-2022 AppWork GmbH"));
        stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_plugins()), "");
        stats.add(createLink("Copyright \u00A9 2009-2022 JDownloader Community"));
        stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_translations()), "");
        stats.add(createLink("Copyright \u00A9 2009-2022 JDownloader Community"));
        try {
            if (JNA_VERSION_STRING == null) {
                final Class<?> clazz = Class.forName("com.sun.jna.Native");
                final Field version = ReflectionUtils.getField("com.sun.jna.Version", "VERSION", clazz, String.class);
                final Field versionNative = ReflectionUtils.getField("com.sun.jna.Version", "VERSION_NATIVE", clazz, String.class);
                final String jnaVersion = (String) version.get(clazz);
                final String jnaNativeVersion = (String) versionNative.get(clazz);
                JNA_VERSION_STRING = jnaVersion + "/" + jnaNativeVersion;
            }
            stats.add(new JLabel("Java Native Access:"), "");
            stats.add(createLink("JNA " + JNA_VERSION_STRING, "https://github.com/java-native-access/jna"));
        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        try {
            final SSLSocketStreamFactory ssl = HTTPConnectionImpl.getDefaultSSLSocketStreamFactory();
            if (!(ssl instanceof JavaSSLSocketStreamFactory)) {
                if (BC_VERSION == null) {
                    BC_VERSION = new BouncyCastleProvider().getVersion();
                }
                stats.add(new JLabel("TLS:"), "");
                stats.add(createLink("BouncyCastle " + BC_VERSION, "https://www.bouncycastle.org/latest_releases.html"));
            }
        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        if ("SIMPLEUPNP".equals(ReconnectPluginController.getInstance().getActivePlugin().getID())) {
            stats.add(new JLabel("UPNP:"), "");
            stats.add(createLink("Cling", "https://github.com/4thline/cling"));
        }
        stats.add(new JLabel("Extraction:"));
        final JPanel extraction = new JPanel(new MigLayout("ins 0,wrap 2"));
        extraction.add(createLink("7ZipJBindings (" + get7ZipJBindingDetails() + ")", "https://github.com/borisbrodski/sevenzipjbinding"));
        extraction.add(createLink("Zip4J 1.3.3", "https://github.com/srikanth-lingala/zip4j"));
        stats.add(extraction);
        final LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null) {
            stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_laf()), "");
            if (StringUtils.containsIgnoreCase(laf.getID(), "FlatLaf")) {
                stats.add(createLink(laf.getName(), "https://www.formdev.com/flatlaf/"));
            } else if (StringUtils.containsIgnoreCase(laf.getID(), "Synthetica")) {
                stats.add(createLink(laf.getName(), "https://www.jyloo.com/synthetica/"));
                try {
                    final Object info = UIManager.get("Synthetica.license.info");
                    if (info instanceof String[]) {
                        final String license = StringUtils.join(Arrays.asList((String[]) info), "\r");
                        final String Licensee = new Regex(license, "Licensee\\s*=\\s*([^\r\n]+)").getMatch(0);
                        final String LicenseRegistrationNumber = new Regex(license, "LicenseRegistrationNumber\\s*=\\s*([^\r\n]+)").getMatch(0);
                        stats.add(createLink(_GUI.T.jd_gui_swing_components_AboutDialog_synthetica2(Licensee + "(#" + LicenseRegistrationNumber + ")"), "https://www.jyloo.com/synthetica/license/"), "skip");
                    }
                } catch (Throwable ignore) {
                }
            } else if (StringUtils.containsIgnoreCase(laf.getID(), "Substance")) {
                stats.add(createLink(laf.getName(), "https://github.com/kirill-grouchnikov/radiance"));
            } else {
                stats.add(createLink(laf.getName(), ""));
            }
        }
        stats.add(new JLabel(_GUI.T.jd_gui_swing_components_AboutDialog_icons()), "");
        stats.add(createLink("See /themes/* folder for Icon Licenses"), "");
        final JPanel icons = new JPanel(new MigLayout("ins 0,wrap 3"));
        icons.add(createLink("Icons8", "https://icons8.com"));
        icons.add(createLink("Tango Icons", "https://en.wikipedia.org/wiki/Tango_Desktop_Project"));
        icons.add(createLink("FatCow-Farm Fresh Icons", "https://www.fatcow.com/free-icons"));
        icons.add(createLink("Mimi Glyphs Set", "http://salleedesign.com/blog/mimi-glyphs/"));
        icons.add(createLink("Bright Mix Set", "http://brightmix.com/blog/brightmix-icon-set-free-for-all/"));
        icons.add(createLink("Picol Icon Set", "http://www.picol.org/"));
        icons.add(createLink("Aha Soft Icon Set", "http://www.aha-soft.com"));
        icons.add(createLink("Oxygen Team", "https://techbase.kde.org/Projects/Oxygen/Licensing"));
        icons.add(createLink("further icons by AppWork GmbH"), "skip 2");
        icons.add(createLink("& the JDownloader Community"));
        contentpane.add(icons);
        contentpane.add(links1stRow, "gaptop 5, growx, pushx, spanx");
        contentpane.add(links2ndRow, "growx, pushx, spanx");
        this.registerEscape(contentpane);
        return contentpane;
    }

    private String get7ZipJBindingDetails() {
        String version = "4.65";
        try {
            version = ReflectionUtils.invoke("net.sf.sevenzipjbinding.SevenZip", "getSevenZipJBindingVersion", null, String.class, new Object[0]);
            final String usedPlatform = ReflectionUtils.invoke("net.sf.sevenzipjbinding.SevenZip", "getUsedPlatform", null, String.class, new Object[0]);
            if (usedPlatform != null) {
                return version + "/" + usedPlatform;
            }
        } catch (final Throwable ignore1) {
        }
        return version;
    }

    private ExtButton createLink(final Object object, final String url) {
        ExtButton ret = new ExtButton(new AppAction() {
            /**
             *
             */
            private static final long serialVersionUID = 2L;
            {
                if (StringUtils.startsWithCaseInsensitive(url, "http") && CrossSystem.isOpenBrowserSupported()) {
                    setName("<html><u>" + object + "</u></html>");
                } else {
                    setName(String.valueOf(object));
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ClipboardMonitoring.getINSTANCE().setCurrentContent(getName());
                if (StringUtils.startsWithCaseInsensitive(url, "http") && CrossSystem.isOpenBrowserSupported()) {
                    CrossSystem.openURL(url);
                } else {
                    BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
                        @Override
                        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                            return new BasicNotify(_GUI.T.lit_clipboard(), _GUI.T.AboutDialog_actionPerformed_clipboard_(getName()), new AbstractIcon(IconKey.ICON_CLIPBOARD, 20));
                        }
                    });
                }
            }
        });
        ret.setBorderPainted(false);
        ret.setContentAreaFilled(false);
        ret.setEnabled(true);
        ret.setMaximumSize(new Dimension(1000, labelHeight));
        return ret;
    }

    private ExtButton createLink(final Object object) {
        return createLink(object, null);
    }
}