package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.UpdateController;

public abstract class ExtensionModule extends Module {

    private Header         header;
    private JLabel         icon;
    private ExtTextArea    desc;
    private JTextArea      txt;
    private JButton        install;

    private AbstractButton uninstall;
    private JProgressBar   bar;
    private String         id;
    private static long    LAST_INSTALLACTION;

    public ExtensionModule(String s) {
        super("ins 0,wrap 1", "[grow,fill]", "[][]");
        id = s;
        header = new Header(createTitle(), null);

        uninstall = new JButton(_GUI._.ExtensionModule_ExtensionModule_uninstall_());
        uninstall.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread("InstallException") {

                    public void run() {
                        install(_GUI._.ExtensionModule_run_uninstalling());
                        try {
                            LAST_INSTALLACTION = System.currentTimeMillis();
                            while (System.currentTimeMillis() - LAST_INSTALLACTION < 5000) {
                                Thread.sleep(100);
                            }
                            UpdateController.getInstance().runExtensionUnInstallation(getID());
                            while (true) {
                                Thread.sleep(500);
                                if (!UpdateController.getInstance().isRunning()) break;
                                UpdateController.getInstance().waitForUpdate();
                            }

                        } catch (Exception e) {
                            Log.exception(e);
                        } finally {
                            restore();
                        }
                    }

                }.start();
            }
        });

        install = new JButton(_GUI._.ExtensionModule_ExtensionModule_install_());
        install.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread("InstallException") {
                    public void run() {
                        install(_GUI._.ExtensionModule_run_installing_());

                        try {
                            LAST_INSTALLACTION = System.currentTimeMillis();
                            while (System.currentTimeMillis() - LAST_INSTALLACTION < 5000) {
                                Thread.sleep(100);
                            }
                            UpdateController.getInstance().runExtensionInstallation(getID());
                            while (true) {
                                Thread.sleep(500);
                                if (!UpdateController.getInstance().isRunning()) break;
                                UpdateController.getInstance().waitForUpdate();
                            }
                        } catch (Exception e) {
                            Log.exception(e);
                        } finally {
                            restore();

                        }
                    }

                }.start();

            }
        });
        if (UpdateController.getInstance().isExtensionInstalled(getID())) {
            install.setEnabled(false);
        } else {
            uninstall.setEnabled(false);

        }

        icon = new JLabel(NewTheme.I().getIcon(getIconKey(), 32));
        txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        // txt.setEnabled(false);

        txt.setText(getDescription());
        add(header, "gapbottom 5");

        add(icon, "split 2,width 32!,gapleft 22,gapright 10");
        // txt.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        add(txt, "gaptop 0,spanx,growx,pushx,gapbottom 15,wmin 10, aligny top");

        if (!UpdateController.getInstance().isHandlerSet()) {
            install.setEnabled(false);
            uninstall.setEnabled(false);
        }
        MigPanel tb = new MigPanel("ins 0", "[grow,fill]0[]0[]", "[]");
        tb.setOpaque(false);
        tb.setBackground(null);
        tb.add(Box.createHorizontalGlue());
        bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setVisible(false);
        tb.add(bar, "hidemode 3");
        tb.add(install, "sg 1,hidemode 3");
        tb.add(uninstall, "sg 1,hidemode 3,gapleft 3");
        add(tb);

    }

    public void restore() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                bar.setIndeterminate(false);
                bar.setEnabled(false);
                bar.setString(_GUI._.ExtensionModule_runInEDT_restart_required_());
                // bar.setVisible(false);
                // install.setVisible(true);
                // uninstall.setVisible(true);
                //
                // if (UpdateController.getInstance().isExtensionInstalled(getID())) {
                // install.setEnabled(false);
                // uninstall.setEnabled(false);
                // } else {
                // install.setEnabled(false);
                // uninstall.setEnabled(false);
                //
                // }

            }
        };
    }

    private String createTitle() {
        if (UpdateController.getInstance().isExtensionInstalled(getID())) { return _GUI._.ExtensionModule_createTitle_installed(getTitle()); }
        return _GUI._.ExtensionModule_createTitle_not_installed(getTitle());
    }

    public String getID() {
        return id;
    }

    protected abstract String getTitle();

    protected abstract String getIconKey();

    protected abstract String getDescription();

    public void install(final String string) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                bar.setPreferredSize(new Dimension(install.getSize().width + uninstall.getSize().width + 2, install.getSize().height));

                bar.setStringPainted(true);
                bar.setVisible(true);
                install.setVisible(false);
                uninstall.setVisible(false);

                bar.setString(string);
            }
        };
    }

}
