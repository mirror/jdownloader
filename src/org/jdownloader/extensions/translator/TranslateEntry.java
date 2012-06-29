package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.appwork.txtresource.Description;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationSource;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;

/**
 * Basic Data class for each Key-value pair
 * 
 * @author thomas
 * 
 */
public class TranslateEntry {

    private TranslateInterface tinterface;
    private Method             method;
    private String             translation;

    private int                cntErrors      = 0;
    private Boolean            isMissing      = false;
    private Boolean            isDefault      = false;

    private TranslationSource  source;
    private boolean            translationSet = false;
    private String             directTranslation;
    private String             parameterString;
    private String             description;

    public boolean isTranslationSet() {
        return translationSet;
    }

    @Override
    public int hashCode() {
        return (getMethod().getName() + tinterface._getHandler().getID()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TranslateEntry) { return ((TranslateEntry) obj).getMethod().equals(getMethod()) && tinterface._getHandler().getID() == ((TranslateEntry) obj).tinterface._getHandler().getID(); }
        return false;
    }

    /**
     * 
     * @param t
     *            Translationinterface that handles the given method
     * @param m
     *            translation method
     * @param svn
     */
    public TranslateEntry(TranslateInterface t, Method m) {
        tinterface = t;
        method = m;
        // Get Translation String without replacing the %s*wildcard

        source = tinterface._getHandler().getSource(method);
        Description da = m.getAnnotation(Description.class);
        if (da != null) {
            description = da.value();
        }
        // validates the entry
        translation = tinterface._getHandler().getTranslation(method).replace("\r", "\\r").replace("\n", "\\n");
        directTranslation = tinterface._getHandler().getTranslation(method).replace("\r", "\\r").replace("\n", "\\n");
        StringBuilder sb = new StringBuilder();

        for (Type tt : m.getParameterTypes()) {

            if (sb.length() > 0) sb.append("; ");
            if (tt instanceof Class) {
                sb.append(((Class) tt).getSimpleName());
            } else {
                sb.append(tt.toString());
            }
        }
        parameterString = sb.toString();
        validate();

    }

    public Method getMethod() {
        return method;
    }

    /**
     * validates the translation, and scans for usual errors
     */
    private void validate() {

        // missing check
        validateExists();
        isDefault = false;
        cntErrors = 0;
        if (!isMissing) {
            // default check
            isDefault = !tinterface._getHandler().getID().equals("en") && translation.equals(getDefault());
            // parameter check.
            if (!validateParameterCount(getTranslation())) {
                cntErrors++;
            }
        }

    }

    private void validateExists() {
        if (isTranslationSet() && !StringUtils.isEmpty(translation)) {
            isMissing = false;
            return;
        }
        if (source == null || !source.isFromLngFile() || !source.getID().equals(tinterface._getHandler().getID())) {

            isMissing = true;
        } else
            isMissing = false;
    }

    /**
     * Checks if the translated string has all wildcards defined by the translation interface
     */
    public boolean validateParameterCount(String t) {

        for (int i = 0; i < getParameters().length; i++) {
            if (!t.contains("%s" + (i + 1))) {

            return false; }
        }

        for (int i = getParameters().length; i < 20; i++) {
            if (t.contains("%s" + (i + 1))) {

            return false; }
        }
        return true;
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
        if (tinterface.getClass().getInterfaces()[0].getName().startsWith("org.jdownloader.extensions")) {
            return "Extension: " + tinterface.getClass().getInterfaces()[0].getSimpleName().replaceAll("Translation", "");
        } else {
            return tinterface.getClass().getInterfaces()[0].getSimpleName().replaceAll("Translation", "");
        }
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
     * @return a long key which might help the translater to identify the origin of the translation entry.
     */
    public String getFullKey() {
        return tinterface.getClass().getInterfaces()[0].getName() + "." + method.getName();
    }

    /**
     * 
     * @return a String of all parameters and Parameterclasses
     */
    public String getParameterString() {
        return parameterString;
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
     * @return Description assigned in the TranslationInterface for this entry or null
     */
    public String getDescription() {

        return description;
    }

    /**
     * 
     * @return List of all Parameter Types
     */
    public Type[] getParameters() {
        return method.getGenericParameterTypes();
    }

    /**
     * Sets a new TRanslation, and validates it. call {@link #getErrors()} afterwards to check for warnings or errors. This does NOT throw
     * an Exception
     * 
     * @param value
     */
    public void setTranslation(String value) {
        if (translation.equals(value) && translationSet) return;
        if (value.startsWith("Please translate:")) {
            // this is the help text;
            Dialog.getInstance().showErrorDialog("The translation starts with \"Please translate:\" and this is invalid. \r\nPlease Remove it from your translation.");
            value = "";
        }
        translation = value;
        translationSet = true;
        source = new TranslationSource(tinterface._getHandler().getID(), getMethod());
        validate();
    }

    public boolean isMissing() {
        return isMissing;
    }

    public boolean isDefault() {
        return isDefault;
        // return (translation.equals(getDefault()) &&
        // !this.getKey().endsWith("_accelerator") &&
        // !this.getKey().endsWith("_mnemonic") &&
        // !this.getKey().endsWith("_mnemonics"));
    }

    public boolean isOK() {
        return (!isMissing && cntErrors <= 0);
    }

    public TranslationSource getSource() {
        return source;
    }

    public boolean isParameterInvalid() {
        return cntErrors > 0;
    }

    public TranslateInterface getInterface() {
        return tinterface;
    }

    public void reset() {
        translationSet = false;

        source = tinterface._getHandler().getSource(method);
        // validates the entry
        translation = tinterface._getHandler().getTranslation(method);
        validate();
    }

    public String getDirect() {
        return directTranslation;
    }
}
