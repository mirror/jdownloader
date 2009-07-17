package jd.gui.skins.jdgui.views;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import jd.gui.skins.jdgui.views.info.InfoPanel;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.tasks.TaskPanel;
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
public abstract class View extends JPanel {
    public static final int ICON_SIZE = 16;
    private JPanel rightPane;
    private JScrollPane sidebar;
    private TaskPanel sidebarContent;
    private JTabbedPanel content;
    private JPanel topContent;
    private JToolBar toolBar;
    private JPanel bottomContent;
    private InfoPanel infoBar;

    public View() {
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
        add(topContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock NORTH,hidemode 3");
        topContent.setVisible(false);

        add(bottomContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock SOUTH");
        bottomContent.setVisible(false);
    }

    /**
     * SOUTH CONTENT sets the south infopanel.
     * 
     * @param info
     */
    protected void setInfo(InfoPanel info) {
        if (info == null) {
            bottomContent.setVisible(false);

        } else {
            bottomContent.setVisible(true);
            bottomContent.removeAll();
            bottomContent.add(info);
        }

        this.infoBar = info;
    }

    /**
     * TOPCONTENT Sets the views toolbar. null removes the toolbar
     * 
     * @param toolbar
     */
    protected void setToolBar(JToolBar toolbar) {
        if (toolbar == null) {
            topContent.setVisible(false);

        } else {
            topContent.setVisible(true);
            topContent.removeAll();
            topContent.add(toolbar);
        }

        this.toolBar = toolbar;

    }

    /**
     * CENTER-MAIN-CONTENT Sets the left side main content bar
     * 
     * @param right
     */
    protected void setContent(JTabbedPanel right) {
        rightPane.removeAll();
        rightPane.add(right);
        this.content = right;
        this.revalidate();
    }

    /**
     * SIDEBAR WEST CONTENT sets the left sidebar
     * 
     * @param left
     */
    protected void setSideBar(TaskPanel left) {
        if (left == null) {
            sidebar.setVisible(false);

        } else {
            sidebar.setVisible(true);
            sidebar.setViewportView(left);

        }
        this.sidebarContent = left;
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
