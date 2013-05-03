package org.jdownloader.settings.advanced;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.handler.KeyHandler;

public class AdvancedConfigEntry  {

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
        return configInterface.getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + "." + keyHandler.getKey();
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
            keyHandler.getSetter().getMethod().invoke(configInterface, new Object[] { value });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
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
