package org.jdownloader.extensions.translator;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.ImageIcon;

import jd.captcha.translate.CaptchaTranslation;
import jd.controlling.reconnect.pluginsinc.batch.translate.BatchTranslation;
import jd.controlling.reconnect.pluginsinc.extern.translate.ExternTranslation;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.LiveheaderTranslation;
import jd.controlling.reconnect.pluginsinc.upnp.translate.UpnpTranslation;
import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.swing.exttable.ExtTableTranslation;
import org.appwork.txtresource.DynamicResourcePath;
import org.appwork.txtresource.TranslateData;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslateResource;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.txtresource.TranslationHandler;
import org.appwork.txtresource.TranslationSource;
import org.appwork.txtresource.TranslationUtils;
import org.appwork.update.standalone.translate.StandaloneUpdaterTranslation;
import org.appwork.update.updateclient.translation.UpdateTranslation;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.locale.AWUTranslation;
import org.appwork.utils.logging.Log;
import org.appwork.utils.svn.FilePathFilter;
import org.appwork.utils.svn.Subversion;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.api.cnl2.translate.ExternInterfaceTranslation;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.jdownloader.gui.translate.GuiTranslation;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate.JdownloaderTranslation;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Extensionclass. NOTE: All extensions have to follow the namescheme to end with "Extension" and have to extend AbstractExtension
 * 
 * @author thomas
 * 
 */
public class TranslatorExtension extends AbstractExtension<TranslatorConfig, TranslateInterface> {
    /**
     * Extension GUI
     */
    private TranslatorGui                  gui;
    /**
     * List of all available languages
     */
    private ArrayList<TLocale>             translations;
    /**
     * If a translation is loaded, this list contains all it's entries
     */
    private ArrayList<TranslateEntry>      translationEntries;
    /**
     * currently loaded Language
     */
    private TLocale                        loaded;
    private TranslatorExtensionEventSender eventSender;
    private Thread                         timer;
    private String                         fontname;

    public String getFontname() {
        return fontname;
    }

