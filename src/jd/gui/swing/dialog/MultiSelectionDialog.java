package jd.gui.swing.dialog;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

/**
 * An {@link AbstractDialog} which allows for selecting multiple options from
 * given list.
 * 
 * @author mccartney (Grzegorz Oledzki)
 */
// TODO to be moved to AppWorkUtils org/appwork/utils/swing/dialog
// TODO [Refactoring] A common part with ComboBoxDialog can be extracted to a
// superclass
public class MultiSelectionDialog extends AbstractDialog<int[]> {

    private String           message;
    private ListCellRenderer renderer;
    private Object[]         options;
    private JTextPane        textpane;
    private JList            listComponent;

    public MultiSelectionDialog(int flag, String title, String question, Object[] options, ImageIcon icon, String okText, String cancelText, ListCellRenderer renderer) {
        super(flag, title, icon, okText, cancelText);
        this.message = question;
        this.renderer = renderer;
        this.options = options;
    }

    @Override
    protected int[] createReturnValue() {
        return this.getSelectedIndices();
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel contentpane = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]"));
        this.textpane = new JTextPane();
        this.textpane.setBorder(null);
        this.textpane.setBackground(null);
        this.textpane.setOpaque(false);
        this.textpane.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        this.textpane.setText(this.message);
        this.textpane.setEditable(false);

        contentpane.add(this.textpane);

        this.listComponent = this.createListComponent();
        contentpane.add(this.listComponent, "pushy,growy, width n:n:450,height 100::");

        return contentpane;
    }

    private JList createListComponent() {
        final JList ret = new JList(options);
        final ListCellRenderer rend = this.getRenderer(ret.getCellRenderer());
        if (rend != null) {
            ret.setCellRenderer(rend);
        }
        return ret;
    }

    protected int[] getSelectedIndices() {
        return this.listComponent.getSelectedIndices();
    }

    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
        return this.renderer;
    }

}
