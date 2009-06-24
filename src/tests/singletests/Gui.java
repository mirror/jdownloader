package tests.singletests;

import static org.junit.Assert.assertTrue;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.gui.skins.simple.SimpleGUI;

import org.fest.swing.fixture.FrameFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import tests.utils.TestUtils;

public class Gui {
    private static FrameFixture frame;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        TestUtils.initJD();

        Thread.sleep(2000);

        frame = new FrameFixture(SimpleGUI.CURRENTGUI);
    }

    @Test
    public void startDownloads() throws InterruptedException {
        frame.button("playButton").click();

        Thread.sleep(5000);

        assertTrue(DownloadWatchDog.getInstance().getActiveDownloads() > 0);
    }

    @Test
    public void pauseDownloads() throws InterruptedException {
        frame.toggleButton("pauseButton").click();

        Thread.sleep(3000);

        assertTrue(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) == SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10));
    }

    @Test
    public void unpauseDownloads() throws InterruptedException {
        frame.toggleButton("pauseButton").click();

        Thread.sleep(3000);

        assertTrue(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) != SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10));
    }

    @Test
    public void stopDownloads() throws InterruptedException {
        frame.button("stopButton").click();

        Thread.sleep(3000);

        assertTrue(DownloadWatchDog.getInstance().getActiveDownloads() == 0);
    }

    @AfterClass
    public static void tearDown() {
        // JDUtilities.getController().exit();
    }
}