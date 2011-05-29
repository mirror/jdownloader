package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.table.columns.ExtComponentColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EditColumn extends ExtComponentColumn<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    class InfoAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;

        public InfoAction() {
            super("Info", NewTheme.I().getIcon("info", 16));
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

    class ResetAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;

        public ResetAction() {
            super("Reset to Default", NewTheme.I().getIcon("reset", 16));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Dialog.getInstance().showConfirmDialog(0, "Reset to default?", "Really reset " + value.getKey() + " to " + value.getDefault());
                value.setValue(value.getDefault());
                EditColumn.this.getModel().fireTableDataChanged();
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        }

        public void setEntry(AdvancedConfigEntry value) {
            if (value.getValue() == null) {
                setEnabled(value.getDefault() != null);
            } else {
                setEnabled(!value.getValue().equals(value.getDefault()));
            }

            this.value = value;
        }

    }

    public static final int SIZE = 16;
    private JPanel          renderer;
    private JPanel          editor;
    private InfoAction      editorInfo;
    private InfoAction      rendererInfo;
    private ResetAction     editorReset;
    private ResetAction     rendererReset;
    private JButton         reset;

    public EditColumn() {
        super("Actions");
        renderer = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        editor = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        editorInfo = new InfoAction();

        rendererInfo = new InfoAction();

        editorReset = new ResetAction();
        rendererReset = new ResetAction();
        // renderer.add(getButton(rendererInfo), "width 18!,height 18!");
        // editor.add(getButton(editorInfo), "width 18!,height 18!");
        renderer.add(getButton(rendererReset), "width 18!,height 18!");
        editor.add(reset = getButton(editorReset), "width 18!,height 18!");
        // add(info);
    }

    private JButton getButton(AbstractAction action) {
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
        editorInfo.setEntry(value);
        editorReset.setEntry(value);
        reset.setToolTipText("Reset to " + value.getDefault());
        // rendererReset = new ResetAction();
        return editor;
    }

    @Override
    protected JComponent getRendererComponent(AdvancedConfigEntry value, boolean isSelected, int row, int column) {
        rendererInfo.setEntry(value);
        rendererReset.setEntry(value);
        return renderer;
    }

}
