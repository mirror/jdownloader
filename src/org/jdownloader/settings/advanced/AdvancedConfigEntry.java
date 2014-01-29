package org.jdownloader.settings.advanced;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AdvancedConfigEntry {

    private ConfigInterface configInterface;
    private KeyHandler<?>   keyHandler;

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

    public String getKey() {
        return configInterface._getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + "." + keyHandler.getKey();
    }

    public Object getValue() {
        return keyHandler.getValue();
    }

    public Class<?> getType() {
        return keyHandler.getRawClass();
    }

    public String getDescription() {

        DescriptionForConfigEntry an = keyHandler.getAnnotation(DescriptionForConfigEntry.class);
        if (an != null) { return an.value(); }
        return null;
    }

    public Validator getValidator() {
        SpinnerValidator an = keyHandler.getAnnotation(SpinnerValidator.class);
        if (an != null) return new org.jdownloader.settings.advanced.RangeValidator(an.min(), an.max());

        return null;
    }

    public void setValue(Object value) {

        try {
            Object v = getValue();
            keyHandler.getSetter().getMethod().invoke(configInterface, new Object[] { value });
            if (!equals(v, value)) {

                if (keyHandler.getAnnotation(RequiresRestart.class) != null) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.AdvancedConfigEntry_setValue_restart_warning_title(keyHandler.getKey()), _GUI._.AdvancedConfigEntry_setValue_restart_warning(keyHandler.getKey()), NewTheme.I().getIcon(IconKey.ICON_WARNING, 32), null, null) {

                            @Override
                            public String getDontShowAgainKey() {
                                return "RestartRequiredAdvancedConfig";
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
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private boolean equals(Object v, Object value) {
        if (value == null && v == null) return true;
        if (v == null && value != null) return false;
        if (value == null && v != null) return false;

        return v.equals(value);

    }

    public Object getDefault() {
        return keyHandler.getDefaultValue();
    }

    public String getTypeString() {
        Validator v = getValidator();
        Type gen = keyHandler.getGetter().getMethod().getGenericReturnType();
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
}
