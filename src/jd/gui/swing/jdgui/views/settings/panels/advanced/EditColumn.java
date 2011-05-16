package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.table.columns.ExtComponentColumn;
import org.jdownloader.images.Theme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EditColumn extends ExtComponentColumn<AdvancedConfigEntry> {
    class InfoAction extends AbstractAction {
        private AdvancedConfigEntry value;

        public InfoAction() {
            super("Info", Theme.getIcon("info", 16));
        }

        public void actionPerformed(ActionEvent e) {
            StringBuilder sb = new StringBuilder();

            if (value.getDescription() != null) {
                sb.append(value.getDescription());
            }
            if (value.getDefault() != null) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append("Defaultvalue: " + JSonStorage.toString(value.getDefault()));
            }
            if (value.getValidator() != null) {
                if (sb.length() > 0) sb.append("\r\n\r\n");
                sb.append(value.getValidator());
            }
            Dialog.getInstance().showMessageDialog(Dialog.STYLE_LARGE, value.getKey(), sb.toString());
        }

        public void setEntry(AdvancedConfigEntry value) {

            this.value = value;
        }

    }

    public static final int SIZE = 16;
    private JPanel          renderer;
    private JPanel          editor;
    private InfoAction      info;
    private InfoAction      rendererInfo;

    public EditColumn() {
        super("Actions");
        renderer = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        editor = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        info = new InfoAction();
        rendererInfo = new InfoAction();
        renderer.add(getButton(rendererInfo), "width 18!,height 18!");
        editor.add(getButton(info), "width 18!,height 18!");
        // add(info);
    }

    private void add(AbstractAction action) {
        renderer.add(getButton(action), "width 18!,height 18!");
        editor.add(getButton(action), "width 18!,height 18!");
    }

    private Component getButton(AbstractAction action) {
        final JButton bt = new JButton(action);

        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setContentAreaFilled(false);
        bt.setBorderPainted(false);
        // bt.setBorder(null);
        bt.setToolTipText(bt.getText());
        bt.setText("");
        return bt;
    }

    @Override
    protected int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public int getMinWidth() {
        return 30;
    }

    @Override
    protected JComponent getEditorComponent(AdvancedConfigEntry value, boolean isSelected, int row, int column) {
        info.setEntry(value);

        return editor;
    }

    @Override
    protected JComponent getRendererComponent(AdvancedConfigEntry value, boolean isSelected, int row, int column) {
        rendererInfo.setEntry(value);
        return renderer;
    }

    private void setAdvancedConfigEntry(AdvancedConfigEntry value) {
        info.setEntry(value);
    }

}
