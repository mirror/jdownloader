package org.jdownloader.settings.advanced;

import java.awt.Dialog.ModalityType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Locale;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.ConfigEntryKeywords;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
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

    public String internalKey = null;

    public String getInternalKey() {
        if (internalKey == null) {
            final String ret = getKey().replaceAll("[^a-zA-Z0-9 ]+", "").replace("colour", "color").replace("directory", "folder").toLowerCase(Locale.ENGLISH);
            internalKey = ret;
            return ret;
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

    public String getDescription() {
        final DescriptionForConfigEntry an = keyHandler.getAnnotation(DescriptionForConfigEntry.class);
        if (an != null) {
            return an.value();
        } else {
            return null;
        }
    }

    public String[] getKeywords() {
        final ConfigEntryKeywords an = keyHandler.getAnnotation(ConfigEntryKeywords.class);
        if (an != null) {
            return an.value();
        } else {
            return null;
        }
    }

    public Validator getValidator() {
        final SpinnerValidator an = keyHandler.getAnnotation(SpinnerValidator.class);
        if (an != null) {
            return new org.jdownloader.settings.advanced.RangeValidator(an.min(), an.max());
        } else {
            return null;
        }
    }

    public void setValue(Object value) {
        try {
            final Object v = getValue();
            if (value instanceof Number) {
                value = ReflectionUtils.castNumber((Number) value, getClazz());
            }
            keyHandler.getSetMethod().invoke(configInterface, new Object[] { value });
            if (!equals(v, value)) {
                if (keyHandler.getAnnotation(RequiresRestart.class) != null) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.AdvancedConfigEntry_setValue_restart_warning_title(keyHandler.getKey()), _GUI.T.AdvancedConfigEntry_setValue_restart_warning(keyHandler.getKey()), NewTheme.I().getIcon(IconKey.ICON_WARNING, 32), null, null) {
                            @Override
                            public String getDontShowAgainKey() {
                                return "RestartRequiredAdvancedConfig_" + getKey();
                            }
                        };
                        d.show();
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

    private boolean equals(Object v, Object value) {
        if (value == null && v == null) {
            return true;
        }
        if (v == null && value != null) {
            return false;
        }
        if (value == null && v != null) {
            return false;
        }
        return v.equals(value);
    }

    public Object getDefault() {
        return keyHandler.getDefaultValue();
    }

    public String getTypeString() {
        final Validator v = getValidator();
        final Type gen = keyHandler.getGetMethod().getGenericReturnType();
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
        return keyHandler.getRawClass();
    }
}
