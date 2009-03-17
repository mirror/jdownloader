package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;

import javax.swing.tree.TreePath;

import jd.plugins.FilePackage;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

public abstract class FilepackageRowHighlighter extends ColorHighlighter {

    public FilepackageRowHighlighter(final DownloadTreeTable table, Color cellBackground, Color cellForeground, Color selectedBackground, Color selectedForeground) {
        super(cellBackground, cellForeground, selectedBackground, selectedForeground);
        HighlightPredicate hp = new HighlightPredicate() {
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
        this.setHighlightPredicate(hp);
    }

    public abstract boolean doHighlight(FilePackage fp);
}
