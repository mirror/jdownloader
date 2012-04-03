package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SpeedLimitator extends SubMenuEditor {

    private DownloadLink contextObject;
    private ExtCheckBox  enabledBox;
    private SizeSpinner  spinner;

    public SpeedLimitator(final DownloadLink contextObject, ArrayList<DownloadLink> links, ArrayList<FilePackage> fps) {
        super();
        setLayout(new MigLayout("ins 2,wrap 2", "[][grow,fill]", "[]"));
        setOpaque(false);
        this.contextObject = contextObject;
        JLabel lbl = getLbl(_GUI._.CustomSpeedLimitator_SpeedlimitEditor__lbl(), NewTheme.I().getIcon("speed", 18));
        add(SwingUtils.toBold(lbl), "spanx");

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
        add(enabledBox);
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
        add(spinner);
        spinner.setValue(contextObject.getCustomSpeedLimit());
        enabledBox.setSelected(contextObject.getCustomSpeedLimit() > 0);
    }

    @Override
    public void reload() {

    }

    @Override
    public void save() {
        contextObject.setCustomSpeedLimit((int) (enabledBox.isSelected() ? spinner.getLongValue() : 0));
    }
}
