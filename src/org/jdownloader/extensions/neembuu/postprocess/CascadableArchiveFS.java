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

import java.util.Iterator;
import java.util.Set;
import jpfm.*;
import jpfm.fs.BasicCascadableProvider;
import jpfm.fs.BasicFileSystem;
import jpfm.fs.ReadOnlyRawFileData;
import jpfm.fs.SimpleReadOnlyFileSystem;

/**
 *
 * @author Shashank Tulsyan
 */
public final class CascadableArchiveFS extends SimpleReadOnlyFileSystem implements DirectoryStream  {

    public CascadableArchiveFS(DirectoryStream rootDirectoryStream) {
        super(rootDirectoryStream);
    }

    
    
    @Override
    public Iterator<FileAttributesProvider> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileType getFileType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getFileSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getCreateTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getAccessTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getWriteTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getChangeTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileDescriptor getParentFileDescriptor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileFlags getFileFlags() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    private final class ArchiveCascadableProvider implements BasicCascadableProvider{

        @Override
        public BasicFileSystem getFileSystem(Set<ReadOnlyRawFileData> dataProviders, FileDescriptor parentFileDescriptor) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<FileAttributesProvider> filesCascadingOver() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int suggestDataGlimpseSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String suggestedName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
}
