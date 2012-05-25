package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.appwork.txtresource.Description;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationSource;
import org.tmatesoft.svn.core.SVNDirEntry;

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

    private int                cntErrors = 0;
    private Boolean            isMissing = false;
    private Boolean            isDefault = false;

    private SVNDirEntry        svnEntry;
    private TranslationSource  source;

    /**
     * 
     * @param t
     *            Translationinterface that handles the given method
     * @param m
     *            translation method
     * @param svn
     */
    public TranslateEntry(TranslateInterface t, Method m, SVNDirEntry svn) {
        tinterface = t;
        method = m;
        // Get Translation String without replacing the %s*wildcard
        translation = tinterface._getHandler().getTranslation(method);
        source = tinterface._getHandler().getSource(method);

        svnEntry = svn;
        // validates the entry
        validate();
    }

    /**
     * validates the translation, and scans for usual errors
     */
    private void validate() {

        // missing check
        validateExists();
        if (!isMissing) {
            // default check
            isDefault = translation.equals(getDefault());
        }
        // parameter check.
        validateParameterCount();
    }

    private void validateExists() {

        if (source == null || !source.isFromLngFile() || !source.getID().equals(tinterface._getHandler().getID())) {

            isMissing = true;
        } else
            isMissing = false;
    }

    /**
     * Checks if the translated string has all wildcards defined by the translation interface
     */
    private void validateParameterCount() {

        cntErrors = 0;
        for (int i = 0; i < getParameters().length; i++) {
            if (!getTranslation().contains("%s" + (i + 1))) {

                cntErrors++;
                return;
            }
        }
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
     * @return Description assigned in the TranslationInterface for this entry or null
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
     * Sets a new TRanslation, and validates it. call {@link #getErrors()} afterwards to check for warnings or errors. This does NOT throw
     * an Exception
     * 
     * @param value
     */
    public void setTranslation(String value) {
        translation = value;
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
}
