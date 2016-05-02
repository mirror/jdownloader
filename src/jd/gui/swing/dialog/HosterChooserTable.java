package jd.gui.swing.dialog;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.updatev2.gui.LAFOptions;

public class HosterChooserTable extends BasicJDTable<LazyHostPlugin> {

    private LazyHostPlugin defaultPlugin;

    public HosterChooserTable(List<LazyHostPlugin> plugins) {
        super(new HosterChooserTableModel(plugins));
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getSelectionModel().addListSelectionListener(this);
        refresh(null);
    }

    @Override
    protected void initMouseOverRowHighlighter() {
        // super.initMouseOverRowHighlighter();
    }

    @Override
    protected void initAlternateRowHighlighter() {
        // super.initAlternateRowHighlighter();
    }

    private final AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();
        g2.setComposite(alpha);
        if (getModel().getTableData().size() == 0) {
            g2.setColor(LAFOptions.getInstance().getColorForTableAccountErrorRowBackground());
            g2.fillRect(visibleRect.x, visibleRect.y, visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height);
            g2.setComposite(comp);
            g2.setFont(g2.getFont().deriveFont(g2.getFont().getStyle() ^ Font.BOLD));
            String str = _GUI.T.AddAccountDialog_empty_table();
            g2.setColor(LAFOptions.getInstance().getColorForTableAccountErrorRowForeground());
            g2.drawString(str, (getWidth() - g2.getFontMetrics().stringWidth(str)) / 2, (int) (getHeight() * 0.5d));
        }

        g2.setComposite(comp);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);
    }

    @Override
    public HosterChooserTableModel getModel() {
        return (HosterChooserTableModel) super.getModel();
    }

    public void refresh(String text) {
        getModel().refresh(text);
    }

    public LazyHostPlugin getSelectedPlugin() {
        final List<LazyHostPlugin> objects = getModel().getSelectedObjects(1);
        if (objects == null || objects.size() == 0) {
            return defaultPlugin;
        }
        return objects.get(0);
    }

    public void setSelectedPlugin(LazyHostPlugin lazyp) {
        if (defaultPlugin == null) {
            defaultPlugin = lazyp;
        }
        getModel().setSelectedObject(lazyp);
        scrollToSelection(0);
    }

    public void onKeyDown() {
        if (getSelectedRow() == getModel().getTableData().size() - 1) {
            getModel().setSelectedRows(new int[] { 0 });
        } else {
            getModel().setSelectedRows(new int[] { getSelectedRow() + 1 });
        }
    }

    public void onKeyUp() {
        if (getSelectedRow() - 1 < 0) {
            getModel().setSelectedRows(new int[] { getModel().getTableData().size() - 1 });
        } else {
            getModel().setSelectedRows(new int[] { getSelectedRow() - 1 });
        }
    }

}
