package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EditColumn extends ExtComponentColumn<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    static class InfoAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;
        private ImageIcon           iconEnabled      = NewTheme.I().getIcon("help", 16);
        private ImageIcon           iconDisabled     = new ImageIcon(GrayFilter.createDisabledImage(NewTheme.I().getIcon("help", 16).getImage()));

        public InfoAction() {
            super("Help", NewTheme.I().getIcon("help", 16));
        }

        @Override
        public boolean isEnabled() {
            if (value == null) return false;
            if (value.getDescription() != null) { return true; }
            if (value.getDefault() != null) { return true; }
            if (value.getValidator() != null) { return true; }
            return false;
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
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
            if (isEnabled()) {
                putValue(Action.SMALL_ICON, iconEnabled);
            } else {
                putValue(Action.SMALL_ICON, iconDisabled);
            }
        }

    }

    class ResetAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;
        private ImageIcon           reset_no         = NewTheme.I().getIcon(IconKey.ICON_RESET, 16);
        private ImageIcon           reset_yes        = new ImageIcon(GrayFilter.createDisabledImage(NewTheme.I().getIcon(IconKey.ICON_RESET, 16).getImage()));
        private boolean             resetable        = false;

        public ResetAction() {
            super("Reset to Default");
            setEnabledIntern(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!resetable) return;
            EditColumn.this.stopCellEditing();
            new EDTHelper<Void>() {

                @Override
                public Void edtRun() {
                    try {
                        Dialog.getInstance().showConfirmDialog(0, "Reset to default?", "Really reset " + value.getKey() + " to " + value.getDefault());
                        value.setValue(value.getDefault());
                        EditColumn.this.getModel().getTable().repaint();
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                    return null;
                }
            }.start(true);

        }

        public void setEntry(AdvancedConfigEntry value) {
            if (value.getValue() == null) {
                setEnabledIntern(value.getDefault() != null);
            } else {
                setEnabledIntern(!value.getValue().equals(value.getDefault()));
            }
            this.value = value;
        }

        public void setEnabledIntern(boolean b) {
            if (b) {
                putValue(Action.SMALL_ICON, reset_no);
            } else {
                putValue(Action.SMALL_ICON, reset_yes);
            }
            resetable = b;
            super.setEnabled(b);
        }

    }

    public static final int  SIZE = 16;
    private RendererMigPanel renderer;
    private RendererMigPanel editor;
    private InfoAction       editorInfo;
    private InfoAction       rendererInfo;
    private ResetAction      editorReset;
    private ResetAction      rendererReset;
    private JButton          reset;

    public EditColumn() {
        super("Actions");
        renderer = new RendererMigPanel("ins 2", "[]", "[]");
        editor = new RendererMigPanel("ins 2", "[]", "[]");
        editorInfo = new InfoAction();
        rendererInfo = new InfoAction();

        editorReset = new ResetAction();
        rendererReset = new ResetAction();
        renderer.add(getButton(rendererInfo), "width 18!,height 18!");
        editor.add(getButton(editorInfo), "width 18!,height 18!");
        renderer.add(getButton(rendererReset), "width 18!,height 18!");
        editor.add(reset = getButton(editorReset), "width 18!,height 18!");
        // add(info);
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

    private JButton getButton(final AbstractAction action) {
        final JButton bt = new JButton(action) {

            @Override
            public void setEnabled(boolean b) {
            }

        };
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setContentAreaFilled(false);
        bt.setBorderPainted(false);
        // bt.setBorder(null);
        bt.setToolTipText(bt.getText());
        bt.setText("");
        return bt;
    }

    @Override
    public int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public int getMinWidth() {
        return 45;
    }

    @Override
    public void configureEditorComponent(AdvancedConfigEntry value, boolean isSelected, int row, int column) {
        editorInfo.setEntry(value);
        editorReset.setEntry(value);
    }

    @Override
    public void configureRendererComponent(AdvancedConfigEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
        rendererInfo.setEntry(value);
        rendererReset.setEntry(value);
    }

    @Override
    protected String getTooltipText(AdvancedConfigEntry obj) {
        return "Reset to " + obj.getDefault();
    }

    @Override
    protected JComponent getInternalEditorComponent(AdvancedConfigEntry value, boolean isSelected, int row, int column) {

        return editor;
    }

    @Override
    protected JComponent getInternalRendererComponent(AdvancedConfigEntry value, boolean isSelected, boolean hasFocus, int row, int column) {

        return renderer;
    }

}
