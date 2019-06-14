package org.jdownloader.api.downloads.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.PluginProgress;
import jd.plugins.PluginStateCollection;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.DomainInfo;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.api.utils.SelectionInfoUtils;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions;
import org.jdownloader.myjdownloader.client.bindings.PriorityStorable;
import org.jdownloader.myjdownloader.client.bindings.SkipReasonStorable;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsListInterface;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.UrlDisplayType;

public class DownloadsAPIV2Impl implements DownloadsAPIV2 {
    private final PackageControllerUtils<FilePackage, DownloadLink> packageControllerUtils;

    public DownloadsAPIV2Impl() {
        RemoteAPIController.validateInterfaces(DownloadsAPIV2.class, DownloadsListInterface.class);
        packageControllerUtils = new PackageControllerUtils<FilePackage, DownloadLink>(DownloadController.getInstance());
    }

    @Override
    public List<FilePackageAPIStorableV2> queryPackages(PackageQueryStorable queryParams) throws BadParameterException {
        DownloadController dlc = DownloadController.getInstance();
        // filter out packages, if specific packageUUIDs given, else return all packages
        List<FilePackage> packages = null;
        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {
            packages = packageControllerUtils.getPackages(queryParams.getPackageUUIDs());
        } else {
            packages = dlc.getPackagesCopy();
        }
        List<FilePackageAPIStorableV2> ret = new ArrayList<FilePackageAPIStorableV2>(packages.size());
        if (packages.size() == 0) {
            return ret;
        }
        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();
        if (startWith > packages.size() - 1) {
            return ret;
        }
        if (startWith < 0) {
            startWith = 0;
        }
        if (maxResults < 0) {
            maxResults = packages.size();
        }
        for (int i = startWith; i < Math.min(startWith + maxResults, packages.size()); i++) {
            FilePackage fp = packages.get(i);
            boolean readL = fp.getModifyLock().readLock();
            try {
                FilePackageAPIStorableV2 fps = toStorable(queryParams, fp, this);
                ret.add(fps);
            } finally {
                fp.getModifyLock().readUnlock(readL);
            }
        }
        return ret;
    }

    public static FilePackageAPIStorableV2 setStatus(FilePackageAPIStorableV2 fps, FilePackageView fpView) {
        PluginStateCollection ps = fpView.getPluginStates();
        if (ps.size() > 0) {
            fps.setStatusIconKey(RemoteAPIController.getInstance().getContentAPI().getIconKey(ps.getMergedIcon()));
            fps.setStatus(ps.isMultiline() ? "" : ps.getText());
            return fps;
        }
        if (fpView.isFinished()) {
            fps.setStatusIconKey(IconKey.ICON_TRUE);
            fps.setStatus(_GUI.T.TaskColumn_getStringValue_finished_());
            return fps;
        } else if (fpView.getETA() != -1) {
            fps.setStatus(_GUI.T.TaskColumn_getStringValue_running_());
            return fps;
        }
        return fps;
    }

    @Override
    public List<DownloadLinkAPIStorableV2> queryLinks(LinkQueryStorable queryParams) {
        List<DownloadLinkAPIStorableV2> result = new ArrayList<DownloadLinkAPIStorableV2>();
        DownloadController dlc = DownloadController.getInstance();
        final List<FilePackage> packages;
        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {
            packages = packageControllerUtils.getPackages(queryParams.getPackageUUIDs());
        } else {
            packages = dlc.getPackagesCopy();
        }
        final List<DownloadLink> links = new ArrayList<DownloadLink>();
        if (queryParams.getJobUUIDs() != null && queryParams.getJobUUIDs().length > 0) {
            final Set<Long> jobUUIDs = new HashSet<Long>();
            for (final long id : queryParams.getJobUUIDs()) {
                jobUUIDs.add(id);
            }
            for (FilePackage pkg : packages) {
                final boolean readL = pkg.getModifyLock().readLock();
                try {
                    for (DownloadLink link : pkg.getChildren()) {
                        if (jobUUIDs.contains(link.getJobID())) {
                            links.add(link);
                        }
                    }
                } finally {
                    pkg.getModifyLock().readUnlock(readL);
                }
            }
        } else {
            // collect children of the selected packages and convert to storables for response
            for (FilePackage pkg : packages) {
                final boolean b = pkg.getModifyLock().readLock();
                try {
                    links.addAll(pkg.getChildren());
                } finally {
                    pkg.getModifyLock().readUnlock(b);
                }
            }
        }
        if (links.isEmpty()) {
            return result;
        }
        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();
        if (startWith > links.size() - 1) {
            return result;
        }
        if (startWith < 0) {
            startWith = 0;
        }
        if (maxResults < 0) {
            maxResults = links.size();
        }
        int until = Math.min(startWith + maxResults, links.size());
        for (int i = startWith; i < until; i++) {
            final DownloadLink dl = links.get(i);
            final DownloadLinkAPIStorableV2 dls = toStorable(queryParams, dl, this);
            result.add(dls);
        }
        return result;
    }

