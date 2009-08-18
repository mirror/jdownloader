package jd.gui.swing.components.JDTable;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jd.config.SubConfiguration;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.jdgui.views.downloadview.DownloadTable;
import jd.utils.JDUtilities;

public class JDTable extends JTable {

    /**
     * 
     */
    private static final long serialVersionUID = -6631229711568284941L;
    private JDTableModel model;
    private SubConfiguration tableconfig;

    public JDTable(JDTableModel model) {
        super(model);
        this.model = model;
        tableconfig = model.getConfig();
        createColumns();
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        getTableHeader().addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == getTableHeader()) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        JPopupMenu popup = new JPopupMenu();
                        JCheckBoxMenuItem[] mis = new JCheckBoxMenuItem[getJDTableModel().getRealColumnCount()];

                        for (int i = 0; i < getJDTableModel().getRealColumnCount(); ++i) {
                            final int j = i;
                            final JExtCheckBoxMenuItem mi = new JExtCheckBoxMenuItem(getJDTableModel().getRealColumnName(i));
                            mi.setHideOnClick(false);
                            mis[i] = mi;
                            if (i == 0) mi.setEnabled(false);
                            mi.setSelected(getJDTableModel().isVisible(i));
                            mi.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    getJDTableModel().setVisible(j, mi.isSelected());
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
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoscrolls(false);
        this.setRowHeight(DownloadTable.ROWHEIGHT);
        getTableHeader().setPreferredSize(new Dimension(getColumnModel().getTotalColumnWidth(), 19));
        // This method is 1.6 only
        if (JDUtilities.getJavaVersion() >= 1.6) this.setFillsViewportHeight(true);
    }

    public ArrayList<JDRowHighlighter> getJDRowHighlighter() {
        return model.getJDRowHighlighter();
    }

    public void addJDRowHighlighter(JDRowHighlighter high) {
        model.addJDRowHighlighter(high);
    }

    public JDTableModel getJDTableModel() {
        return model;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        return model.getJDTableColumn(model.toModel(col));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        return model.getJDTableColumn(model.toModel(column));
    }

    private void createColumns() {
        setAutoCreateColumnsFromModel(false);
        TableColumnModel tcm = getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }
        for (int i = 0; i < getModel().getColumnCount(); ++i) {
            final int j = i;
            TableColumn tableColumn = new TableColumn(i);
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("width")) {
                        tableconfig.setProperty("WIDTH_COL_" + model.getRealColumnName(model.toModel(j)), evt.getNewValue());
                        tableconfig.save();
                    }
                }
            });
            tableColumn.setPreferredWidth(tableconfig.getIntegerProperty("WIDTH_COL_" + model.getRealColumnName(model.toModel(j)), tableColumn.getWidth()));
            addColumn(tableColumn);
        }
    }

    public int getRealColumnAtPoint(int x) {
        /*
         * diese funktion gibt den echten columnindex zurück, da durch
         * an/ausschalten dieser anders kann
         */
        x = getColumnModel().getColumnIndexAtX(x);
        return model.toModel(x);
    }

}
