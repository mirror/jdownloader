package org.jdownloader.extensions.extraction.multi;

public enum ArchiveType {
    /**
     * Is a single file archive.
     */
    SINGLE_FILE,
    /**
     * Is a 7zip or HJSplit multipar archive.
     */
    MULTI,
    /**
     * Is a multipart rar archive.
     */
    MULTI_RAR;
}
