package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.MergedIcon;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EditColumn extends ExtTextColumn<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    static class InfoAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;
        private Icon                iconEnabled      = new AbstractIcon(IconKey.ICON_HELP, 16);
        private Icon                iconDisabled     = NewTheme.I().getDisabledIcon(new AbstractIcon(IconKey.ICON_HELP, 16));

        public InfoAction() {
            super("Help", new AbstractIcon(IconKey.ICON_HELP, 16));
        }

        @Override
        public boolean isEnabled() {
            if (value == null) {
                return false;
            } else if (value.hasDescription()) {
                return true;
            } else if (value.hasValidator()) {
                return true;
            } else if (value.hasDefaultValue()) {
                return true;
            } else {
                return false;
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (value.getDescription() != null) {
                sb.append(value.getDescription());
            }
            final Object defaultValue = value.getDefault();
            if (defaultValue != null) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append("Defaultvalue: " + JSonStorage.toString(defaultValue));
            }
            if (value.getValidator() != null) {
                if (sb.length() > 0) {
                    sb.append("\r\n\r\n");
                }
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

    public static class ResetAction extends AbstractAction {
        private static final long   serialVersionUID = 1L;
        private AdvancedConfigEntry value;
        private final Icon          reset_no         = NewTheme.I().getIcon(IconKey.ICON_RESET, 16);
        private final Icon          reset_yes        = NewTheme.I().getDisabledIcon(reset_no);
        private boolean             resetable        = false;
        private final EditColumn    editColumn;

        public ResetAction(EditColumn editcolumn) {
            super("Reset to Default");
            setEnabledIntern(true);
            this.editColumn = editcolumn;
        }

        public void actionPerformed(ActionEvent e) {
            if (!resetable) {
                return;
            }
            if (editColumn != null) {
                editColumn.stopCellEditing();
            }
            new EDTHelper<Void>() {
                @Override
                public Void edtRun() {
                    try {
                        final Object defaultValue = value.getDefault();
                        Dialog.getInstance().showConfirmDialog(0, "Reset to default?", "Really reset " + value.getKey() + " to " + defaultValue);
                        value.setValue(defaultValue);
                        if (editColumn != null) {
                            editColumn.getModel().getTable().repaint();
                        }
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
            final Object currentValue = value.getValue();
            if (currentValue == null) {
                setEnabledIntern(value.hasDefaultValue());
            } else {
                final Object defaultValue = value.getDefault();
                final boolean isDefault = AdvancedConfigEntry.equals(currentValue, defaultValue);
                setEnabledIntern(!isDefault);
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
        iconDD = new MergedIcon(org.jdownloader.images.NewTheme.I().getDisabledIcon(new AbstractIcon(IconKey.ICON_HELP, 16)), org.jdownloader.images.NewTheme.I().getDisabledIcon(new AbstractIcon(IconKey.ICON_RESET, 16)));
        iconED = new MergedIcon(new AbstractIcon(IconKey.ICON_HELP, 16), org.jdownloader.images.NewTheme.I().getDisabledIcon(new AbstractIcon(IconKey.ICON_RESET, 16)));
        iconDE = new MergedIcon(org.jdownloader.images.NewTheme.I().getDisabledIcon(new AbstractIcon(IconKey.ICON_HELP, 16)), new AbstractIcon(IconKey.ICON_RESET, 16));
        iconEE = new MergedIcon(new AbstractIcon(IconKey.ICON_HELP, 16), new AbstractIcon(IconKey.ICON_RESET, 16));
        info = new InfoAction();
        reset = new ResetAction(this);
        setRowSorter(new ExtDefaultRowSorter<AdvancedConfigEntry>() {
            @Override
            public int compare(AdvancedConfigEntry o1, AdvancedConfigEntry o2) {
                final Icon ic1 = getIcon(o1);
                final Icon ic2 = getIcon(o2);
                final int h1 = ic1 == null ? 0 : getIconID(ic1);
                final int h2 = ic2 == null ? 0 : getIconID(ic2);
                if (h1 == h2) {
                    return 0;
                }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return h1 > h2 ? -1 : 1;
                } else {
                    return h2 > h1 ? -1 : 1;
                }
            }

            private int getIconID(Icon ic) {
                if (ic == iconDE) {
                    return 1;
                }
                if (ic == iconEE) {
                    return 2;
                }
                if (ic == iconED) {
                    return 3;
                }
                if (ic == iconDD) {
                    return 4;
                }
                return 0;
            }
        });
    }

    @Override
    public JPopupMenu createHeaderPopup() {
        final JPopupMenu ret = new JPopupMenu();
        final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(_GUI.T.AdvancedTableModel_initColumns_modifiedonly_checkboxaction_());
        cb.setSelected(isResetOnlyFilterEnabled());
        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setResetOnlyFilterEnabled(cb.isSelected());
            }
        });
        ret.add(cb);
        return ret;
    }

    @Override
    public int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public boolean isSortable(AdvancedConfigEntry obj) {
        return true;
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

    protected boolean isResetOnlyFilterEnabled() {
        return false;
    }

    protected void setResetOnlyFilterEnabled(boolean enabled) {
    }

    @Override
    public boolean onSingleClick(MouseEvent e, AdvancedConfigEntry obj) {
        if (e.getPoint().x - getBounds().x < getWidth() / 2) {
            // left
            final InfoAction info = new InfoAction();
            info.setEntry(obj);
            info.actionPerformed(null);
        } else {
            // right
            final ResetAction reset = new ResetAction(this);
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
