package org.jdownloader.extensions.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.jdownloader.controlling.FileCreationManager;

public class ExtractLogFileWriter implements ShutdownVetoListener {

    private File           currentFile = null;
    private BufferedWriter output      = null;
    private final File     finalFile;

    public ExtractLogFileWriter(String name, String filePath, String id) {
        finalFile = Archive.getArchiveLogFileById(id);
        currentFile = new File(new File(finalFile.getParentFile(), "open"), finalFile.getName());
        FileCreationManager.getInstance().mkdir(currentFile.getParentFile());
        if (deleteOnShutDown()) {
            ShutdownController.getInstance().addShutdownVetoListener(this);
        }
        FileOutputStream fos = null;
        try {
            currentFile.createNewFile();
            if (!currentFile.canWrite()) {
                throw new IllegalArgumentException("Cannot write to file: " + currentFile);
            }
            output = new BufferedWriter(new OutputStreamWriter(fos = new FileOutputStream(currentFile, false), "UTF-8"));
        } catch (IOException e) {
            try {
                if (fos != null) {
                    fos.close();
                    fos = null;
                }
            } catch (IOException e2) {
            }
            delete();
            e.printStackTrace();
        }
        write("Archive Name: " + name);
        write("Archive Path: " + filePath);
    }

    protected boolean deleteOnShutDown() {
        return true;
    }

    public void delete() {
        close();
        FileCreationManager.getInstance().delete(currentFile, null);
        ShutdownController.getInstance().removeShutdownVetoListener(this);
    }

    public void write(String string) {
        if (output != null) {
            try {
                output.write(System.currentTimeMillis() + " - " + string);
                output.write("\r\n");
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        try {
            final BufferedWriter os = output;
            output = null;
            if (os != null) {
                os.close();
                FileCreationManager.getInstance().delete(finalFile, null);
                if (currentFile.renameTo(finalFile)) {
                    currentFile = finalFile;
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
        delete();
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

}
