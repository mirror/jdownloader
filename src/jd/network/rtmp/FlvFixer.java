//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

// source@GPLv3: https://github.com/K-S-V/Scripts/blob/master/FlvFixer.php
package jd.network.rtmp;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;
import jd.utils.JDHexUtils;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class FlvFixer {

    private static enum Tag {
        AUDIO((byte) 0x08),
        VIDEO((byte) 0x09),
        SCRIPT_DATA((byte) 0x12),
        FRAME_TYPE_INFO((byte) 0x05),
        CODEC_ID_AVC((byte) 0x07),
        CODEC_ID_AAC((byte) 0x0A),
        AVC_SEQUENCE_HEADER((byte) 0x00),
        AAC_SEQUENCE_HEADER((byte) 0x00),
        AVC_SEQUENCE_END((byte) 0x02);

        private byte hex;

        Tag(byte b) {
            hex = b;
        }
    }

    private File             corruptFile;

    private File             fixedFile;

    private FileInputStream  fis               = null;

    private FileOutputStream fos               = null;

    final static int         FRAMEGAP_DURATION = 8;

    final static int         INVALID_TIMESTAMP = -1;

    boolean                  DEBUG             = false;

    private final LogSource  logger;

    private DownloadLink     downloadLink;

    public FlvFixer() {
        logger = LogController.CL();
    }

    protected boolean setDebug(boolean b) {
        return DEBUG = b;
    }

    protected void setInputFile(File f) {
        this.corruptFile = f;
    }

    protected File getoutputFile() {
        return fixedFile;
    }

    protected boolean scan(DownloadLink dl) throws Exception {

        if (!(corruptFile.exists() && corruptFile.length() > 0)) {
            logger.severe("File " + corruptFile.getAbsolutePath() + " not found!");
            return false;
        }

        this.downloadLink = dl;

        boolean audio = false;
        boolean video = false;
        boolean metadata = true;

        final String formatDebug = "%14s%14s%14s%14s";
        final String format = "%14s%14s%14s";

        int baseTS = -1;
        int prevTagSize = 4;
        int tagHeaderLen = 11;
        int filePos = 13;

        int prevAudioTS = INVALID_TIMESTAMP;
        int prevVideoTS = INVALID_TIMESTAMP;
        long pAudioTagPos = 0l;
        long pVideoTagPos = 0l;
        long pMetaTagPos = 0l;

        boolean prevAVCHeader = false;
        boolean prevAACHeader = false;
        boolean AVCHeaderWritten = false;
        boolean AACHeaderWritten = false;

        int fileSize = (int) corruptFile.length();
        int cFilePos = 0, pFilePos = 0, packetSize = 0, packetTS = 0, tagPos = 0;

        byte[] flvHeader = JDHexUtils.getByteArray("464c5601050000000900000000");
        byte[] flvTag = new byte[tagHeaderLen];

        final List<Byte> extEnum = new ArrayList<Byte>();
        extEnum.add((byte) 0x08);
        extEnum.add((byte) 0x09);
        extEnum.add((byte) 0x12);

        String msg;
        boolean progressView = (DEBUG || fileSize > 100 * 1024 * 1024);

        fixedFile = new File(corruptFile.getAbsolutePath().replace(".part", ".fixed"));

        if (progressView) setProgress(0, 0, null);

        try {

            fis = new FileInputStream(corruptFile);
            fos = new FileOutputStream(fixedFile);

            /* FLV? */
            byte[] flvTagH = new byte[flvHeader.length];
            fis.read(flvTagH);
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            if (!"FLV".equals(new String(flvTagH).substring(0, 3))) {
                logger.severe("Input file is not a valid FLV file");
                return false;
            } else {
                fos.write(flvTagH);
            }

            msg = "FlvFixer: starting scan process... ";
            downloadLink.getLinkStatus().setStatusText(msg);
            logger.info("#### " + msg + " ####");
            logger.info("processing file: " + corruptFile.getAbsolutePath());

            if (DEBUG) logOutput("Type :", formatDebug, "CurrentTS", "PreviousTS", "Size", "Position", Level.FINEST);

            try {
                while (filePos < fileSize) {

                    /* 11 byte header */
                    fis.read(flvTag);

                    /* size and type of data */
                    byte packetType = flvTag[0];
                    packetSize = getInt(flvTag, 1);
                    packetTS = getInt(flvTag, 4) | (flvTag[7] & 0xff) << 24;

                    if (baseTS < 0 && (packetType == Tag.AUDIO.hex || packetType == Tag.VIDEO.hex)) baseTS = packetTS;
                    if (baseTS > 1000) {
                        packetTS -= baseTS;
                        writeFlvTimestamp(packetTS, flvTag);
                    }

                    /* header + data */
                    int totalTagLen = tagHeaderLen + packetSize + prevTagSize;
                    byte[] newflvTag = new byte[totalTagLen];
                    System.arraycopy(flvTag, 0, newflvTag, 0, flvTag.length);

                    int leftBytes = fis.available();
                    fis.read(newflvTag, tagHeaderLen, packetSize + prevTagSize);

                    if (newflvTag.length != totalTagLen || leftBytes < packetSize + prevTagSize) {
                        logger.warning("Broken FLV tag encountered! Aborting further processing.");
                        break;
                    }

                    byte AAC_PacketType = 0;
                    byte AVC_PacketType = 0;

                    switch (Tag.values()[extEnum.indexOf(packetType)]) {
                    case AUDIO:
                        if (packetTS > prevAudioTS - FRAMEGAP_DURATION * 5) {
                            byte FrameInfo = newflvTag[tagPos + tagHeaderLen];
                            byte CodecID = (byte) ((FrameInfo & 0xF0) >> 4);
                            if (CodecID == Tag.CODEC_ID_AAC.hex) {
                                AAC_PacketType = newflvTag[tagPos + tagHeaderLen + 1];
                                if (AAC_PacketType == Tag.AAC_SEQUENCE_HEADER.hex) {
                                    if (AACHeaderWritten) {
                                        logOutput("Skipping AAC sequence header AUDIO:", format, packetTS, prevAudioTS, packetSize, Level.INFO);
                                        break;
                                    } else {
                                        logger.info("Writing AAC sequence header");
                                        AACHeaderWritten = true;
                                    }
                                }
                                if (!AACHeaderWritten) {
                                    logOutput("Discarding audio packet received before AAC sequence header AUDIO:", format, packetTS, prevAudioTS, packetSize, Level.WARNING);
                                    break;
                                }
                            }
                            if (packetSize > 0) {
                                /* Check for packets with non-monotonic audio timestamps and fix them */
                                if (!(CodecID == Tag.CODEC_ID_AAC.hex && (AAC_PacketType == Tag.AAC_SEQUENCE_HEADER.hex || prevAACHeader))) {
                                    if (prevAudioTS != INVALID_TIMESTAMP && packetTS <= prevAudioTS) {
                                        logOutput("Fixing audio timestamp AUDIO:", format, packetTS, prevAudioTS, packetSize, Level.INFO);
                                        packetTS += FRAMEGAP_DURATION + (prevAudioTS - packetTS);
                                        writeFlvTimestamp(packetTS, flvTag);
                                    }
                                }
                                pAudioTagPos = fos.getChannel().position();
                                fos.write(newflvTag);
                                if (DEBUG) logOutput("AUDIO:", formatDebug, packetTS, prevAudioTS, packetSize, pAudioTagPos, Level.FINEST);
                                if (!(CodecID == Tag.CODEC_ID_AAC.hex && AAC_PacketType == Tag.AAC_SEQUENCE_HEADER.hex)) {
                                    prevAACHeader = false;
                                    prevAudioTS = packetTS;
                                } else {
                                    prevAACHeader = true;
                                }
                            } else {
                                logOutput("Skipping small sized audio packet AUDIO:", format, packetTS, prevAudioTS, packetSize, Level.INFO);
                            }
                        } else {
                            logOutput("Skipping audio packet AUDIO:", format, packetTS, prevAudioTS, packetSize, Level.INFO);
                        }
                        if (!audio) audio = true;
                        break;
                    case VIDEO:
                        if (packetTS > prevVideoTS - FRAMEGAP_DURATION * 5) {
                            byte FrameInfo = newflvTag[tagPos + tagHeaderLen];
                            byte FrameType = (byte) ((FrameInfo & 0xF0) >> 4);
                            byte CodecID = (byte) (FrameInfo & 0x0F);
                            if (FrameType == Tag.FRAME_TYPE_INFO.hex) {
                                logOutput("Skipping video info frame VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.WARNING);
                                break;
                            }
                            if (CodecID == Tag.CODEC_ID_AVC.hex) {
                                AVC_PacketType = newflvTag[tagPos + tagHeaderLen + 1];
                                if (AVC_PacketType == Tag.AVC_SEQUENCE_HEADER.hex) {
                                    if (AVCHeaderWritten) {
                                        logOutput("Skipping AVC sequence header VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.INFO);
                                        break;
                                    } else {
                                        logger.info("Writing AVC sequence header");
                                        AVCHeaderWritten = true;
                                    }
                                }
                                if (!AVCHeaderWritten) {
                                    logOutput("Discarding video packet received before AVC sequence header VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.WARNING);
                                    break;
                                }
                            }
                            if (packetSize > 0) {
                                /* Check for packets with non-monotonic video timestamps and fix them */
                                if (!(CodecID == Tag.CODEC_ID_AVC.hex && (AVC_PacketType == Tag.AVC_SEQUENCE_HEADER.hex || AVC_PacketType == Tag.AVC_SEQUENCE_END.hex || prevAVCHeader))) {
                                    if (prevVideoTS != INVALID_TIMESTAMP && packetTS <= prevVideoTS) {
                                        logOutput("Fixing video timestamp VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.INFO);
                                        packetTS += FRAMEGAP_DURATION + (prevVideoTS - packetTS);
                                        writeFlvTimestamp(packetTS, flvTag);
                                    }
                                }
                                pVideoTagPos = fos.getChannel().position();
                                fos.write(newflvTag);
                                if (DEBUG) logOutput("VIDEO:", formatDebug, packetTS, prevVideoTS, packetSize, pVideoTagPos, Level.FINEST);
                                if (!(CodecID == Tag.CODEC_ID_AVC.hex && AVC_PacketType == Tag.AVC_SEQUENCE_HEADER.hex)) {
                                    prevAVCHeader = false;
                                    prevVideoTS = packetTS;
                                } else {
                                    prevAVCHeader = true;
                                }
                            } else {
                                logOutput("Skipping small sized video packet VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.INFO);
                            }
                        } else {
                            logOutput("Skipping video packet VIDEO:", format, packetTS, prevVideoTS, packetSize, Level.INFO);
                        }
                        if (!video) video = true;
                        break;
                    case SCRIPT_DATA:
                        if (metadata) {
                            pMetaTagPos = fos.getChannel().position();
                            fos.write(newflvTag);
                            if (DEBUG) logOutput("META :", formatDebug, packetTS, 0, packetSize, pMetaTagPos, Level.FINEST);
                        }
                        break;
                    }
                    filePos += totalTagLen;
                    cFilePos = (int) filePos / (1024 * 1024);
                    if (progressView) {
                        if (cFilePos > pFilePos && cFilePos % 10 == 0 || cFilePos == 0) {
                            setProgress(filePos, fileSize, Color.YELLOW.darker());
                            pFilePos = cFilePos;
                        }
                    }
                }

                /* Fix flv header when required */
                if (!(audio && video)) {
                    if (audio && !video) {
                        flvHeader[4] = 4;
                    } else if (video && !audio) {
                        flvHeader[4] = 1;
                    }
                    fos.getChannel().position(0l);
                    fos.write(flvHeader, 0, flvHeader.length);
                    logger.info("Fix flv header --> " + (audio ? "audio" : "video"));
                }

                if (progressView) setProgress(fileSize, fileSize, Color.YELLOW.darker());
            } catch (ClosedByInterruptException e) {
                msg = String.format("FlvFixer: User aborted processing! Processed: %d/%.2f MB", (filePos / (1024 * 1024)), (double) (fileSize / (1024 * 1024)));
                logger.warning("#### " + msg);
                downloadLink.getLinkStatus().setStatusText(msg);
                return false;
            } catch (Throwable e) {
                logger.severe("#### FlvFixer: an error has occurred!");
                e.printStackTrace();
                return false;
            }

            msg = "FlvFixer: scan process done!";
            downloadLink.getLinkStatus().setStatusText(msg);
            logger.info("#### " + msg + " ####");
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            downloadLink.setPluginProgress(null);
        }
        return true;
    }

    private void setProgress(long value, long max, Color color) {
        if (value <= 0 && max <= 0) {
            downloadLink.setPluginProgress(null);
        } else {
            PluginProgress progress = downloadLink.getPluginProgress();
            if (progress != null) {
                progress.updateValues(value, max);
                progress.setCurrent(value);
            } else {
                progress = new FlvFixerProgress(value, max, color);
                progress.setIcon(NewTheme.I().getIcon("wait", 16));
                progress.setProgressSource(this);
                downloadLink.setPluginProgress(progress);
            }
        }
    }

    private void logOutput(String msg, String formatter, Integer a, Integer b, Integer c, Level l) {
        logger.log(l, String.format(msg + formatter, a, b, c));
    }

    private void logOutput(String msg, String formatter, Object a, Object b, Object c, Object d, Level l) {
        logger.log(l, String.format(msg + formatter, a, b, c, d));
    }

    private void writeFlvTimestamp(int baseTS, byte[] flvTag) {
        System.arraycopy(JDHexUtils.getByteArray(String.format("%08x", baseTS)), 0, flvTag, 4, 4);
    }

    private int getInt(byte[] array, int offset) {
        return ((0x00 & 0xff) << 24) | ((array[offset] & 0xff) << 16) | ((array[offset + 1] & 0xff) << 8) | (array[offset + 2] & 0xff);
    }

}