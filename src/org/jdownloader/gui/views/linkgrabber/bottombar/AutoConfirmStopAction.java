package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import jd.controlling.linkcollector.LinkCollector;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.contextmenu.CustomSettingsPanelInterface;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.AutoStartOptions;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnOfflineLinksAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoConfirmStopAction extends CustomizableAppAction implements CustomSettingsPanelInterface {
    public AutoConfirmStopAction() {
        super();
        setName(_GUI.T.AutoConfirmMenuLink_getName());
        setIconKey(IconKey.ICON_GO_NEXT);
        setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    public static final String AUTO_START = "autoStart";

    @Override
    public JComponent createSettingsPanel() {
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        SwingUtils.setOpaque(p, false);
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
        priority.setSelectedItem(CFG_LINKGRABBER.CFG.getAutoConfirmManagerPriority());
        priority.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_LINKGRABBER.CFG.setAutoConfirmManagerPriority((Priority) priority.getSelectedItem());
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
    public void actionPerformed(ActionEvent e) {
        LinkCollector.getInstance().getAutoStartManager().interrupt();
    }
}
