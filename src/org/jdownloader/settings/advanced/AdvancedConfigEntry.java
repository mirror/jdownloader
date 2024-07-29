package org.jdownloader.settings.advanced;

import java.awt.Dialog.ModalityType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.ConfigEntryKeywords;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.HexColorString;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

public class AdvancedConfigEntry {
    private final ConfigInterface configInterface;
    private final KeyHandler<?>   keyHandler;

    public KeyHandler<?> getKeyHandler() {
        return keyHandler;
    }

    public AdvancedConfigEntry(ConfigInterface cf, KeyHandler<?> m) {
        configInterface = cf;
        keyHandler = m;
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
    }

    public String internalKey[] = null;

    public String[] getInternalKey() {
        if (internalKey == null) {
            final List<String> ret = new ArrayList<String>();
            ret.add(getKey());
            final String[] lookupKeys = getKeyHandler().getBackwardsCompatibilityLookupKeys();
            if (lookupKeys != null) {
                ret.addAll(Arrays.asList(lookupKeys));
            }
            for (int i = 0; i < ret.size(); i++) {
                final String key = ret.get(i).replaceAll("[^a-zA-Z0-9 ]+", "").replace("colour", "color").replace("directory", "folder").toLowerCase(Locale.ENGLISH);
                ret.set(i, key);
            }
            internalKey = ret.toArray(new String[0]);
            return internalKey;
        } else {
            return internalKey;
        }
    }

    private String key = null;

    public String getKey() {
        if (key == null) {
            final String ret = getConfigInterfaceName().concat(".").concat(getHandlerKey());
            key = ret;
            return ret;
        } else {
            return key;
        }
    }

