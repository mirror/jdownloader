package jd.gui.swing.jdgui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.utils.ColorUtils;
import org.appwork.utils.swing.table.AlternateHighlighter;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtComponentRowHighlighter;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;

public class BasicJDTable<T> extends ExtTable<T> {

    private static final long serialVersionUID = -9181860215412270250L;
    private Color             sortNotifyColor;
    private Color             sortNotifyColorTransparent;

    public BasicJDTable(ExtTableModel<T> tableModel) {
        super(tableModel);
        this.setShowVerticalLines(true);
        this.setShowGrid(true);
        this.setShowHorizontalLines(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setRowHeight(22);

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor();
        Color b2;
        Color f2;
        if (c >= 0) {
            b2 = new Color(c);
            f2 = new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderForegroundColor());
        } else {
            b2 = getForeground();
            f2 = getBackground();
        }
        this.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor()));
        // this.addRowHighlighter(new SelectionHighlighter(null, b2));
        this.getExtTableModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>(f2, b2, null) {

            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return selected;
            }

        });
        // this.getExtTableModel().addExtComponentRowHighlighter(new
        // ExtComponentRowHighlighter<T>(f2, b2.darker(), null) {
        //
        // @Override
        // public boolean accept(ExtColumn<T> column, T value, boolean selected,
        // boolean focus, int row) {
        // return getExtTableModel().getSortColumn() == column;
        // }
        //
        // });
        this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));
        this.setIntercellSpacing(new Dimension(0, 0));

        sortNotifyColor = Color.ORANGE;
        sortNotifyColorTransparent = new Color(sortNotifyColor.getRed(), sortNotifyColor.getGreen(), sortNotifyColor.getBlue(), 0);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        final Rectangle visibleRect = this.getVisibleRect();
        Rectangle first, last;
        // get current width;

        int index = getExtTableModel().getSortColumn().getIndex();
        first = this.getCellRect(0, index, true);
        System.out.println(index);
        // getExtColumnIndexByPoint(point)
        // w= getExtTableModel().getSortColumn().getWidth();
        // // getExtTableModel().getC getExtTableModel().getSortColumn()
        g2.setColor(Color.ORANGE);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        // g2.fill(new Rectangle2D.Float(first.x, 0,
        // getExtTableModel().getSortColumn().getWidth(), visibleRect.height));
        // g2.fill(new Rectangle2D.Float(first.x, 0,
        // getExtTableModel().getSortColumn().getWidth(), visibleRect.height));

        if (getExtTableModel().getSortColumn().getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
            GradientPaint gp = new GradientPaint(0, visibleRect.y, sortNotifyColor, 0, visibleRect.y + visibleRect.height, sortNotifyColorTransparent);

            g2.setPaint(gp);
        } else {
            GradientPaint gp = new GradientPaint(0, visibleRect.y, sortNotifyColorTransparent, 0, visibleRect.y + visibleRect.height, sortNotifyColor);

            g2.setPaint(gp);
        }
        g2.fillRect(visibleRect.x + first.x, visibleRect.y, visibleRect.x + getExtTableModel().getSortColumn().getWidth(), visibleRect.y + visibleRect.height);
        // g2.fill(new Polygon(new int[] { first.x, first.x +
        // getExtTableModel().getSortColumn().getWidth(), first.x }, new int[] {
        // 0, visibleRect.height, visibleRect.height }, 3));
    }
}
