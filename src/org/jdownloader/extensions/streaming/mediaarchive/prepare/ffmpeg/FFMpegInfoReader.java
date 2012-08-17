package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.StreamingExtension;

public class FFMpegInfoReader {

    private DownloadLink downloadLink;

    public FFMpegInfoReader(DownloadLink dl) {
        this.downloadLink = dl;
    }

    public void load(StreamingExtension extension) throws InterruptedException, IOException {

        String id = new UniqueAlltimeID().toString();
        String streamurl = extension.createStreamUrl(id, "ffmpeg");
        try {
            extension.addDownloadLink(id, downloadLink);
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffprobe.exe");
            if (ffprobe.exists()) {
                String result = execute(ffprobe.getAbsolutePath(), "-show_streams", "-of", "json", "-i", streamurl);
                int i = result.indexOf("\"streams\":");
                String json = result.substring(i + 11, result.length() - 2).trim().replace("\r\n", "");
                ArrayList<FFProbeResult> info = JSonStorage.restoreFromString(json, new TypeRef<ArrayList<FFProbeResult>>() {

                });
                System.out.println(info);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            extension.removeDownloadLink(id);
        }
    }

    private String execute(String... cmds) throws InterruptedException, IOException {

        final ProcessBuilder pb = ProcessBuilderFactory.create(cmds);

        final StringBuilder sb = new StringBuilder();
        final Process process = pb.start();
        new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb.append(IO.readInputStreamToString(process.getInputStream()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // new Thread("ffmpegReader") {
        // public void run() {
        // try {
        // sb.append(IO.readInputStreamToString(process.getErrorStream()));
        // } catch (UnsupportedEncodingException e) {
        // e.printStackTrace();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        // }.start();
        System.out.println(process.waitFor());

        return sb.toString();

    }
}
