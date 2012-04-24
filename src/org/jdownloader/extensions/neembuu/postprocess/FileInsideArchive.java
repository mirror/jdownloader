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

import jpfm.DirectoryStream;
import jpfm.operations.readwrite.ReadRequest;
import jpfm.volume.BasicAbstractFile;

/**
 *
 * @author Shashank Tulsyan
 */
public class FileInsideArchive extends BasicAbstractFile {

    public FileInsideArchive(String name, long fileSize, DirectoryStream parent) {
        super(name, fileSize, parent);
    }
   
    @Override
    public void open() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void read(ReadRequest readRequest) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
