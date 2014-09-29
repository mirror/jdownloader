package org.jdownloader.extensions.schedulerV2;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DevConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.updatev2.gui.LAFOptions;

public class SchedulerConfigPanel extends ExtensionConfigPanel<SchedulerExtension> {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private AdvancedConfigTableModel model;

    public ArrayList<AdvancedConfigEntry> register() {
        ArrayList<AdvancedConfigEntry> configInterfaces = new ArrayList<AdvancedConfigEntry>();
        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : getExtension().getSettings()._getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) {
                continue;
            }

            if (m.getAnnotation(AboutConfig.class) != null && (m.getAnnotation(DevConfig.class) == null || !Application.isJared(null))) {
                if (m.getSetter() == null) {
                    throw new RuntimeException("Setter for " + m.getGetter().getMethod() + " missing");
                } else if (m.getGetter() == null) {
                    throw new RuntimeException("Getter for " + m.getSetter().getMethod() + " missing");
                } else {
                    synchronized (configInterfaces) {
                        configInterfaces.add(new AdvancedConfigEntry(getExtension().getSettings(), m));
                    }
                    map.put(m, true);
                }
            }

        }

        return configInterfaces;
    }

    public SchedulerConfigPanel(SchedulerExtension extension) {
        super(extension);
        JLabel lbl;
        add(SwingUtils.toBold(lbl = new JLabel("THIS EXTENSION IS STILL UNDER CONSTRUCTION. Feel free to test it and to give Feedback.")));
        lbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());
        add(new AdvancedTable(model = new AdvancedConfigTableModel("SchedulerExtension") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        }));
        model.refresh("Scheduler");

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