    public static FilePackageAPIStorableV2 toStorable(PackageQueryStorable queryParams, FilePackage fp, Object caller) {
        final DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        final FilePackageAPIStorableV2 fps = new FilePackageAPIStorableV2(fp);
        final FilePackageView fpView = new FilePackageView(fp);
        fpView.aggregate();
        if (queryParams.isPriority()) {
            fps.setPriority(org.jdownloader.myjdownloader.client.bindings.PriorityStorable.get(fp.getPriorityEnum().name()));
        }
        if (queryParams.isSaveTo()) {
            fps.setSaveTo(fpView.getDownloadDirectory());
        }
        if (queryParams.isBytesTotal()) {
            fps.setBytesTotal(fpView.getSize());
        }
        if (queryParams.isChildCount()) {
            fps.setChildCount(fpView.size());
        }
        if (queryParams.isHosts()) {
            DomainInfo[] di = fpView.getDomainInfos();
            String[] hosts = new String[di.length];
            for (int j = 0; j < hosts.length; j++) {
                hosts[j] = di[j].getTld();
            }
            fps.setHosts(hosts);
        }
        if (queryParams.isSpeed()) {
            fps.setSpeed(dwd.getDownloadSpeedbyFilePackage(fp));
        }
        if (queryParams.isStatus()) {
            setStatus(fps, fpView);
        }
        if (queryParams.isFinished()) {
            fps.setFinished(fpView.isFinished());
        }
        if (queryParams.isEta()) {
            fps.setEta(fpView.getETA());
        }
        if (queryParams.isBytesLoaded()) {
            fps.setBytesLoaded(fpView.getDone());
        }
        if (queryParams.isComment()) {
            fps.setComment(fp.getComment());
        }
        if (queryParams.isEnabled()) {
            fps.setEnabled(fpView.isEnabled());
        }
        if (queryParams.isRunning()) {
            fps.setRunning(dwd.getRunningFilePackages().contains(fp));
        }
        return fps;
    }

