package org.jdownloader.settings.advanced;

import java.lang.reflect.InvocationTargetException;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.KeyHandler;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.RangeValidatorMarker;
import org.jdownloader.settings.RegexValidatorAnnotation;

public class AdvancedConfigInterfaceEntry implements AdvancedConfigEntry {

    private ConfigInterface configInterface;
    private KeyHandler      keyHandler;

    public KeyHandler getKeyHandler() {
        return keyHandler;
    }

    public AdvancedConfigInterfaceEntry(ConfigInterface cf, KeyHandler m) {
        configInterface = cf;
        keyHandler = m;
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
    }

    public String getKey() {
        return configInterface.getStorageHandler().getConfigInterface().getSimpleName() + "." + keyHandler.getKey();
    }

    public Object getValue() {
        return keyHandler.getValue();
    }

    public Class<?> getType() {
        return keyHandler.getRawClass();
    }

    public String getDescription() {
        Description an = keyHandler.getAnnotation(Description.class);
        if (an != null) { return an.value(); }
        return null;
    }

    public Validator getValidator() {
        RangeValidatorMarker an = keyHandler.getAnnotation(RangeValidatorMarker.class);
        if (an != null) return new RangeValidator(an.range()[0], an.range()[1]);

        RegexValidatorAnnotation an2 = keyHandler.getAnnotation(RegexValidatorAnnotation.class);
        if (an2 != null) return new RegexValidator(an2.value());
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

}
