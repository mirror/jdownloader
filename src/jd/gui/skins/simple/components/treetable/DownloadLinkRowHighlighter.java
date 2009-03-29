package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.LinearGradientPaint;

import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;

public abstract class DownloadLinkRowHighlighter extends PainterHighlighter {

    protected JXTreeTable table;

    public DownloadLinkRowHighlighter(JXTreeTable table, Color colora) {
        this(table, new Color(colora.getRed(), colora.getGreen(), colora.getBlue(), 40), new Color(colora.getRed(), colora.getGreen(), colora.getBlue(), 200));

    }

    public DownloadLinkRowHighlighter(JXTreeTable table, Color colora, Color colorb) {
        super();

        this.setPainter(getGradientPainter(colora, colorb));
        this.table = table;
        this.setHighlightPredicate(getPredicate());
    }

    protected HighlightPredicate getPredicate() {
        return new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                TreePath path = table.getPathForRow(adapter.row);
                Object element;
                if (path != null) {
                    element = path.getLastPathComponent();
                    if (element instanceof DownloadLink) { return doHighlight((DownloadLink) element); }
                }
                return false;
            }
        };
    }

    public Painter getGradientPainter(Color colora, Color colorb) {
        int height = 20;

        LinearGradientPaint gradientPaint = new LinearGradientPaint(1, 0, 1, height, new float[] { 0.0f, 1.0f }, new Color[] { colora, colorb });

        return new MattePainter(gradientPaint);
    }

    public abstract boolean doHighlight(DownloadLink link);
}
