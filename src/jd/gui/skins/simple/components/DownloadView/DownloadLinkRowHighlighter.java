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
import java.awt.LinearGradientPaint;

import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

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

    public Painter<Component> getGradientPainter(Color colora, Color colorb) {
        int height = 20;
if(JDUtilities.getJavaVersion()>=1.6){
    LinearGradientPaint gradientPaint = new LinearGradientPaint(1, 0, 1, height, new float[] { 0.0f, 1.0f }, new Color[] { colora, colorb });

    return new MattePainter<Component>(gradientPaint);
}else{

    return new MattePainter<Component>(colora);  
}
     
    }

    public abstract boolean doHighlight(DownloadLink link);
}