    public static DownloadLinkAPIStorableV2 toStorable(LinkQueryStorable queryParams, DownloadLink dl, Object caller) {
        final DownloadLinkAPIStorableV2 dls = new DownloadLinkAPIStorableV2(dl);
        if (queryParams.isPassword()) {
            dls.setDownloadPassword(dl.getDownloadPassword());
        }
        if (queryParams.isPriority()) {
            dls.setPriority(org.jdownloader.myjdownloader.client.bindings.PriorityStorable.get(dl.getPriorityEnum().name()));
        }
        if (queryParams.isHost()) {
            dls.setHost(dl.getHost());
        }
        if (queryParams.isBytesTotal()) {
            dls.setBytesTotal(dl.getView().getBytesTotalEstimated());
        }
        if (queryParams.isStatus()) {
            setStatus(dls, dl, caller);
        }
        if (queryParams.isBytesLoaded()) {
            dls.setBytesLoaded(dl.getView().getBytesLoaded());
        }
        if (queryParams.isSpeed()) {
            dls.setSpeed(dl.getView().getSpeedBps());
        }
        if (queryParams.isEta()) {
            PluginProgress plg = dl.getPluginProgress();
            if (plg != null) {
                dls.setEta(plg.getETA());
            } else {
                dls.setEta(-1l);
            }
        }
        if (queryParams.isFinished()) {
            dls.setFinished((FinalLinkState.CheckFinished(dl.getFinalLinkState())));
        }
        if (queryParams.isRunning()) {
            dls.setRunning(dl.getDownloadLinkController() != null);
        }
        if (queryParams.isSkipped()) {
            dls.setSkipped(dl.isSkipped());
        }
        if (queryParams.isUrl()) {
            dls.setUrl(dl.getView().getDisplayUrl());
        }
        if (queryParams.isEnabled()) {
            dls.setEnabled(dl.isEnabled());
        }
        if (queryParams.isExtractionStatus()) {
            final ExtractionStatus extractionStatus = dl.getExtractionStatus();
            if (extractionStatus != null) {
                dls.setExtractionStatus(extractionStatus.name());
            }
        }
        if (queryParams.isComment()) {
            dls.setComment(dl.getComment());
        }
        if (queryParams.isAddedDate()) {
            dls.setAddedDate(dl.getCreated());
        }
        if (queryParams.isFinishedDate()) {
            dls.setFinishedDate(dl.getFinishedDate());
        }
        dls.setPackageUUID(dl.getParentNode().getUniqueID().getID());
        return dls;
    }

