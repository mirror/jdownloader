package org.jdownloader.extensions.extraction.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ExtractorProgress extends IconedProcessIndicator {
    /**
	 * 
	 */
    private static final long       serialVersionUID = -7582954114939419184L;
    private ExtractorToolTip        tooltip;
    private ExtractionExtension     extension;
    private JPopupMenu              pu;
    private ExtractionJobTableModel tModel;
    private ExtractionJobTable      table;

    public ExtractorProgress(ExtractionExtension extractionExtension) {
        super(NewTheme.I().getIcon("archive", 16));
        setEnabled(false);

        setTitle(_GUI._.StatusBarImpl_initGUI_extract());
        this.extension = extractionExtension;
        // IconedProcessIndicator comp = new IconedProcessIndicator(32);
        // comp.valuePainter = valuePainter;
        // comp.nonValuePainter = nonValuePainter;
        // comp.activeValuePainter = activeValuePainter;
        // comp.activeNonValuePainter = activeNonValuePainter;
        // comp.setActive(isActive());
        // comp.setEnabled(isEnabled());
        // comp.setIndeterminate(isIndeterminate());
        // comp.setPreferredSize(new Dimension(32, 32));
        tooltip = new ExtractorToolTip(extractionExtension);
        pu = new JPopupMenu();
        tModel = new ExtractionJobTableModel(new JLabel().getForeground()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = 7254604415572431495L;

            protected void initColumns() {
                super.initColumns();

                addColumn(new ExtComponentColumn<ExtractionController>("Cancel") {
                    /**
					 * 
					 */
                    private static final long    serialVersionUID = 261994408344732524L;
                    private ExtButton            renderer;
                    private ExtButton            editor;
                    private ExtractionController activeValue;

                    {
                        renderer = getButton();
                        editor = getButton();

                    }

                    @Override
                    protected JComponent getInternalEditorComponent(ExtractionController value, boolean isSelected, int row, int column) {
                        return editor;
                    }

                    private ExtButton getButton() {
                        ExtButton ret = new ExtButton(new AppAction() {
                            /**
							 * 
							 */
                            private static final long serialVersionUID = -2183896670625238331L;

                            {
                                setSmallIcon(NewTheme.I().getIcon("cancel", 16));
                            }

                            public void actionPerformed(ActionEvent e) {
                                extension.cancel(activeValue);
                                pu.setVisible(false);
                            }
                        });
                        ret.setRolloverEffectEnabled(true);
                        return ret;
                    }

                    @Override
                    public int getDefaultWidth() {
                        return 25;
                    }

                    protected int getMaxWidth() {
                        return getDefaultWidth();
                    }

                    /**
                     * @return
                     */
                    public int getMinWidth() {
                        return getDefaultWidth();
                    }

                    @Override
                    protected JComponent getInternalRendererComponent(ExtractionController value, boolean isSelected, boolean hasFocus, int row, int column) {
                        return renderer;
                    }

                    @Override
                    public void configureEditorComponent(ExtractionController value, boolean isSelected, int row, int column) {
                        activeValue = value;
                    }

                    @Override
                    public void configureRendererComponent(ExtractionController value, boolean isSelected, boolean hasFocus, int row, int column) {
                    }

                    @Override
                    public void resetEditor() {
                    }

                    @Override
                    public void resetRenderer() {
                    }
                });
            }
        };

        table = new ExtractionJobTable(tModel);

        pu.add(table);
        pu.addPopupMenuListener(new PopupMenuListener() {

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                ToolTipController.getInstance().register(ExtractorProgress.this);
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
                ToolTipController.getInstance().register(ExtractorProgress.this);
            }
        });
    }

    @Override
    public boolean isTooltipDisabledUntilNextRefocus() {

        return false;
    }

    public ExtTooltip createExtTooltip(final Point mousePosition) {
        tooltip.update();
        return tooltip;

    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            tModel.getTableData().clear();
            java.util.List<ExtractionController> jpobs = extension.getJobQueue().getJobs();
            if (jpobs.size() > 0) {
                tModel.addAllElements(jpobs);
                ToolTipController.getInstance().unregister(this);
                Dimension psize = pu.getPreferredSize();
                pu.show(this, -psize.width + getWidth(), -psize.height);
            } else {
                ToolTipController.getInstance().show(this);
            }
        } else {
            ToolTipController.getInstance().show(this);
        }

    }

    public void update(Type type, ExtractionController con) {
        if (tooltip.getParent() != null) {
            // is visible
            tooltip.update();
        }

        java.util.List<ExtractionController> entries;
        switch (type) {
        case EXTRACTING:
            setIndeterminate(false);
            break;
        case START:
            setIndeterminate(true);
            setValue(10);

        case QUEUED:
            if (con.getExtractionQueue().size() > 0) {
                if (!isEnabled()) {

                    setEnabled(true);
                }
            }
            if (pu.isVisible()) {
                if (table.isEditing()) table.getCellEditor().stopCellEditing();

                tModel.getTableData().clear();
                tModel.addAllElements(extension.getJobQueue().getJobs());

            }
            return;
        case CLEANUP:
            setIndeterminate(false);
            if (pu.isVisible()) {
                if (table.isEditing()) table.getCellEditor().stopCellEditing();

                tModel.getTableData().clear();
                tModel.addAllElements(extension.getJobQueue().getJobs());

            }

            setValue(0);
            if (con.getExtractionQueue().size() <= 1) {
                /*
                 * <=1 because current element is still running at this point
                 */
                if (isEnabled()) {

                    setEnabled(false);
                }
            }
            return;
        }
        entries = extension.getJobQueue().getJobs();
        int progress = 0;
        for (ExtractionController ec : entries) {
            if (ec.getArchiv().getContentView().getTotalSize() > 0) {
                progress += ec.getProgress();
            }
        }
        if (entries.size() > 0) {
            progress /= entries.size();
        }
        setValue(progress);

        if (pu.isVisible()) {

            tModel.refreshSort();

        }

    }
}
