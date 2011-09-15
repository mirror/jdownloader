package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Color;

import javax.swing.JTextArea;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.filter.FilterRule;

public class FilterTooltip extends ExtTooltip {

    private FilterRule rule;
    private JTextArea  tf;

    @Override
    public TooltipPanel createContent() {
        TooltipPanel p = new TooltipPanel("ins 2,wrap 1", "[]", "[]");
        this.tf = new JTextArea();
        // this.tf.setEnabled(false);
        this.tf.setForeground(new Color(this.getConfig().getForegroundColor()));
        this.tf.setBackground(null);
        this.tf.setEditable(false);
        SwingUtils.setOpaque(this.tf, false);

        p.add(this.tf);
        return p;
    }

    public void updateRule(FilterRule obj) {
        this.rule = obj;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                tf.setText(JSonStorage.toString(rule));
            }
        };
    }

}
