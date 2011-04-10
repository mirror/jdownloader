package jd.gui.swing.components.table;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class SortMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6328630034846759725L;
    private Object            obj              = null;
    private JDTableColumn     column           = null;
    private static String     defaultString    = T._.gui_table_contextmenu_sort();

    public SortMenuItem() {
        super(SortMenuItem.defaultString);
        this.setIcon(JDTheme.II("gui.images.sort", 16, 16));
        this.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (this.column == null) { return; }
        if (this.column.isSortable(this.obj)) {
            this.column.doSort(this.obj);
        }
    }

    public void set(final JDTableColumn column, final Object obj, String desc) {
        if (desc == null) {
            desc = SortMenuItem.defaultString;
        }
        this.column = column;
        this.obj = obj;
        this.setText(desc);
    }
}