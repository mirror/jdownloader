package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountEntry;
import jd.plugins.DownloadLink;

import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CandidateTooltip extends PanelToolTip {

    public static CandidateTooltip create(Point position, AbstractNode obj) {
        if (obj instanceof DownloadLink) {
            List<HistoryEntry> his = ((DownloadLink) obj).getHistory();
            if (his != null) {
                return new CandidateTooltip(position, (DownloadLink) obj, his);
            }
        }
        return null;
    }

    private Color                      color;
    private CandidateTooltipTable      table;
    private CandidateTooltipTableModel model;
    private Point                      position;

    public Point getDesiredLocation(JComponent activeComponent, Point ttPosition) {

        return MouseInfo.getPointerInfo().getLocation();

    }

    @Override
    public void onShow() {
        addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }

            @Override
            public void ancestorAdded(AncestorEvent event) {
                invalidate();
            }
        });
    }

    public CandidateTooltip(Point position, DownloadLink obj, List<HistoryEntry> history) {
        super(new TooltipPanel("ins 0,wrap 1", "[]", "[grow,fill]") {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                // pref.width = 1000;
                // pref.height = 600;
                // System.out.println(pref);
                return pref;
            }
        });
        this.position = position;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());

        final LinkedList<AccountEntry> domains = new LinkedList<AccountEntry>();

        table = new CandidateTooltipTable(this, model = new CandidateTooltipTableModel(history));
        this.update();
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {

                table.getTableHeader().repaint();
            }
        });
        table.getTableHeader().setOpaque(false);

        JScrollPane sp;

        panel.add(table.getTableHeader());
        panel.add(table);

    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        // javax.swing.PopupFactory.class.getName();
        // ret.width = Math.max(ret.width, JDGui.getInstance().getMainFrame().getWidth());

        return ret;
    }

    public void update() {
        table.getModel().fireTableStructureChanged();
        panel.repaint();

    }

}
