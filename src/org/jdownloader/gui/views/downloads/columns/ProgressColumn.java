package org.jdownloader.gui.views.downloads.columns;

import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;

import org.appwork.swing.components.multiprogressbar.MultiProgressBar;
import org.appwork.swing.components.multiprogressbar.Range;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.swing.exttable.columns.ExtProgressColumn;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ProgressColumn extends ExtProgressColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               big;
    private int               medium;

    public ProgressColumn() {
        super(_GUI._.ProgressColumn_ProgressColumn());
        FontMetrics fm = determinatedRenderer.getFontMetrics(determinatedRenderer.getFont());

        big = fm.stringWidth(df.format(123.45d) + "%");
        medium = fm.stringWidth(df.format(123.45d));
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    protected boolean isIndeterminated(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        return getValue(value) < 0;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {

        return obj.isEnabled();
    }

    public boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ToolTipController.getInstance().show(getModel().getTable().createExtTooltip(null));
            }
        });
        return false;
    }

    public ExtTooltip createToolTip(final Point position, final AbstractNode obj) {
        TooltipPanel panel = new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]");
        final MultiProgressBar mpb = new MultiProgressBar(1000);
        mpb.setForeground((LAFOptions.getInstance().getColorForTooltipForeground()));

        updateRanges(obj, mpb);

        JLabel lbl = new JLabel(_GUI._.ProgressColumn_createToolTip_object_());
        lbl.setForeground((LAFOptions.getInstance().getColorForTooltipForeground()));
        SwingUtils.toBold(lbl);
        panel.add(lbl);
        mpb.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, (LAFOptions.getInstance().getColorForTooltipForeground())));
        panel.add(mpb, "width 300!,height 24!");

        return new PanelToolTip(panel) {
            /**
             * 
             */
            private static final long serialVersionUID = 1036923322222455495L;
            private Timer             timer;

            /**
             * 
             */
            public void onShow() {
                this.timer = new Timer(1000, new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        updateRanges(obj, mpb);
                        repaint();
                    }

                });
                timer.start();
            }

            /**
             * 
             */
            public void onHide() {
                timer.stop();
            }
        };
    }

    public void updateRanges(final AbstractNode obj, final MultiProgressBar mpb) {
        if (obj instanceof DownloadLink) {
            mpb.getModel().setMaximum(((DownloadLink) obj).getKnownDownloadSize());
            java.util.List<Range> ranges = new ArrayList<Range>();

            long[] chunks = ((DownloadLink) obj).getChunksProgress();
            if (chunks != null) {
                long part = ((DownloadLink) obj).getKnownDownloadSize() / chunks.length;
                for (int i = 0; i < chunks.length; i++) {
                    ranges.add(new Range(i * part, chunks[i]));
                }
                mpb.getModel().setRanges(ranges.toArray(new Range[] {}));
            }
        } else if (obj instanceof FilePackage) {
            long size = ((FilePackage) obj).getView().getSize();
            mpb.getModel().setMaximum(size);
            java.util.List<Range> ranges = new ArrayList<Range>();

            boolean readL = ((FilePackage) obj).getModifyLock().readLock();
            try {
                List<DownloadLink> children = ((FilePackage) obj).getChildren();
                long all = 0;
                for (int i = 0; i < children.size(); i++) {
                    ranges.add(new Range(all, all + children.get(i).getDownloadCurrent()));
                    all += children.get(i).getDownloadSize();
                }
                mpb.getModel().setRanges(ranges.toArray(new Range[] {}));
            } finally {
                ((FilePackage) obj).getModifyLock().readUnlock(readL);
            }
        }
    }

    @Override
    public int getMinWidth() {
        return 16;
    }

    @Override
    public int getDefaultWidth() {
        return 100;
    }

    @Override
    protected String getString(AbstractNode value, long current, long total) {
        if (value instanceof FilePackage) {
            return format(getPercentString(current, total));
        } else {
            DownloadLink dLink = (DownloadLink) value;
            PluginProgress progress;
            if (dLink.getDefaultPlugin() == null) {
                return _GUI._.gui_treetable_error_plugin();
            } else if ((progress = dLink.getPluginProgress()) != null && !(progress.getProgressSource() instanceof PluginForHost)) {
                double prgs = progress.getPercent();
                if (prgs < 0) { return ""; }
                return format(prgs);
            }
        }
        if (total < 0) return "~";
        return format(getPercentString(current, total));
    }

    private NumberFormat df = NumberFormat.getInstance();

    private String format(double percentString) {
        if (getWidth() < big) {
            if (getWidth() < medium) {
                return "";
            } else {
                return df.format(percentString);
            }
        } else {
            return df.format(percentString) + "%";
        }

    }

    @Override
    protected long getMax(AbstractNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            if (fp.getView().isFinished()) {
                return 100;
            } else {
                return (Math.max(1, fp.getView().getSize()));
            }
        } else {
            DownloadLink dLink = (DownloadLink) value;
            PluginProgress progress = null;
            long size = -1;
            if (dLink.getDefaultPlugin() == null) {
                return 100;
            } else if ((progress = dLink.getPluginProgress()) != null && !(progress.getProgressSource() instanceof PluginForHost)) {
                return (progress.getTotal());
            } else if (FinalLinkState.CheckFinished(dLink.getFinalLinkState())) {
                return 100;
            } else if ((size = dLink.getKnownDownloadSize()) > 0) {
                return size;
            } else {
                return -1;
            }
        }
    }

    @Override
    protected void prepareGetter(AbstractNode value) {
    }

    @Override
    protected int getFps() {
        return 5;
    }

    @Override
    protected long getValue(AbstractNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            if (fp.getView().isFinished()) {
                return 100;
            } else {
                return (fp.getView().getDone());
            }
        } else {
            DownloadLink dLink = (DownloadLink) value;
            PluginProgress progress = null;
            if (dLink.getDefaultPlugin() == null) {
                return -1;
            } else if ((progress = dLink.getPluginProgress()) != null && !(progress.getProgressSource() instanceof PluginForHost)) {
                return (progress.getCurrent());
            } else if (FinalLinkState.CheckFinished(dLink.getFinalLinkState())) {
                return 100;
            } else if (dLink.getKnownDownloadSize() > 0) {
                return dLink.getDownloadCurrent();
            } else {
                return 0;
            }
        }
    }

}
