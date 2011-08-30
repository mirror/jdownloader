package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.ScrollPaneUI;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddOptionsAction;
import org.jdownloader.gui.views.linkgrabber.actions.ClearAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAllAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmOptionsAction;

public class LinkGrabberPanel extends SwitchPanel implements LinkCollectorListener {
    private LinkGrabberTableModel tableModel;
    private LinkGrabberTable      table;
    private JScrollPane           tableScrollPane;
    private LinkGrabberSidebar    sidebar;
    private JButton               addLinks;
    private JButton               confirmAll;
    private JButton               clearAll;
    private JButton               popup;
    private JButton               popupConfirm;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]"));
        tableModel = new LinkGrabberTableModel();
        table = new LinkGrabberTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        // tableScrollPane.setBorder(null);
        sidebar = new LinkGrabberSidebar(table);

        this.add(tableScrollPane, "pushx,growx");
        HeaderScrollPane sp = new HeaderScrollPane(sidebar) {
            // protected int getHeaderHeight() {
            // return (int)
            // table.getTableHeader().getPreferredSize().getHeight();
            // }
        };
        ScrollPaneUI udi = sp.getUI();
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        LayoutManager lm = sp.getLayout();

        if (c >= 0) {
            sp.setBackground(new Color(c));
            sp.setOpaque(true);
        }
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        // sp.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        add(sp, "width 220!");
        addLinks = new JButton(new AddLinksAction());
        confirmAll = new JButton(new ConfirmAllAction());
        clearAll = new JButton(new ClearAction());
        popup = new JButton(new AddOptionsAction(addLinks)) {
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        popupConfirm = new JButton(new ConfirmOptionsAction(table, confirmAll)) {
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };

        MigPanel leftBar = new MigPanel("ins 0", "[]1[][][grow,fill]", "[]");
        MigPanel rightBar = new MigPanel("ins 0", "[grow,fill]1[]0", "[]");
        add(leftBar);

        add(rightBar, "");
        leftBar.add(addLinks, "height 24!");

        leftBar.add(popup, "height 24!,width 8!");
        leftBar.add(clearAll, "width 24!,height 24!");
        leftBar.add(Box.createGlue());
        rightBar.add(confirmAll, "height 24!");
        rightBar.add(popupConfirm, "height 24!,width 8!");
    }

    @Override
    protected void onShow() {
        table.recreateModel();
        LinkCollector.getInstance().addListener(this);
    }

    @Override
    protected void onHide() {
        LinkCollector.getInstance().removeListener(this);
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REFRESH_STRUCTURE:
        case REMOVE_CONTENT:
            table.recreateModel();
            break;
        case REFRESH_DATA:
            table.refreshModel();
            break;
        }
    }

}
