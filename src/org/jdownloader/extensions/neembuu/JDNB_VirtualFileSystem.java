/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu;

import jpfm.DirectoryStream;
import jpfm.fs.SimpleReadOnlyFileSystem;
import jpfm.volume.vector.VectorRootDirectory;

/**
 *
 * @author Shashank Tulsyan
 */
public final class JDNB_VirtualFileSystem extends SimpleReadOnlyFileSystem {
    
    static JDNB_VirtualFileSystem newInstance(){
        return new JDNB_VirtualFileSystem(new VectorRootDirectory());
    }
    
    private JDNB_VirtualFileSystem(DirectoryStream rootDirectoryStream) {
        super(rootDirectoryStream);
    }

    public final VectorRootDirectory getVectorRootDirectory() {
        return (VectorRootDirectory)rootDirectoryStream;
    }
    
}
