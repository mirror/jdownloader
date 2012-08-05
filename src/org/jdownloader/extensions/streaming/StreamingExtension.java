package org.jdownloader.extensions.streaming;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.Launcher;
import jd.nutils.Executer;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.streaming.gui.VLCGui;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.menu.eventsender.MenuFactoryListener;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class StreamingExtension extends AbstractExtension<StreamingConfig, StreamingTranslation> implements MenuFactoryListener {

    private LogSource           logger;
    protected StreamingProvider streamProvider = null;

    public StreamingExtension() {
        logger = LogController.getInstance().getLogger("Streaming");
        System.out.println(1);
    }

    private HttpApiImpl          vlcstreamingAPI;
    private String               vlcBinary   = null;
    private StreamingConfigPanel configPanel = null;
    protected VLCGui             tab;
    private MediaServer          mediaServer;

    @Override
    protected void stop() throws StopException {
        try {
            streamProvider = null;

            if (mediaServer != null) mediaServer.shutdown();
            MenuFactoryEventSender.getInstance().removeListener(this);
            if (vlcstreamingAPI != null) {
                RemoteAPIController.getInstance().unregister(vlcstreamingAPI);
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
        if (vlcBinary == null) {
            vlcBinary = getVLCBinary();
            if (StringUtils.isEmpty(vlcBinary)) { throw new StartException("Could not find vlc binary"); }
        }
        streamProvider = new StreamingProvider(this);
        vlcstreamingAPI = new HttpApiImpl(this);

        startMediaServer();
        RemoteAPIController.getInstance().register(vlcstreamingAPI);
        MenuFactoryEventSender.getInstance().addListener(this, true);
        // new EDTRunner() {
        //
        // @Override
        // protected void runInEDT() {
        // tab = new VLCGui(StreamingExtension.this);
        // }
        // }.waitForEDT();
    }

    private void startMediaServer() {

        Thread serverThread = new Thread(mediaServer = new MediaServer());
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
        exec.setRunin(Application.getRoot(Launcher.class));
        exec.setWaitTimeout(5);
        exec.start();
        exec.waitTimeout();
        String response = exec.getOutputStream();
        return new Regex(response, "VLC.*?\\((.*?)\\)").getMatch(0);
    }

    @Override
    protected void initExtension() throws StartException {
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
    public ImageIcon getIcon(int size) {
        return NewTheme.I().getIcon("video", size);
    }

    @Override
    public VLCGui getGUI() {
        return tab;
    }

    public String getVLCBinary() {
        String ret = getSettings().getVLCCommand();
        if (!StringUtils.isEmpty(ret)) return ret;
        if (CrossSystem.isWindows()) {
            return getVLCBinaryFromWindowsRegistry();
        } else if (CrossSystem.isMac()) {
            return "/Applications/VLC.app/Contents/MacOS/VLC";
        } else if (CrossSystem.isLinux()) {
            return "vlc";
        } else {
            return null;
        }
    }

    @Override
    public void onExtendPopupMenu(final MenuContext<?> context) {
        if (context instanceof DownloadTableContext) {

            final JMenu menu = new JMenu(T._.popup_streaming()) {
                /**
                 * 
                 */
                private static final long serialVersionUID = -5156294142768994122L;

                protected JMenuItem createActionComponent(Action a) {
                    if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                    return super.createActionComponent(a);
                }
            };

            // menu.setEnabled(false);
            ((DownloadTableContext) context).getMenu().add(menu);
            menu.setIcon(getIcon(18));
            menu.setEnabled(((DownloadTableContext) context).getSelectionInfo().isLinkContext());
            menu.add(new AppAction() {
                DownloadLink              link             = null;

                private static final long serialVersionUID = 1375146705091555054L;

                {
                    setName(T._.popup_streaming_playvlc());
                    Image front = NewTheme.I().getImage("media-playback-start", 20, true);
                    setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));
                    if (((DownloadTableContext) context).getSelectionInfo().isLinkContext()) link = ((DownloadTableContext) context).getSelectionInfo().getContextLink();
                    setEnabled(isDirectPlaySupported(link));
                }

                public boolean isDirectPlaySupported(DownloadLink link) {
                    if (link == null) return false;
                    String filename = link.getName().toLowerCase(Locale.ENGLISH);
                    if (filename.endsWith("mkv")) return true;
                    if (filename.endsWith("mov")) return true;
                    if (filename.endsWith("avi")) return true;
                    if (filename.endsWith("mp4")) return true;
                    if (filename.endsWith("flac")) return true;
                    if (filename.endsWith("mp3")) return true;
                    if (filename.endsWith("wav")) return true;
                    return false;
                }

                public void actionPerformed(ActionEvent e) {
                    new Thread() {
                        public void run() {
                            try {
                                String ID = link.getUniqueID().toString();
                                StreamingInterface streamingInterface = getVlcstreamingAPI().getStreamingInterface(ID);
                                if (streamingInterface == null) {
                                    getVlcstreamingAPI().addHandler(ID, new DirectStreamingImpl(StreamingExtension.this, link));
                                }
                                String[] params = new String[] { "http://127.0.0.1:" + RemoteAPIController.getInstance().getApiPort() + "/vlcstreaming/play?id=" + ID };
                                if (false) {
                                    System.out.println(params[0]);
                                    return;
                                }
                                Executer exec = new Executer(getVLCBinary());
                                exec.setLogger(LogController.CL());
                                exec.addParameters(params);
                                exec.setRunin(Application.getRoot(Launcher.class));
                                exec.setWaitTimeout(0);
                                exec.start();
                            } catch (final Throwable e) {
                                logger.log(e);
                            }
                        };
                    }.start();

                }
            });

            // Rar
            new Thread("MenuCreator") {
                public void run() {
                    try {
                        LazyExtension plg = ExtensionController.getInstance().getExtension(ExtractionExtension.class);
                        if (plg._isEnabled()) {
                            final ExtractionExtension extractor = (ExtractionExtension) plg._getExtension();

                            final ValidateArchiveAction<FilePackage, DownloadLink> validation = new ValidateArchiveAction<FilePackage, DownloadLink>(extractor, ((DownloadTableContext) context).getSelectionInfo());
                            final Archive archive = validation.getArchives().get(0);
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    menu.add(new RarStreamAction(archive, extractor, StreamingExtension.this));
                                }
                            };
                        }
                    } catch (Throwable e) {
                        logger.log(e);
                    }

                }
            }.start();

        }
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
}
