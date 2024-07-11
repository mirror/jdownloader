package jd.plugins;

import java.util.regex.Pattern;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.CompiledFiletypeExtension;

import jd.parser.Regex;

public class ParsedFilename {
    private final String                    originalfilename;
    private final String                    filenameWithoutExtension;
    private final String                    filenameWithoutExtensionAdvanced;
    private final String                    extension;
    private final String                    extensionAdvanced;
    private final CompiledFiletypeExtension extCompiled;

    public ParsedFilename(final String filename) {
        originalfilename = filename;
        String extAdvanced = null;
        if (filename.contains(".")) {
            /* Filename with extension */
            final int extensionPosition = filename.lastIndexOf(".");
            extension = filename.substring(extensionPosition);
            extCompiled = CompiledFiletypeFilter.getExtensionsFilterInterface(extension);
            filenameWithoutExtension = filename.substring(0, extensionPosition);
            if (filenameWithoutExtension.contains(".")) {
                /* Look for double-dot multipart archive file ending */
                final Pattern[] doubleDotExtensionPatterns = new Pattern[] { Pattern.compile("(\\.part[0-9]+\\.rar)$", Pattern.CASE_INSENSITIVE), Pattern.compile("(\\.7z\\.[0-9]+)$", Pattern.CASE_INSENSITIVE) };
                for (final Pattern doubleDotExtensionPattern : doubleDotExtensionPatterns) {
                    extAdvanced = new Regex(filename, doubleDotExtensionPattern).getMatch(0);
                    if (extAdvanced != null) {
                        break;
                    }
                }
            }
        } else {
            /* Filename without extension */
            extension = null;
            extCompiled = null;
            filenameWithoutExtension = filename;
        }
        if (extAdvanced != null) {
            extensionAdvanced = extAdvanced;
            filenameWithoutExtensionAdvanced = filename.replaceFirst(extAdvanced + "$", "");
        } else {
            extensionAdvanced = extension;
            filenameWithoutExtensionAdvanced = filenameWithoutExtension;
        }
    }

    public String getFilenameWithoutExtension() {
        return filenameWithoutExtension;
    }

    public String getFilenameWithoutExtensionAdvanced() {
        return filenameWithoutExtensionAdvanced;
    }

    /** Returns extension without multipart extension if available. */
    public String getExtension() {
        return extension;
    }

    /** Returns extension with multipart archive prefix if available. */
    public String getExtensionAdvanced() {
        return extensionAdvanced;
    }

    public CompiledFiletypeExtension getExtCompiled() {
        return extCompiled;
    }

    public String getOriginalfilename() {
        return originalfilename;
    }
}
