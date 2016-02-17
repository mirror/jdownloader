package org.jdownloader.gui.views.downloads.table.linkproperties;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.TaskQueue;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;
import org.jdownloader.images.AbstractIcon;

public class LinkURLEditor<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends MigPanel {

    private final SelectionInfo<PackageType, ChildrenType> si;

    public LinkURLEditor(SelectionInfo<PackageType, ChildrenType> selectionInfo) {
        super("ins 2,wrap 2", "[grow,fill][]", "[][grow,fill]");

        setOpaque(false);
        this.si = selectionInfo;
        JLabel lbl = getLbl(_GUI.T.LinkURLEditor(), new AbstractIcon(IconKey.ICON_URL, 18));
        add(SwingUtils.toBold(lbl), "spanx");
        final ExtTableModel<AbstractNode> model = new ExtTableModel<AbstractNode>("linkurleditor") {
            {
                getTableData().addAll(si.getChildren());
            }

            @Override
            protected void initColumns() {
                addColumn(new FileColumn() {
                    {
                        this.leftGapBorder = normalBorder;
                    }

                    @Override
                    public int getDefaultWidth() {
                        return 150;
                    }

                    @Override
                    public boolean isEnabled(AbstractNode obj) {
                        return true;
                    }

                    public boolean isEditable(AbstractNode obj) {
                        return false;
                    }

                    @Override
                    public boolean isDefaultVisible() {
                        return true;
                    }

                    @Override
                    public boolean isHidable() {
                        return false;
                    }
                });
                addColumn(new UrlColumn() {
                    @Override
                    public int getDefaultWidth() {
                        return 350;
                    }

                    @Override
                    public boolean isEnabled(AbstractNode obj) {
                        return true;
                    }

                    @Override
                    public boolean isDefaultVisible() {
                        return true;
                    }

                    @Override
                    public boolean isHidable() {
                        return false;
                    }

                });

            }
        };
        BasicJDTable table = new BasicJDTable<AbstractNode>(model) {

            @Override
            protected JPopupMenu onContextMenu(JPopupMenu popup, AbstractNode contextObject, java.util.List<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent mouseEvent) {

                popup.add(new AppAction() {
                    {
                        setName(_GUI.T.LinkURLEditor_onContextMenu_copy_());
                        setSmallIcon(new AbstractIcon(IconKey.ICON_COPY, 20));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onShortcutCopy(model.getSelectedObjects(), null);
                    }
                });
                return popup;
            }

            protected boolean onShortcutCopy(final List<AbstractNode> selectedObjects, final KeyEvent evt) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        final Set<String> urls;
                        if (selectedObjects.size() == 0) {
                            urls = LinkTreeUtils.getURLs(si, false);
                        } else {
                            urls = LinkTreeUtils.getURLs(new SelectionInfo<PackageType, ChildrenType>(null, selectedObjects), false);
                        }
                        final StringBuilder sb = new StringBuilder();
                        for (final String url : urls) {
                            if (sb.length() > 0) {
                                sb.append("\r\n");
                            }
                            sb.append(url);
                        }
                        ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
                        return null;
                    }
                });
                return true;
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(650, 250));
        JScrollPane sp = new JScrollPane(table);
        add(sp, "spanx");
    }

    private JLabel getLbl(String linkURLEditor, Icon icon) {
        JLabel ret = new JLabel(linkURLEditor);
        ret.setIcon(icon);
        return ret;
    }

}
