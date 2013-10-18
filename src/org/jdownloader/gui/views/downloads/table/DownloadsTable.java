package org.jdownloader.gui.views.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtCheckBoxMenuItem;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;
import org.jdownloader.gui.views.downloads.action.DeleteSelectionAction;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadsTable extends PackageControllerTable<FilePackage, DownloadLink> {

    private static final long          serialVersionUID = 8843600834248098174L;
    private HashMap<KeyStroke, Action> shortCutActions;
    private LogSource                  logger;

    public DownloadsTable(final DownloadsTableModel tableModel) {
        super(tableModel);
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setTransferHandler(new DownloadsTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        logger = LogController.getInstance().getLogger(DownloadsTable.class.getName());
        onSelectionChanged();
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // loaderPanel.setSize(400, 400);

        final LayoutManager orgLayout = getLayout();
        final Component rendererPane = getComponent(0);

        setLayout(new MigLayout("ins 0", "[grow]", "[grow]"));

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

                    CFG_GUI.HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED.setValue(true);
                    setColumnSaveID("hBAR");
                    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                }
            }
        };

    }

    protected JPopupMenu columnControlMenu(final ExtColumn<AbstractNode> extColumn) {
        JPopupMenu popup = super.columnControlMenu(extColumn);
        popup.add(new JSeparator());
        popup.add(new ExtCheckBoxMenuItem(new HorizontalScrollbarAction(this, CFG_GUI.HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED)));
        return popup;
    }

    protected boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        // showPropertiesMenu(e.getPoint(), obj);

        return false;
    }

    @Override
    public boolean isSearchEnabled() {

        return false;
    }

    protected boolean onSingleClick(MouseEvent e, final AbstractNode obj) {

        // if (e.isAltDown() || e.isAltGraphDown()) {
        // showPropertiesMenu(e.getPoint(), obj);
        // return true;
        // }
        return super.onSingleClick(e, obj);
    }

    // private void showPropertiesMenu(Point point, AbstractNode obj) {
    // JPopupMenu m = new JPopupMenu();
    //
    // if (obj instanceof AbstractPackageNode) {
    //
    // Image back = (((AbstractPackageNode<?, ?>) obj).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) :
    // NewTheme.I().getImage("tree_package_closed", 32));
    //
    // m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new
    // ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)), SwingConstants.LEFT)));
    // m.add(new JSeparator());
    // } else if (obj instanceof DownloadLink) {
    //
    // Image back = (((DownloadLink) obj).getIcon().getImage());
    //
    // m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new
    // ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)), SwingConstants.LEFT)));
    // m.add(new JSeparator());
    // }
    //
    // final ExtColumn<AbstractNode> col = this.getExtColumnAtPoint(point);
    //
    // for (Component mm : DownloadTableContextMenuFactory.fillPropertiesMenu(new SelectionInfo<FilePackage, DownloadLink>(obj,
    // getModel().getSelectedObjects()), col)) {
    // m.add(mm);
    // }
    // m.show(this, point.x, point.y);
    // }

    @Override
    protected boolean onShortcutDelete(final java.util.List<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {

        new DeleteSelectionAction(new SelectionInfo<FilePackage, DownloadLink>(null, selectedObjects, null, evt, null, this)).actionPerformed(null);
        return true;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final java.util.List<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {
        /* split selection into downloadlinks and filepackages */
        return DownloadTableContextMenuFactory.getInstance().create(this, popup, contextObject, selection, column, ev);
    }

    @Override
    public boolean editCellAt(int row, int column) {

        boolean ret = super.editCellAt(row, column);

        return ret;
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean ret = super.editCellAt(row, column, e);

        return ret;
    }

    @Override
    protected boolean onHeaderSortClick(final MouseEvent e1, final ExtColumn<AbstractNode> oldSortColumn, String oldSortId, ExtColumn<AbstractNode> newColumn) {

        // own thread to
        new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Timer t = (Timer) e.getSource();
                t.stop();
                if (oldSortColumn == getModel().getSortColumn()) return;
                if (getModel().getSortColumn() != null) {
                    HelpDialog.show(e1.getLocationOnScreen(), "downloadtabe_sortwarner", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsTable_actionPerformed_sortwarner_title(getModel().getSortColumn().getName()), _GUI._.DownloadsTable_actionPerformed_sortwarner_text(), NewTheme.I().getIcon("sort", 32));

                }

            }
        }).start();
        return false;
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

    @SuppressWarnings("unchecked")
    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        try {
            final InputMap map = getInputMap(condition);
            final ActionMap am = getActionMap();

            if (map != null && am != null && isEnabled()) {
                final Object binding = map.get(stroke);
                final Action action = (binding == null) ? null : am.get(binding);
                if (action != null && action instanceof AbstractSelectionContextAction) {

                    ((AbstractSelectionContextAction) action).setSelection(getModel().createSelectionInfo());

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

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return DownloadsTableModel.getInstance().expandCollapse;
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
        fillActions(MenuManagerDownloadTableContext.getInstance().getMenuData());
        fillActions(MenuManagerDownloadTabBottomBar.getInstance().getMenuData());

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
                    action = mi.createAction(null);
                    KeyStroke keystroke;
                    if (StringUtils.isNotEmpty(mi.getShortcut())) {
                        keystroke = KeyStroke.getKeyStroke(mi.getShortcut());
                        if (keystroke != null) {
                            action.setAccelerator(keystroke);
                        }
                    }

                    if (action != null && (keystroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY)) != null) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
