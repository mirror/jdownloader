package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.AutoStartOptions;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnOfflineLinksAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoConfirmMenuLink extends MenuItemData implements MenuLink, SelfLayoutInterface {

    private static final String SHORTCUT2  = "shortcut";

    public static final String  AUTO_START = "autoStart";

    @Override
    public List<AppAction> createActionsToLink() {
        ArrayList<AppAction> ret = new ArrayList<AppAction>();
        // DownloadsTableSearchField item = DownloadsTableSearchField.getInstance();
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        try {

            ActionData ad = this.getActionData();
            Object sc = ad.fetchSetup(SHORTCUT2);
            if (sc != null && sc instanceof String) {

                ks = KeyStroke.getKeyStroke((String) sc);
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
    public JComponent createSettingsPanel() {

        ActionData ad = ensureActionData();

        final ActionData actionData = ad;
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        SwingUtils.setOpaque(p, false);
        final ExtTextField shortcut = new ExtTextField();
        shortcut.setHelpText(_GUI.T.InfoPanel_InfoPanel_shortcuthelp2());
        shortcut.setEditable(false);
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        try {
            Object sc = ad.fetchSetup(SHORTCUT2);
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
                actionData.putSetup(SHORTCUT2, currentShortcut == null ? null : currentShortcut.toString());

            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

        });

        p.add(new JLabel(_GUI.T.InfoPanel_InfoPanel_shortcuts()));
        p.add(shortcut, "newline");
        JButton shortCutReset;
        p.add(shortCutReset = new JButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_RESET);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                        String msg1 = KeyUtils.getShortcutString(ks, true);
                        shortcut.setText(msg1);
                        actionData.putSetup(SHORTCUT2, null);
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

    @Override
    public String getIconKey() {
        return org.jdownloader.gui.IconKey.ICON_PARALELL;
    }

    @Override
    public String getName() {
        return _GUI.T.AutoConfirmMenuLink_getName();
    }

    public ActionData ensureActionData() {
        ActionData ad = this.getActionData();
        if (ad == null) {
            ad = new ActionData();
            setActionData(ad);
        }
        return ad;
    }

}
