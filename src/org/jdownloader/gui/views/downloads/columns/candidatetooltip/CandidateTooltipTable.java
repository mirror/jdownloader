package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltipTableModel.MaxWidthProvider;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CandidateTooltipTable extends BasicJDTable<CandidateAndResult> {

    private static final long serialVersionUID            = -2166408567306279016L;
    private boolean           repaintBecauseOfSizeChanged = false;
    private int               preferredWidth;
    private CandidateTooltip  owner;

    public void setRepaintBecauseOfSizeChanged(boolean repaintBecauseOfSizeChanged) {
        this.repaintBecauseOfSizeChanged = repaintBecauseOfSizeChanged;
    }

    public CandidateTooltipTable(CandidateTooltip candidateTooltip, CandidateTooltipTableModel accountListTableModel) {
        super(accountListTableModel);
        this.owner = candidateTooltip;
        ToolTipController.getInstance().unregister(this);
        this.setBackground(null);
        setOpaque(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resetColumnDimensions();
        // this.setShowVerticalLines(false);
        // this.setShowGrid(false);
        // this.setShowHorizontalLines(false);

        // this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<CandidateAndResult>(Color.BLACK,
        // ColorUtils.getAlphaInstance(Color.RED, 50), null) {
        // public int getPriority() {
        // return Integer.MAX_VALUE - 1;
        // }
        //
        // @Override
        // protected Color getBackground(Color current) {
        // return super.getBackground(current);
        // }
        //
        // @Override
        // public boolean accept(ExtColumn<CandidateAndResult> column, CandidateAndResult value, boolean selected, boolean focus, int row) {
        // return false;
        // }
        //
        // });
    }

    @Override
    public void paintComponent(Graphics g) {
        boolean init = false;
        for (ExtColumn<CandidateAndResult> col : getModel().getColumns()) {
            if (col instanceof MaxWidthProvider) {
                int pref = ((MaxWidthProvider) col).getMaxPreferredWitdh();
                if (pref > 0) {
                    init = true;
                    if (col.getMinWidth() > 0) {
                        pref = Math.max(col.getMinWidth(), pref);
                    }
                    if (col.getMaxWidth() > 0) {
                        pref = Math.min(col.getMaxWidth(), pref);
                    }
                    col.getTableColumn().setPreferredWidth(pref);
                    col.getTableColumn().setMaxWidth(pref);
                    col.getTableColumn().setMinWidth(pref);
                    // col.getTableColumn().setWidth(pref);
                }
            }
        }
        // setVisible(false);
        super.paintComponent(g);

        if (!init) {
            // thw first paint finds the correct size. the second paint resizes the tooltip
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    // setVisible(true);
                    ToolTipController.getInstance().show(owner);

                }
            });
        }

    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        // int prefWidth = 0;
        // for (ExtColumn<CandidateAndResult> col : getModel().getColumns()) {
        // if (col instanceof MaxWidthProvider) {
        // int pref = ((MaxWidthProvider) col).getMaxPreferredWitdh();
        // if (pref > 0) {
        //
        // prefWidth += pref + 10;
        // }
        // }
        // }
        //
        // if (prefWidth != ret.width) {
        // ret.width = prefWidth;
        // }
        return ret;
    }

    @Override
    protected void initAlternateRowHighlighter() {

    }

    protected ExtTableHeaderRenderer createDefaultHeaderRenderer(ExtColumn<CandidateAndResult> column) {
        ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(column, getTableHeader());

        setHeaderRendererColors(ret);
        return ret;
    }

    @Override
    protected void addSelectionHighlighter() {

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
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<CandidateAndResult>(f, b, null) {
            public int getPriority() {
                return Integer.MAX_VALUE - 1;

            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<CandidateAndResult> column, CandidateAndResult value, boolean selected, boolean focus, int row) {
                return mouseOverRow == row;
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.java.util.List , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(java.util.List<CandidateAndResult> selectedObjects, KeyEvent evt, boolean direct) {
        // new RemoveAction(selectedObjects, direct).actionPerformed(null);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu , java.lang.Object, java.util.java.util.List,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, CandidateAndResult contextObject, java.util.List<CandidateAndResult> selection, ExtColumn<CandidateAndResult> column, MouseEvent ev) {
        return null;
    }

}
