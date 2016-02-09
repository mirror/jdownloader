package jd.gui.swing.jdgui.menu;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.ToolTipHandler;
import org.appwork.swing.components.tooltips.TooltipTextDelegateFactory;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.gui.AfterLayerUpdateInterface;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.components.SetIconInterface;
import jd.gui.swing.components.SetLabelInterface;
import net.miginfocom.swing.MigLayout;

public class MenuEditor extends MigPanel implements ToolTipHandler, SetIconInterface, SetLabelInterface, AfterLayerUpdateInterface {
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
        super();
        setLayout(new MigLayout("ins " + getInsetsString(), "6[grow,fill][fill]", "[fill," + getComponentHeight() + "!]"));
        shrink = b;
        this.tooltipFactory = new TooltipTextDelegateFactory(this);
        // setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        setOpaque(false);

    }

    protected String getInsetsString() {
        return "1";
    }

    protected int getComponentHeight() {
        return 22;
    }

    protected JLabel getLbl(String name, Icon icon) {
        label = new JLabel(shrink ? "" : name, icon, JLabel.LEADING);
        setToolTipText(name);
        ToolTipController.getInstance().register(this);
        label.setIconTextGap(getIconTextGap());
        return label;
    }

    protected int getIconTextGap() {
        return 7;
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
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        // ret.height = 22;
        return ret;
    }

    @Override
    public boolean isTooltipWithoutFocusEnabled() {
        return true;
    }

    // public int getPreferredEditorWidth() {
    // return 100;
    // }

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

    @Override
    public void onAfterLayerDone(JComponent root, MenuContainer md) {
        for (Component c : root.getComponents()) {
            if (c instanceof JCheckBoxMenuItem) {
                label.setBorder(BorderFactory.createEmptyBorder(0, LAFOptions.getInstance().getExtension().customizeMenuItemIndentForToggleItems(), 0, 0));

                return;
            }
        }
    }
}
