//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils.svn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.jdownloader.controlling.FileCreationManager;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;

/**
 * ISVNEditor implementation that will add directories and files into the target directory accordingly to update instructions sent by the
 * server.
 */
public class ExportEditor implements ISVNEditor {

    private File              myRootDirectory;
    private SVNDeltaProcessor myDeltaProcessor;

    /**
     * root - the local directory where the node tree is to be exported into.
     */
    public ExportEditor(File root) {
        myRootDirectory = root;
        /*
         * Utility class that will help us to transform 'deltas' sent by the server to the new file contents.
         */
        myDeltaProcessor = new SVNDeltaProcessor();
    }

    /**
     * Server reports revision to which application of the further instructions will update working copy to.
     */
    public void targetRevision(long revision) throws SVNException {
    }

    /**
     * Called before sending other instructions.
     */
    public void openRoot(long revision) throws SVNException {
    }

    /**
     * Called when a new directory has to be added.
     * 
     * For each 'addDir' call server will call 'closeDir' method after all children of the added directory are added.
     * 
     * This implementation creates corresponding directory below root directory.
     */
    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        File newDir = new File(myRootDirectory, path);
        if (!newDir.exists()) {
            if (!FileCreationManager.getInstance().mkdir(newDir)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "error: failed to add the directory ''{0}''.", newDir);
                throw new SVNException(err);
            }
        }
        // System.out.println("dir added: " + path);
    }

    /**
     * Called when there is an existing directory that has to be 'opened' either to modify this directory properties or to process other
     * files and directories inside this directory.
     * 
     * In case of export this method will never be called because we reported that our 'working copy' is empty and so server knows that
     * there are no 'existing' directories.
     */
    public void openDir(String path, long revision) throws SVNException {
    }

    /**
     * Instructs to change opened or added directory property.
     * 
     * This method is called to update properties set by the user as well as those created automatically, like "svn:committed-rev". See
     * SVNProperty class for default property names.
     * 
     * When property has to be deleted value will be 'null'.
     */

    public void changeDirProperty(String name, SVNPropertyValue property) throws SVNException {
    }

    /**
     * Called when a new file has to be created.
     * 
     * For each 'addFile' call server will call 'closeFile' method after sending file properties and contents.
     * 
     * This implementation creates empty file below root directory, file contents will be updated later, and for empty files may not be sent
     * at all.
     */
    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        File file = new File(myRootDirectory, path);
        if (file.exists()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "error: exported file ''{0}'' already exists!", file);
            throw new SVNException(err);
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "error: cannot create new  file ''{0}''", file);
            throw new SVNException(err);
        }
    }

    /**
     * Called when there is an existing files that has to be 'opened' either to modify file contents or properties.
     * 
     * In case of export this method will never be called because we reported that our 'working copy' is empty and so server knows that
     * there are no 'existing' files.
     */
    public void openFile(String path, long revision) throws SVNException {
    }

    /**
     * Instructs to add, modify or delete file property. In this example we skip this instruction, but 'real' export operation may inspect
     * 'svn:eol-style' or 'svn:mime-type' property values to transfor file contents propertly after receiving.
     */
    public void changeFileProperty(String path, String name, SVNPropertyValue property) throws SVNException {
    }

    /**
     * Called before sending 'delta' for a file. Delta may include instructions on how to create a file or how to modify existing file. In
     * this example delta will always contain instructions on how to create a new file and so we set up deltaProcessor with 'null' base file
     * and target file to which we would like to store the result of delta application.
     */
    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        myDeltaProcessor.applyTextDelta((File) null, new File(myRootDirectory, path), false);
    }

    /**
     * Server sends deltas in form of 'diff windows'. Depending on the file size there may be several diff windows. Utility class
     * SVNDeltaProcessor processes these windows for us.
     */
    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return myDeltaProcessor.textDeltaChunk(diffWindow);
    }

    /**
     * Called when all diff windows (delta) is transferred.
     */
    public void textDeltaEnd(String path) throws SVNException {
        myDeltaProcessor.textDeltaEnd();
    }

    /**
     * Called when file update is completed. This call always matches addFile or openFile call.
     */
    public void closeFile(String path, String textChecksum) throws SVNException {
    }

    /**
     * Called when all child files and directories are processed. This call always matches addDir, openDir or openRoot call.
     */
    public void closeDir() throws SVNException {
    }

    /**
     * Insturcts to delete an entry in the 'working copy'. Of course will not be called during export operation.
     */
    public void deleteEntry(String path, long revision) throws SVNException {
    }

    /**
     * Called when directory at 'path' should be somehow processed, but authenticated user (or anonymous user) doesn't have enough access
     * rights to get information on this directory (properties, children).
     */
    public void absentDir(String path) throws SVNException {
    }

    /**
     * Called when file at 'path' should be somehow processed, but authenticated user (or anonymous user) doesn't have enough access rights
     * to get information on this file (contents, properties).
     */
    public void absentFile(String path) throws SVNException {
    }

    /**
     * Called when update is completed.
     */
    public SVNCommitInfo closeEdit() throws SVNException {
        return null;
    }

    /**
     * Called when update is completed with an error or server requests client to abort update operation.
     */
    public void abortEdit() throws SVNException {
    }

}
