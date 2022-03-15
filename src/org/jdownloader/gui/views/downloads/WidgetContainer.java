package org.jdownloader.gui.views.downloads;

import java.awt.Container;

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
                    private boolean isVisible(PropertiesScrollPaneInterface propertiesPanel) {
                        final Container parent = propertiesPanel.getParent();
                        if (parent == null) {
                            return false;
                        } else {
                            // JD_PLAIN theme(maybe others too), relayout()->removeAll(), PropertiesScrollPaneInterface still has MigPanel
                            // as parent, so check
                            // parent twice
                            return parent.getParent() != null;
                        }
                    }

                    @Override
                    protected void runInEDT() {
                        final PropertiesScrollPaneInterface propertiesPanel = createPropertiesPanel();
                        final AbstractNode selectedObject = getTable().getModel().hasSelectedObjects() ? getTable().getModel().getObjectbyRow(getTable().getSelectionModel().getLeadSelectionIndex()) : null;
                        if (selectedObject != null) {
                            if (isVisible(propertiesPanel)) {
                                // no relayout but update PropertiesScrollPaneInterface only
                                createPropertiesPanel().update(selectedObject);
                                return;
                            } else {
                                setPropertiesPanelVisible(true);
                            }
                        } else if (isVisible(propertiesPanel)) {
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
                } else {
                    propertiesDelayer.run();
                }
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
