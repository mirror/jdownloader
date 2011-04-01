//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.interfaces;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.ViewToolbar;
import net.miginfocom.swing.MigLayout;

/**
 * A view is an abstract class for a contentpanel in {@link JDGui}
 * 
 * @author Coalado
 */
public abstract class View extends SwitchPanel {

    private static final long serialVersionUID = 8661526331504317690L;
    public static final int   ICON_SIZE        = 16;

    private JPanel            rightPane;

    private SwitchPanel       content;
    private JPanel            topContent;
    private JPanel            bottomContent;
    private SwitchPanel       infoPanel;
    private SwitchPanel       defaultInfoPanel;

    public View() {
        SwingGui.checkEDT();
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));

        this.add(this.rightPane = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]")));

        this.add(this.topContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "gapbottom 3,dock NORTH,hidemode 3");
        this.topContent.setVisible(false);

        this.add(this.bottomContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock SOUTH,hidemode 3");
        this.bottomContent.setVisible(false);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof View)) { return false; }
        if (this.getID().equalsIgnoreCase(((View) o).getID())) { return true; }
        return false;
    }

    public SwitchPanel getContent() {
        return this.content;
    }

    /**
     * returns the tab icon
     * 
     * @return
     */
    abstract public Icon getIcon();

    /**
     * returns ID for the View/Tab
     * 
     * @return
     */
    abstract public String getID();

    public SwitchPanel getInfoPanel() {
        return this.infoPanel;
    }

    /**
     * Returns the tab title
     * 
     * @return
     */
    abstract public String getTitle();

    /**
     * returns the Tab tooltip
     * 
     * @return
     */
    abstract public String getTooltip();

    /**
     * CENTER-MAIN-CONTENT Sets the left side main content bar
     * 
     * @param right
     */
    public synchronized void setContent(final SwitchPanel right) {
        SwingGui.checkEDT();
        boolean found = false;
        for (final Component c : this.rightPane.getComponents()) {
            c.setVisible(false);
            if (c == right) {
                found = true;
            }
        }

        if (right != null) {
            right.setVisible(true);
            if (!found) {
                this.rightPane.add(right, "hidemode 3");
            }
        }
        if (this.content != null && this.isShown()) {
            this.content.setHidden();
        }
        this.content = right;
        this.revalidate();
        if (this.content != null && this.isShown()) {
            this.content.setShown();
        }
    }

    /**
     * Sets the default infopanel
     * 
     * @param panel
     */
    protected void setDefaultInfoPanel(final DroppedPanel panel) {

        this.defaultInfoPanel = panel;
        if (this.getInfoPanel() == null) {
            this.setInfoPanel(panel);
        }
    }

    /**
     * SOUTH CONTENT sets the south infopanel. if set to null, the default info
     * panel is shown. of this is null, too the info area is hidden
     * 
     * @param infoPanel
     */
    public void setInfoPanel(SwitchPanel info) {
        SwingGui.checkEDT();
        if (info == null) {
            info = this.defaultInfoPanel;
        }
        if (this.infoPanel == info) { return; }

        if (info == null) {
            this.bottomContent.setVisible(false);
        } else {
            this.bottomContent.setVisible(true);
            this.bottomContent.removeAll();
            this.bottomContent.add(info);
        }
        if (this.infoPanel != null && this.isShown()) {
            this.infoPanel.setHidden();
        }
        this.revalidate();
        this.infoPanel = info;
        if (this.infoPanel != null && this.isShown()) {
            this.infoPanel.setShown();
        }
    }

    /**
     * TOPCONTENT Sets the views toolbar. null removes the toolbar
     * 
     * @param toolbar
     */
    protected void setToolBar(final ViewToolbar toolbar) {
        SwingGui.checkEDT();
        if (toolbar == null) {
            this.topContent.setVisible(false);
        } else {
            this.topContent.setVisible(true);
            this.topContent.removeAll();
            this.topContent.add(toolbar);
        }
        this.revalidate();
    }

}
