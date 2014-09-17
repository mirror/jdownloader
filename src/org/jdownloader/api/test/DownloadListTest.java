package org.jdownloader.api.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import jd.nutils.DiffMatchPatch;
import jd.nutils.DiffMatchPatch.Patch;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.DiffHandler;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadLinkQuery;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadLinkStorable;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadPackageQuery;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsListInterface;
import org.jdownloader.myjdownloader.client.json.JSonRequest;
import org.jdownloader.myjdownloader.client.json.ObjectData;

public class DownloadListTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        String dev;
        DownloadsListInterface link = api.link(DownloadsListInterface.class, dev = chooseDevice(api));

        DownloadPackageQuery pq = new DownloadPackageQuery();
        pq.setStatus(true);
        api.setDiffhandler(new DiffHandler() {

            @Override
            public void prepare(JSonRequest payload, String deviceID, String action) {
                payload.setDiffKA(60 * 60 * 1000);
                String storageID = Hash.getMD5(payload.getUrl() + "(" + JSonStorage.serializeToJson(payload.getParams()) + ")");
                File tmp = Application.getTempResource("apidiffs/" + storageID + ".dat");
                payload.setDiffType("patch");
                if (tmp.exists()) {
                    payload.setDiffID(Hash.getMD5(tmp));
                }

            }

            @Override
            public String handle(JSonRequest payload, ObjectData dataObject, String deviceID, String action, String diffString) {

                String type = dataObject.getDiffType();

                String diffID = dataObject.getDiffID();

                String storageID = Hash.getMD5(payload.getUrl() + "(" + JSonStorage.serializeToJson(payload.getParams()) + ")");
                File tmp = Application.getTempResource("apidiffs/" + storageID + ".dat");
                try {
                    if (StringUtils.isEmpty(type)) {
                        // complete

                        IO.secureWrite(tmp, diffString.getBytes("UTF-8"));

                        return diffString;
                    } else if ("patch".equalsIgnoreCase(type)) {

                        String old = IO.readFileToString(tmp);

                        diffString = merge(old, diffString);
                        String newHash = Hash.getMD5(diffString);
                        boolean ok = StringUtils.equals(newHash, dataObject.getDiffID());
                        IO.secureWrite(tmp, diffString.getBytes("UTF-8"));
                        return diffString;
                    } else {
                        throw new WTFException("Unknown Diff Type");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            private String merge(String old, String diffString) {
                DiffMatchPatch differ = new DiffMatchPatch();
                List<Patch> patches = differ.patchFromText(diffString);
                Object[] diffs = differ.patchApply(new LinkedList<DiffMatchPatch.Patch>(patches), old);
                return diffs[0] + "";
            }
        });
        // List<DownloadPackageStorable> packages = link.queryPackages(pq);

        // byte[] ico = api.link(ContentInterface.class, dev).getIcon(packages.get(0).getStatusIconKey(), 32);
        // List<DownloadLinkStorable> smallList = link.queryLinks(new DownloadLinkQuery());
        DownloadLinkQuery query = new DownloadLinkQuery();
        query.setBytesLoaded(true);
        query.setBytesTotal(true);
        query.setEnabled(true);
        query.setEta(true);
        query.setExtractionStatus(true);
        query.setFinished(true);
        query.setHost(true);
        query.setRunning(true);
        query.setSkipped(true);
        query.setSpeed(true);
        query.setStatus(true);
        // query.setUrl(true);
        query.setPriority(true);
        List<DownloadLinkStorable> bigList = link.queryLinks(query);

        // link.setPriority(PriorityStorable.HIGHEST, new long[] { 1400828251836l }, null);
        // DownloadPackageQuery pq = new DownloadPackageQuery();
        // pq.setHosts(true);
        // link.queryPackages(pq);

        System.out.println(1);
    }
}
