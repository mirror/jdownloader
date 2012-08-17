package org.jdownloader.gui.views.downloads.table.linkproperties;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;
import org.jdownloader.images.NewTheme;

public class LinkURLEditor<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends MigPanel {

    private SelectionInfo<PackageType, ChildrenType> si;

    public LinkURLEditor(SelectionInfo<PackageType, ChildrenType> selectionInfo) {
        super("ins 2,wrap 2", "[grow,fill][]", "[][grow,fill]");

        setOpaque(false);
        this.si = selectionInfo;
        JLabel lbl = getLbl(_GUI._.LinkURLEditor(), NewTheme.I().getIcon("url", 18));
        add(SwingUtils.toBold(lbl), "spanx");
        final ExtTableModel<AbstractNode> model = new ExtTableModel<AbstractNode>("linkurleditor") {
            {
                getTableData().addAll(si.getSelectedChildren());
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
                        setName(_GUI._.LinkURLEditor_onContextMenu_copy_());
                        setSmallIcon(NewTheme.I().getIcon("copy", 20));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onShortcutCopy(model.getSelectedObjects(), null);
                    }
                });
                return popup;
            }

            protected boolean onShortcutCopy(final java.util.List<AbstractNode> selectedObjects, final KeyEvent evt) {
                StringBuilder sb = new StringBuilder();
                List<AbstractNode> links = new ArrayList<AbstractNode>();
                if (selectedObjects.size() == 0) {
                    links.addAll(si.getSelectedChildren());
                } else {
                    links.addAll(selectedObjects);

                }
                for (String url : LinkTreeUtils.getURLs(links)) {
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(url);
                }
                ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
                return true;
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(650, 250));
        JScrollPane sp = new JScrollPane(table);
        add(sp, "spanx");
    }

    private JLabel getLbl(String linkURLEditor, ImageIcon icon) {
        JLabel ret = new JLabel(linkURLEditor);
        ret.setIcon(icon);
        return ret;
    }

}
