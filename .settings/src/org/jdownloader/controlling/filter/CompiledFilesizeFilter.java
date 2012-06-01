package org.jdownloader.controlling.filter;

public class CompiledFilesizeFilter extends FilesizeFilter {

    public CompiledFilesizeFilter(FilesizeFilter org) {
        super(org.getFrom(), org.getTo(), org.isEnabled(), org.getMatchType());
    }

    public boolean matches(long downloadSize) {
        switch (getMatchType()) {
        case BETWEEN:
            return getFrom() <= downloadSize && getTo() >= downloadSize;
        case NOT_BETWEEN:
            return getFrom() > downloadSize || getTo() < downloadSize;
        }
        return false;

    }
}
