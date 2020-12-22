package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class SpeedLimitator extends AbstractDialog<Object> {
    private AbstractNode                 contextObject;
    private ExtCheckBox                  enabledBox;
    private SizeSpinner                  spinner;
    private java.util.List<AbstractNode> inteliSelect;

    public SpeedLimitator(final AbstractNode contextObject, java.util.List<AbstractNode> inteliSelect) {
        super(0, _GUI.T.SpeedLimitator_SpeedLimitator_object_(), null, null, null);
        this.contextObject = contextObject;
        this.inteliSelect = inteliSelect;
    }

    private JLabel getLbl(String customSpeedLimitator_SpeedlimitEditor__lbl, Icon icon) {
        JLabel ret = new JLabel(customSpeedLimitator_SpeedlimitEditor__lbl);
        ret.setIcon(icon);
        return ret;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            for (AbstractNode n : inteliSelect) {
                final DownloadLink link;
                if (n instanceof CrawledLink) {
                    link = ((CrawledLink) n).getDownloadLink();
                } else if (n instanceof DownloadLink) {
                    link = (DownloadLink) n;
                } else {
                    continue;
                }
                if (link != null) {
                    link.setCustomSpeedLimit((int) (enabledBox.isSelected() ? spinner.getLongValue() : 0));
                }
            }
        }
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = new MigPanel("ins 2,wrap 2", "[][grow,fill]", "[]");
        ret.setOpaque(false);
        JLabel lbl = getLbl(_GUI.T.CustomSpeedLimitator_SpeedlimitEditor__lbl(), new AbstractIcon(IconKey.ICON_SPEED, 18));
        ret.add(SwingUtils.toBold(lbl), "spanx");
        spinner = new SizeSpinner(1, Long.MAX_VALUE, 1024) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            protected String longToText(long longValue) {
                return _GUI.T.SpeedlimitEditor_format(SizeFormatter.formatBytes(longValue));
            }
        };
        enabledBox = new ExtCheckBox();
        ret.add(enabledBox);
        enabledBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!enabledBox.isSelected()) {
                    spinner.setValue(0);
                }
            }
        });
        ret.add(spinner);
        if (contextObject instanceof CrawledLink) {
            spinner.setValue(((CrawledLink) contextObject).getDownloadLink().getCustomSpeedLimit());
            enabledBox.setSelected(((CrawledLink) contextObject).getDownloadLink().getCustomSpeedLimit() > 0);
        } else if (contextObject instanceof DownloadLink) {
            spinner.setValue(((DownloadLink) contextObject).getCustomSpeedLimit());
            enabledBox.setSelected(((DownloadLink) contextObject).getCustomSpeedLimit() > 0);
        } else {
            spinner.setValue(0);
            enabledBox.setSelected(false);
        }
        return ret;
    }
}
