package jd.gui.swing.jdgui.views.premiumview;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jd.config.SubConfiguration;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.jdgui.views.downloadview.DownloadTable;

public class PremiumTable extends JTable implements MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 9049514723238421532L;
    private PremiumPanel panel;
    private PremiumJTableModel model;
    private TableColumn[] cols;
    private PremiumTableRenderer cellRenderer;
    private PremiumTableEditor mycellEditor;

    public PremiumTable(PremiumJTableModel model, PremiumPanel panel) {
        super(model);
        this.panel = panel;
        this.model = model;
        createColumns();
        setShowGrid(false);
        addMouseListener(this);
        cellRenderer = new PremiumTableRenderer(this);
        mycellEditor = new PremiumTableEditor(this);
        getTableHeader().addMouseListener(this);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoscrolls(false);

        this.setRowHeight(DownloadTable.ROWHEIGHT);
        getTableHeader().setPreferredSize(new Dimension(getColumnModel().getTotalColumnWidth(), 19));
        this.setFillsViewportHeight(true);
    }

    public PremiumJTableModel getTableModel() {
        return model;
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
    }

    public TableCellEditor getCellEditor(int row, int column) {
        switch (column) {
        case PremiumJTableModel.COL_ENABLED:
            return mycellEditor;
        default:
            return super.getCellEditor(row, column);
        }
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        model.setValueAt(value, rowIndex, columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return model.isCellEditable(rowIndex, columnIndex);
    }

    public void createColumns() {
        setAutoCreateColumnsFromModel(false);
        TableColumnModel tcm = getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }

        final SubConfiguration config = SubConfiguration.getConfig("premiumview");
        cols = new TableColumn[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); ++i) {
            final int j = i;
            TableColumn tableColumn = new TableColumn(i);
            cols[i] = tableColumn;
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("width")) {
                        config.setProperty("WIDTH_COL_" + model.toModel(j), evt.getNewValue());
                        config.save();
                    }
                }
            });
            tableColumn.setPreferredWidth(config.getIntegerProperty("WIDTH_COL_" + model.toModel(j), tableColumn.getWidth()));
            addColumn(tableColumn);
        }
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                final Rectangle viewRect = panel.getScrollPane().getViewport().getViewRect();
                int[] rows = getSelectedRows();
                final ArrayList<Object> selected = new ArrayList<Object>();
                for (int row : rows) {
                    Object elem = model.getValueAt(row, 0);
                    if (elem != null) selected.add(elem);
                }
                model.refreshModel();
                model.fireTableStructureChanged();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (Object obj : selected) {
                            int row = model.getRowforObject(obj);
                            if (row != -1) addRowSelectionInterval(row, row);
                        }
                        scrollRectToVisible(viewRect);
                    }
                });
                return null;
            }
        }.start();
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == getTableHeader()) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();
                JCheckBoxMenuItem[] mis = new JCheckBoxMenuItem[model.getRealColumnCount()];

                for (int i = 0; i < model.getRealColumnCount(); ++i) {
                    final int j = i;
                    final JExtCheckBoxMenuItem mi = new JExtCheckBoxMenuItem(model.getRealColumnName(i));
                    mi.setHideOnClick(false);
                    mis[i] = mi;
                    if (i == 0) mi.setEnabled(false);
                    mi.setSelected(model.isVisible(i));
                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            model.setVisible(j, mi.isSelected());
                            createColumns();
                            revalidate();
                            repaint();
                        }

                    });
                    popup.add(mi);
                }
                popup.show(getTableHeader(), e.getX(), e.getY());
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