    public static void main(String[] args) {
        ArrayList<File> files = Files.getFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (!pathname.getName().endsWith(".lng")) return false;

                if (pathname.getName().contains("es-castillian")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("zh-hans")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("zh-hant")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("sr-latin")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("pt-BR")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("bg-incomplete")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("pt-PT")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("bg_incomplete")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("_", "__")));

                }
                return false;
            }

            private void renameTo(File pathname, File file) {
                System.out.println(pathname + "->" + file);
                if (file.exists()) file.delete();
                pathname.renameTo(file);
            }
        }, new File("c:\\workspace\\JDownloader"));

    }

    public TranslatorExtension() {
        // Name. The translation Extension itself does not need translation. All
        // translators should be able to read english
        setTitle("Translator");
        eventSender = new TranslatorExtensionEventSender();
        // get all LanguageIDs
        List<String> ids = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class, GuiTranslation.class);
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

        // unload extensions on exit
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            {
                setHookPriority(Integer.MAX_VALUE);
            }

            @Override
            public void run() {

                if (!getSettings().isRememberLoginsEnabled()) {
                    doLogout();
                } else {
                    try {
                        write();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        // init extension GUI
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                gui = new TranslatorGui(TranslatorExtension.this);
                return null;
            }
        }.getReturnValue();

    }

    public TranslatorExtensionEventSender getEventSender() {
        return eventSender;
    }

    /**
     * Has to return the Extension MAIN Icon. This icon will be used,for example, in the settings pane
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
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
    }

    /**
     * Actions "onStart". is called each time the user enables the extension
     */
    @Override
    protected void start() throws StartException {
        Log.L.finer("Started " + getClass().getSimpleName());
        timer = new Thread("TranslatorSyncher") {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1 * 60 * 1000);

                        if (getGUI().isShown()) {
                            if (loggedIn) {

                                refresh();
                            }

                        }

                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.start();
    }

    protected void refresh() throws InterruptedException {
        if (loaded != null) {
            try {
                write();
                load(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
     * Returns the Settingspanel for this extension. If this extension does not have a configpanel, null can be returned
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
    public AddonPanel<? extends AbstractExtension<TranslatorConfig, TranslateInterface>> getGUI() {
        return gui;
    }

    /**
     * Loads the given language
     * 
     * @param locale
     * @throws InterruptedException
     */
    public void load(TLocale locale) throws InterruptedException {
        if (locale == null) return;
        loaded = locale;

        getSettings().setLastLoaded(locale.getId());
        try {
            List<SVNDirEntry> svnEntries = null;
            // List<SVNDirEntry> svnEntries = listSvnLngFileEntries();

            ArrayList<TranslateEntry> tmp = new ArrayList<TranslateEntry>();

            Subversion s = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations/translations/", getSettings().getSVNUser(), getSettings().getSVNPassword());
            try {
                s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
            } catch (SVNException e) {
                Log.exception(e);
                s.cleanUp(Application.getResource("translations/custom"), true);
                s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
            }
            s.resolveConflicts(Application.getResource("translations/custom"), new ConflictResolveHandler());
            s.dispose();
            for (LazyExtension le : ExtensionController.getInstance().getExtensions()) {
                try {
                    le.init();

                    if (le._getExtension().getTranslation() == null) continue;
                    load(tmp, svnEntries, locale, (Class<? extends TranslateInterface>) le._getExtension().getTranslation().getClass().getInterfaces()[0]);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            // use Type Hierarchy in IDE to get all interfaces
            // Extension Translations should not be referenced here
            load(tmp, svnEntries, locale, AWUTranslation.class);
            load(tmp, svnEntries, locale, BatchTranslation.class);
            load(tmp, svnEntries, locale, CaptchaTranslation.class);
            load(tmp, svnEntries, locale, ExternInterfaceTranslation.class);
            load(tmp, svnEntries, locale, ExternTranslation.class);
            load(tmp, svnEntries, locale, ExtTableTranslation.class);
            GuiTranslation guiInterface = load(tmp, svnEntries, locale, GuiTranslation.class);
            fontname = guiInterface.fontname();
            if (fontname.equalsIgnoreCase("default")) fontname = "Tahoma";
            load(tmp, svnEntries, locale, JdownloaderTranslation.class);
            load(tmp, svnEntries, locale, LiveheaderTranslation.class);
            load(tmp, svnEntries, locale, StandaloneUpdaterTranslation.class);
            load(tmp, svnEntries, locale, UpdateTranslation.class);
            load(tmp, svnEntries, locale, UpnpTranslation.class);
            // there should be no more entries. all of them should have been
            // mapped to an INterface
            if (svnEntries != null) {
                for (SVNDirEntry e : svnEntries) {
                    Log.L.warning("No Interface for " + e.getRelativePath());
                }
            }

            this.translationEntries = tmp;
            // System.out.println(1);
            getEventSender().fireEvent(new TranslatorExtensionEvent(this, org.jdownloader.extensions.translator.TranslatorExtensionEvent.Type.LOADED_TRANSLATION));
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    private List<SVNDirEntry> listSvnLngFileEntries() throws SVNException, InterruptedException {

        final String extension = loaded.getId() + ".lng";
        Subversion s = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/", getSettings().getSVNUser(), getSettings().getSVNPassword());
        List<SVNDirEntry> files = s.listFiles(new FilePathFilter() {
            @Override
            public boolean accept(SVNDirEntry path) {

                return path.getName().endsWith(extension);
            }

        }, "");
        s.dispose();
        return files;

    }

    private <T extends TranslateInterface> T load(ArrayList<TranslateEntry> tmp, List<SVNDirEntry> svnEntries, TLocale locale, Class<T> class1) {
        Log.L.info("Load Translation " + locale + " " + class1);
        String lookupPath = class1.getName().replace(".", "/") + "." + locale.getId() + ".lng";
        SVNDirEntry svn = null;
        if (svnEntries != null) {
            for (SVNDirEntry d : svnEntries) {
                // System.out.println(d.getRelativePath());
                // System.out.println(lookupPath);
                if (d.getRelativePath().endsWith(lookupPath)) {

                    svn = d;
                    svnEntries.remove(svn);
                    break;
                }
            }
        }
        TranslateInterface t = (TranslateInterface) Proxy.newProxyInstance(class1.getClassLoader(), new Class[] { class1 }, new TranslationHandler(class1, locale.getId()));

        for (Method m : t._getHandler().getMethods()) {
            tmp.add(new TranslateEntry(t, m, svn));
        }
        return (T) t;

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

    private boolean loggedIn = false;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
        getEventSender().fireEvent(new TranslatorExtensionEvent(this, TranslatorExtensionEvent.Type.LOGIN_STATUS_CHANGED));
    }

    public void doLogin() throws InterruptedException {
        if (isLoggedIn()) return;
        if (getSettings().isRememberLoginsEnabled()) {
            if (validateSvnLogin(getSettings().getSVNUser(), getSettings().getSVNPassword())) { return; }
        }
        requestSvnLogin();
    }

    public void doLogout() {
        try {
            write();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getSettings().setSVNUser(null);
        getSettings().setSVNPassword(null);
        getSettings().setRememberLoginsEnabled(false);
        loaded = null;
        translationEntries = null;
        setLoggedIn(false);

    }

    public boolean validateSvnLogin(String svnUser, String svnPass) throws InterruptedException {
        setLoggedIn(false);
        if (svnUser.length() > 3 && svnPass.length() > 3) {
            String url = "svn://svn.jdownloader.org/jdownloader";

            Subversion s = null;
            try {
                s = new Subversion(url, svnUser, svnPass);
                setLoggedIn(true);
                // if (getSettings().getLastLoaded() != null) {

                TLocale pre = getTLocaleByID(getSettings().getLastLoaded());
                if (pre == null) {
                    pre = new TLocale(TranslationFactory.getDesiredLocale().toString());
                }
                load(pre);
                // }
                return true;
            } catch (SVNException e) {
                Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
            } finally {
                try {
                    s.dispose();
                } catch (final Throwable e) {
                }
            }
        } else {
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Username and/or password seem malformed. Test failed.");
        }
        return false;

    }

    public TLocale getTLocaleByID(String lastLoaded) {
        if (lastLoaded == null) return null;
        for (TLocale t : getTranslations()) {
            if (t.getId().equals(lastLoaded)) { return t; }
        }
        return null;
    }

    public void requestSvnLogin() throws InterruptedException {

        while (true) {

            final LoginDialog d = new LoginDialog(0, "Translation Server Login", "To modify existing translations, or to create a new one, you need a JDownloader Translator Account.", null);
            d.setUsernameDefault(getSettings().getSVNUser());
            d.setPasswordDefault(getSettings().getSVNPassword());
            d.setRememberDefault(getSettings().isRememberLoginsEnabled());

            LoginData response;
            try {
                response = Dialog.getInstance().showDialog(d);
            } catch (DialogClosedException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            } catch (DialogCanceledException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            }
            if (validateSvnLogin(response.getUsername(), response.getPassword())) {
                getSettings().setSVNUser(response.getUsername());
                getSettings().setSVNPassword(response.getPassword());
                getSettings().setRememberLoginsEnabled(response.isSave());

                return;
            }
        }

    }

    public void setDefault(ArrayList<TranslateEntry> selection) {

        for (TranslateEntry te : selection) {
            te.setTranslation(te.getDirect());
        }

        getEventSender().fireEvent(new TranslatorExtensionEvent(this, org.jdownloader.extensions.translator.TranslatorExtensionEvent.Type.REFRESH_DATA));

    }

    public void setTranslation(ArrayList<TranslateEntry> selection, TLocale sel) {
        int failed = 0;
        for (TranslateEntry te : selection) {
            Class<? extends TranslateInterface> class1 = (Class<? extends TranslateInterface>) te.getInterface().getClass().getInterfaces()[0];
            TranslateInterface t = (TranslateInterface) Proxy.newProxyInstance(class1.getClassLoader(), new Class[] { class1 }, new TranslationHandler(class1, sel.getId()));
            String translation = t._getHandler().getTranslation(te.getMethod());
            TranslationSource source = t._getHandler().getSource(te.getMethod());
            if (source == null || !source.getID().equals(sel.getId())) {
                failed++;
                continue;
            }

            te.setTranslation(translation);
        }
        if (failed > 0) {
            Dialog.getInstance().showErrorDialog("The " + sel + " Translation is not complete. Not all requested entries could be cloned");
        }
        getEventSender().fireEvent(new TranslatorExtensionEvent(this, org.jdownloader.extensions.translator.TranslatorExtensionEvent.Type.REFRESH_DATA));

    }

    public void reset(TranslateEntry te) {
        te.reset();
    }

    public double getPercent() {

        return (getOK() * 10000 / getTranslationEntries().size()) / 100.0;
    }

    public int getOK() {

        int ok = 0;
        for (TranslateEntry te : getTranslationEntries()) {
            if (te.isMissing() || te.isParameterInvalid()) {

            } else {
                ok++;
            }
        }
        return ok;
    }

    public SVNCommitPacket save() throws IOException, SVNException {

        write();

        return upload();
    }

    public void write() throws IOException {
        if (loaded == null) return;
        HashSet<TranslationHandler> set = new HashSet<TranslationHandler>();
        HashMap<Method, TranslateEntry> map = new HashMap<Method, TranslateEntry>();
        for (TranslateEntry te : getTranslationEntries()) {
            set.add(te.getInterface()._getHandler());
            map.put(te.getMethod(), te);
        }

        for (TranslationHandler h : set) {
            TranslateResource res = h.getResource(loaded.getId());

            final TranslateData data = new TranslateData();

            for (Method m : h.getMethods()) {
                TranslateEntry te = map.get(m);
                if (te.isOK() || te.isDefault()) {
                    data.put(te.getKey(), te.getTranslation());
                }
            }
            if (data.size() == 0) continue;
            String file = TranslationUtils.serialize(data);
            URL url = res.getUrl();
            File newFile = null;
            // if (url != null) {
            // try {
            // newFile = new File(url.toURI());
            // } catch (URISyntaxException e) {
            // newFile = new File(url.getPath());
            // }
            // } else {
            System.out.println("NO URL");
            DynamicResourcePath rPath = h.getInterfaceClass().getAnnotation(DynamicResourcePath.class);
            if (rPath != null) {

                try {
                    newFile = Application.getResource("translations/custom/" + rPath.value().newInstance().getPath() + "." + loaded.getId() + ".lng");
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (newFile == null) {
                newFile = Application.getResource("translations/custom/" + h.getInterfaceClass().getName().replace(".", "/") + "." + loaded.getId() + ".lng");
            }
            // }
            newFile.delete();
            newFile.getParentFile().mkdirs();
            IO.writeStringToFile(newFile, file);

            Log.L.info("Updated " + file);

        }
    }

    public SVNCommitPacket upload() throws SVNException {
        Subversion s = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations/translations/", getSettings().getSVNUser(), getSettings().getSVNPassword());
        try {
            s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
        } catch (SVNException e) {
            Log.exception(e);
            s.cleanUp(Application.getResource("translations/custom"), true);
            s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
        }
        s.resolveConflicts(Application.getResource("translations/custom"), new ConflictResolveHandler());
        s.getWCClient().doAdd(Application.getResource("translations/custom"), true, false, true, SVNDepth.INFINITY, false, false);
        Log.L.finer("Create CommitPacket");
        final SVNCommitPacket packet = s.getCommitClient().doCollectCommitItems(new File[] { Application.getResource("translations/custom") }, false, false, SVNDepth.INFINITY, null);

        Log.L.finer("Transfer Package");
        s.getCommitClient().doCommit(packet, true, false, "Updated " + loaded.getLocale().getDisplayName() + " Translation", null);
        s.dispose();
        return packet;
    }

    public void revert() throws SVNException, InterruptedException {
        Subversion s = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations/translations/", getSettings().getSVNUser(), getSettings().getSVNPassword());
        try {
            s.revert(Application.getResource("translations/custom"));

            s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
        } catch (SVNException e) {
            Log.exception(e);
            s.cleanUp(Application.getResource("translations/custom"), true);
            s.update(Application.getResource("translations/custom"), SVNRevision.HEAD, null);
        }
        s.resolveConflicts(Application.getResource("translations/custom"), new ConflictResolveHandler());
        s.dispose();
        load(loaded);
    }

}
