package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ExtractionJobTable extends BasicJDTable<ExtractionController> {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2198672161538215628L;

    public ExtractionJobTable(ExtractionJobTableModel extractionJobTableModel) {
        super(extractionJobTableModel);

        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);

        ToolTipController.getInstance().unregister(this);
        this.setBackground(null);
        setOpaque(false);
    }

    @Override
    protected void initAlternateRowHighlighter() {

    }

    @Override
    protected void addSelectionHighlighter() {

    }

    protected ExtTableHeaderRenderer createDefaultHeaderRenderer(ExtColumn<ExtractionController> column) {
        ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(column, getTableHeader());

        setHeaderRendererColors(ret);
        return ret;
    }

    public static void setHeaderRendererColors(ExtTableHeaderRenderer ret) {
        ret.setFocusBackground(new Color(255, 255, 255, 80));
        ret.setBackgroundC(new Color(255, 255, 255, 80));
        ret.setFocusForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        ret.setForegroundC(LAFOptions.getInstance().getColorForTooltipForeground());
    }

    protected void initMouseOverRowHighlighter() {
        Color f = (LAFOptions.getInstance().getColorForTableMouseOverRowForeground());
        Color b = (LAFOptions.getInstance().getColorForTableMouseOverRowBackground());
        f = Color.black;
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<ExtractionController>(f, b, null) {
            public int getPriority() {
                return Integer.MAX_VALUE - 1;

            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<ExtractionController> column, ExtractionController value, boolean selected, boolean focus, int row) {
                return mouseOverRow == row;
            }

        });
    }

}
