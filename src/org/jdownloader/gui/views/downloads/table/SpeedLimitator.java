package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SpeedLimitator extends AbstractDialog<Object> {

    private AbstractNode            contextObject;
    private ExtCheckBox             enabledBox;
    private SizeSpinner             spinner;
    private java.util.List<AbstractNode> inteliSelect;

    public SpeedLimitator(final AbstractNode contextObject, java.util.List<AbstractNode> inteliSelect) {
        super(0, _GUI._.SpeedLimitator_SpeedLimitator_object_(), null, null, null);
        this.contextObject = contextObject;
        this.inteliSelect = inteliSelect;
    }

    private JLabel getLbl(String customSpeedLimitator_SpeedlimitEditor__lbl, ImageIcon icon) {
        JLabel ret = new JLabel(customSpeedLimitator_SpeedlimitEditor__lbl);
        ret.setIcon(icon);
        return ret;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            for (AbstractNode n : inteliSelect) {
                DownloadLink link = null;
                if (n instanceof CrawledLink) {
                    link = ((CrawledLink) n).getDownloadLink();
                } else if (n instanceof DownloadLink) {
                    link = (DownloadLink) n;
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

        JLabel lbl = getLbl(_GUI._.CustomSpeedLimitator_SpeedlimitEditor__lbl(), NewTheme.I().getIcon("speed", 18));
        ret.add(SwingUtils.toBold(lbl), "spanx");

        spinner = new SizeSpinner(1, Long.MAX_VALUE, 1024) {
            /**
         *
         */
            private static final long serialVersionUID = 1L;

            protected String longToText(long longValue) {

                return _GUI._.SpeedlimitEditor_format(SizeFormatter.formatBytes(longValue));
            }
        };

        enabledBox = new ExtCheckBox(spinner);
        ret.add(enabledBox);
        spinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

            }
        });

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
