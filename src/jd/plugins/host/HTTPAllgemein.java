//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.IOException;
import java.util.Vector;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.HTTPConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDMediaConvert;
import jd.utils.JDUtilities;

public class HTTPAllgemein extends PluginForHost {

    public static final String DISABLED = "HttpAllgemeinDisabled";

    private String contentType;
    public HTTPAllgemein(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    public String getFileInformationString(DownloadLink parameter) {
        return "(" + contentType + ")" + parameter.getName();
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException {
        String linkurl;
        downloadLink.setUrlDownload(linkurl = downloadLink.getDownloadURL().replaceAll("httpviajd://", "http://"));

        if (linkurl != null) {
            br.setFollowRedirects(true);

            HTTPConnection urlConnection;
            try {
                urlConnection = br.openGetConnection(linkurl);
                if (!urlConnection.isOK()) return false;
                downloadLink.setName(Plugin.getFileNameFormHeader(urlConnection));
                downloadLink.setBrowserUrl(linkurl);
                downloadLink.setDownloadSize(urlConnection.getContentLength());
                urlConnection.disconnect();
                this.contentType = urlConnection.getContentType();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    @Override
    public void handle(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        if(!downloadLink.getDownloadURL().toLowerCase().endsWith(".flv")){
            dl.startDownload();
        }else{
            //Es handelt sich um eine Flash Datei
            Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);
            ConversionMode convertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), downloadLink.getName());
            if (convertTo != null) {
                downloadLink.setFinalFileName(downloadLink.getName() + ".tmp");
                downloadLink.setSourcePluginComment("Convert to " + convertTo.GetText());
                if(dl.startDownload()){
                    ConversionMode inType = ConversionMode.VIDEOFLV;
                    if (!JDMediaConvert.ConvertFile(downloadLink, inType, convertTo)) {
                        logger.severe("Video-Convert failed!");
                    }
                }
            }
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {

    }
    private void setConfigElements() {
//        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DISABLED, JDLocale.L("plugins.host.HttpAllgemein.Disable", "Disable plugin")).setDefaultValue(false));
    }

}