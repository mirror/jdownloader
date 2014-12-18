package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ExtractionInterface;

public class ValidateArchiveTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        String dev;
        ExtractionInterface link = api.link(ExtractionInterface.class, dev = chooseDevice(api));
        String rawLinkIdsInput = Dialog.getInstance().showInputDialog("Enter link id");
        String rawPkgsIdsInput = Dialog.getInstance().showInputDialog("Enter package id");
        if (StringUtils.isEmpty(rawLinkIdsInput) && StringUtils.isEmpty(rawPkgsIdsInput)) {
            Dialog.getInstance().showMessageDialog("No link or package id provided. Try again.");
        } else {
            String[] rawLinkIds = rawLinkIdsInput.split(",");
            String[] rawPkgIds = rawPkgsIdsInput.split(",");
            long[] linkIds = new long[rawLinkIds.length];
            long[] pkgIds = new long[rawPkgIds.length];
            try {
                for (int i = 0; i < linkIds.length; i++) {
                    if (!StringUtils.isEmpty(rawLinkIds[i])) {
                        linkIds[i] = Long.valueOf(rawLinkIds[i].trim());
                    }
                }
                for (int i = 0; i < pkgIds.length; i++) {
                    if (!StringUtils.isEmpty(rawPkgIds[i])) {
                        pkgIds[i] = Long.valueOf(rawPkgIds[i].trim());
                    }
                }
                Dialog.getInstance().showMessageDialog("" + (link.getArchiveInfo(linkIds, pkgIds) != null));
            } catch (NumberFormatException e) {
                Dialog.getInstance().showMessageDialog("Wrong UUID format. Try again.");
            }
        }
    }
}
