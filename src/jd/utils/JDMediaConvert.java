//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.utils;

import java.io.File;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;
import de.savemytube.flv.FLV;

public class JDMediaConvert {

    private static final Logger LOG = JDLogger.getLogger();

    private static final String TEMP_EXT = ".tmp$";

    public static boolean ConvertFile(final DownloadLink downloadlink, final ConversionMode InType, final ConversionMode OutType) {
        LOG.info("Convert " + downloadlink.getName() + " - " + InType.getText() + " - " + OutType.getText());
        if (InType.equals(OutType)) {
            LOG.info("No Conversion needed, renaming...");
            final File oldone = new File(downloadlink.getFileOutput());
            final File newone = new File(downloadlink.getFileOutput().replaceAll(TEMP_EXT, OutType.getExtFirst()));
            downloadlink.setFinalFileName(downloadlink.getName().replaceAll(TEMP_EXT, OutType.getExtFirst()));
            oldone.renameTo(newone);
            return true;
        }

        final ProgressController progress = new ProgressController(JDL.L("convert.progress.convertingto", "Konvertiere zu") + " " + OutType.toString(), 3, null);
        downloadlink.getLinkStatus().setStatusText(JDL.L("convert.progress.convertingto", "Konvertiere zu") + " " + OutType.toString());
        progress.increase(1);
        switch (InType) {
        case VIDEOFLV:
            // Inputformat FLV
            switch (OutType) {
            case AUDIOMP3:
                LOG.info("Convert FLV to mp3...");
                new FLV(downloadlink.getFileOutput(), true, false);
                progress.increase(1);
                // FLV löschen
                if (!new File(downloadlink.getFileOutput()).delete()) {
                    new File(downloadlink.getFileOutput()).deleteOnExit();
                }
                progress.doFinalize();
                return true;
            case AUDIOMP3_AND_VIDEOFLV:
                LOG.info("Convert FLV to mp3 (keep FLV)...");
                new FLV(downloadlink.getFileOutput(), true, false);
                progress.increase(1);
                // Rename tmp to flv
                new File(downloadlink.getFileOutput()).renameTo(new File(downloadlink.getFileOutput().replaceAll(".tmp", ConversionMode.VIDEOFLV.getExtFirst())));
                progress.doFinalize();
                return true;
            default:
                LOG.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
                downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unbekanntes Format"));
                progress.doFinalize();
                return false;
            }
        default:
            LOG.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
            downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unbekanntes Format"));
            progress.doFinalize();
            return false;
        }
    }
}