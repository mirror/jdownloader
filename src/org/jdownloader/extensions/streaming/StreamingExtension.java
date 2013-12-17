package org.jdownloader.extensions.streaming;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;

import jd.SecondLevelLaunch;
import jd.nutils.Executer;
import jd.parser.Regex;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.gui.VLCGui;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.extensions.streaming.upnp.PlayToDevice;
import org.jdownloader.extensions.streaming.upnp.PlayToUpnpRendererDevice;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class StreamingExtension extends AbstractExtension<StreamingConfig, StreamingTranslation> implements MenuExtenderHandler {

    protected StreamingProvider streamProvider = null;
    static {
        // we need a testcommit 9
        // we need to load the profiles
        Profile.init();
    }

    public StreamingExtension() {

    }

    private HttpApiImpl                   vlcstreamingAPI;

    private volatile StreamingConfigPanel configPanel = null;
    protected VLCGui                      tab;
    private MediaServer                   mediaServer;

    public MediaServer getMediaServer() {
        return mediaServer;
    }

    private String                 wmpBinary;
    private String                 vlcBinary;
    private MediaArchiveController mediaArchive;
    private HttpHandlerInfo        streamServerHandler;

    @Override
    protected void stop() throws StopException {
        try {
            streamProvider = null;

            if (mediaServer != null) mediaServer.shutdown();

            if (streamServerHandler != null) {

                DeprecatedAPIHttpServerController.getInstance().unregisterRequestHandler(streamServerHandler);
            }
        } finally {

            vlcstreamingAPI = null;
        }
    }

    public StreamingProvider getStreamProvider() {
        return streamProvider;
    }

    @Override
    protected void start() throws StartException {

        mediaArchive = new MediaArchiveController(this);
        streamProvider = new StreamingProvider(this);
        tab = new VLCGui(this);
        vlcBinary = findVLCBinary();
        wmpBinary = findWindowsMediaPlayer();

        startMediaServer();

        vlcstreamingAPI = new HttpApiImpl(this, this.mediaServer);

        try {
            streamServerHandler = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(getSettings().getStreamServerPort(), false, vlcstreamingAPI);
        } catch (IOException e) {
            e.printStackTrace();
            throw new StartException(e);
        }

        // new EDTRunner() {
        //
        // @Override
        // protected void runInEDT() {
        // tab = new VLCGui(StreamingExtension.this);
        // }
        // }.waitForEDT();
    }

    private String findVLCBinary() {
        String ret = getSettings().getVLCCommand();
        if (!StringUtils.isEmpty(ret)) return ret;
        if (CrossSystem.isWindows()) {
            return getVLCBinaryFromWindowsRegistry();
        } else if (CrossSystem.isMac()) {
            if (new File("/Applications/VLC.app/Contents/MacOS/VLC").exists()) { return "/Applications/VLC.app/Contents/MacOS/VLC"; }
        } else if (CrossSystem.isLinux()) { return "vlc"; }
        return null;

    }

    private String findWindowsMediaPlayer() {
        if (CrossSystem.isWindows()) {
            String path86 = System.getenv("ProgramFiles(x86)");
            String path = System.getenv("ProgramFiles");
            if (path86 != null) {
                File file = new File(new File(path86), "Windows Media Player/wmplayer.exe");
                if (file.exists()) return file.getAbsolutePath();
            }

            if (path != null) {
                File file = new File(new File(path), "Windows Media Player/wmplayer.exe");
                if (file.exists()) return file.getAbsolutePath();
            }

        }
        return null;
    }

    private void startMediaServer() {

        Thread serverThread = new Thread(mediaServer = new MediaServer(this));
        serverThread.setDaemon(false);
        serverThread.start();
    }

    public HttpApiImpl getVlcstreamingAPI() {
        return vlcstreamingAPI;
    }

    protected String getVLCRevision(String binary) {
        Executer exec = new Executer(binary);
        exec.setLogger(LogController.CL());
        exec.addParameters(new String[] { "--version" });
        exec.setRunin(Application.getRoot(SecondLevelLaunch.class));
        exec.setWaitTimeout(5);
        exec.start();
        exec.waitTimeout();
        String response = exec.getOutputStream();
        return new Regex(response, "VLC.*?\\((.*?)\\)").getMatch(0);
    }

    @Override
    protected void initExtension() throws StartException {

        MenuManagerDownloadTableContext.getInstance().registerExtender(this);
        MenuManagerLinkgrabberTableContext.getInstance().registerExtender(this);
    }

    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        if (configPanel != null) return configPanel;
        synchronized (this) {
            if (configPanel != null) return configPanel;
            configPanel = new StreamingConfigPanel(this, getSettings());
        }
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Audio & Video Streaming";
    }

    @Override
    public VLCGui getGUI() {
        return tab;
    }

    public String getVLCBinary() {
        return vlcBinary;
    }

    //
    // @Override
    // public void onExtendPopupMenu(final MenuContext<?> context) {
    // if (context instanceof DownloadTableContext) {
    //
    // final JMenu menu = new JMenu(T._.popup_streaming()) {
    // /**
    // *
    // */
    // private static final long serialVersionUID = -5156294142768994122L;
    //
    // protected JMenuItem createActionComponent(Action a) {
    // if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
    // return super.createActionComponent(a);
    // }
    // };
    //
    // // menu.setEnabled(false);
    // ((DownloadTableContext) context).getMenu().add(menu);
    // menu.setIcon(getIcon(18));
    // menu.setEnabled(((DownloadTableContext) context).getSelectionInfo().isLinkContext());
    // if (((DownloadTableContext) context).getSelectionInfo().isLinkContext()) {
    // onExtendDownloadTableLinkContext(context, menu);
    //
    // } else {
    // onExtendDownloadTablePackageContext(context, menu);
    // }
    //
    // } else if (context instanceof LinkgrabberTableContext) {
    // ((LinkgrabberTableContext) context).getMenu().add(new JMenuItem(new AddToLibraryAction(this, ((LinkgrabberTableContext)
    // context).getSelectionInfo())), null, 1);
    //
    // }
    // }

    // private void onExtendDownloadTablePackageContext(MenuContext<?> context, JMenu menu) {
    // }

    private static final HashSet<String> SUPPORTED_DIRECT_PLAY_FORMATS = new HashSet<String>();
    static {
        SUPPORTED_DIRECT_PLAY_FORMATS.add("mkv");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("mov");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("avi");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("mp4");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("flac");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("flv");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("flac");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("mp3");
        SUPPORTED_DIRECT_PLAY_FORMATS.add("wav");
    }

    // private void onExtendDownloadTableLinkContext(final MenuContext<?> context, final JMenu menu) {
    // DownloadLink link = ((DownloadTableContext) context).getSelectionInfo().getContextLink();
    // String filename = link.getName().toLowerCase(Locale.ENGLISH);
    //
    // final List<PlayToDevice> renderer = getPlayToRenderer();
    //
    // if (SUPPORTED_DIRECT_PLAY_FORMATS.contains(Files.getExtension(filename))) {
    //
    // for (PlayToDevice d : renderer) {
    // menu.add(new DirectPlayToAction(this, d, link));
    //
    // }
    // } else {
    //
    // // Rar
    // new Thread("MenuCreator") {
    // public void run() {
    // try {
    // LazyExtension plg = ExtensionController.getInstance().getExtension(ExtractionExtension.class);
    // if (plg._isEnabled()) {
    // final ExtractionExtension extractor = (ExtractionExtension) plg._getExtension();
    //
    // final ValidateArchiveAction<FilePackage, DownloadLink> validation = new ValidateArchiveAction<FilePackage, DownloadLink>(extractor,
    // ((DownloadTableContext) context).getSelectionInfo());
    // final Archive archive = validation.getArchives().get(0);
    // java.util.List<ArchiveItem> ais = archive.getSettings().getArchiveItems();
    //
    // if (ais != null && ais.size() > 0) {
    //
    // for (final ArchiveItem ai : ais) {
    // if (SUPPORTED_DIRECT_PLAY_FORMATS.contains(Files.getExtension(ai.getPath()))) {
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    //
    // for (PlayToDevice d : renderer) {
    //
    // menu.add(new RarPlayToAction(StreamingExtension.this, d, archive, extractor, ai));
    //
    // }
    //
    // }
    // };
    // }
    //
    // }
    // } else {
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    //
    // for (PlayToDevice d : renderer) {
    //
    // menu.add(new RarPlayToAction(StreamingExtension.this, d, archive, extractor, null));
    //
    // }
    //
    // }
    // };
    //
    // }
    // }
    // } catch (Throwable e) {
    // logger.log(e);
    // }
    //
    // }
    // }.start();
    //
    // }
    //
    // }

    private List<PlayToDevice> getPlayToRenderer() {
        java.util.List<PlayToDevice> ret = new ArrayList<PlayToDevice>();
        for (PlayToUpnpRendererDevice mr : mediaServer.getPlayToRenderer()) {
            ret.add(mr);
        }
        if (!StringUtils.isEmpty(getVLCBinary())) {
            ret.add(new PlayToVLCDevice(this, getVLCBinary()));
        }
        if (!StringUtils.isEmpty(getWMPBinary())) {
            ret.add(new PlayToWMPDevice(this, getWMPBinary()));
        }

        return ret;
    }

    private String getWMPBinary() {

        return wmpBinary;
    }

    private String getVLCBinaryFromWindowsRegistry() {
        // Retrieve a reference to the root of the user preferences tree
        String ret = getCommand(Preferences.userRoot());
        if (ret == null) {
            ret = getCommand(Preferences.systemRoot());
        }
        return ret;
    }

    private String getCommand(Preferences systemRoot) {
        Method closeKey = null;
        int hKey = -1;
        try {
            final Class clz = systemRoot.getClass();
            final int KEY_READ = 0x20019;
            Class[] params1 = { byte[].class, int.class, int.class };
            final Method openKey = clz.getDeclaredMethod("openKey", params1);
            openKey.setAccessible(true);
            Class[] params2 = { int.class };
            closeKey = clz.getDeclaredMethod("closeKey", params2);
            closeKey.setAccessible(true);
            final Method winRegQueryValue = clz.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
            winRegQueryValue.setAccessible(true);
            String key = "SOFTWARE\\Classes\\Applications\\vlc.exe\\shell\\Open\\command";
            hKey = (Integer) openKey.invoke(systemRoot, toByteEncodedString(key), KEY_READ, KEY_READ);
            byte[] valb = (byte[]) winRegQueryValue.invoke(systemRoot, hKey, toByteEncodedString(""));
            String vals = (valb != null ? new String(valb).trim() : null);
            return new Regex(vals, "\"(.*?\\.exe)\"").getMatch(0);
        } catch (Throwable e) {
            LogController.CL().log(e);
            return null;
        } finally {
            try {
                if (hKey != -1) closeKey.invoke(Preferences.userRoot(), hKey);
            } catch (final Throwable e) {
            }
        }
    }

    private static byte[] toByteEncodedString(String str) {
        byte[] result = new byte[str.length() + 1];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }

    public String getHost() {
        try {
            return mediaServer.getHost();
        } catch (Throwable e) {
            logger.log(e);
        }
        return "127.0.0.1";
    }

    public String createStreamUrl(String id, String deviceID, String format, String subpath) {

        try {
            if (StringUtils.isEmpty(deviceID)) {
                deviceID = "UnknownDevice";
            }
            String ret = "http://" + getHost() + ":" + getSettings().getStreamServerPort() + "/stream/" + encode(deviceID) + "/" + encode(id) + "/" + encode(format);
            if (!StringUtils.isEmpty(subpath)) ret += "/" + encode(subpath);
            return ret;
        } catch (Throwable e) {
            logger.log(e);
            throw new WTFException(e);
        }

    }

    private String encode(String string) {
        if (StringUtils.isEmpty(string)) return null;
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MediaArchiveController getMediaArchiveController() {
        return mediaArchive;
    }

    public ExtractionExtension getExtractingExtension() {
        return (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();

    }

    private HashMap<String, MediaItem> linkIdMap = new HashMap<String, MediaItem>();

    public void linkMediaItem(String id, MediaItem mediaItem) {
        linkIdMap.put(id, mediaItem);
    }

    public void unlinkMediaItem(String id) {
        linkIdMap.remove(id);
    }

    public DownloadLink getLinkById(String id) {

        MediaItem mi = getItemById(id);
        if (mi != null) { return mi.getDownloadLink(); }
        return null;
    }

    public MediaItem getItemById(String id) {

        MediaItem mi = linkIdMap.get(id);
        if (mi != null) return mi;

        try {
            mi = ((MediaItem) mediaArchive.getItemById(id));
            if (mi != null) return mi;
        } catch (Throwable e) {

        }
        return null;
    }

    @Override
    public String getIconKey() {
        return "video";
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        return null;
    }

    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon(getIconKey(), i);
    }

}
