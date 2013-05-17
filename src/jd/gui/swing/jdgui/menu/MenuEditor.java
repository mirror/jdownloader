package jd.gui.swing.jdgui.menu;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.ToolTipHandler;
import org.appwork.swing.components.tooltips.TooltipTextDelegateFactory;

public class MenuEditor extends MigPanel implements ToolTipHandler {
    /**
	 * 
	 */
    private static final long          serialVersionUID = -1702338721344188944L;
    private boolean                    shrink;
    private TooltipTextDelegateFactory tooltipFactory;

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

    protected JLabel getLbl(String name, ImageIcon icon) {
        JLabel ret = new JLabel(shrink ? "" : name, icon, JLabel.LEADING);
        setToolTipText(name);
        ToolTipController.getInstance().register(this);
        ret.setIconTextGap(7);
        return ret;
    }

    public void addLbl(String chunksEditor_ChunksEditor_, ImageIcon icon) {

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

        if (mousePositionOnScreen.x < 24) { return 200; }
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
}
