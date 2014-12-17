package org.jdownloader.extensions.extraction.multi;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.SevenZipException;

public class DummyOpener implements IArchiveOpenCallback, ICryptoGetTextPassword {

    private final String password;

    public DummyOpener(final String password) {
        if (password == null) {
            /* password null will crash jvm */
            this.password = "";
        } else {
            this.password = password;
        }
    }

    @Override
    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }

    @Override
    public void setCompleted(Long arg0, Long arg1) throws SevenZipException {
    }

    @Override
    public void setTotal(Long arg0, Long arg1) throws SevenZipException {
    }

}
