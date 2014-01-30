package org.jdownloader.extensions.extraction;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.Out;

public interface ExtractPasswordDialogInterface extends InputDialogInterface {
    @Out
    public String getArchiveName();

    @Out
    public ArchiveLinkStructure getArchiveLinkIds();
}
