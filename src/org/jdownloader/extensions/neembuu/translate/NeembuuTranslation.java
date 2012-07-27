package org.jdownloader.extensions.neembuu.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface NeembuuTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Neembuu" })
    String title();

    @Default(lngs = { "en" }, values = { "Stream Data - Watch as you download" })
    String description();

    @Default(lngs = { "en" }, values = { "Neembuu" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Stream Data - Watch as you download" })
    String gui_tooltip();

    @Default(lngs = { "en" }, values = { "Virtual filesystem unchecked" })
    String vfsUnchecked();

    @Default(lngs = { "en" }, values = { "Virtual filesystem not working" })
    String vfsNotWorking();

    @Default(lngs = { "en" }, values = { "Virtual filesystem works" })
    String vfsWorking();

    @Default(lngs = { "en" }, values = { "Settings" })
    String settingsPanelTitle();

    @Default(lngs = { "en" }, values = { "Check virtual filesystem capability" })
    String checkVFSButton();

    @Default(lngs = { "en" }, values = { "Basic Mount Location" })
    String basicMountLocation();

    @Default(lngs = { "en" }, values = { "Browse" })
    String browse();

    @Default(lngs = { "en" }, values = { "Open basic mount location folder" })
    String openBasicMountLocation();

    @Default(lngs = { "en" }, values = { "Mount location" })
    String mountLocation();

    @Default(lngs = { "en" }, values = { "Open mount location" })
    String openMountLocation();

    @Default(lngs = { "en" }, values = { "Are you sure you want to unmount?" })
    String unmountConfirm();

    @Default(lngs = { "en" }, values = { "Neembuu could not handle this link" })
    String neembuu_could_not_handle_title();

    @Default(lngs = { "en" }, values = { "Do you want to download this file instead ?" })
    String neembuu_could_not_handle_message();

    @Default(lngs = { "en" }, values = { "Neembuu watch as you download failed." })
    String failed_WatchAsYouDownload_Title();

    @Default(lngs = { "en" }, values = { "Could not open mount location" })
    String couldNotOpenMountLocation();

    @Default(lngs = { "en" }, values = { "Neembuu watch as you download is powered by <b>Pismo File Mount</b>" })
    String poweredByPismo();

    @Default(lngs = { "en" }, values = { "Neembuu Watch as you download works best with vlc" })
    String worksBestWithVlc();

    @Default(lngs = { "en" }, values = { "You can set vlc\'s location (this is not compulsary) so that vlc may be used to play videos while they are downloaded." })
    String vlcPathOption();

    @Default(lngs = { "en" }, values = { "Link" })
    String filePanel_link();

    @Default(lngs = { "en" }, values = { "FileSize" })
    String filePanel_linkFileSize();

    @Default(lngs = { "en" }, values = { "FileName" })
    String filePanel_linkFileName();

    @Default(lngs = { "en" }, values = { "Region Downloaded" })
    String filePanel_regionDownloadedTitle();

    @Default(lngs = { "en" }, values = { "Region Requested" })
    String filePanel_regionRequestedTitle();

    @Default(lngs = { "en" }, values = { "Download entire file" })
    String filePanel_autoCompleteButtonEnabled();

    @Default(lngs = { "en" }, values = { "Download as less as possible" })
    String filePanel_autoCompleteButtonDisabled();

    @Default(lngs = { "en" }, values = { "Open File" })
    String filePanel_openFile();

    @Default(lngs = { "en" }, values = { "Advanced Controls" })
    String filePanel_advancedViewButton();

    @Default(lngs = { "en" }, values = { "Total Download Speed" })
    String filePanel_totalDownloadSpeed();

    @Default(lngs = { "en" }, values = { "Total Request Rate" })
    String filePanel_totalRequestRate();

    @Default(lngs = { "en" }, values = { "Select Next Downloaded Region" })
    String filePanel_selectNextRegionButton();

    @Default(lngs = { "en" }, values = { "Previous Region" })
    String filePanel_selectPreviousRegionButton();

    @Default(lngs = { "en" }, values = { "Kill Connection" })
    String filePanel_killConnection();

    @Default(lngs = { "en" }, values = { "<html>Click on a <b>downloaded region</b> to activate speed chart</html>" })
    String filePanel_graph_noRegionSelected();

    @Default(lngs = { "en" }, values = { "Download Speed" })
    String filePanel_graph_downloadSpeed();

    @Default(lngs = { "en" }, values = { "Request speed" })
    String filePanel_graph_requestSpeed();

    @Default(lngs = { "en" }, values = { "<html>Speed (KiB/s)</html>" })
    String filePanel_graph_yaxis();

    @Default(lngs = { "en" }, values = { "Cannot create a new connection, unmounting as we already retried " })
    String troubleHandler_retriedConnection();

    @Default(lngs = { "en" }, values = { " times" })
    String troubleHandler_retriedConnection_times();

    @Default(lngs = { "en" }, values = { "Cannot create a new connection, unmounting as we already retried " })
    String troubleHandler_pendingRequests();

    @Default(lngs = { "en" }, values = { " minute(s)" })
    String troubleHandler_pendingRequests_minutes();

    @Default(lngs = { "en" }, values = { "Try watching the file after completely downloading it.\n\"Watch as you download\" is difficult on this file.\nUnmounting to prevent Not Responding state of the application used to open this file." })
    String troubleHandler_pendingRequests_Solution();

}
