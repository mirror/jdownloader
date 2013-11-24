package org.jdownloader.api.extraction;

import org.jdownloader.extensions.extraction.ExtractionExtension;

public class ExtractionAPIImpl implements ExtractionAPI {
    @Override
    public void addArchivePassword(String password) {
        ExtractionExtension.getInstance().addPassword(password);
    }
}
