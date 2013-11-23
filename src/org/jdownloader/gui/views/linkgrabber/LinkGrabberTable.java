package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.plugins.DownloadLink.AvailableStatus;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtCheckBoxMenuItem;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ContextMenuFactory;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;

public class LinkGrabberTable extends PackageControllerTable<CrawledPackage, CrawledLink> {

    private static final long          serialVersionUID = 8843600834248098174L;
    private ContextMenuFactory         contextMenuFactory;
    private HashMap<KeyStroke, Action> shortCutActions;
    private LogSource                  logger;
    private static LinkGrabberTable    INSTANCE;

    public LinkGrabberTable(LinkGrabberPanel linkGrabberPanel, final LinkGrabberTableModel tableModel) {
        super(tableModel);
        INSTANCE = this;
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setTransferHandler(new LinkGrabberTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        logger = LogController.getInstance().getLogger(LinkGrabberTable.class.getName());
        contextMenuFactory = new ContextMenuFactory(this, linkGrabberPanel);
        final MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]");
        // loaderPanel.setPreferredSize(new Dimension(200, 200));

        loaderPanel.setOpaque(false);
        loaderPanel.setBackground(null);

        final CircledProgressBar loader = new CircledProgressBar() {
            public int getAnimationFPS() {
                return 25;
            }
        };

        loader.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 1.0f));

        loader.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 0.1f));
        ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
        ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
        loader.setIndeterminate(true);

        loaderPanel.add(loader);

        final JProgressBar ph = new JProgressBar();

        ph.setString(_GUI._.DownloadsTable_DownloadsTable_init_plugins());

        LinkCollector.CRAWLERLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ph.setString(_GUI._.LinkGrabberTable_LinkGrabberTable_object_wait_for_loading_links());

                    }

                };
            }
        });

        ph.setStringPainted(true);
        ph.setIndeterminate(true);
        loaderPanel.add(ph, "alignx center");
        // loaderPanel.setSize(400, 400);

        final LayoutManager orgLayout = getLayout();
        final Component rendererPane = getComponent(0);

        setLayout(new MigLayout("ins 0", "[grow]", "[grow]"));
        removeAll();
        add(loaderPanel, "alignx center,aligny 20%");
        LinkCollector.CRAWLERLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                removeLoaderPanel(loaderPanel, orgLayout, rendererPane);
            }
        });
    }

    @Override
    protected void processMouseEvent(final MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (e.getButton() == MouseEvent.BUTTON2) {
                if ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                    if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
                        // middle click
                        final int row = rowAtPoint(e.getPoint());
                        final AbstractNode obj = this.getModel().getObjectbyRow(row);
                        final ExtColumn<AbstractNode> col = this.getExtColumnAtPoint(e.getPoint());
                        //
                        if (LinkGrabberTable.this.isRowSelected(row)) {
                            // clicked on a selected row. let's confirm them all

                            ConfirmLinksContextAction.confirmSelection(getSelectionInfo(true, true), org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getValue(), false, false);
                        } else {
                            // clicked on a not-selected row. only add the context item

                            ConfirmLinksContextAction.confirmSelection(new SelectionInfo<CrawledPackage, CrawledLink>(obj, null), org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getValue(), false, false);

                        }

                    }
                }
            }
        }
        super.processMouseEvent(e);
    }

    protected void fireColumnModelUpdate() {
        super.fireColumnModelUpdate();
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                boolean alllocked = true;
                for (ExtColumn<?> c : getModel().getColumns()) {
                    if (c.isResizable()) {
                        alllocked = false;
                        break;
                    }
                }
                if (alllocked) {
                    JScrollPane sp = (JScrollPane) getParent().getParent();

                    CFG_GUI.HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED.setValue(true);
                    setColumnSaveID("hBAR");
                    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                }
            }
        };

    }

    protected void removeLoaderPanel(final MigPanel loaderPanel, final LayoutManager orgLayout, final Component rendererPane) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                remove(loaderPanel);
                setLayout(orgLayout);

                loaderPanel.setVisible(false);
                add(rendererPane);
                repaint();
            }
        };
    }

    public void sortPackageChildren(ExtDefaultRowSorter<AbstractNode> rowSorter, String nextSortIdentifier) {
        // TODO:
        // set LinkGrabberTableModel.setTRistate....to false and implement sorter here
    }

    protected boolean onHeaderSortClick(final MouseEvent event, final ExtColumn<AbstractNode> oldColumn, final String oldIdentifier, ExtColumn<AbstractNode> newColumn) {
        if (((LinkGrabberTableModel) getModel()).isTristateSorterEnabled()) return false;

        //
        if (JDGui.bugme(WarnLevel.NORMAL)) {
            UIOManager.I().showConfirmDialog(UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.getNextSortIdentifier_sort_warning_rly_title_(), _JDT._.getNextSortIdentifier_sort_warning_rly_msg(newColumn.getName()), NewTheme.I().getIcon("help", 32), _JDT._.basics_yes(), _JDT._.basics_no());
        }
        sortPackageChildren(newColumn.getRowSorter(), getModel().getNextSortIdentifier(newColumn.getSortOrderIdentifier()));

        return true;
    }

    @Override
    public boolean isSearchEnabled() {
        return false;
    }

    @Override
    protected boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {

        return false;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final java.util.List<AbstractNode> selection, final ExtColumn<AbstractNode> column, MouseEvent event) {

        return contextMenuFactory.createPopup(contextObject, selection, column, event);
    }

    protected JPopupMenu columnControlMenu(final ExtColumn<AbstractNode> extColumn) {
        JPopupMenu popup = super.columnControlMenu(extColumn);
        popup.add(new JSeparator());
        popup.add(new ExtCheckBoxMenuItem(new HorizontalScrollbarAction(this, CFG_GUI.HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED)));
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(final java.util.List<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {

        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        boolean containsOnline = false;
        for (final CrawledLink dl : getSelectionInfo().getChildren()) {

            nodesToDelete.add(dl);

            if (TYPE.OFFLINE == dl.getParentNode().getType()) continue;
            if (TYPE.POFFLINE == dl.getParentNode().getType()) continue;
            if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                containsOnline = true;

            }

        }
        LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, _GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all(), KeyObserver.getInstance().isControlDown(false), false, false, false, false);
        return true;
    }

    @Override
    protected boolean updateMoveButtonEnabledStatus() {
        return super.updateMoveButtonEnabledStatus();
    }

    @Override
    protected boolean onShortcutCopy(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        if (evt.isAltDown() || evt.isMetaDown() || evt.isAltGraphDown() || evt.isShiftDown()) return false;
        TransferHandler.getCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        return true;
    }

    @Override
    protected boolean onShortcutCut(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        if (evt.isAltDown() || evt.isMetaDown() || evt.isAltGraphDown() || evt.isShiftDown()) return false;
        TransferHandler.getCutAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cut"));
        return true;
    }

    @Override
    protected boolean onShortcutPaste(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        if (evt.isAltDown() || evt.isMetaDown() || evt.isAltGraphDown() || evt.isShiftDown()) return false;
        TransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));
        return true;
    }

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return LinkGrabberTableModel.getInstance().expandCollapse;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        try {
            final InputMap map = getInputMap(condition);
            final ActionMap am = getActionMap();

            if (map != null && am != null && isEnabled()) {
                final Object binding = map.get(stroke);
                final Action action = (binding == null) ? null : am.get(binding);
                if (action != null) {
                    if (action instanceof CustomizableAppAction) {
                        ((CustomizableAppAction) action).requestUpdate(this);
                    }
                    if (!action.isEnabled()) {

                        Toolkit.getDefaultToolkit().beep();
                    } else {

                        return SwingUtilities.notifyAction(action, stroke, evt, this, evt.getModifiers());

                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.processKeyBinding(stroke, evt, condition, pressed);
    }

    public void updateContextShortcuts() {

        final InputMap input = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final InputMap input2 = getInputMap(JComponent.WHEN_FOCUSED);
        final InputMap input3 = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actions = getActionMap();

        if (shortCutActions != null) {
            for (Entry<KeyStroke, Action> ks : shortCutActions.entrySet()) {
                Object binding = input.get(ks.getKey());
                input.remove(ks.getKey());
                input2.remove(ks.getKey());
                input3.remove(ks.getKey());
                actions.remove(binding);

            }
        }

        shortCutActions = new HashMap<KeyStroke, Action>();
        fillActions(MenuManagerLinkgrabberTableContext.getInstance().getMenuData());
        fillActions(MenuManagerLinkgrabberTabBottombar.getInstance().getMenuData());

    }

    private void fillActions(MenuContainer menuData) {
        final InputMap input = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final InputMap input2 = getInputMap(JComponent.WHEN_FOCUSED);
        final InputMap input3 = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        final ActionMap actions = getActionMap();

        for (MenuItemData mi : menuData.getItems()) {
            if (mi instanceof MenuContainer) {
                fillActions((MenuContainer) mi);
            } else if (mi instanceof SeperatorData) {
                continue;
            } else if (mi instanceof MenuLink) {
                continue;
            } else {
                AppAction action;
                try {
                    if (mi.getActionData() == null) continue;
                    action = mi.createAction();
                    KeyStroke keystroke;
                    if (StringUtils.isNotEmpty(mi.getShortcut())) {
                        keystroke = KeyStroke.getKeyStroke(mi.getShortcut());
                        if (keystroke != null) {
                            action.setAccelerator(keystroke);
                        }
                    } else if (MenuItemData.EMPTY_NAME.equals(mi.getShortcut())) {
                        action.setAccelerator(null);
                    }
                    keystroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
                    linkAction(input, input2, input3, actions, action, keystroke);
                    if (action instanceof CustomizableAppAction) {
                        List<KeyStroke> moreShortCuts = ((CustomizableAppAction) action).getAdditionalShortcuts(keystroke);
                        if (moreShortCuts != null) {
                            for (KeyStroke ks : moreShortCuts) {
                                if (ks != null) {
                                    linkAction(input, input2, input3, actions, action, ks);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    protected void linkAction(final InputMap input, final InputMap input2, final InputMap input3, final ActionMap actions, AppAction action, KeyStroke keystroke) {
        if (action != null && keystroke != null) {
            String key = "CONTEXT_ACTION_" + keystroke;
            try {
                Object old = input.get(keystroke);
                if (old != null && action.getClass() != actions.get(old).getClass()) {
                    logger.warning("Duplicate Shortcuts: " + action + " overwrites " + actions.get(old) + "(" + old + ")" + " for keystroke " + keystroke);
                }
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                Object old = input2.get(keystroke);
                if (old != null && action.getClass() != actions.get(old).getClass()) {
                    logger.warning("Duplicate Shortcuts: " + action + " overwrites " + actions.get(old) + "(" + old + ")" + " for keystroke " + keystroke);
                }
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                Object old = input3.get(keystroke);
                if (old != null && action.getClass() != actions.get(old).getClass()) {
                    logger.warning("Duplicate Shortcuts: " + action + " overwrites " + actions.get(old) + "(" + old + ")" + " for keystroke " + keystroke);
                }
            } catch (Exception e) {
                logger.log(e);
            }

            logger.info(keystroke + " -> " + action);

            input.put(keystroke, key);
            input2.put(keystroke, key);
            input3.put(keystroke, key);
            actions.put(key, action);
            shortCutActions.put(keystroke, action);

        }
    }

    public static LinkGrabberTable getInstance() {
        return INSTANCE;
    }

}
