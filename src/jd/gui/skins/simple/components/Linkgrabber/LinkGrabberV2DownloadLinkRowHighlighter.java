package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;

import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

public abstract class LinkGrabberV2DownloadLinkRowHighlighter extends ColorHighlighter {

    public LinkGrabberV2DownloadLinkRowHighlighter(final LinkGrabberV2TreeTable table, Color cellBackground, Color cellForeground, Color selectedBackground, Color selectedForeground) {
        super(cellBackground, cellForeground, selectedBackground, selectedForeground);
        HighlightPredicate hp = new HighlightPredicate() {
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
        this.setHighlightPredicate(hp);
    }

    public abstract boolean doHighlight(DownloadLink link);
}
