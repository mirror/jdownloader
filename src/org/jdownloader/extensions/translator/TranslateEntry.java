package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.appwork.txtresource.Description;
import org.appwork.txtresource.TranslateInterface;

/**
 * Basic Data class for each Key-value pair
 * 
 * @author thomas
 * 
 */
public class TranslateEntry {

    private TranslateInterface          tinterface;
    private Method                      method;
    private String                      translation;
    private ArrayList<TranslationProblem> errors;

    /**
     * 
     * @param t
     *            Translationinterface that handles the given method
     * @param m
     *            translation method
     */
    public TranslateEntry(TranslateInterface t, Method m) {
        tinterface = t;
        method = m;
        // Get Translation String without replacing the %s*wildcards

        translation = tinterface._getHandler().getTranslation(method);
        errors = new ArrayList<TranslationProblem>();
        // validates the entry
        validate();
    }

    /**
     * validates the translation, and scans for usual errors
     */
    private void validate() {
        errors.clear();
        // parameter check.
        validateParameterCount();
        // length check
        validateStringLength();
    }

    /**
     * writes a warning of length differs too much from default String length
     * TODO: Probably it would be better, to do a real String width check
     * instead of letter counter. A b etter algorithm would be fine, too.
     * no<->nein has 100% difference but does not really matter in most cases
     */
    private void validateStringLength() {
        String def = this.getDefault();
        if (getTranslation().length() == 0) {
            errors.add(new TranslationProblem(TranslationProblem.Type.WARNING, "Translation is missing."));

        } else if (def != null && def.length() > 0) {
            if ((100 * Math.abs(def.length() - getTranslation().length())) / Math.min(def.length(), getTranslation().length()) > 80) {
                errors.add(new TranslationProblem(TranslationProblem.Type.WARNING, "Translation length differs a lot from default. Translation should have roughly the same length!"));
            }
        }
    }

    /**
     * Checks if the translated string has all wildcards defined by the
     * translation interface
     */
    private void validateParameterCount() {

        for (int i = 0; i < getParameters().length; i++) {
            if (!getTranslation().contains("%s" + (i + 1))) {
                errors.add(new TranslationProblem(TranslationProblem.Type.ERROR, "Parameter %s" + (i + 1) + " is missing"));
                return;
            }
        }
    }

    /**
     * Returns all errors and warnings for this entry
     * 
     * @return
     */
    public ArrayList<TranslationProblem> getErrors() {
        return errors;
    }

    /**
     * returns the Key
     * 
     * @return
     */
    public String getKey() {
        return method.getName();
    }

    /**
     * 
     * @return the category of this entry
     */
    public String getCategory() {
        return tinterface.getClass().getInterfaces()[0].getSimpleName().replaceAll("Translation", "");
    }

    /**
     * 
     * @return translation(value) for this entry. It contains all wildcards.
     */
    public String getTranslation() {
        return translation;
    }

    /**
     * 
     * @return a long key which might help the translater to identify the origin
     *         of the translation entry.
     */
    public String getFullKey() {
        return tinterface.getClass().getInterfaces()[0].getName() + "." + method.getName();
    }

    /**
     * 
     * @return a String of all parameters and Parameterclasses
     */
    public String getParameterString() {
        return method.toGenericString();
    }

    /**
     * 
     * @return Default translation without replacing its wildcards
     */
    public String getDefault() {
        return tinterface._getHandler().getDefault(method);
    }

    /**
     * 
     * @return Description assigned in the TranslationInterface for this entry
     *         or null
     */
    public String getDescription() {
        Description ann = method.getAnnotation(Description.class);
        if (ann != null) return ann.value();
        return null;
    }

    /**
     * 
     * @return List of all Parameter Types
     */
    public Type[] getParameters() {
        return method.getGenericParameterTypes();
    }

    /**
     * Sets a new TRanslation, and validates it. call {@link #getErrors()}
     * afterwards to check for warnings or errors. This does NOT throw an
     * Exception
     * 
     * @param value
     */
    public void setTranslation(String value) {
        translation = value;
        validate();
    }

}
