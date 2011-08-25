package org.jdownloader.gui.views.linkgrabber;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;
import org.jdownloader.gui.views.downloads.columns.StopSignColumn;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmSingleNodeAction;
import org.jdownloader.gui.views.linkgrabber.actions.RemoveAction;
import org.jdownloader.images.NewTheme;

public class LinkGrabberTableModel extends LinkTableModel {

    private static final long serialVersionUID = -198189279671615981L;

    public LinkGrabberTableModel() {
        super("LinkGrabberTableModel");

    }

    @Override
    protected void initColumns() {
        this.addColumn(new FileColumn());
        this.addColumn(new ExtComponentColumn<PackageLinkNode>("Start") {

            private ConfirmSingleNodeAction confirmAction;
            private MigPanel                renderer;
            private MigPanel                editor;

            private RemoveAction            removeAction;
            private JLabel                  confirmRenderer;

            {
                confirmAction = new ConfirmSingleNodeAction();
                removeAction = new RemoveAction();

                renderer = new MigPanel("ins 1 0 0 0", "[]0[]", "[]");
                editor = new MigPanel("ins 1 0 0 0", "[]0[]", "[]");
                renderer.add(confirmRenderer = createLabel(confirmAction), "width 20!,height 20!,hidemode 2");
                // renderer.add(createLabel(removeAction),
                // "width 20!,height 20!");
                editor.add(getButton(confirmAction), "width 20!,height 20!,hidemode 2");
                // editor.add(getButton(removeAction), "width 20!,height 20!");
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("media-playback-start", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            private JLabel createLabel(AbstractAction action) {
                JLabel ret = new JLabel((ImageIcon) action.getValue(AbstractAction.SMALL_ICON));
                ret.setEnabled(false);
                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 20;
            }

            @Override
            public boolean isEditable(PackageLinkNode obj) {
                return obj instanceof FilePackage;
            }

            private JButton getButton(AbstractAction action) {
                final JButton ret = new JButton(action);
                ret.setContentAreaFilled(false);
                ret.setBorderPainted(false);
                ret.setToolTipText((String) action.getValue(AbstractAction.SHORT_DESCRIPTION));
                ret.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        ret.setContentAreaFilled(true);
                        ret.setBorderPainted(true);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        ret.setContentAreaFilled(false);
                        ret.setBorderPainted(false);
                    }
                });
                return ret;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            protected JComponent getInternalEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            protected JComponent getInternalRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public void configureEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
                confirmAction.setValue(value);
                removeAction.setValue(value);

            }

            @Override
            public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {

                if (value instanceof FilePackage) {
                    confirmRenderer.setVisible(true);
                } else {
                    confirmRenderer.setVisible(false);
                }

            }

            @Override
            public void resetEditor() {
                editor.setBackground(null);
                editor.setOpaque(false);

            }

            @Override
            public void resetRenderer() {
                renderer.setBackground(null);
                renderer.setOpaque(false);

            }

        });
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new AddedDateColumn());
        this.addColumn(new PriorityColumn());
        this.addColumn(new StopSignColumn());

    }

    /**
     * we want to return to default sort after each start
     */
    protected boolean isSortStateSaverEnabled() {
        return true;
    }

    // /**
    // * @return
    // */
    // protected ExtColumn<PackageLinkNode> getDefaultSortColumn() {
    // downloadOrder.setSortOrderIdentifier(ExtColumn.SORT_DESC);
    // return downloadOrder;
    // }

}
