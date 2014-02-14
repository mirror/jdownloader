package org.jdownloader.extensions.extraction.multi;

import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.Signature;

public class SignatureCheckingOutStream implements ISequentialOutStream {

    private final AtomicBoolean                 passwordfound;
    private final FileSignatures                filesignatures;
    private final ReusableByteArrayOutputStream buffer;
    private int                                 signatureMinLength = 32;
    private final long                          maxPWCheckSize;
    private String                              itemName;
    private long                                itemSize           = -1;
    private boolean                             optimized;
    private boolean                             ignoreWrite        = false;

    public SignatureCheckingOutStream(AtomicBoolean passwordfound, FileSignatures filesignatures, ReusableByteArrayOutputStream buffer, long maxPWCheckSize, boolean optimized) {
        this.passwordfound = passwordfound;
        this.filesignatures = filesignatures;
        this.buffer = buffer;
        this.maxPWCheckSize = maxPWCheckSize;
        this.optimized = optimized;
    }

    public int write(byte[] data) throws SevenZipException {
        if (ignoreWrite == false) {
            int toWrite = Math.min(buffer.free(), data.length);
            if (toWrite > 0) {
                /* we still have enough buffer left to write the data */
                buffer.write(data, 0, toWrite);
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
                Signature signature = filesignatures.getSignature(sigger.toString());
                if (signature != null) {
                    if (signature.getExtensionSure() != null && (itemName == null || signature.getExtensionSure().matcher(itemName).matches())) {
                        /* signature matches, lets abort PWFinding now */
                        passwordfound.set(true);
                        return 0;
                    }
                }
            }
        }
        if ((itemSize >= 0 && itemSize <= maxPWCheckSize) || !optimized) {
            /* we still allow further extraction as the itemSize <= maxPWCheckSize */
            return data.length;
        } else {
            /* this will throw SevenZipException */
            return 0;
        }
    }

    public void reset() {
        buffer.reset();
        signatureMinLength = 32;
        itemName = null;
        itemSize = -1;
    }

    public long getWritten() {
        return buffer.size();
    }

    public void setSignatureLength(String itemName, long itemSize) {
        if (new Regex(itemName, ".+\\.iso").matches()) {
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
