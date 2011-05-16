package org.jdownloader.settings.advanced;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.MethodHandler;
import org.appwork.storage.config.annotations.Description;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.settings.RangeValidatorMarker;
import org.jdownloader.settings.RegexValidatorAnnotation;

public class AdvancedConfigInterfaceEntry implements AdvancedConfigEntry {

    private MethodHandler   getter;
    private MethodHandler   setter;
    private ConfigInterface configInterface;

    public AdvancedConfigInterfaceEntry(ConfigInterface cf) {
        configInterface = cf;
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
    }

    public MethodHandler getSetter() {
        return setter;
    }

    public void setSetter(MethodHandler setter) {
        this.setter = setter;
    }

    public MethodHandler getGetter() {
        return getter;
    }

    public void setGetter(MethodHandler m) {
        getter = m;
    }

    public String getKey() {
        return configInterface.getStorageHandler().getConfigInterface().getName().replace("org.jdownloader.", "") + "." + getter.getKey();
    }

    public Object getValue() {
        try {
            return getter.getMethod().invoke(configInterface, new Class[] {});
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Class<?> getType() {
        return getter.getRawClass();
    }

    public String getDescription() {
        Description an = getAnnotation(Description.class);
        if (an != null) { return an.value(); }
        return null;
    }

    private <T extends Annotation> T getAnnotation(Class<T> class1) {
        T an = getter.getMethod().getAnnotation(class1);
        if (an == null) an = setter.getMethod().getAnnotation(class1);
        return an;
    }

    public Validator getValidator() {
        RangeValidatorMarker an = getAnnotation(RangeValidatorMarker.class);
        if (an != null) return new RangeValidator(an.range()[0], an.range()[1]);

        RegexValidatorAnnotation an2 = getAnnotation(RegexValidatorAnnotation.class);
        if (an2 != null) return new RegexValidator(an2.value());
        return null;
    }

    public Object getDefault() {
        if (getter.isPrimitive()) {
            if (Clazz.isBoolean(getter.getRawClass())) {
                return getter.getDefaultBoolean();
            } else if (Clazz.isLong(getter.getRawClass())) {
                return getter.getDefaultLong();
            } else if (Clazz.isInteger(getter.getRawClass())) {
                return getter.getDefaultInteger();
            } else if (Clazz.isByte(getter.getRawClass())) {
                return getter.getDefaultByte();
            } else if (Clazz.isFloat(getter.getRawClass())) {
                return getter.getDefaultFloat();
            } else if (getter.getRawClass() == String.class) {
                return getter.getDefaultString();
            } else if (getter.getRawClass().isEnum()) {

                return getter.getDefaultEnum();
            } else if (Clazz.isDouble(getter.getRawClass())) {
                return getter.getDefaultDouble();
            } else {
                return null;
            }
        } else {
            return getter.getDefaultObject();

        }
    }

    public void setValue(Object value) {
        try {
            setter.getMethod().invoke(configInterface, new Object[] { getType().cast(value) });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
