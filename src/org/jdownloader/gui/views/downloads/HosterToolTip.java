package org.jdownloader.gui.views.downloads;

import java.awt.Color;

import javax.swing.JLabel;

import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;

public class HosterToolTip extends ExtTooltip {
    public HosterToolTip(FilePackage obj) {
        super();

        this.panel = new TooltipPanel("ins 3,wrap 1", "[grow,fill]", "[grow,fill]");

        for (PluginForHost link : obj.getFilePackageInfo().getIcons()) {

            JLabel lbl;
            panel.add(lbl = new JLabel(link.getHost(), link.getHosterIconScaled(), JLabel.LEADING));
            SwingUtils.setOpaque(lbl, false);
            lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
        }
        this.panel.setOpaque(false);
        add(panel);

    }

    @Override
    public TooltipPanel createContent() {

        return null;
    }

}
