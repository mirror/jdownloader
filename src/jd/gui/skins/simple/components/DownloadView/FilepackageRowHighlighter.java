//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
