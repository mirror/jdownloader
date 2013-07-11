package org.jdownloader.gui.views.downloads.columns;

import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.ProxyBlock;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.laf.LookAndFeelController;
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
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;

public class ProgressColumn extends ExtProgressColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ProxyBlock        block            = null;

    public ProgressColumn() {
        super(_GUI._.ProgressColumn_ProgressColumn());

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
            mpb.getModel().setMaximum(((DownloadLink) obj).getDownloadMax());
            java.util.List<Range> ranges = new ArrayList<Range>();

            long[] chunks = ((DownloadLink) obj).getChunksProgress();
            if (chunks != null) {
                long part = ((DownloadLink) obj).getDownloadMax() / chunks.length;
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
            return checkWidth(getPercentString(current, total));
        } else {
            DownloadLink dLink = (DownloadLink) value;
            PluginProgress progress;
            if (dLink.getDefaultPlugin() == null) {
                return _GUI._.gui_treetable_error_plugin();
            } else if ((progress = dLink.getPluginProgress()) != null && !(progress.getProgressSource() instanceof PluginForHost)) {
                //

                // SwingUtilities2.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str,
                // getTableColumn().getWidth() - rendererIcon.getPreferredSize().width - 32)

                return checkWidth(progress.getPercent());
            }
        }
        return checkWidth(getPercentString(current, total));
    }

    private String checkWidth(double d) {
        FontMetrics fm = determinatedRenderer.getFontMetrics(determinatedRenderer.getFont());
        String ret = d + " %";
        int w = fm.stringWidth(ret);

        if (w < getWidth() - 6) {
            return ret;
        } else {
            ret = (int) d + " %";
            w = fm.stringWidth(ret);

            if (w < getWidth() - 6) {
                return ret;
            } else {
                return "";

            }

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
            if (dLink.getDefaultPlugin() == null) {
                return 100;
            } else if ((progress = dLink.getPluginProgress()) != null && !(progress.getProgressSource() instanceof PluginForHost)) {
                return (progress.getTotal());
            } else if (dLink.getLinkStatus().isFinished()) {
                return 100;
            } else if (block != null && !dLink.getLinkStatus().isPluginActive() && !dLink.isSkipped() && dLink.isEnabled()) {
                return block.getBlockedUntil();
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) { return (dLink.getDownloadSize());

            }
        }
        return 100;
    }

    @Override
    protected void prepareGetter(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dLink = (DownloadLink) value;
            block = ProxyController.getInstance().getHostIPBlockTimeout(dLink.getHost());
            if (block == null) block = ProxyController.getInstance().getHostBlockedTimeout(dLink.getHost());
        } else {
            block = null;
        }
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
            } else if (dLink.getLinkStatus().isFinished()) {
                return 100;
            } else if (block != null && !dLink.getLinkStatus().isPluginActive() && !dLink.isSkipped() && dLink.isEnabled()) {
                return block.getBlockedTimeout();
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) { return (dLink.getDownloadCurrent()); }
        }
        return 0;
    }

}
