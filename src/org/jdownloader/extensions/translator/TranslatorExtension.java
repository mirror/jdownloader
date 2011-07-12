package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;

import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate.JdownloaderTranslation;

/**
 * Extensionclass. NOTE: All extensions have to follow the namescheme to end
 * with "Extension" and have to extend AbstractExtension
 * 
 * @author thomas
 * 
 */
public class TranslatorExtension extends AbstractExtension<TranslatorConfig> {
    /**
     * Extension GUI
     */
    private TranslatorGui             gui;
    /**
     * List of all available languages
     */
    private ArrayList<TLocale>        translations;
    /**
     * If a translation is loaded, this list contains all it's entries
     */
    private ArrayList<TranslateEntry> translationEntries;
    /**
     * currently loaded Language
     */
    private TLocale                   loaded;

    public TranslatorExtension() {
        // Name. The translation Extension itself does not need translation. All
        // translators should be able to read english
        super("Translator");
        // get all LanguageIDs
        ArrayList<String> ids = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class);
        // create a list of TLocale instances
        translations = new ArrayList<TLocale>();
        for (String id : ids) {
            translations.add(new TLocale(id));
        }
        // sort the list.
        Collections.sort(translations, new Comparator<TLocale>() {

            public int compare(TLocale o1, TLocale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        // init extension GUI
        gui = new TranslatorGui(this);
    }

    /**
     * Has to return the Extension MAIN Icon. This icon will be used,for
     * example, in the settings pane
     */
    @Override
    public ImageIcon getIcon(int size) {
        return NewTheme.I().getIcon("language", size);
    }

    /**
     * Action "onStop". Is called each time the user disables the extension
     */
    @Override
    protected void stop() throws StopException {
        Log.L.finer("Stopped " + getClass().getSimpleName());
    }

    /**
     * Actions "onStart". is called each time the user enables the extension
     */
    @Override
    protected void start() throws StartException {
        Log.L.finer("Started " + getClass().getSimpleName());
    }

    /**
     * 
     * @return {@link #translations}
     */
    public ArrayList<TLocale> getTranslations() {
        return translations;
    }

    /**
     * gets called once as soon as the extension is loaded.
     */
    @Override
    protected void initExtension() throws StartException {
    }

    /**
     * Returns the Settingspanel for this extension. If this extension does not
     * have a configpanel, null can be returned
     */
    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return null;
    }

    /**
     * Should return false of this extension has no configpanel
     */
    @Override
    public boolean hasConfigPanel() {
        return false;
    }

    /**
     * DO NOT USE THIS FUNCTION. it is only used for compatibility reasons
     */
    @Override
    @Deprecated
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return "Coalado";
    }

    @Override
    public String getDescription() {
        return "This Extension can be used to edit JDownloader translations. You need a developer account to use this extension";
    }

    /**
     * Returns the gui
     */
    @Override
    public AddonPanel<? extends AbstractExtension<TranslatorConfig>> getGUI() {
        return gui;
    }

    /**
     * Loads the given language
     * 
     * @param locale
     */
    public void load(TLocale locale) {
        loaded = locale;
        ArrayList<TranslateEntry> tmp = new ArrayList<TranslateEntry>();
        for (TranslateInterface ti : TranslationFactory.getCachedInterfaces()) {
            Class<TranslateInterface> i = (Class<TranslateInterface>) ti.getClass().getInterfaces()[0];
            TranslateInterface t = TranslationFactory.create(i, locale.getId());
            for (Method m : t._getHandler().getMethods()) {
                tmp.add(new TranslateEntry(t, m));
            }

        }

        this.translationEntries = tmp;
    }

    /**
     * 
     * @return {@link #translationEntries}
     */
    public ArrayList<TranslateEntry> getTranslationEntries() {
        return translationEntries;
    }

    /**
     * 
     * @return {@link #loaded}
     */
    public TLocale getLoadedLocale() {
        return loaded;
    }

}
