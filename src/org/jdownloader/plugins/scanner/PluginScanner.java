package org.jdownloader.plugins.scanner;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.encoding.Encoding;
import jd.plugins.DecrypterPlugin;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.logging.Log;

public class PluginScanner {
    private static final String DECRYPTERPATH = "jd/plugins/decrypter";

    static {
        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());
        // do this call to keep the correct root in Application Cache
        Application.setApplication(".jd_home");
        Application.getRoot(PluginScanner.class);
    }

    public static void main(String[] args) throws MalformedURLException {
        // PluginScanner.getInstance().writeHosterCache();
        // PluginScanner.getInstance().writeDecrypterCache();
        DecrypterCache dc = getInstance().loadDecrypterCache();
        HosterCache hc = getInstance().loadHosterCache();

        // PluginScanner.getInstance().writeAddonCache();
    }

    public HosterCache loadHosterCache() {
        long t = System.currentTimeMillis();
        try {
            return JSonStorage.restoreFrom(getHostCacheFile(), new HosterCache());
        } finally {
            Log.L.finer("Loaded HosterCache in " + (System.currentTimeMillis() - t) + "ms");
        }
    }

    public DecrypterCache loadDecrypterCache() {
        long t = System.currentTimeMillis();
        try {
            return JSonStorage.restoreFrom(getDecrypterCacheFile(), new DecrypterCache());
        } finally {
            Log.L.finer("Loaded Decryptercache in " + (System.currentTimeMillis() - t) + "ms");
        }
    }

    private void writeDecrypterCache() throws MalformedURLException {

        ArrayList<DecrypterPluginInfo> decrypter = PluginScanner.getInstance().getDecrypter();
        DecrypterCache hc = new DecrypterCache();
        for (DecrypterPluginInfo h : decrypter) {
            String[] names, patterns;
            names = h.getAnnotation().names();
            patterns = h.getAnnotation().urls();
            if (names.length == 0) {
                try {
                    patterns = (String[]) h.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                    names = (String[]) h.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});

                } catch (Throwable e) {
                    Log.exception(e);
                }
            }
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    try {
                        CachedDecrypterInfo ch = new CachedDecrypterInfo(names[i], patterns[i], h.getAnnotation().revision(), Hash.getMD5(h.getFile()), h.getClazz().getName());

                        PluginForDecrypt plg = (PluginForDecrypt) h.getClazz().getConstructor(new Class[] { PluginWrapper.class }).newInstance(new DecryptPluginWrapper(names[i], h.getClazz().getSimpleName(), patterns[i], 0, h.getAnnotation().revision()));
                        ch.setHost(plg.getHost());

                        ch.setSettingsAvailable(plg.getConfig() != null && plg.getConfig().getEntries() != null && plg.getConfig().getEntries().size() > 0);

                        hc.add(ch);

                    } catch (Throwable e) {
                        Log.exception(e);
                    }

                }
            }
            // h.getClazz().getAnnotation();

        }

        JSonStorage.saveTo(getDecrypterCacheFile(), hc);
    }

    private File getDecrypterCacheFile() {
        return new File(Application.getResource(DECRYPTERPATH), "cache.json");
    }

    private ArrayList<DecrypterPluginInfo> getDecrypter() throws MalformedURLException {
        final File[] files = Application.getResource(DECRYPTERPATH).listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".class") && !name.contains("$");
            }
        });
        final URLClassLoader cl = new PluginClassLoader();
        final String pkg = PluginScanner.DECRYPTERPATH.replace("/", ".");
        final ArrayList<DecrypterPluginInfo> ret = new ArrayList<DecrypterPluginInfo>();
        Class<?> clazz;
        for (final File f : files) {
            try {

                Log.L.finer("Loaded from: " + cl.getResource(DECRYPTERPATH + "/" + f.getName()));
                clazz = cl.loadClass(pkg + "." + f.getName().substring(0, f.getName().length() - 6));
                DecrypterPlugin a = clazz.getAnnotation(DecrypterPlugin.class);
                if (a != null) {

                    if (PluginForDecrypt.class.isAssignableFrom(clazz)) {
                        ret.add(new DecrypterPluginInfo(f, clazz, a));
                        System.out.println(clazz);
                    }
                } else {
                    Log.L.severe("@DecrypterPlugin missing for " + f);
                }
            } catch (final ClassNotFoundException e) {
                Log.exception(Level.WARNING, e);

            }
        }
        return ret;
    }

    private void writeHosterCache() throws MalformedURLException {

        ArrayList<HosterPluginInfo> hoster = PluginScanner.getInstance().getHoster();
        HosterCache hc = new HosterCache();
        for (HosterPluginInfo h : hoster) {
            String[] names, patterns;
            names = h.getAnnotation().names();
            patterns = h.getAnnotation().urls();
            if (names.length == 0) {
                try {
                    patterns = (String[]) h.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                    names = (String[]) h.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});

                } catch (Throwable e) {
                    Log.exception(e);
                }
            }
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    try {
                        CachedHosterInfo ch = new CachedHosterInfo(names[i], patterns[i], h.getAnnotation().revision(), Hash.getMD5(h.getFile()), h.getClazz().getName());

                        PluginForHost plg = (PluginForHost) h.getClazz().getConstructor(new Class[] { PluginWrapper.class }).newInstance(new HostPluginWrapper(names[i], h.getClazz().getSimpleName(), patterns[i], 0, h.getAnnotation().revision()));
                        ch.setHost(plg.getHost());
                        ch.setPremiumEnabled(plg.isPremiumEnabled());
                        ArrayList<MenuAction> mnu = plg.createMenuitems();
                        ch.setMenuItemsAvailable(mnu != null && mnu.size() > 0);
                        ch.setSettingsAvailable(plg.getConfig() != null && plg.getConfig().getEntries() != null && plg.getConfig().getEntries().size() > 0);

                        ch.setTosLink(plg.getAGBLink());
                        String purl = plg.getBuyPremiumUrl();
                        if (purl != null && purl.startsWith("http://jdownloader.org/r.php?u=")) {
                            purl = Encoding.urlDecode(purl.substring(31), false);
                        }

                        ch.setPremiumLink(purl);
                        hc.add(ch);

                    } catch (Throwable e) {
                        Log.exception(e);
                    }

                }
            }
            // h.getClazz().getAnnotation();

        }

        JSonStorage.saveTo(getHostCacheFile(), hc);
    }

    private static File getHostCacheFile() {
        return new File(Application.getResource(HOSTERPATH), "cache.json");
    }

    private static final PluginScanner INSTANCE   = new PluginScanner();
    private static final String        HOSTERPATH = "jd/plugins/hoster";

    /**
     * get the only existing instance of PluginScanner. This is a singleton
     * 
     * @return
     */
    public static PluginScanner getInstance() {
        return PluginScanner.INSTANCE;
    }

    /**
     * Create a new instance of PluginScanner. This is a singleton class. Access
     * the only existing instance by using {@link #getInstance()}.
     */
    private PluginScanner() {

    }

    @SuppressWarnings("unchecked")
    public ArrayList<HosterPluginInfo> getHoster() throws MalformedURLException {

        final File[] files = Application.getResource(HOSTERPATH).listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".class") && !name.contains("$");
            }
        });
        final URLClassLoader cl = new PluginClassLoader();
        final String pkg = PluginScanner.HOSTERPATH.replace("/", ".");
        final ArrayList<HosterPluginInfo> ret = new ArrayList<HosterPluginInfo>();
        Class<?> clazz;
        for (final File f : files) {
            try {

                Log.L.finer("Loaded from: " + cl.getResource(HOSTERPATH + "/" + f.getName()));
                clazz = cl.loadClass(pkg + "." + f.getName().substring(0, f.getName().length() - 6));
                HostPlugin a = clazz.getAnnotation(HostPlugin.class);
                if (a != null) {

                    if (PluginForHost.class.isAssignableFrom(clazz)) {
                        ret.add(new HosterPluginInfo(f, clazz, a));
                        System.out.println(clazz);
                    }
                } else {
                    Log.L.severe("@HostPlugin missing for " + f);
                }
            } catch (final ClassNotFoundException e) {
                Log.exception(Level.WARNING, e);

            }
        }
        return ret;
    }

    public void updateCache() {

        try {
            writeDecrypterCache();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            writeHosterCache();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

}
