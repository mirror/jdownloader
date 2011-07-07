package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.appwork.utils.swing.table.columns.ExtCompoundColumn;
import org.appwork.utils.swing.table.columns.ExtSpinnerColumn;
import org.appwork.utils.swing.table.columns.ExtTextAreaColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.RangeValidator;

public class AdvancedValueColumn extends ExtCompoundColumn<AdvancedConfigEntry> {

    private static final long                         serialVersionUID = 1L;
    private ExtTextColumn<AdvancedConfigEntry>        stringColumn;
    private ExtCheckColumn<AdvancedConfigEntry>       booleanColumn;
    private ExtTextAreaColumn<AdvancedConfigEntry>    defaultColumn;
    private ArrayList<ExtColumn<AdvancedConfigEntry>> columns;
    private ExtSpinnerColumn<AdvancedConfigEntry>     longColumn;
    private ExtComboColumn<AdvancedConfigEntry>       enumColumn;

    public AdvancedValueColumn() {
        super("Value");
        columns = new ArrayList<ExtColumn<AdvancedConfigEntry>>();
        initColumns();
    }

    @Override
    public boolean isEditable(AdvancedConfigEntry obj) {
        return true;
    }

    @Override
    public boolean isEnabled(AdvancedConfigEntry obj) {
        return true;
    }

    @Override
    public boolean isHidable() {
        return false;
    }

    private void initColumns() {
        stringColumn = new ExtTextColumn<AdvancedConfigEntry>(getName()) {
            private static final long serialVersionUID = 1L;
            {
                renderer.setHorizontalAlignment(SwingConstants.RIGHT);

            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getValue() + "";
            }

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            protected void setStringValue(String value, AdvancedConfigEntry object) {
                object.setValue(value);
            }
        };
        register(stringColumn);
        defaultColumn = new ExtTextAreaColumn<AdvancedConfigEntry>(getName()) {
            private static final long serialVersionUID = 1L;

            {
                renderer.setHorizontalAlignment(SwingConstants.RIGHT);
            }

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return JSonStorage.toString(value.getValue());
            }

            @Override
            protected void setStringValue(String value, AdvancedConfigEntry object) {

                Object newV = JSonStorage.restoreFromString(value, new TypeRef<Object>(object.getType()) {
                }, null);
                if (newV != null) {
                    object.setValue(newV);
                } else {
                    if (!"null".equalsIgnoreCase(value.trim())) {
                        Dialog.getInstance().showErrorDialog("'" + value + "' is not a valid '" + object.getTypeString() + "'");
                    }
                }

            }
        };
        register(defaultColumn);
        booleanColumn = new ExtCheckColumn<AdvancedConfigEntry>(getName()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean getBooleanValue(AdvancedConfigEntry value) {
                return (Boolean) value.getValue();
            }

            {
                this.renderer.setHorizontalAlignment(SwingConstants.RIGHT);
                renderer.setHorizontalTextPosition(SwingConstants.LEFT);

                this.editor.setHorizontalAlignment(SwingConstants.RIGHT);
            }

            @Override
            protected void setBooleanValue(boolean value, AdvancedConfigEntry object) {
                object.setValue(value);
            }
        };
        register(booleanColumn);

        longColumn = new ExtSpinnerColumn<AdvancedConfigEntry>(getName()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected SpinnerNumberModel getModel(AdvancedConfigEntry value, Number n) {
                SpinnerNumberModel ret = super.getModel(value, n);

                if (value.getValidator() != null) {
                    if (value.getValidator() instanceof RangeValidator) {

                        if (Clazz.isDouble(n.getClass())) {
                            ret.setMaximum((double) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((double) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((double) ((RangeValidator) value.getValidator()).getSteps());
                        } else if (Clazz.isFloat(n.getClass())) {
                            ret.setMaximum((float) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((float) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((float) ((RangeValidator) value.getValidator()).getSteps());
                        } else if (Clazz.isLong(n.getClass())) {
                            ret.setMaximum((long) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((long) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((long) ((RangeValidator) value.getValidator()).getSteps());
                        } else if (Clazz.isInteger(n.getClass())) {
                            ret.setMaximum((int) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((int) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((int) ((RangeValidator) value.getValidator()).getSteps());
                        } else if (Clazz.isShort(n.getClass())) {
                            ret.setMaximum((short) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((short) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((short) ((RangeValidator) value.getValidator()).getSteps());
                        } else if (Clazz.isByte(n.getClass())) {
                            ret.setMaximum((byte) ((RangeValidator) value.getValidator()).getMax());
                            ret.setMinimum((byte) ((RangeValidator) value.getValidator()).getMin());
                            ret.setStepSize((byte) ((RangeValidator) value.getValidator()).getSteps());
                        }

                    }
                }
                return ret;

            }

            @Override
            protected Number getNumber(AdvancedConfigEntry value) {
                return (Number) value.getValue();
            }

            @Override
            protected void setNumberValue(Number value, AdvancedConfigEntry object) {
                object.setValue(value);
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getValue() + "";
            }

        };
        register(longColumn);

        enumColumn = new ExtComboColumn<AdvancedConfigEntry>(getName(), null) {
            private static final long serialVersionUID = 1L;

            public ComboBoxModel updateModel(final ComboBoxModel dataModel, final AdvancedConfigEntry value) {

                Object[] values;
                try {
                    values = (Object[]) value.getType().getMethod("values", new Class[] {}).invoke(null, new Object[] {});
                    return new DefaultComboBoxModel(values);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;

            }

            @Override
            protected int getSelectedIndex(AdvancedConfigEntry value) {
                return ((Enum<?>) value.getValue()).ordinal();
            }

            @Override
            protected void setSelectedIndex(int value, AdvancedConfigEntry object) {

                Object[] values;
                try {
                    values = (Object[]) object.getType().getMethod("values", new Class[] {}).invoke(null, new Object[] {});
                    object.setValue(values[value]);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        };
        register(enumColumn);
    }

    private void register(ExtColumn<AdvancedConfigEntry> col) {
        columns.add(col);
    }

    @Override
    public String getSortString(AdvancedConfigEntry o1) {
        return null;
    }

    @Override
    public ExtColumn<AdvancedConfigEntry> selectColumn(AdvancedConfigEntry object) {
        if (Clazz.isBoolean(object.getType())) {
            return booleanColumn;
        } else if (object.getType() == String.class) {
            return stringColumn;
        } else if (Clazz.isDouble(object.getType()) || Clazz.isFloat(object.getType()) || Clazz.isLong(object.getType()) || Clazz.isInteger(object.getType()) || Clazz.isByte(object.getType())) {
            return longColumn;
        } else if (Enum.class.isAssignableFrom(object.getType())) {
            return enumColumn;
        } else {
            return defaultColumn;
        }

    }

}
