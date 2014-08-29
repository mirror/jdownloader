package jd.gui.swing.jdgui.menu;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jd.gui.swing.components.SetIconInterface;
import jd.gui.swing.components.SetLabelInterface;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.ToolTipHandler;
import org.appwork.swing.components.tooltips.TooltipTextDelegateFactory;

public class MenuEditor extends MigPanel implements ToolTipHandler, SetIconInterface, SetLabelInterface {
    /**
	 * 
	 */
    private static final long          serialVersionUID = -1702338721344188944L;
    private boolean                    shrink;
    private TooltipTextDelegateFactory tooltipFactory;
    private JLabel                     label;

    public boolean isShrink() {
        return shrink;
    }

    public void setShrink(boolean shrink) {
        this.shrink = shrink;
    }

    public MenuEditor(boolean b) {
        super("ins 0", "6[grow,fill][fill]", "[]");
        shrink = b;
        this.tooltipFactory = new TooltipTextDelegateFactory(this);

        setOpaque(false);
    }

    protected JLabel getLbl(String name, Icon icon) {
        label = new JLabel(shrink ? "" : name, icon, JLabel.LEADING);
        setToolTipText(name);
        ToolTipController.getInstance().register(this);
        label.setIconTextGap(7);
        return label;
    }

    public void addLbl(String chunksEditor_ChunksEditor_, Icon icon) {

        add(getLbl(chunksEditor_ChunksEditor_, icon));
    }

    @Override
    public ExtTooltip createExtTooltip(Point mousePosition) {
        return tooltipFactory.createTooltip();
    }

    @Override
    public boolean isTooltipDisabledUntilNextRefocus() {
        return false;
    }

    protected int getEditorWidth() {

        return new JLabel("500.00 KB/s").getPreferredSize().width + 30;
    }

    @Override
    public int getTooltipDelay(Point mousePositionOnScreen) {
        SwingUtilities.convertPointFromScreen(mousePositionOnScreen, this);

        if (mousePositionOnScreen.x < 24) {
            return 200;
        }
        return 1000;
    }

    @Override
    public boolean updateTooltip(ExtTooltip activeToolTip, MouseEvent e) {
        return false;
    }

    @Override
    public boolean isTooltipWithoutFocusEnabled() {
        return true;
    }

    public int getPreferredEditorWidth() {
        return 100;
    }

    @Override
    public void setText(String text) {
        label.setText(text);
    }

    @Override
    public void setIcon(Icon icon) {
        if (label != null) {
            label.setIcon(icon);
        }
    }
}
