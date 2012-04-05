package org.jdownloader.gui.views.downloads.table.linkproperties;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.table.SubMenuEditor;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;
import org.jdownloader.images.NewTheme;

public class LinkURLEditor extends SubMenuEditor {

    private FileColumn file;
    private UrlColumn  url;

    public LinkURLEditor(final DownloadLink contextObject, final ArrayList<DownloadLink> links, ArrayList<FilePackage> fps) {
        super();
        setLayout(new MigLayout("ins 2,wrap 2", "[grow,fill][]", "[][grow,fill]"));
        setOpaque(false);

        JLabel lbl = getLbl(_GUI._.LinkURLEditor(), NewTheme.I().getIcon("url", 18));
        add(SwingUtils.toBold(lbl), "spanx");
        file = new FileColumn() {
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
        };
        url = new UrlColumn() {
            @Override
            public int getDefaultWidth() {
                return 350;
            }

            @Override
            public boolean isEnabled(AbstractNode obj) {
                return true;
            }
        };
        final ExtTableModel<AbstractNode> model = new ExtTableModel<AbstractNode>("linkurleditor") {
            {
                getTableData().addAll(links);
            }

            @Override
            protected void initColumns() {
                addColumn(file);
                addColumn(url);

            }
        };
        BasicJDTable table = new BasicJDTable<AbstractNode>(model) {

            @Override
            protected JPopupMenu onContextMenu(JPopupMenu popup, AbstractNode contextObject, ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent mouseEvent) {

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

            protected boolean onShortcutCopy(final ArrayList<AbstractNode> selectedObjects, final KeyEvent evt) {
                StringBuilder sb = new StringBuilder();
                for (AbstractNode n : selectedObjects) {
                    if (n instanceof DownloadLink) {
                        if (sb.length() > 0) sb.append("\r\n");
                        sb.append(((DownloadLink) n).getBrowserUrl());
                    }

                }
                ClipboardMonitoring.getINSTANCE().setCurrentContent(sb.toString());
                return true;
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(650, 250));
        model.setColumnVisible(url, true);
        JScrollPane sp = new JScrollPane(table);

        add(sp, "spanx");
    }

    @Override
    public Point getDesiredLocation() {

        return new Point(100, 100);

    }

    @Override
    public void save() {

    }

}
