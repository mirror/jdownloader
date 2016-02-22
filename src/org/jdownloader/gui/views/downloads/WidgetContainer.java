package org.jdownloader.gui.views.downloads;

import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.properties.PropertiesScrollPaneInterface;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class WidgetContainer extends MigPanel {
    private final DelayedRunnable            propertiesDelayer;
    private final BasicJDTable<AbstractNode> table;

    public WidgetContainer(BasicJDTable<AbstractNode> t, final BooleanKeyHandler propertiesToggleHandler) {
        setOpaque(false);
        this.table = t;
        propertiesDelayer = new DelayedRunnable(100l, 1000l) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (table.getSelectedRowCount() > 0) {
                            if (createPropertiesPanel().getParent() != null) {
                                // no relayout but update
                                createPropertiesPanel().update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
                                return;
                            }
                            setPropertiesPanelVisible(true);
                        } else {
                            if (createPropertiesPanel().getParent() == null) {
                                return;
                            }
                            setPropertiesPanelVisible(false);
                        }
                        relayout();
                    }
                };
            }

            @Override
            public String getID() {
                return "updateDelayer";
            }

        };

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || table.getModel().isTableSelectionClearing() || propertiesToggleHandler == null || !propertiesToggleHandler.isEnabled()) {
                    return;
                }
                propertiesDelayer.run();
            }
        });
    }

    public BasicJDTable<AbstractNode> getTable() {
        return table;
    }

    abstract protected void setPropertiesPanelVisible(boolean b);

    abstract protected PropertiesScrollPaneInterface createPropertiesPanel();

    public abstract void relayout();

    protected JComponent wrap(HeaderScrollPane panel) {
        return LAFOptions.getInstance().getExtension().customizeLayoutWrapTitledPanels(panel);
    }

    public void refreshAfterTabSwitch() {
    }

}
