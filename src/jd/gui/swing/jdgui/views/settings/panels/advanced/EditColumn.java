package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.components.MergedIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EditColumn extends ExtTextColumn<AdvancedConfigEntry> {
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

    public static final int SIZE = 16;

    private InfoAction      editorInfo;
    private InfoAction      rendererInfo;

    private MergedIcon      iconDD;

    private MergedIcon      iconED;

    private MergedIcon      iconDE;

    private MergedIcon      iconEE;

    private InfoAction      info;

    private ResetAction     reset;

    public EditColumn() {
        super("Actions");

        iconDD = new MergedIcon(ImageProvider.getDisabledIcon(NewTheme.I().getIcon("help", 16)), ImageProvider.getDisabledIcon(NewTheme.I().getIcon("reset", 16)));
        iconED = new MergedIcon(NewTheme.I().getIcon("help", 16), ImageProvider.getDisabledIcon(NewTheme.I().getIcon("reset", 16)));
        iconDE = new MergedIcon(ImageProvider.getDisabledIcon(NewTheme.I().getIcon("help", 16)), NewTheme.I().getIcon("reset", 16));
        iconEE = new MergedIcon(NewTheme.I().getIcon("help", 16), NewTheme.I().getIcon("reset", 16));
        info = new InfoAction();
        reset = new ResetAction();
    }

    @Override
    public int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public int getMinWidth() {
        return 45;
    }

    public ExtTooltip createToolTip(final Point p, final AdvancedConfigEntry obj) {
        if (p.x - getBounds().x < getWidth() / 2) {
            // left
            this.tooltip.setTipText("Click to Open an infopanel");

        } else {
            // right
            this.tooltip.setTipText("Click to reset to " + obj.getDefault());
            System.out.println("RIGHT");
        }

        return this.tooltip;
    }

    @Override
    public boolean onSingleClick(MouseEvent e, AdvancedConfigEntry obj) {
        if (e.getPoint().x - getBounds().x < getWidth() / 2) {
            // left
            System.out.println("LEFT");
            InfoAction info = new InfoAction();
            info.setEntry(obj);
            info.actionPerformed(null);
        } else {
            // right
            System.out.println("RIGHT");
            ResetAction reset = new ResetAction();
            reset.setEntry(obj);
            reset.actionPerformed(null);
        }
        return super.onSingleClick(e, obj);
    }

    @Override
    protected String getTooltipText(AdvancedConfigEntry obj) {
        return null;
    }

    @Override
    public String getStringValue(AdvancedConfigEntry value) {
        return null;
    }

    @Override
    protected Icon getIcon(AdvancedConfigEntry value) {
        info.setEntry(value);
        reset.setEntry(value);
        boolean resetable = reset.isEnabled();
        boolean info = this.info.isEnabled();

        if (resetable && info) {
            return iconEE;
        } else if (!resetable && info) {
            return iconED;
        } else if (!resetable && !info) {
            return iconDD;
        } else {
            return iconDE;
        }
    }

}
