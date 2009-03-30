package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;

import javax.swing.tree.TreePath;

import jd.gui.skins.simple.components.treetable.DownloadLinkRowHighlighter;
import jd.plugins.DownloadLink;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

//
//public abstract class LinkGrabberV2FilePackageRowHighlighter extends ColorHighlighter {
//
//    public LinkGrabberV2FilePackageRowHighlighter(final LinkGrabberV2TreeTable table, Color cellBackground, Color cellForeground, Color selectedBackground, Color selectedForeground) {
//        super(cellBackground, cellForeground, selectedBackground, selectedForeground);
//        HighlightPredicate hp = new HighlightPredicate() {
//            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
//                TreePath path = table.getPathForRow(adapter.row);
//                Object element;
//                if (path != null) {
//                    element = path.getLastPathComponent();
//                    if (element instanceof LinkGrabberV2FilePackage) { return doHighlight((LinkGrabberV2FilePackage) element); }
//                }                     
//                return false;
//            }           
//        };
//        this.setHighlightPredicate(hp);
//    }
//
//    public abstract boolean doHighlight(LinkGrabberV2FilePackage fp);

public abstract class LinkGrabberV2FilePackageRowHighlighter extends DownloadLinkRowHighlighter {

    public LinkGrabberV2FilePackageRowHighlighter(JXTreeTable table, Color colora) {
        super(table, colora);
    }

    public LinkGrabberV2FilePackageRowHighlighter(JXTreeTable table, Color colora, Color colorb) {
        super(table, colora, colorb);
    }

    protected HighlightPredicate getPredicate() {
        return new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                TreePath path = table.getPathForRow(adapter.row);
                Object element;
                if (path != null) {
                    element = path.getLastPathComponent();
                    if (element instanceof LinkGrabberV2FilePackage) { return doHighlight((LinkGrabberV2FilePackage) element); }
                }
                return false;
            }

        };
    }

    public boolean doHighlight(DownloadLink link) {
        return false;
    }

    public abstract boolean doHighlight(LinkGrabberV2FilePackage link);

}