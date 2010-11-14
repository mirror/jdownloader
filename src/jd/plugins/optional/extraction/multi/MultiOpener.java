package jd.plugins.optional.extraction.multi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

/**
 * Used to join the separated HJSplit and 7z files.
 * 
 * @author botzi
 *
 */
class MultiOpener implements IArchiveOpenVolumeCallback, ICryptoGetTextPassword {
    private Map<String, RandomAccessFile> openedRandomAccessFileList = new HashMap<String, RandomAccessFile>();
    private String password;

    MultiOpener() {
        this.password = "";
    }
    
    MultiOpener(String password) {
        this.password = password;
    }
    
    public Object getProperty(PropID propID) throws SevenZipException {
        return null;
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {
            RandomAccessFile randomAccessFile = openedRandomAccessFileList.get(filename);
            if (randomAccessFile != null) {
                randomAccessFile.seek(0);
                return new RandomAccessFileInStream(randomAccessFile);
            }
            
            randomAccessFile = new RandomAccessFile(filename, "r");
            openedRandomAccessFileList.put(filename, randomAccessFile);
        
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

    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }
}