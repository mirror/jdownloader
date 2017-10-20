package org.jdownloader.par2.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Enumeration;

import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.par2.AsciiCommentPacket;
import org.jdownloader.par2.CreatorPacket;
import org.jdownloader.par2.FileDescriptionPacket;
import org.jdownloader.par2.MainPacket;
import org.jdownloader.par2.RawPacket;
import org.jdownloader.par2.UnicodeCommentPacket;
import org.jdownloader.par2.UnicodeFilenamePacket;

public class Test {
    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream(new File(""));
        while (true) {
            RawPacket next = RawPacket.readNext(fis);
            if (next == null) {
                break;
            } else if (!next.verifyMD5()) {
                throw new Exception();
            } else {
                if (Arrays.equals(next.getType(), FileDescriptionPacket.MAGIC)) {
                    System.out.println(new FileDescriptionPacket(next));
                } else if (Arrays.equals(next.getType(), AsciiCommentPacket.MAGIC)) {
                    System.out.println(new AsciiCommentPacket(next));
                } else if (Arrays.equals(next.getType(), UnicodeFilenamePacket.MAGIC)) {
                    System.out.println(new UnicodeFilenamePacket(next));
                } else if (Arrays.equals(next.getType(), CreatorPacket.MAGIC)) {
                    System.out.println(new CreatorPacket(next));
                } else if (Arrays.equals(next.getType(), UnicodeCommentPacket.MAGIC)) {
                    System.out.println(new UnicodeCommentPacket(next));
                } else if (Arrays.equals(next.getType(), MainPacket.MAGIC)) {
                    MainPacket mainPacket = new MainPacket(next);
                    System.out.println(mainPacket);
                    Enumeration<byte[]> it = mainPacket.getRecoveryFileIDs();
                    while (it.hasMoreElements()) {
                        System.out.println("RecoveryFileID:" + HexFormatter.byteArrayToHex(it.nextElement()));
                    }
                } else {
                    System.out.println("Unsupported:" + new String(next.getType()));
                }
            }
        }
        fis.close();
    }
}
