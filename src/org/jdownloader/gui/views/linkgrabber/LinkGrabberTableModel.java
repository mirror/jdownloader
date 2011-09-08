package org.jdownloader.gui.views.linkgrabber;

import java.util.ArrayList;

import javax.swing.Icon;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.logging.Log;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.CommentColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;

public class LinkGrabberTableModel extends PackageControllerTableModel<CrawledPackage, CrawledLink> {

    private static final long   serialVersionUID      = -198189279671615981L;
    private static final String SORT_LINKGRABBERORDER = "LINKGRABBER";

    public LinkGrabberTableModel() {
        super(LinkCollector.getInstance(), "LinkGrabberTableModel");
    }

    @Override
    protected void initColumns() {
        this.addColumn(new FileColumn());
        // this.addColumn(new ExtComponentColumn<AbstractNode>("Start") {
        //
        // private ConfirmSingleNodeAction confirmAction;
        // private MigPanel renderer;
        // private MigPanel editor;
        //
        // private RemoveAction removeAction;
        // private JLabel confirmRenderer;
        //
        // {
        // confirmAction = new ConfirmSingleNodeAction();
        // removeAction = new RemoveAction();
        //
        // renderer = new MigPanel("ins 1 0 0 0", "[]0[]", "[]");
        // editor = new MigPanel("ins 1 0 0 0", "[]0[]", "[]");
        // renderer.add(confirmRenderer = createLabel(confirmAction),
        // "width 20!,height 20!,hidemode 2");
        // // renderer.add(createLabel(removeAction),
        // // "width 20!,height 20!");
        // editor.add(getButton(confirmAction),
        // "width 20!,height 20!,hidemode 2");
        // // editor.add(getButton(removeAction), "width 20!,height 20!");
        // }
        //
        // public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader
        // jTableHeader) {
        //
        // final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this,
        // jTableHeader) {
        // private static final long serialVersionUID = 1L;
        //
        // @Override
        // public Component getTableCellRendererComponent(JTable table, Object
        // value, boolean isSelected, boolean hasFocus, int row, int column) {
        // super.getTableCellRendererComponent(table, value, isSelected,
        // hasFocus, row, column);
        // setIcon(NewTheme.I().getIcon("media-playback-start", 14));
        // setHorizontalAlignment(CENTER);
        // setText(null);
        // return this;
        // }
        //
        // };
        //
        // return ret;
        // }
        //
        // private JLabel createLabel(AbstractAction action) {
        // JLabel ret = new JLabel((ImageIcon)
        // action.getValue(AbstractAction.SMALL_ICON));
        // ret.setEnabled(false);
        // return ret;
        // }
        //
        // @Override
        // public int getMaxWidth() {
        // return 20;
        // }
        //
        // @Override
        // public boolean isEditable(AbstractNode obj) {
        // return obj instanceof FilePackage;
        // }
        //
        // private JButton getButton(AbstractAction action) {
        // final JButton ret = new JButton(action);
        // ret.setContentAreaFilled(false);
        // ret.setBorderPainted(false);
        // ret.setToolTipText((String)
        // action.getValue(AbstractAction.SHORT_DESCRIPTION));
        // ret.addMouseListener(new MouseAdapter() {
        //
        // @Override
        // public void mouseEntered(MouseEvent e) {
        // ret.setContentAreaFilled(true);
        // ret.setBorderPainted(true);
        // }
        //
        // @Override
        // public void mouseExited(MouseEvent e) {
        // ret.setContentAreaFilled(false);
        // ret.setBorderPainted(false);
        // }
        // });
        // return ret;
        // }
        //
        // @Override
        // public int getMinWidth() {
        // return getMaxWidth();
        // }
        //
        // @Override
        // protected JComponent getInternalEditorComponent(AbstractNode value,
        // boolean isSelected, int row, int column) {
        // return editor;
        // }
        //
        // @Override
        // protected JComponent getInternalRendererComponent(AbstractNode value,
        // boolean isSelected, boolean hasFocus, int row, int column) {
        // return renderer;
        // }
        //
        // @Override
        // public void configureEditorComponent(AbstractNode value, boolean
        // isSelected, int row, int column) {
        // confirmAction.setValue(value);
        // removeAction.setValue(value);
        //
        // }
        //
        // @Override
        // public void configureRendererComponent(AbstractNode value, boolean
        // isSelected, boolean hasFocus, int row, int column) {
        //
        // if (value instanceof FilePackage) {
        // confirmRenderer.setVisible(true);
        // } else {
        // confirmRenderer.setVisible(false);
        // }
        //
        // }
        //
        // @Override
        // public void resetEditor() {
        // editor.setBackground(null);
        // editor.setOpaque(false);
        //
        // }
        //
        // @Override
        // public void resetRenderer() {
        // renderer.setBackground(null);
        // renderer.setOpaque(false);
        //
        // }
        //
        // });
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new AvailabilityColumn());
        this.addColumn(new AddedDateColumn());
        this.addColumn(new PriorityColumn());
        this.addColumn(new CommentColumn());
    }

    /**
     * we want to return to default sort after each start
     */
    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    // /**
    // * @return
    // */
    protected ExtColumn<AbstractNode> getDefaultSortColumn() {
        return null;
    }

    @Override
    public String getNextSortIdentifier(String sortOrderIdentifier) {
        if (sortOrderIdentifier == null || sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
            return ExtColumn.SORT_DESC;
        } else if (sortOrderIdentifier.equals(ExtColumn.SORT_DESC)) {
            return SORT_LINKGRABBERORDER;
        } else {
            return ExtColumn.SORT_ASC;
        }
    }

    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_LINKGRABBERORDER.equals(sortOrderIdentifier)) { return null; }
        return super.getSortIcon(sortOrderIdentifier);
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep
     * their structure alive,data is only used to specify the size of the new
     * ArrayList
     */
    @Override
    public ArrayList<AbstractNode> sort(final ArrayList<AbstractNode> data, ExtColumn<AbstractNode> column) {
        if (column == null || column.getSortOrderIdentifier() == SORT_LINKGRABBERORDER) {
            this.sortColumn = null;
            try {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            } catch (final Exception e) {
                Log.exception(e);
            }
            ArrayList<AbstractNode> packages = null;
            final boolean readL = pc.readLock();
            try {
                /* get all packages from controller */
                packages = new ArrayList<AbstractNode>(pc.size());
                packages.addAll(pc.getPackages());
            } finally {
                pc.readUnlock(readL);
            }
            ArrayList<AbstractNode> newData = new ArrayList<AbstractNode>(Math.max(data.size(), packages.size()));
            for (AbstractNode node : packages) {
                newData.add(node);
                if (!((CrawledPackage) node).isExpanded()) continue;
                ArrayList<AbstractNode> files = null;
                synchronized (node) {
                    files = new ArrayList<AbstractNode>(((CrawledPackage) node).getChildren());
                }
                newData.addAll(files);
            }
            return newData;

        } else {
            return super.sort(data, column);
        }
    }

}
