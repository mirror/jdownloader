package org.jdownloader.gui.notify;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.images.NewTheme;

public abstract class AbstractBubbleContentPanel extends MigPanel {

    protected IconedProcessIndicator progressCircle;
    protected long                   startTime;

    private String clean(String label) {
        label = label.trim();
        while (label.endsWith(":")) {
            label = label.substring(0, label.length() - 1);
        }
        return label + ":";
    }

    abstract public void updateLayout();

    public class Pair {

        public JLabel lbl;
        public JLabel value;

        public void setText(Object v) {
            value.setText(v + "");
        }

        public void setVisible(boolean b) {
            lbl.setVisible(b);
            value.setVisible(b);
        }

        public void setTooltip(String filePath) {

            lbl.setToolTipText(filePath);
            value.setToolTipText(filePath);

        }

    }

    protected Pair addPair(Pair existingPair, String lbl, Icon iconWait) {
        if (existingPair == null) {
            existingPair = new Pair();
            existingPair.lbl = createHeaderLabel(lbl, iconWait);
            existingPair.value = new JLabel("");
        }

        add(existingPair.lbl, "hidemode 3");

        add(existingPair.value, "hidemode 3");
        return existingPair;
    }

    protected Pair addPair(Pair existingPair, String lbl, String icon) {

        return addPair(existingPair, lbl, NewTheme.I().getIcon(icon, 18));
    }

    protected Pair addPair(String lbl, String iconWait) {
        return addPair(null, lbl, iconWait);
    }

    public void stop() {
        progressCircle.setIndeterminate(false);
        progressCircle.setMaximum(100);
        progressCircle.setValue(100);
    }

    protected JLabel createHeaderLabel(String lbl, Icon icon) {
        JLabel ret = createHeaderLabel(lbl);
        if (icon != null) {
            ret.setDisabledIcon(icon);
            ret.setHorizontalTextPosition(SwingConstants.LEFT);
        }
        return ret;
    }

    protected JLabel createHeaderLabel(String lbl, String icon) {

        return createHeaderLabel(lbl, NewTheme.I().getIcon(icon, 18));
    }

    protected JLabel createHeaderLabel(String label) {
        JLabel lbl = new JLabel(clean(label));
        SwingUtils.toBold(lbl);
        lbl.setEnabled(false);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    public AbstractBubbleContentPanel(String iconKey) {
        this(NewTheme.I().getIcon(iconKey, 20));

    }

    public AbstractBubbleContentPanel(ImageIcon icon) {
        super("ins 3 3 0 3,wrap 3", "[][fill][grow,fill]", "[]");

        progressCircle = new IconedProcessIndicator(icon);

        progressCircle.setIndeterminate(false);
        progressCircle.setEnabled(false);
        progressCircle.setValue(100);
        addProgress();
        startTime = System.currentTimeMillis();
        SwingUtils.setOpaque(this, false);
    }

    protected void addProgress() {
        add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany,aligny top");
    }

    public AbstractBubbleContentPanel() {
        super("ins 3 3 0 3,wrap 2", "[fill][grow,fill]", "[]");
        startTime = System.currentTimeMillis();
        SwingUtils.setOpaque(this, false);
    }

}
