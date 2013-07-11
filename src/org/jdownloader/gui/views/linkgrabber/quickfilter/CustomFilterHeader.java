package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.LinkgrabberFilter;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class CustomFilterHeader extends MigPanel implements HeaderInterface {

    private ExtCheckBox       checkBox;
    private JLabel            lbl;
    private BooleanKeyHandler keyHandler;
    private JLabel            counter;
    private ExtButton         config;

    public void setEnabled(boolean enabled) {
        // checkBox.setEnabled(enabled);
        lbl.setEnabled(enabled);

    }

    public CustomFilterHeader() {
        super("ins 0", "2[][grow,fill][]8[]4", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");

        config = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("exttable/columnButton", 14));
            }

            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                SwingGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                LinkgrabberFilter.getInstance().setSelectedIndex(1);
                ConfigurationView.getInstance().setSelectedSubPanel(jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber.class);
            }
        });
        config.setRolloverEffectEnabled(true);
        add(config, "height 16!,width 16!");
        // org.jdownloader.settings.statics.LINKFILTER.LG_QUICKFILTER_EXCEPTIONS_VISIBLE
        lbl = SwingUtils.toBold(new JLabel(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_exceptionfilter()));
        counter = SwingUtils.toBold(new JLabel(""));
        // lbl.setForeground(new
        // Color(LAFOptions.getInstance().getPanelHeaderLineColor()));
        add(counter);
        add(lbl);
        keyHandler = org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED;
        checkBox = new ExtCheckBox(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.EXCEPTION_AS_QUICKFILTER_ENABLED, lbl, counter);
        add(checkBox);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));

    }

    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public void setFilterCount(final int size) {

    }

}
