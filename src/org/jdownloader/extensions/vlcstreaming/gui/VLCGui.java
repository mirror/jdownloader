package org.jdownloader.extensions.vlcstreaming.gui;

import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.vlcstreaming.VLCStreamingExtension;
import org.jdownloader.logging.LogController;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.player.AudioOutput;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.windows.WindowsCanvas;
import uk.co.caprica.vlcj.runtime.windows.WindowsRuntimeUtil;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class VLCGui extends AddonPanel<VLCStreamingExtension> implements MouseListener {

    private static final String ID = "VLCGUI";
    private SwitchPanel         panel;

    private LogSource           logger;

    public VLCGui(VLCStreamingExtension plg) {
        super(plg);
        logger = LogController.getInstance().getLogger("VLCGUI");
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            @Override
            protected void onShow() {

            }

            @Override
            protected void onHide() {
            }
        };
        // layout all contents in panel
        this.setContent(panel);

        layoutPanel();

    }

    private Canvas              videoSurface;
    private MediaPlayerFactory  mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;

    private void layoutPanel() {

        try {
            System.out.println(RuntimeUtil.getLibVlcLibraryName());
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Users\\Thomas\\Downloads\\vlc-2.0.2-win64\\vlc-2.0.2");
            Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        LibVlc libVlc = LibVlcFactory.factory().create();
        //
        logger.info("  version: " + libVlc.libvlc_get_version());
        logger.info(" compiler: " + libVlc.libvlc_get_compiler());
        logger.info("changeset: " + libVlc.libvlc_get_changeset());

        if (CrossSystem.isWindows()) {
            videoSurface = new WindowsCanvas();
        } else {
            videoSurface = new Canvas();
        }
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        videoSurface.addMouseListener(this);
        List<String> vlcArgs = new ArrayList<String>();

        // vlcArgs.add("--ffmpeg-hw"); // <--- if your system supports it, this might be useful
        vlcArgs.add("--no-plugins-cache");
        vlcArgs.add("--no-video-title-show");
        vlcArgs.add("--no-snapshot-preview");
        vlcArgs.add("--quiet");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--intf");
        vlcArgs.add("dummy");

        System.out.println(WindowsRuntimeUtil.getVlcInstallDir());
        // Special case to help out users on Windows (supposedly this is not actually needed)...
        // if(RuntimeUtil.isWindows()) {
        // vlcArgs.add("--plugin-path=" + WindowsRuntimeUtil.getVlcInstallDir() + "\\plugins");
        // }
        // else {
        // vlcArgs.add("--plugin-path=/home/linux/vlc/lib");
        // }

        // vlcArgs.add("--plugin-path=" + System.getProperty("user.home") + "/.vlcj");

        logger.info("vlcArgs=" + vlcArgs);

        FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(JDGui.getInstance().getMainFrame());

        mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
        mediaPlayerFactory.setUserAgent("vlcj test player");
        List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
        logger.info("audioOutputs=" + audioOutputs);

        mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
        mediaPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(videoSurface));
        mediaPlayer.setPlaySubItems(true);

        // mediaPlayer.setEnableKeyInputHandling(false);
        // mediaPlayer.setEnableMouseInputHandling(false);

        // controlsPanel = new PlayerControlsPanel(mediaPlayer);
        // videoAdjustPanel = new PlayerVideoAdjustPanel(mediaPlayer);
        //

        panel.add(videoSurface);
        // mainFrame.add(controlsPanel, BorderLayout.SOUTH);
        // mainFrame.add(videoAdjustPanel, BorderLayout.EAST)

        panel.add(new ExtButton(new AppAction() {
            {
                setName("Play");
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                mediaPlayer.toggleFullScreen();

            }
        }));
    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
    }

    /**
     * is called, if the gui has been opened.
     */
    @Override
    protected void onActivated() {
        Log.L.finer("onActivated " + getClass().getSimpleName());
    }

    @Override
    public Icon getIcon() {
        return this.getExtension().getIcon(16);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getTitle() {
        return getExtension()._.gui_title();
    }

    @Override
    public String getTooltip() {
        return getExtension()._.gui_tooltip();
    }

    /**
     * Is called if gui is visible now, and has not been visible before. For example, user starte the extension, opened the view, or
     * switched form a different tab to this one
     */
    @Override
    protected void onShow() {
        Log.L.finer("Shown " + getClass().getSimpleName());
    }

    /**
     * gets called of the extensiongui is not visible any more. for example because it has been closed or user switched to a different
     * tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public void playMedia(String string) {
        mediaPlayer.playMedia(string);

        JDGui.getInstance().getMainTabbedPane().setSelectedComponent(this);
    }

}
