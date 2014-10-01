package org.jdownloader.extensions.schedulerV2;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DevConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.Application;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.schedulerV2.gui.ScheduleTableModel;
import org.jdownloader.extensions.schedulerV2.gui.SchedulerTable;
import org.jdownloader.extensions.schedulerV2.gui.actions.NewAction;
import org.jdownloader.extensions.schedulerV2.gui.actions.RemoveAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.updatev2.gui.LAFOptions;

public class SchedulerConfigPanel extends ExtensionConfigPanel<SchedulerExtension> {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private AdvancedConfigTableModel model;
    private JLabel                   lbl;
    private SchedulerTable           table;
    private MigPanel                 myContainer;

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
        myContainer = new MigPanel("ins 0, wrap 1", "[grow]", "[][]");
        SwingUtils.setOpaque(myContainer, false);
        add(myContainer, "pushx,pushy,growx,growy,spanx,spany");
        initPanel();

    }

    // @Override
    // protected Header initHeader(SchedulerExtension plg) {
    // setLayout(new MigLayout("ins 0, wrap 1", "[][grow,fill]", "[]"));
    // return super.initHeader(plg);
    // }

    private void initPanel() {
        myContainer.removeAll();
        myContainer.setLayout("ins 0, wrap 1", "[grow]", "[][]");// TODO debug here
        myContainer.add(SwingUtils.toBold(lbl = new JLabel("THIS EXTENSION IS STILL UNDER CONSTRUCTION. Feel free to test it and to give Feedback.")));
        lbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());

        // myContainer.add(table = new AdvancedTable(model = new AdvancedConfigTableModel("SchedulerExtension") {
        // @Override
        // public void refresh(String filterText) {
        // _fireTableStructureChanged(register(), true);
        // }
        // }), "spanx,grow");
        table = new SchedulerTable(new ScheduleTableModel());
        myContainer.add(new JScrollPane(table), "grow");

        MigPanel bottomMenu = new MigPanel("ins 0", "[]", "[]");
        bottomMenu.setLayout("ins 0", "[][][fill]", "[]");
        bottomMenu.setOpaque(false);
        myContainer.add(bottomMenu);

        NewAction na;
        ExtButton newButton;
        bottomMenu.add(newButton = new ExtButton(na = new NewAction()), "sg 1,height 26!");
        na.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));

        RemoveAction ra;
        ExtButton removeButton;
        bottomMenu.add(removeButton = new ExtButton(ra = new RemoveAction(table)), "sg 1,height 26!");

        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));
        // model.refresh("Scheduler");
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

    public void updateLayout() {

        initPanel();
    }

}
