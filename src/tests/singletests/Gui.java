//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package tests.singletests;

import static org.junit.Assert.assertTrue;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.SwingGui;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;

import org.fest.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.Test;

import tests.utils.TestUtils;

public class Gui {
    private static FrameFixture frame;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        TestUtils.initJD();

        Thread.sleep(2000);

        frame = new FrameFixture(SwingGui.getInstance().getMainFrame());
    }

    @Test
    public void addDownloads() throws InterruptedException {
        String url = "http://rapidshare.com/files/248009194/JDownloader_0.6.193.zip";
        new DistributeData(url, false).start();
        Thread.sleep(5000);
        int links = 0;
        for (DownloadLink dl : DownloadController.getInstance().getAllDownloadLinks()) {
            if (dl.getLinkStatus().hasStatus(LinkStatus.TODO)) links++;
        }
        frame.button("addAllPackages").click();

        Thread.sleep(5000);
        for (DownloadLink dl : DownloadController.getInstance().getAllDownloadLinks()) {
            if (dl.getLinkStatus().hasStatus(LinkStatus.TODO)) links--;
        }
        assertTrue("Adding link failed", links < 0);
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

}