package jd.gui.skins.simple.components.DownloadView;

import java.awt.Color;
import java.awt.Component;

import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

public abstract class FilepackageRowHighlighter extends DownloadLinkRowHighlighter {

    public FilepackageRowHighlighter(DownloadTreeTable table, Color colora) {
        super(table, colora);
    }

    public FilepackageRowHighlighter(DownloadTreeTable table, Color colora, Color colorb) {
        super(table, colora, colorb);
    }

    protected HighlightPredicate getPredicate() {
        return new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                TreePath path = table.getPathForRow(adapter.row);
                Object element;
                if (path != null) {
                    element = path.getLastPathComponent();
                    if (element instanceof FilePackage) { return doHighlight((FilePackage) element); }
                }
                return false;
            }
        };
    }

    public boolean doHighlight(DownloadLink link) {
        return false;
    }

    public abstract boolean doHighlight(FilePackage link);
}
