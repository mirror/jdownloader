package org.jdownloader.extensions.translator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;

import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationFactory;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate.JdownloaderTranslation;

public class TranslatorExtension extends AbstractExtension<TranslatorConfig> {

    private TranslatorGui             gui;
    private ArrayList<TLocale>        translations;
    private ArrayList<TranslateEntry> translationEntries;
    private TLocale                   loaded;

    public TranslatorExtension() {
        super("Translator");

        ArrayList<String> ids = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class);
        translations = new ArrayList<TLocale>();
        for (String id : ids) {
            translations.add(new TLocale(id));
        }
        Collections.sort(translations, new Comparator<TLocale>() {

            public int compare(TLocale o1, TLocale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        gui = new TranslatorGui(this);
    }

    @Override
    public ImageIcon getIcon(int size) {
        return NewTheme.I().getIcon("language", size);
    }

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {

    }

    public ArrayList<TLocale> getTranslations() {
        return translations;
    }

    @Override
    protected void initExtension() throws StartException {
    }

    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return null;
    }

    @Override
    public boolean hasConfigPanel() {
        return false;
    }

    @Override
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

    @Override
    public AddonPanel<? extends AbstractExtension<TranslatorConfig>> getGUI() {
        return gui;
    }

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

    public ArrayList<TranslateEntry> getTranslationEntries() {
        return translationEntries;
    }

    public TLocale getLoadedLocale() {
        return loaded;
    }

}
