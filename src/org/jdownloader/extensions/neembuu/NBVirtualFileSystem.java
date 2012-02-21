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
public final class NBVirtualFileSystem extends SimpleReadOnlyFileSystem {
    
    static NBVirtualFileSystem newInstance(){
        return new NBVirtualFileSystem(new VectorRootDirectory());
    }
    
    private NBVirtualFileSystem(DirectoryStream rootDirectoryStream) {
        super(rootDirectoryStream);
    }

    public final VectorRootDirectory getVectorRootDirectory() {
        return (VectorRootDirectory)rootDirectoryStream;
    }
    
}
