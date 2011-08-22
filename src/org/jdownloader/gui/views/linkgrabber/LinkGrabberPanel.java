package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.ClearAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAllAction;

public class LinkGrabberPanel extends SwitchPanel {
    private LinkGrabberTableModel tableModel;
    private LinkGrabberTable      table;
    private JScrollPane           tableScrollPane;
    private LinkGrabberSidebar    sidebar;
    private JButton               addLinks;
    private JButton               confirmAll;
    private JButton               clearAll;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill][fill]", "[grow, fill][]"));
        tableModel = new LinkGrabberTableModel();
        table = new LinkGrabberTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        // tableScrollPane.setBorder(null);
        sidebar = new LinkGrabberSidebar(table);

        this.add(tableScrollPane, "pushx,growx");
        JScrollPane sp = new HeaderScrollPane(sidebar);
        sp.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        // sp.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        add(sp, "width 200!");
        addLinks = new JButton(new AddLinksAction());
        confirmAll = new JButton(new ConfirmAllAction());
        clearAll = new JButton(new ClearAction());
        MigPanel leftBar = new MigPanel("ins 0", "[][][grow,fill]", "[]");
        MigPanel rightBar = new MigPanel("ins 0", "[grow,fill]", "[]");
        add(leftBar);
        add(rightBar, "");
        leftBar.add(addLinks, "height 24!");
        leftBar.add(clearAll, "width 24!,height 24!");
        leftBar.add(Box.createGlue());
        rightBar.add(confirmAll, "height 24!");

    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

}
