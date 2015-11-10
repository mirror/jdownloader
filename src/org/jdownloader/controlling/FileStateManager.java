package org.jdownloader.controlling;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FileStateManager {

    public static enum FILESTATE {
        // READ_SHARED,
        // WRITE_SHARED,
        // READ_EXCLUSIVE,
        WRITE_EXCLUSIVE
    }

    private final static FileStateManager INSTANCE = new FileStateManager();

    public static final FileStateManager getInstance() {
        return INSTANCE;
    }

    private FileStateManager() {
    }

    private static class FileStateOwner {
        private final FILESTATE fileState;

        private final FILESTATE getFileState() {
            return fileState;
        }

        private final Object getOwner() {
            return owner.get();
        }

        private final WeakReference<Object> owner;

        private FileStateOwner(FILESTATE fileState, Object owner) {
            this.fileState = fileState;
            this.owner = new WeakReference<Object>(owner);
        }
    }

    private final HashMap<File, ArrayList<FileStateOwner>> fileStates = new HashMap<File, ArrayList<FileStateOwner>>();

    public synchronized void requestFileState(final File file, final FILESTATE fileState, final Object owner) {
        if (file != null && fileState != null && owner != null) {
            ArrayList<FileStateOwner> fileStateList = fileStates.get(file);
            if (fileStateList == null) {
                fileStateList = new ArrayList<FileStateManager.FileStateOwner>();
                fileStates.put(file, fileStateList);
            }
            fileStateList.add(new FileStateOwner(fileState, owner));
        }
    }

    public synchronized void releaseFileState(final File file, final Object owner) {
        releaseFileState(file, null, owner);
    }

    public synchronized boolean hasFileState(final File file, final FILESTATE fileState) {
        if (file != null && fileState != null) {
            final ArrayList<FileStateOwner> fileStateList = fileStates.get(file);
            if (fileStateList != null) {
                for (FileStateOwner fileStateOwner : fileStateList) {
                    if (fileStateOwner.getOwner() != null && fileState.equals(fileStateOwner.getFileState())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public synchronized void releaseFileState(final File file, final FILESTATE fileState, final Object owner) {
        if (file != null && owner != null) {
            final ArrayList<FileStateOwner> fileStateList = fileStates.get(file);
            if (fileStateList != null) {
                final Iterator<FileStateOwner> it = fileStateList.iterator();
                while (it.hasNext()) {
                    final FileStateOwner next = it.next();
                    final Object nextOwner = next.getOwner();
                    if (nextOwner == null) {
                        it.remove();
                    } else if (owner == nextOwner) {
                        if (fileState == null || fileState.equals(next.getFileState())) {
                            it.remove();
                        }
                    }
                }
                if (fileStateList.size() == 0) {
                    fileStates.remove(file);
                }
            }
        }
    }

}
