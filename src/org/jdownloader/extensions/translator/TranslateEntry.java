package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.appwork.txtresource.Description;
import org.appwork.txtresource.TranslateInterface;

public class TranslateEntry {

    private TranslateInterface          tinterface;
    private Method                      method;
    private String                      translation;
    private ArrayList<TranslationError> errors;

    public TranslateEntry(TranslateInterface t, Method m) {
        tinterface = t;
        method = m;
        translation = tinterface._getHandler().getTranslation(method);
        errors = new ArrayList<TranslationError>();
        scanErrors();
    }

    private void scanErrors() {
        errors.clear();
        scanParameters();
        scanLength();
    }

    private void scanLength() {
        String def = this.getDefault();
        if (getTranslation().length() == 0) {
            errors.add(new TranslationError(TranslationError.Type.WARNING, "Translation is missing."));

        } else if (def != null && def.length() > 0) {
            if ((100 * Math.abs(def.length() - getTranslation().length())) / Math.min(def.length(), getTranslation().length()) > 80) {
                errors.add(new TranslationError(TranslationError.Type.WARNING, "Translation length differs a lot from default. Translation should have roughly the same length!"));
            }
        }
    }

    private void scanParameters() {

        for (int i = 0; i < getParameters().length; i++) {
            if (!getTranslation().contains("%s" + (i + 1))) {
                errors.add(new TranslationError(TranslationError.Type.ERROR, "Parameter %s" + (i + 1) + " is missing"));
                return;
            }
        }
    }

    public ArrayList<TranslationError> getErrors() {
        return errors;
    }

    public String getKey() {
        return method.getName();
    }

    public String getCategory() {
        return tinterface.getClass().getInterfaces()[0].getSimpleName().replaceAll("Translation", "");
    }

    public String getTranslation() {
        return translation;
    }

    public String getFullKey() {
        return tinterface.getClass().getInterfaces()[0].getName() + "." + method.getName();
    }

    public String getParameterString() {
        return method.toGenericString();
    }

    public String getDefault() {
        return tinterface._getHandler().getDefault(method);
    }

    public String getDescription() {
        Description ann = method.getAnnotation(Description.class);
        if (ann != null) return ann.value();
        return null;
    }

    public Type[] getParameters() {
        return method.getGenericParameterTypes();
    }

    public void setTranslation(String value) {
        translation = value;
        scanErrors();
    }

}
