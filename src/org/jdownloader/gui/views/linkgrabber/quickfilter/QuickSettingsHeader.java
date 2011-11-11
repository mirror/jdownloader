package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class QuickSettingsHeader extends MigPanel implements GenericConfigEventListener<Boolean> {

    private JLabel    lbl;

    private ExtButton btn;

    private ExtButton config;

    public QuickSettingsHeader() {
        super("ins 0", "2[][grow,fill][]8[]8", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");

        config = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("exttable/columnButton", 14));
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("Go to Settings");
            }
        });
        config.setRolloverEffectEnabled(true);
        add(config, "height 16!,width 16!");

        lbl = SwingUtils.toBold(new JLabel(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_settings()));

        // lbl.setForeground(new
        // Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor()));
        add(Box.createGlue());
        add(lbl);

        btn = new ExtButton(new BasicAction() {

            public void actionPerformed(ActionEvent e) {
                LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE.setValue(!LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE.getValue());
            }

        });

        add(btn, "height 14!,width 14!");
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));
        LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE.getEventSender().addListener(this);
        onConfigValueModified(null, null);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE.getValue()) {
            btn.setIcon(NewTheme.I().getIcon("popdownButton", -1));
        } else {
            btn.setIcon(NewTheme.I().getIcon("popupButton", -1));
        }
    }
}