    public static boolean equals(Object x, Object y) {
        if (x == null && y == null) {
            return true;
        } else if (x != null && y != null) {
            if (x == y || x.equals(y)) {
                return true;
            } else {
                if (ReflectionUtils.isList(x) && ReflectionUtils.isList(y)) {
                    final int xL = ReflectionUtils.getListLength(x);
                    final int yL = ReflectionUtils.getListLength(y);
                    if (xL == yL) {
                        for (int index = 0; index < xL; index++) {
                            final Object xE = ReflectionUtils.getListElement(x, index);
                            final Object yE = ReflectionUtils.getListElement(y, index);
                            if (equals(xE, yE) == false) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public String getHandlerKey() {
        return keyHandler.getKey();
    }

    private String configInterfaceName = null;

    public String getConfigInterfaceName() {
        if (configInterfaceName == null) {
            String ret = configInterface._getStorageHandler().getConfigInterface().getSimpleName();
            if (ret.contains("Config")) {
                ret = ret.replace("Config", "");
            }
            configInterfaceName = ret;
            return ret;
        }
        return configInterfaceName;
    }

    private String keyText = null;

    public String getKeyText() {
        if (keyText == null) {
            keyText = getConfigInterfaceName() + ": " + getKeyHandler().getReadableName();
            return keyText;
        } else {
            return keyText;
        }
    }

    public Object getValue() {
        return keyHandler.getValue();
    }

    public Type getType() {
        return keyHandler.getRawType();
    }

    private Boolean hasDescription = null;

    public boolean hasDescription() {
        if (hasDescription == null) {
            hasDescription = getKeyHandler().getAnnotation(DescriptionForConfigEntry.class) != null;
        }
        return hasDescription;
    }

    private Boolean hasHexColorString = null;

    public boolean hasHexColorString() {
        if (hasHexColorString == null) {
            hasHexColorString = getKeyHandler().getAnnotation(HexColorString.class) != null;
        }
        return hasHexColorString;
    }

    private Boolean hasDefaultValue = null;

    public boolean hasDefaultValue() {
        if (hasDefaultValue == null) {
            hasDefaultValue = getKeyHandler().hasDefaultValue();
        }
        return hasDefaultValue;
    }

    public String getDescription() {
        if (!hasDescription()) {
            return null;
        } else {
            final DescriptionForConfigEntry an = getKeyHandler().getAnnotation(DescriptionForConfigEntry.class);
            if (an != null) {
                return an.value();
            } else {
                return null;
            }
        }
    }

    public Class<? extends AdvandedValueEditor> getAdvancedValueEditor() {
        final AdvancedValueEditorFactory an = getKeyHandler().getAnnotation(AdvancedValueEditorFactory.class);
        if (an != null) {
            return an.value();
        } else {
            return null;
        }
    }

    private Boolean hasKeywords = null;

    public boolean hasKeywords() {
        if (hasKeywords == null) {
            hasKeywords = getKeyHandler().getAnnotation(ConfigEntryKeywords.class) != null;
        }
        return hasKeywords;
    }

    public String[] getKeywords() {
        if (!hasKeywords()) {
            return null;
        } else {
            final ConfigEntryKeywords an = getKeyHandler().getAnnotation(ConfigEntryKeywords.class);
            if (an != null) {
                return an.value();
            } else {
                return null;
            }
        }
    }

    private Boolean hasValidator = null;

    public boolean hasValidator() {
        if (hasValidator == null) {
            hasValidator = getKeyHandler().getAnnotation(SpinnerValidator.class) != null;
        }
        return hasValidator;
    }

    public Validator getValidator() {
        if (!hasValidator()) {
            return null;
        } else {
            final SpinnerValidator an = getKeyHandler().getAnnotation(SpinnerValidator.class);
            if (an != null) {
                return new org.jdownloader.settings.advanced.RangeValidator(an.min(), an.max());
            } else {
                return null;
            }
        }
    }

    public void setValue(Object value) {
        try {
            final Method setMethod = keyHandler.getSetMethod();
            if (setMethod == null) {
                // get only entry
                return;
            }
            final Object valueBefore = getValue();
            if (value instanceof Number) {
                value = ReflectionUtils.castNumber((Number) value, getClazz());
            }
            setMethod.invoke(configInterface, new Object[] { value });
            final Object valueAfter = getValue();
            if (equals(valueBefore, valueAfter)) {
                return;
            } else if (!equals(valueBefore, value)) {
                if (keyHandler.getAnnotation(RequiresRestart.class) != null) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        new Thread("RestartRequired:" + keyHandler.getKey()) {
                            {
                                setDaemon(true);
                            }

                            @Override
                            public void run() {
                                final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.AdvancedConfigEntry_setValue_restart_warning_title(keyHandler.getKey()), _GUI.T.AdvancedConfigEntry_setValue_restart_warning(keyHandler.getKey()), NewTheme.I().getIcon(IconKey.ICON_WARNING, 32), null, null) {
                                    @Override
                                    public String getDontShowAgainKey() {
                                        return "RestartRequiredAdvancedConfig_" + getKey();
                                    }
                                };
                                d.show();
                            }
                        }.start();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
            if (e.getTargetException() instanceof ValidationException) {
                new Thread() {
                    {
                        setDaemon(true);
                    }

                    public void run() {
                        final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_CANCEL, _AWU.T.DIALOG_MESSAGE_TITLE(), e.getTargetException().getMessage(), null, null, null) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }

                            @Override
                            public boolean isRemoteAPIEnabled() {
                                return true;
                            }
                        };
                        UIOManager.I().show(ConfirmDialogInterface.class, confirm);
                    };
                }.start();
            }
        }
    }

    public Object getDefault() {
        return getKeyHandler().getDefaultValue();
    }

    public String getTypeString() {
        final Validator v = getValidator();
        final Type gen = getKeyHandler().getGetMethod().getGenericReturnType();
        String ret;
        if (gen instanceof Class) {
            ret = ((Class<?>) gen).getSimpleName();
        } else {
            ret = gen.toString();
        }
        if (v != null) {
            ret += " [" + v + "]";
        }
        return ret;
    }

    public boolean isEditable() {
        return true;
    }

    public Class<?> getClazz() {
        return getKeyHandler().getRawClass();
    }
}
