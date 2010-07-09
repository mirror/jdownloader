/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jd.plugins.optional.neembuu;

import java.nio.ByteBuffer;
import jd.plugins.DownloadLink;
import jpfm.DirectoryStream;
import jpfm.FileFlags;
import jpfm.annotations.NonBlocking;
import jpfm.operations.readwrite.ReadRequest;
import jpfm.volume.BasicAbstractFile;
import jpfm.volume.CommonFileAttributesProvider;

/**
 *
 * @author Shashank Tulsyan
 * @author coalado
 */
public class JDFile extends BasicAbstractFile {
    private DownloadLink downloadLink;

    public JDFile(
                DownloadLink downloadLink,
                DirectoryStream parent){
        this(downloadLink,parent,
                CommonFileAttributesProvider.DEFAULT
                    /*contains junk values for creation date and other similar fields*/);
    }


    public JDFile(
                DownloadLink downloadLink,
                DirectoryStream parent,// the parent virtual folder
                CommonFileAttributesProvider fileAttributesProvider // contains
                                        // info like creation date
                                        // can have junk values
                                        // can be changed during runtime
            ) {
        super(
                downloadLink.getFinalFileName(),
                downloadLink.getDownloadSize(), // assuming this means size of total file
                parent,
                fileAttributesProvider);
        this.downloadLink = downloadLink;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }


    public void open() {
        // called when the file is doubled clicked and opened
        // in something like vlc.
        // this is called again only if all instances are closed
        // and file is opened again
    }

    public int read(long offset, ByteBuffer directByteBuffer) {
        // forget this, this is just for possible improved compatibility.

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @NonBlocking(usesJava1_7NIOClasses=true)
    public void read(ReadRequest read) throws Exception {
        
    }

    public void close() {
        //called when each and every instance of file is closed.
    }

    @Override
    public FileFlags getFileFlags() {
        //super.getFileFlags() is fake and useless

        return new FileFlags.Builder()
                .setOffline()//these 2 flags means
                .setNoIndex()//  that thumnail rendering by explorer/nautilus is disabled :)
                .setReadOnly()
                .build();
    }

}
