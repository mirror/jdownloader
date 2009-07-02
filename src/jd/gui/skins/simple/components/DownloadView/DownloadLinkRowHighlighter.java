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

import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;

public abstract class DownloadLinkRowHighlighter extends PainterHighlighter {

    protected JXTable table;

    public DownloadLinkRowHighlighter(JXTable table, Color colora) {
        this(table, new Color(colora.getRed(), colora.getGreen(), colora.getBlue(), 40), new Color(colora.getRed(), colora.getGreen(), colora.getBlue(), 200));

    }

    public DownloadLinkRowHighlighter(JXTable table, Color colora, Color colorb) {
        super();
        this.setPainter(getGradientPainter(colora, colorb));
        this.table = table;
        this.setHighlightPredicate(getPredicate());
    }

    protected HighlightPredicate getPredicate() {
        return new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                if (adapter.row == -1) return false;
                Object element = table.getModel().getValueAt(adapter.row, 0);
                if (element != null && element instanceof DownloadLink) { return doHighlight((DownloadLink) element); }
                return false;
            }
        };
    }

    public Painter<?> getGradientPainter(Color colora, Color colorb) {
        int height = 20;
        if (JDUtilities.getJavaVersion() >= 1.6) {
            LinearGradientPaint gradientPaint = new LinearGradientPaint(1, 0, 1, height, new float[] { 0.0f, 1.0f }, new Color[] { colora, colorb });

            return new MattePainter(gradientPaint);
        } else {

            return new MattePainter(colora);
        }

    }

    public abstract boolean doHighlight(DownloadLink link);
}
