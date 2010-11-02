package jd.plugins.optional.extraction.multi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import jd.plugins.optional.extraction.ExtractionConstants;
import jd.plugins.optional.extraction.ExtractionController;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Gets the decrypted bytes and writes it into the file.
 * 
 * @author botzi
 *
 */
class MultiCallback implements ISequentialOutStream {
    private FileOutputStream fos;
    private ExtractionController con;
    private CRC32 crc;
    boolean shouldCrc = false;
    private long time;
    
    MultiCallback(File file, ExtractionController con, boolean shouldCrc) throws FileNotFoundException {
        this.con = con;
        this.shouldCrc = shouldCrc;
        
        fos = new FileOutputStream(file, true);
        
//        if(shouldCrc) {
//            crc = new CRC32();
//        }
        
        time = System.currentTimeMillis();
    }
    
    public int write(byte[] data) throws SevenZipException {
        try {
            fos.write(data);
            
            con.getArchiv().setExtracted(con.getArchiv().getExtracted() + data.length);

            if((System.currentTimeMillis() - time) > 1000) {
                con.fireEvent(ExtractionConstants.WRAPPER_ON_PROGRESS);
                time = System.currentTimeMillis();
            }
            
//            if(shouldCrc) {
//                crc.update(data);
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return data.length;
    }
    
    /**
     * Retruns the computed CRC.
     * 
     * @return The computed CRC.
     */
    String getComputedCRC() {
        return Long.toHexString(crc.getValue());
    }
}