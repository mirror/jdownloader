package jd.gui.swing.jdgui.interfaces;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;

/**
 * A view is an abstract class for a contentpanel in JDGui
 * 
 * @author Coalado
 * 
 */
public abstract class View extends SwitchPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 8661526331504317690L;
    public static final int ICON_SIZE = 16;
    private JPanel rightPane;
    protected JScrollPane sidebar;
    private SideBarPanel sidebarContent;
    private SwitchPanel content;
    private JPanel topContent;
    private JPanel bottomContent;
    private DroppedPanel infoPanel;
    private DroppedPanel defaultInfoPanel;
    @SuppressWarnings("unused")
    private ViewToolbar toolbar;

    public View() {
        SwingGui.checkEDT();
        this.setLayout(new MigLayout("ins 0", "[]0[grow,fill]", "[grow,fill]"));

        add(sidebar = new JScrollPane(), "width 200!,hidemode 1");
        Color line;
        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            Color frameColor = AbstractLookAndFeel.getTheme().getBackgroundColor();
            line = ColorHelper.darker(frameColor, 20);
        } else {
            // MetalLookAndFeel.getControlDarkShadow();
            // MetalLookAndFeel.getControlHighlight() ;
            line = MetalLookAndFeel.getControl();
        }
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, line));
        sidebar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.setVisible(false);
        rightPane = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        add(rightPane);
        add(topContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "gapbottom 3,dock NORTH,hidemode 3");
        topContent.setVisible(false);
        add(bottomContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock SOUTH,hidemode 3");
        bottomContent.setVisible(false);
    }

    /**
     * Sets the default infopanel
     * 
     * @param panel
     */
    protected void setDefaultInfoPanel(DroppedPanel panel) {

        this.defaultInfoPanel = panel;
        if (this.getInfoPanel() == null) setInfoPanel(panel);
    }

    /**
     * SOUTH CONTENT sets the south infopanel. if set to null, the default info
     * panel is shown. of this is null, too the info area is hidden
     * 
     * @param infoPanel
     */
    public void setInfoPanel(DroppedPanel info) {
        SwingGui.checkEDT();
        if (info == null) info = defaultInfoPanel;
        if (infoPanel == info) return;
        if (info == null) {
            bottomContent.setVisible(false);
        } else {
            bottomContent.setVisible(true);
            bottomContent.removeAll();
            bottomContent.add(info);
        }
        if (infoPanel != null && isShown()) infoPanel.setHidden();
        revalidate();
        this.infoPanel = info;
        if (this.infoPanel != null && isShown()) this.infoPanel.setShown();
    }

    public DroppedPanel getInfoPanel() {
        return infoPanel;
    }

    /**
     * TOPCONTENT Sets the views toolbar. null removes the toolbar
     * 
     * @param toolbar
     */
    protected void setToolBar(ViewToolbar toolbar) {
        SwingGui.checkEDT();
        if (toolbar == null) {
            topContent.setVisible(false);
        } else {
            topContent.setVisible(true);
            topContent.removeAll();
            topContent.add(toolbar);
        }
        this.toolbar = toolbar;
        revalidate();
    }

    /**
     * CENTER-MAIN-CONTENT Sets the left side main content bar
     * 
     * @param right
     */
    public synchronized void setContent(SwitchPanel right) {
        SwingGui.checkEDT();

        for (Component c : rightPane.getComponents()) {
            c.setVisible(false);
        }

        if (right != null) {
            right.setVisible(true);
            rightPane.add(right, "hidemode 3");
        }
        if (this.content != null && isShown()) this.content.setHidden();
        this.content = right;
        this.revalidate();
        if (this.content != null && isShown()) this.content.setShown();
    }

    public SwitchPanel getContent() {
        return content;
    }

    /**
     * SIDEBAR WEST CONTENT sets the left sidebar
     * 
     * @param left
     */
    public void setSideBar(SideBarPanel left) {
        SwingGui.checkEDT();
        if (left == sidebarContent) return;
        if (left == null) {
            sidebar.setVisible(false);
        } else {
            sidebar.setVisible(true);
            sidebar.setViewportView(left);
        }

        if (sidebarContent != null && isShown()) sidebarContent.setHidden();

        this.sidebarContent = left;
        if (isShown()) left.setShown();
    }

    /**
     * returns the Tab tooltip
     * 
     * @return
     */
    abstract public String getTooltip();

    /**
     * Returns the tab title
     * 
     * @return
     */
    abstract public String getTitle();

    /**
     * returns the tab icon
     * 
     * @return
     */
    abstract public Icon getIcon();

    /**
     * Returns the defaulticon
     * 
     * @return
     */
    public static Icon getDefaultIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.add_package", 16, 16);
    }

}
