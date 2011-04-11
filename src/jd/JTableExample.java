package jd;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import jd.gui.swing.laf.LookAndFeelController;

public class JTableExample extends JFrame {

    String data[][] = { { "John", "Sutherland", "Student" }, { "George", "Davies", "Student" }, { "Melissa", "Anderson", "Associate" }, { "Stergios", "Maglaras", "Developer" }, };

    String fields[] = { "Name", "Surname", "Status" };

    public static void main(String[] argv) {
        LookAndFeelController.getInstance().setUIManager();
        JTableExample myExample = new JTableExample("JTable Example");
    }

    public JTableExample(String title) {
        super(title);
        setSize(150, 150);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dispose();
                System.exit(0);
            }
        });
        init();
        pack();

        setVisible(true);
    }

    private void init() {
        JTable jt = new JTable(data, fields) {

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new SRR(super.getCellRenderer(row, column));
            }

        };
        jt.setShowGrid(true);
        TableCellRenderer r = jt.getCellRenderer(0, 0);

        JScrollPane pane = new JScrollPane(jt);
        // de.javasoft.plaf.synthetica.SyntheticaDefaultTableCellRenderer.class;
        getContentPane().add(pane);
    }
}