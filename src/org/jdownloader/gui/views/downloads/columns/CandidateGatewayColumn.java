package org.jdownloader.gui.views.downloads.columns;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltip;

public class CandidateGatewayColumn extends ExtTextColumn<AbstractNode> {

    public CandidateGatewayColumn() {
        super(_GUI._.CandidateGatewayColumn());
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode obj) {
        return CandidateTooltip.create(position, obj);
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                if (obj instanceof DownloadLink) {

                    List<HistoryEntry> his = ((DownloadLink) obj).getHistory();
                    if (his != null) {
                        ToolTipController.getInstance().show(CandidateTooltip.create(e.getPoint(), obj));
                    }

                }

            }

        });

        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            HistoryEntry history = ((DownloadLink) value).getLatestHistoryEntry();
            if (history != null) {
                return history.getGatewayIcon(18);
            }
        }
        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            HistoryEntry history = ((DownloadLink) value).getLatestHistoryEntry();
            if (history != null) {
                return history.getGatewayStatus();
            }
        }
        return null;
    }

}
