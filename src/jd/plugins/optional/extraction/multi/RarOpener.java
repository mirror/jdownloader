package jd.plugins.optional.extraction.multi;

import java.util.HashMap;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

/**
 * Used to join the separated rar files.
 * 
 * @author botzi
 *
 */
class RarOpener implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword {
    private Map<String, RandomAccessFile> openedRandomAccessFileList = new HashMap<String, RandomAccessFile>();
    private String name ;
    private String password;
    
    RarOpener() {
        this.password = "";
    }
    
    RarOpener(String password) {
        this.password = password;
    }
    
    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
            case NAME:
                return name ;
        }
        return null;
    }
    
    public IInStream getStream(String filename) throws SevenZipException {
        try {
            RandomAccessFile randomAccessFile = openedRandomAccessFileList.get(filename);
            if (randomAccessFile != null) {
                randomAccessFile.seek(0);
                name = filename;
                return new RandomAccessFileInStream(randomAccessFile);
            }
            
            randomAccessFile = new RandomAccessFile(filename, "r");
            
            openedRandomAccessFileList.put(filename, randomAccessFile);
            
            name = filename;
            return new RandomAccessFileInStream(randomAccessFile);
        } catch (FileNotFoundException fileNotFoundException) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    void close() throws IOException {
        for (RandomAccessFile file : openedRandomAccessFileList.values()) {
            file.close();
        }
    }
    
    public void setCompleted(Long files, Long bytes) throws SevenZipException {
    }
    
    public void setTotal(Long files, Long bytes) throws SevenZipException {
    }
    
    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }
}