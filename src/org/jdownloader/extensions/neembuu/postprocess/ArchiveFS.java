/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu.postprocess;

import jpfm.FileAttributesProvider;
import jpfm.fs.SimpleReadOnlyFileSystem;
import jpfm.volume.vector.VectorDirectory;
import jpfm.volume.vector.VectorNode;
import jpfm.volume.vector.VectorRootDirectory;

/**
 *
 * @author Shashank Tulsyan
 */
public final class ArchiveFS extends SimpleReadOnlyFileSystem {
    
    public ArchiveFS(String archivePath) {
        super(new VectorRootDirectory());
        
        // let us say you want to add a directory to the root
        VectorDirectory vd = new VectorDirectory("some folder in archive", getRootDirectory());
        getRootDirectory().add(vd);
        
        // let us say you want to add a directory to the directory "some folder in archive"
        VectorDirectory vd1 = new VectorDirectory("some folder in archive", vd);
        vd.add(vd1);
        
        // let us say there is a file in the archive, in the folder "some folder in archive"
        FileInsideArchive fia = new FileInsideArchive("SomeVideo.avi", 100*1024*1024, vd);
    }

    public VectorRootDirectory getRootDirectory() {
        return (VectorRootDirectory)rootDirectoryStream;
    }

    public static void main(String[] args) {
        ArchiveFS afs = new ArchiveFS(args[0]);
        printNode(afs.getRootDirectory());
    }
    
    private static void printNode(VectorNode vn){
        for(FileAttributesProvider fap : vn){
            if(fap instanceof VectorNode){
                System.out.println(fap+"{");
                printNode((VectorNode)fap);
                System.out.println("}");
            }else 
                System.out.println(fap);
        }
    }
    
}
