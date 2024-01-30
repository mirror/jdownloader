package org.jdownloader.gui.views.downloads;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.UIManager;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.DomainInfo;

public class HosterToolTip extends ExtTooltip {
    public HosterToolTip(DomainInfo[] domainInfos) {
        this.panel = new TooltipPanel("ins 3,wrap 1", "[grow,fill]", "[grow,fill]");
        if (domainInfos != null) {
            for (final DomainInfo link : domainInfos) {
                JLabel lbl;
                panel.add(lbl = new JLabel(link.getDomain(), link.getFavIcon(), JLabel.LEADING));
                SwingUtils.setOpaque(lbl, false);
                Color color = UIManager.getColor(ExtTooltip.APPWORK_TOOLTIP_FOREGROUND);
                if (color != null) {
                    lbl.setForeground(color);
                }
            }
        }
        this.panel.setOpaque(false);
        add(panel);
    }

    @Override
    public TooltipPanel createContent() {
        return null;
    }

    @Override
    public String toText() {
        return null;
    }
}
