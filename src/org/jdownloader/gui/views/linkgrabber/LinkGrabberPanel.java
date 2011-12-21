package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddOptionsAction;
import org.jdownloader.gui.views.linkgrabber.actions.ClearAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAllAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmOptionsAction;
import org.jdownloader.gui.views.linkgrabber.actions.RemoveOptionsAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LinkGrabberPanel extends SwitchPanel implements LinkCollectorListener, GenericConfigEventListener<Boolean> {
    /**
     * 
     */
    private static final long                        serialVersionUID = 1L;
    private LinkGrabberTableModel                    tableModel;
    private LinkGrabberTable                         table;
    private JScrollPane                              tableScrollPane;
    private LinkGrabberSidebar                       sidebar;
    private JButton                                  addLinks;
    private JButton                                  confirmAll;
    private JButton                                  clearAll;
    private JButton                                  popup;
    private JButton                                  popupConfirm;
    private HeaderScrollPane                         sidebarScrollPane;
    private MigPanel                                 leftBar;
    private MigPanel                                 rightBar;
    private SearchField<CrawledPackage, CrawledLink> searchField;
    private ExtButton                                filteredAdd;

    private JButton                                  popupRemove;
    private JToggleButton                            showHideSidebar;
    private AutoConfirmButton                        autoConfirm;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]"));
        tableModel = LinkGrabberTableModel.getInstance();
        table = new LinkGrabberTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        // tableScrollPane.setBorder(null);
        filteredAdd = new ExtButton(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                setIconKey("filter");
            }

            public void actionPerformed(ActionEvent e) {
                ArrayList<CrawledLink> filteredStuff = LinkCollector.getInstance().getFilteredStuff(true);
                LinkCollector.getInstance().addCrawlerJob(filteredStuff);
            }

        });

        // filteredAdd.setVisible(false);
        LinkCollector.getInstance().addListener(new LinkCollectorListener() {

            public void onLinkCollectorEvent(LinkCollectorEvent event) {
                LinkCollector caller = event.getCaller();
                switch (event.getType()) {
                case FILTERED_AVAILABLE:
                    setFilteredAvailable(caller.getfilteredStuffSize());
                    break;
                case FILTERED_EMPTY:
                    setFilteredAvailable(0);
                    break;
                }
            }
        });
        autoConfirm = new AutoConfirmButton();
        autoConfirm.setVisible(false);
        setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
        addLinks = new JButton(new AddLinksAction());
        confirmAll = new JButton(new ConfirmAllAction());
        clearAll = new JButton(new ClearAction());
        popup = new JButton(new AddOptionsAction(addLinks)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        popupRemove = new JButton(new RemoveOptionsAction(table, clearAll)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        popupConfirm = new JButton(new ConfirmOptionsAction(table, confirmAll)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };

        showHideSidebar = new JToggleButton(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {

                putValue(SMALL_ICON, NewTheme.I().getIcon("sidebar", -1));
                setTooltipText(_GUI._.LinkGrabberPanel_LinkGrabberPanel_btn_showsidebar_tt_up());

            }

            public void actionPerformed(ActionEvent e) {
                GraphicalUserInterfaceSettings.CFG.setLinkgrabberSidebarVisible(!GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarVisible());
            }
        });
        showHideSidebar.setSelected(GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getValue());
        leftBar = new MigPanel("ins 0", "[]1[][]1[][grow,fill]0[]", "[]");
        rightBar = new MigPanel("ins 0", "[]0[]1[]0[]0", "[]");

        leftBar.add(addLinks, "height 24!,aligny top");

        leftBar.add(popup, "height 24!,width 12!,aligny top");
        leftBar.add(clearAll, "width 24!,height 24!,aligny top");
        leftBar.add(popupRemove, "height 24!,width 12!,aligny top");
        searchField = new SearchField<CrawledPackage, CrawledLink>(table);
        leftBar.add(searchField, "height 24!,aligny top");
        searchField.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    searchField.setText("");

                }
            }

            public void keyPressed(KeyEvent e) {
            }
        });

        leftBar.add(filteredAdd, "height 24!,hidemode 3,gapleft 4");
        // leftBar.add(Box.createGlue());
        layoutComponents();

        // showHideSidebar.setVisible(GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getValue());
        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getEventSender().addListener(this);

        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED.getEventSender().addListener(this);
        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(this);
    }

    private void setFilteredAvailable(final int size) {
        if (size > 0) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    filteredAdd.setText(_GUI._.RestoreFilteredLinksAction_(size));
                    filteredAdd.setVisible(true);
                }
            };
        } else {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    filteredAdd.setVisible(false);
                }
            };
        }
    }

    private void layoutComponents() {
        rightBar.removeAll();
        rightBar.add(autoConfirm, "height 24!,width 24!,hidemode 3,gapright 3");

        rightBar.add(confirmAll, "height 24!,pushx,growx");
        rightBar.add(popupConfirm, "height 24!,width 12!");
        if (GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED.getValue() && GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getValue()) {
            //
            rightBar.add(showHideSidebar, "height 24!,width 24!,gapleft 4");
        }

        if (GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarEnabled() && GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_VISIBLE.getValue()) {

            if (sidebarScrollPane == null) {
                createSidebar();
            }
            this.add(tableScrollPane, "pushx,growx");
            add(sidebarScrollPane, "width 240!");
        } else {
            this.add(tableScrollPane, "pushx,growx,spanx");

        }

        add(leftBar);
        add(rightBar, "");
    }

    private void createSidebar() {
        sidebar = new LinkGrabberSidebar(table);

        sidebarScrollPane = new HeaderScrollPane(sidebar) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            // protected int getHeaderHeight() {
            // return (int)
            // table.getTableHeader().getPreferredSize().getHeight();
            // }
        };

        // ScrollPaneUI udi = sp.getUI();
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        // LayoutManager lm = sp.getLayout();

        if (c >= 0) {
            sidebarScrollPane.setBackground(new Color(c));
            sidebarScrollPane.setOpaque(true);

        }
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sidebarScrollPane.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        // ExtButton bt = new ExtButton(new AppAction() {
        // {
        // setSmallIcon(NewTheme.I().getIcon("close", -1));
        // setToolTipText(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader_object_());
        // }
        //
        // public void actionPerformed(ActionEvent e) {
        // GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.setValue(false);
        // }
        // });
        //
        // sidebarScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,
        // bt);
        LinkFilterSettings.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

                if (Boolean.TRUE.equals(newValue)) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            sidebarScrollPane.getVerticalScrollBar().setValue(sidebarScrollPane.getVerticalScrollBar().getMaximum());
                        }
                    });

                }
            }
        });

    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        LinkCollector.getInstance().addListener(this);
        table.requestFocusInWindow();
    }

    @Override
    protected void onHide() {
        LinkCollector.getInstance().removeListener(this);
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REFRESH_STRUCTURE:
        case REMOVE_CONTENT:
            tableModel.recreateModel(!QueuePriority.HIGH.equals(event.getPrio()));
            break;
        case REFRESH_DATA:
            tableModel.refreshModel(!QueuePriority.HIGH.equals(event.getPrio()));
            break;
        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (!newValue && keyHandler == GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_VISIBLE) {
            JDGui.help(_GUI._.LinkGrabberPanel_onConfigValueModified_title_(), _GUI._.LinkGrabberPanel_onConfigValueModified_msg_(), NewTheme.I().getIcon("warning_green", 32));

        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                removeAll();
                layoutComponents();

                revalidate();
            }
        };
    }
}