    public static DownloadLinkAPIStorableV2 setStatus(DownloadLinkAPIStorableV2 dls, DownloadLink link, Object caller) {
        Icon icon = null;
        String label = null;
        PluginProgress prog = link.getPluginProgress();
        if (prog != null) {
            icon = prog.getIcon(caller);
            label = prog.getMessage(caller);
            if (icon != null) {
                dls.setStatusIconKey(RemoteAPIController.getInstance().getContentAPI().getIconKey(icon));
            }
            dls.setStatus(label);
            return dls;
        }
        ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
        if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
            icon = conditionalSkipReason.getIcon(caller, null);
            label = conditionalSkipReason.getMessage(caller, null);
            dls.setStatusIconKey(RemoteAPIController.getInstance().getContentAPI().getIconKey(icon));
            dls.setStatus(label);
            return dls;
        }
        SkipReason skipReason = link.getSkipReason();
        if (skipReason != null) {
            icon = skipReason.getIcon(caller, 18);
            label = skipReason.getExplanation(caller);
            dls.setStatusIconKey(RemoteAPIController.getInstance().getContentAPI().getIconKey(icon));
            dls.setStatus(label);
            return dls;
        }
        final FinalLinkState finalLinkState = link.getFinalLinkState();
        if (finalLinkState != null) {
            if (FinalLinkState.CheckFailed(finalLinkState)) {
                label = finalLinkState.getExplanation(caller, link);
                dls.setStatusIconKey(IconKey.ICON_FALSE);
                dls.setStatus(label);
                return dls;
            }
            final ExtractionStatus extractionStatus = link.getExtractionStatus();
            if (extractionStatus != null) {
                switch (extractionStatus) {
                case ERROR:
                case ERROR_PW:
                case ERROR_CRC:
                case ERROR_NOT_ENOUGH_SPACE:
                case ERRROR_FILE_NOT_FOUND:
                    label = extractionStatus.getExplanation();
                    dls.setStatusIconKey(IconKey.ICON_EXTRACT_ERROR);
                    dls.setStatus(label);
                    return dls;
                case SUCCESSFUL:
                    label = extractionStatus.getExplanation();
                    dls.setStatusIconKey(IconKey.ICON_EXTRACT_OK);
                    dls.setStatus(label);
                    return dls;
                case RUNNING:
                    label = extractionStatus.getExplanation();
                    dls.setStatusIconKey(IconKey.ICON_EXTRACT);
                    dls.setStatus(label);
                    return dls;
                }
            }
            if (FinalLinkState.FINISHED_MIRROR.equals(finalLinkState)) {
                dls.setStatusIconKey(IconKey.ICON_TRUE_ORANGE);
            } else {
                dls.setStatusIconKey(IconKey.ICON_TRUE);
            }
            label = finalLinkState.getExplanation(caller, link);
            dls.setStatus(label);
            return dls;
        }
        if (link.getDownloadLinkController() != null) {
            dls.setStatusIconKey(IconKey.ICON_RUN);
            dls.setStatus(_GUI.T.TaskColumn_fillColumnHelper_starting());
            return dls;
        }
        return dls;
    }

    @Override
    public int packageCount() {
        return DownloadController.getInstance().size();
    }

    @Override
    public void removeLinks(final long[] linkIds, final long[] packageIds) {
        packageControllerUtils.remove(linkIds, packageIds);
    }

    @Override
    public void renamePackage(Long packageId, String newName) {
        if (packageId != null && newName != null) {
            DownloadController dlc = DownloadController.getInstance();
            FilePackage fp = null;
            final boolean readL = dlc.readLock();
            try {
                for (final FilePackage pkg : dlc.getPackages()) {
                    if (packageId.longValue() == pkg.getUniqueID().getID()) {
                        fp = pkg;
                        break;
                    }
                }
            } finally {
                dlc.readUnlock(readL);
            }
            if (fp != null) {
                fp.setName(newName);
            }
        }
    }

    @Override
    public void renameLink(Long linkId, String newName) {
        if (newName != null && linkId != null) {
            DownloadController dwd = DownloadController.getInstance();
            final DownloadLink link = dwd.getLinkByID(linkId);
            if (link != null) {
                DownloadWatchDog.getInstance().renameLink(link, newName);
            }
        }
    }

    @Override
    public void movePackages(long[] packageIds, long afterDestPackageId) {
        packageControllerUtils.movePackages(packageIds, afterDestPackageId);
    }

    @Override
    public void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException {
        packageControllerUtils.movetoNewPackage(linkIds, pkgIds, newPkgName, downloadPath);
    }

    @Override
    public void splitPackageByHoster(long[] linkIds, long[] pkgIds) {
        packageControllerUtils.splitPackageByHoster(linkIds, pkgIds);
    }

    @Override
    public void moveLinks(long[] linkIds, long afterLinkID, long destPackageID) {
        packageControllerUtils.moveChildren(linkIds, afterLinkID, destPackageID);
    }

    @Override
    public long getStructureChangeCounter(long structureWatermark) {
        return packageControllerUtils.getChildrenChanged(structureWatermark);
    }

    @Override
    public long getStopMark() {
        Object mark = DownloadWatchDog.getInstance().getSession().getStopMark();
        if (mark != STOPMARK.NONE) {
            return ((AbstractNode) mark).getUniqueID().getID();
        }
        return -1l;
    }

    @Override
    public DownloadLinkAPIStorableV2 getStopMarkedLink() {
        final Object mark = DownloadWatchDog.getInstance().getSession().getStopMark();
        if (mark != null && mark != STOPMARK.NONE) {
            if (mark instanceof DownloadLink) {
                final DownloadLinkAPIStorableV2 dls = new DownloadLinkAPIStorableV2((DownloadLink) mark);
                dls.setPackageUUID(((DownloadLink) mark).getParentNode().getUniqueID().getID());
                return dls;
            }
        }
        return null;
    }

    @Override
    public void setEnabled(boolean enabled, long[] linkIds, long[] packageIds) {
        packageControllerUtils.setEnabled(enabled, linkIds, packageIds);
    }

    @Override
    public void resetLinks(long[] linkIds, long[] packageIds) {
        DownloadWatchDog.getInstance().reset(packageControllerUtils.getSelectionInfo(linkIds, packageIds).getChildren());
    }

    @Override
    public void setPriority(PriorityStorable priority, long[] linkIds, long[] packageIds) throws BadParameterException {
        final org.jdownloader.controlling.Priority jdPriority = org.jdownloader.controlling.Priority.valueOf(priority.name());
        final List<DownloadLink> children = packageControllerUtils.getChildren(linkIds);
        final List<FilePackage> pkgs = packageControllerUtils.getPackages(packageIds);
        for (DownloadLink dl : children) {
            dl.setPriorityEnum(jdPriority);
        }
        for (FilePackage pkg : pkgs) {
            pkg.setPriorityEnum(jdPriority);
        }
    }

    @Override
    public void setStopMark(long linkId, long packageId) {
        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = packageControllerUtils.getSelectionInfo(new long[] { linkId }, new long[] { packageId });
        for (DownloadLink dl : selectionInfo.getChildren()) {
            DownloadWatchDog.getInstance().setStopMark(dl);
            break;
        }
    }

    @Override
    public void removeStopMark() {
        DownloadWatchDog.getInstance().setStopMark(null);
    }

    @Override
    public void resumeLinks(long[] linkIds, long[] packageIds) throws BadParameterException {
        final DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        final List<DownloadLink> links = packageControllerUtils.getSelectionInfo(linkIds, packageIds).getChildren();
        dwd.resume(links);
    }

    @Override
    public boolean forceDownload(final long[] linkIds, long[] packageIds) throws BadParameterException {
        final DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        final List<DownloadLink> links = packageControllerUtils.getSelectionInfo(linkIds, packageIds).getChildren();
        dwd.forceDownload(links);
        return true;
    }

    @Override
    public void setDownloadDirectory(String directory, long[] packageIds) {
        packageControllerUtils.setDownloadDirectory(directory, packageIds);
    }

    @Override
    public void startOnlineStatusCheck(long[] linkIds, long[] packageIds) throws BadParameterException {
        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = packageControllerUtils.getSelectionInfo(linkIds, packageIds);
        SelectionInfoUtils.startOnlineStatusCheck(selectionInfo);
    }

    @Override
    public Map<String, List<Long>> getDownloadUrls(final long[] linkIds, final long[] packageIds, UrlDisplayTypeStorable[] urlDisplayTypes) throws BadParameterException {
        final List<UrlDisplayType> types = new ArrayList<UrlDisplayType>();
        for (final UrlDisplayTypeStorable urlDisplayType : urlDisplayTypes) {
            try {
                types.add(UrlDisplayType.valueOf(urlDisplayType.name()));
            } catch (Exception e) {
                throw new BadParameterException(e.getMessage());
            }
        }
        return SelectionInfoUtils.getURLs(packageControllerUtils.getSelectionInfo(linkIds, packageIds), types);
    }

    @Override
    public boolean setDownloadPassword(final long[] linkIds, final long[] packageIds, final String pass) throws BadParameterException {
        return packageControllerUtils.setDownloadPassword(linkIds, packageIds, pass);
    }

    @Override
    public void cleanup(final long[] linkIds, final long[] packageIds, final CleanupActionOptions.Action action, final CleanupActionOptions.Mode mode, final CleanupActionOptions.SelectionType selectionType) throws BadParameterException {
        packageControllerUtils.cleanup(linkIds, packageIds, action, mode, selectionType);
    }

    @Override
    public boolean unskip(final long[] linkIds, final long[] packageIds, SkipReasonStorable.Reason reason) throws BadParameterException {
        final List<DownloadLink> links = packageControllerUtils.getSelectionInfo(linkIds, packageIds).getChildren();
        List<DownloadLink> unskipLinks = new ArrayList<DownloadLink>();
        if (reason != null) {
            final SkipReason checkReason = SkipReason.valueOf(reason.name());
            for (DownloadLink link : links) {
                final SkipReason skipReason = link.getSkipReason();
                if (skipReason != null && checkReason.equals(skipReason)) {
                    unskipLinks.add(link);
                }
            }
        } else {
            unskipLinks = links;
        }
        DownloadWatchDog.getInstance().unSkip(unskipLinks);
        return true;
    }
}
