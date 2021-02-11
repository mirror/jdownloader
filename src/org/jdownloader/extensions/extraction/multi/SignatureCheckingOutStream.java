package org.jdownloader.extensions.extraction.multi;

import java.util.concurrent.atomic.AtomicReference;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.Signature;

public class SignatureCheckingOutStream implements ISequentialOutStream {
    private final AtomicReference<Signature>    passwordFound;
    private final FileSignatures                filesignatures;
    private final ReusableByteArrayOutputStream buffer;
    private int                                 signatureMinLength = 32;
    private final long                          maxPWCheckSize;
    private String                              itemName;
    private long                                itemSize           = -1;
    private final boolean                       optimized;
    private boolean                             ignoreWrite        = false;
    private final ExtractionController          ctrl;
    private Signature                           lastSignature      = null;

    public SignatureCheckingOutStream(final ExtractionController ctrl, AtomicReference<Signature> passwordFound, FileSignatures filesignatures, ReusableByteArrayOutputStream buffer, long maxPWCheckSize, boolean optimized) {
        this.passwordFound = passwordFound;
        this.filesignatures = filesignatures;
        this.buffer = buffer;
        this.maxPWCheckSize = maxPWCheckSize;
        this.optimized = optimized;
        this.ctrl = ctrl;
    }

    public int write(final byte[] data, final int off, final int len) throws SevenZipException {
        if (ctrl.gotKilled()) {
            throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
        }
        if (ignoreWrite == false) {
            int toWrite = Math.min(buffer.free(), len);
            if (toWrite > 0) {
                /* we still have enough buffer left to write the data */
                buffer.write(data, off, toWrite);
            } else {
                ignoreWrite = true;
            }
            if (buffer.size() >= signatureMinLength) {
                /* we have enough data available for a signature check */
                StringBuilder sigger = new StringBuilder();
                for (int i = 0; i < buffer.size() - 1; i++) {
                    String s = Integer.toHexString(buffer.getInternalBuffer()[i]);
                    s = (s.length() < 2 ? "0" + s : s);
                    s = s.substring(s.length() - 2);
                    sigger.append(s);
                }
                final Signature signature = filesignatures.getSignature(sigger.toString(), itemName);
                if (signature != null) {
                    if (signature.getExtensionSure() != null && (itemName == null || signature.getExtensionSure().matcher(itemName).matches())) {
                        /* signature matches, lets abort PWFinding now */
                        if (signature.isPrecisePatternStart()) {
                            passwordFound.set(signature);
                            return 0;
                        } else {
                            lastSignature = signature;
                        }
                    }
                }
            }
        }
        if ((itemSize >= 0 && itemSize <= maxPWCheckSize) || !optimized || lastSignature != null) {
            /* we still allow further extraction as the itemSize <= maxPWCheckSize */
            return len;
        } else {
            /* this will throw SevenZipException */
            return 0;
        }
    }

    public int write(byte[] data) throws SevenZipException {
        return write(data, 0, data.length);
    }

    public Signature getLastSignature() {
        return lastSignature;
    }

    public void reset() {
        buffer.reset();
        signatureMinLength = 32;
        itemName = null;
        itemSize = -1;
        lastSignature = null;
    }

    public long getWritten() {
        return buffer.size();
    }

    public void setSignatureLength(String itemName, long itemSize) {
        if (new Regex(itemName, ".+\\.(iso|udf)").matches()) {
            signatureMinLength = 37000;
        } else if (new Regex(itemName, ".+\\.mp3").matches()) {
            signatureMinLength = 512;
        } else {
            signatureMinLength = 32;
        }
        this.itemName = itemName;
        this.itemSize = itemSize;
    }
}
