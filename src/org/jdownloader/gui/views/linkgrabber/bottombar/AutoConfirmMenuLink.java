package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import jd.controlling.linkcollector.LinkCollector;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.KeyUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.AutoStartOptions;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnOfflineLinksAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoConfirmMenuLink extends MenuItemData implements MenuLink, SelfLayoutInterface {

    private static final String        ASSIGN_PRIORITY_ENABLED  = "AssignPriorityEnabled";

    private static final String        CLEAR_LIST_AFTER_CONFIRM = "ClearListAfterConfirm";

    private static final String        FORCE_DOWNLOADS          = "ForceDownloads";

    private static final String        HANDLE_OFFLINE           = "HandleOffline";

    private static final String        PIORITY2                 = "Piority";

    protected static AutoConfirmButton INSTANCE;

    public static final String         AUTO_START               = "autoStart";

    @Override
    public List<AppAction> createActionsToLink() {
        ArrayList<AppAction> ret = new ArrayList<AppAction>();
        // DownloadsTableSearchField item = DownloadsTableSearchField.getInstance();
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        try {
            ActionData ad = this.getActionData();
            if (ad != null) {
                Object sc = ad.fetchSetup("shortcut");
                if (sc != null && sc instanceof String) {

                    ks = KeyStroke.getKeyStroke((String) sc);
                }
            }
        } catch (Throwable e) {
        }
        AppAction a = new AppAction() {
            @Override
            public boolean isEnabled() {
                return LinkCollector.getInstance().getAutoStartManager().isRunning();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                LinkCollector.getInstance().getAutoStartManager().interrupt();
            }
        };
        a.setAccelerator(ks);
        ret.add(a);
        return ret;
    }

    @Override
    public String createConstraints() {
        return "height 24!,width 24!,hidemode 3,gapright 3";
    }

    @Override
    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                INSTANCE = new AutoConfirmButton();
                updateButton();

            }
        }.waitForEDT();

        return INSTANCE;

    }

    @Override
    public JComponent createSettingsPanel() {

        ActionData ad = ensureActionData();

        final ActionData actionData = ad;
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        SwingUtils.setOpaque(p, false);
        final ExtTextField shortcut = new ExtTextField();
        shortcut.setHelpText(_GUI._.InfoPanel_InfoPanel_shortcuthelp2());
        shortcut.setEditable(false);
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        try {
            Object sc = ad.fetchSetup("shortcut");
            if (sc != null && sc instanceof String) {

                ks = KeyStroke.getKeyStroke((String) sc);
            }
        } catch (Throwable e) {
        }
        String msg1 = KeyUtils.getShortcutString(ks, true);

        shortcut.setText(msg1);
        shortcut.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent event) {
                String msg1 = KeyUtils.getShortcutString(event, true);
                KeyStroke currentShortcut = KeyStroke.getKeyStroke(event.getKeyCode(), event.getModifiersEx());
                shortcut.setText(msg1);
                actionData.putSetup("shortcut", currentShortcut == null ? null : currentShortcut.toString());
                updateButton();

            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

        });

        p.add(new JLabel(_GUI._.InfoPanel_InfoPanel_shortcuts()));
        p.add(shortcut, "newline");
        JButton shortCutReset;
        p.add(shortCutReset = new JButton(new AppAction() {
            {
                setIconKey("reset");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                        String msg1 = KeyUtils.getShortcutString(ks, true);
                        shortcut.setText(msg1);
                        actionData.putSetup("shortcut", null);
                        updateButton();
                    }
                };
            }

        }), "width 22!,height 22!");

        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForAutoStart()));
        final JComboBox<AutoStartOptions> autostart = new JComboBox<AutoStartOptions>(AutoStartOptions.values());
        p.add(autostart, "newline,spanx");
        autostart.setSelectedItem(CFG_LINKGRABBER.CFG.getAutoConfirmManagerAutoStart());
        autostart.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerAutoStart((AutoStartOptions) autostart.getSelectedItem());
            }
        });
        //
        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForClearListAfterConfirm()), "newline");
        final JCheckBox cb = new JCheckBox();
        SwingUtils.setOpaque(cb, false);
        cb.setSelected(CFG_LINKGRABBER.CFG.isAutoConfirmManagerClearListAfterConfirm());
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerClearListAfterConfirm(cb.isSelected());
            }
        });
        p.add(cb);

        //
        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForPiority()), "newline");

        final JComboBox<Priority> priority = new JComboBox<Priority>(Priority.values());
        p.add(priority, "newline,spanx");
        priority.setSelectedItem(CFG_LINKGRABBER.CFG.getAutoConfirmManagerPiority());
        priority.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerPiority((Priority) priority.getSelectedItem());
            }
        });
        //
        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForAssignPriorityEnabled()), "newline");
        final JCheckBox cbPrioprity = new JCheckBox();
        SwingUtils.setOpaque(cbPrioprity, false);
        cbPrioprity.setSelected(CFG_LINKGRABBER.CFG.isAutoConfirmManagerAssignPriorityEnabled());
        p.add(cbPrioprity);
        cbPrioprity.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerAssignPriorityEnabled(cbPrioprity.isSelected());
            }
        });

        //
        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForForceDownloads()), "newline");

        final JCheckBox cbForce = new JCheckBox();
        cbForce.setSelected(CFG_LINKGRABBER.CFG.isAutoConfirmManagerForceDownloads());
        p.add(cbForce);
        SwingUtils.setOpaque(cbForce, false);
        cbForce.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerForceDownloads(cbForce.isSelected());
            }
        });
        //
        p.add(new JLabel(ConfirmLinksContextAction.getTranslationForHandleOffline()), "newline");

        final JComboBox<OnOfflineLinksAction> onOfflineLinks = new JComboBox<OnOfflineLinksAction>(OnOfflineLinksAction.values());
        p.add(onOfflineLinks, "newline,spanx");
        onOfflineLinks.setSelectedItem(CFG_LINKGRABBER.CFG.getAutoConfirmManagerHandleOffline());
        onOfflineLinks.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerHandleOffline((OnOfflineLinksAction) onOfflineLinks.getSelectedItem());
            }
        });
        return p;
    }

    // public AutoStartOptions getAutoStart() {
    //
    // try {
    // Object label = ensureActionData().fetchSetup(AUTO_START);
    // if (label != null) {
    // return AutoStartOptions.valueOf(label + "");
    // }
    // } catch (Throwable e) {
    //
    // }
    //
    // return AutoStartOptions.AUTO;
    // }

    // public OnOfflineLinksAction getHandleOffline() {
    // try {
    // Object label = ensureActionData().fetchSetup(HANDLE_OFFLINE);
    // if (label != null) {
    // return OnOfflineLinksAction.valueOf(label + "");
    // }
    // } catch (Throwable e) {
    //
    // }
    //
    // return OnOfflineLinksAction.GLOBAL;
    // }

    protected void updateButton() {
        if (INSTANCE != null) {
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            try {
                ActionData ad = getActionData();
                if (ad != null) {
                    Object sc = ad.fetchSetup("shortcut");
                    if (sc != null && sc instanceof String) {

                        ks = KeyStroke.getKeyStroke((String) sc);
                    }
                }
            } catch (Throwable e) {
            }
            INSTANCE.setAccelerator(ks);
        }
    }

    @Override
    public String getIconKey() {
        return "paralell";
    }

    @Override
    public String getName() {
        return _GUI._.AutoConfirmMenuLink_getName();
    }

    // public Priority getPiority() {
    // try {
    // Object label = ensureActionData().fetchSetup(PIORITY2);
    // if (label != null) {
    // return Priority.valueOf(label + "");
    // }
    // } catch (Throwable e) {
    //
    // }
    //
    // return Priority.DEFAULT;
    // }
    //
    // public boolean isAssignPriorityEnabled() {
    // return Boolean.TRUE.equals(ensureActionData().fetchSetup(ASSIGN_PRIORITY_ENABLED));
    // }
    //
    // public boolean isClearListAfterConfirm() {
    // return Boolean.TRUE.equals(ensureActionData().fetchSetup(CLEAR_LIST_AFTER_CONFIRM));
    // }

    // public boolean isForceDownloads() {
    // return Boolean.TRUE.equals(ensureActionData().fetchSetup(FORCE_DOWNLOADS));
    // }
    //
    // public void setAssignPriorityEnabled(boolean assignPriorityEnabled) {
    // ensureActionData().putSetup(ASSIGN_PRIORITY_ENABLED, assignPriorityEnabled);
    //
    //
    // }

    // public void setAutoStart(AutoStartOptions autoStart) {
    // if (autoStart == null) {
    // autoStart = AutoStartOptions.AUTO;
    // }
    // ensureActionData().putSetup(AUTO_START, autoStart.name());
    // }

    public ActionData ensureActionData() {
        ActionData ad = this.getActionData();
        if (ad == null) {
            ad = new ActionData();
            setActionData(ad);
        }
        return ad;
    }

    // public void setClearListAfterConfirm(boolean clearListAfterConfirm) {
    // ensureActionData().putSetup(CLEAR_LIST_AFTER_CONFIRM, clearListAfterConfirm);
    // }

    // public void setForceDownloads(boolean forceDownloads) {
    // ensureActionData().putSetup(FORCE_DOWNLOADS, forceDownloads);
    // }
    //
    // public void setHandleOffline(OnOfflineLinksAction handleOffline) {
    // if (handleOffline == null) {
    // handleOffline = OnOfflineLinksAction.GLOBAL;
    // }
    // ensureActionData().putSetup(HANDLE_OFFLINE, handleOffline.name());
    //
    // }
    //
    // public void setPiority(Priority piority) {
    // if (piority == null) {
    // piority = Priority.DEFAULT;
    // }
    // ensureActionData().putSetup(PIORITY2, piority.name());
    // }

}
