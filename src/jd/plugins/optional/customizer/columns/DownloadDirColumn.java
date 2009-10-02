package jd.plugins.optional.customizer.columns;

import java.awt.Component;

import javax.swing.JFileChooser;

import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.CustomizeSetting;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class DownloadDirColumn extends JDTableColumn {

    private static final long serialVersionUID = 1687752044574718418L;
    private JRendererLabel jlr;
    private BrowseFile file;

    public DownloadDirColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        file = new BrowseFile(new MigLayout("ins 0", "[grow 100,fill,160:null:null]3[min!]"));
        file.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        file.setButtonText("...");
        file.getTextField().setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return file.getText();
    }

    @Override
    public boolean isEditable(Object obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((CustomizeSetting) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        file.setText(((CustomizeSetting) value).getDownloadDir());
        return file;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((CustomizeSetting) value).getDownloadDir());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((CustomizeSetting) object).setDownloadDir((String) value);
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
