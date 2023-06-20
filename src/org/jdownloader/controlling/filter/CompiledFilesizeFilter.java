package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;

public class CompiledFilesizeFilter extends FilesizeFilter implements Storable {
    @StorableAllowPrivateAccessModifier
    private CompiledFilesizeFilter() {
    }

    public CompiledFilesizeFilter(FilesizeFilter org) {
        super(org.getFrom(), org.getTo(), org.isEnabled(), org.getMatchType());
    }

    public boolean matches(long downloadSize) {
        switch (getMatchType()) {
        case BETWEEN:
            return getFrom() <= downloadSize && getTo() >= downloadSize;
        case NOT_BETWEEN:
            return getFrom() > downloadSize || getTo() < downloadSize;
        default:
            return false;
        }
    }
}
