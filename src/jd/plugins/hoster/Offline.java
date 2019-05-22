//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * The idea behind this is to speed up linkchecking for host providers that go permanently offline. URLs tend to stay cached/archived on the
 * intrawebs longer than host provider. By providing the original plugin regular expression(s) we do not have to rely on directhttp plugin
 * for linkchecking, or surrounding issues with 'silent errors' within the linkgrabber if the file extension isn't matched against
 * directhttp. <br />
 * - raztoki<br />
 * <br />
 * Set interfaceVersion to 3 to avoid old Stable trying to load this Plugin<br />
 *
 * @author raztoki<br />
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class Offline extends PluginForHost {
    /* 1st domain = current domain! */
    public static String[] domains = new String[] { "xfilesharing.us", "host.hackerbox.org", "ulozisko.sk", "teramixer.com", "imgspot.org", "uploadadz.com", "bdnupload.com", "catshare.net", "videobash.com", "ultimatedown.com", "d-h.st", "tenlua.vn", "bato.to", "5azn.net", "rapidpaid.com", "iranupload.com", "bytewhale.com", "filecyber.com", "putfiles.in", "fileud.com", "tempfile.ru", "cloud.directupload.net", "streammania.com", "brapid.sk", "watchers.to", "movdivx.com", "megadrive.tv", "megadrive.co", "swoopshare.com", "supershare.pl", "uptodo.net", "rawabbet.com", "zorofiles.com", "arivoweb.com", "hippohosted.com", "rabidfiles.com", "animefiles.online", "uploads.to", "uplod.it", "photo.qip.ru", "file.qip.ru", "hotshag.com", "uploadable.ch", "bigfile.to", "jkpan.cc", "imgzen.com", "imgdragon.com", "coreimg.net", "pic-maniac.com", "filemack.com", "filemac.com", "filekom.com",
            "file.oboz.ua", "protect-url.net", "p-u.in", "speedshare.eu", "magic4up.com", "uploadkadeh.com", "megafiles.us", "fileproject.com.br", "fileinstant.com", "uploadx.org", "uploadx.co", "uploadz.org", "uploadz.co", "uploadz.click", "uploaduj.net", "uploads.ws", "upl.me", "herosh.com", "avatarshare.com", "sju.wang", "uploadkadeh.ir", "uploading.site", "miravideos.net", "1000eb.com", "upload.mn", "upload.af", "sharehost.eu", "linkzhost.com", "files.com", "imgcandy.net", "failai.lt", "eyesfile.ca", "loadgator.com", "megafileupload.com", "megafirez.com", "megawatch.net", "dj97.com", "lacoqui.net", "vodlock.co", "vodlocker.city", "wizupload.com", "ziifile.com", "foxyimg.link", "chronos.to", "minhateca.com.br", "datasbit.com", "tubeq.xxx", "superupload.com", "disk.tom.ru", "mygirlfriendvids.net", "kingvid.tv", "faphub.xxx", "funonly.net", "bemywife.cc", "noslocker.com",
            "nosvideo.com", "hotamateurs.xxx", "bezvadata.cz", "anafile.com", "imgtiger.org", "minup.net", "jumbload.com", "media4up.com", "x3xtube.com", "basicupload.com", "jeodrive.com", "coolbytez.com", "keepshare.net", "superbshare.com", "levinpic.org", "imggold.org", "gavitex.com", "filesflash.com", "share.vnn.vn", "filebebo.cc", "imgdiamond.com", "imgswift.com", "arabloads.net", "filesisland.com", "share.az", "gigasize.com", "tuberealm.com", "nudeflix.com", "grifthost.com", "xvidstage.com", "emuparadise.me", "up07.net", "wastedamateurs.com", "filedais.com", "streamin.to", "myimg.club", "depfile.com", "mangatraders.biz", "vid.me", "uploadrocket.net", "faplust.com", "unlimitzone.com", "copiapop.com", "partagora.com", "filespace.io", "kingfiles.net", "uber-sha.re", "obscuredfiles.com", "uploadlw.com", "ulnow.com", "nashdisk.ru", "partage-facile.com", "ourupload.com", "glbupload.com",
            "imgve.com", "drfile.net", "bitload.org", "megafile.co", "gulf4up.com", "mydrive.com", "uplea.com", "letwatch.us", "onemillionfiles.com", "unlimit.co.il", "ul-instant.pw", "goear.com", "exfile.ru", "extradj.com", "exashare.com", "ecostream.tv", "karadownloads.com", "dropjar.com", "zstream.to", "ulshare.se", "f-bit.ru", "movyo.to", "yourvideohost.com", "tigerfiles.net", "thevideos.tv", "megafile.org", "99yp.cc", "ultrashare.net", "host4desi.com", "coo5shaine.com", "rapidvideo.ws", "neodrive.co", "mediafree.co", "filez.tv", "hochladen.to", "ampya.com", "feemoo.com", "filearn.com", "crocko.com", "porn5.com", "torrent.ajee.sh", "tikfile.com", "sharesend.com", "upfiles.net", "rioupload.com", "limefile.com", "powerwatch.pw", "up4file.com", "sli.mg", "sfshare.se", "rabbitfile.com", "megadysk.pl", "megadrv.com", "grimblr.com", "dwn.so", "cloudshares.net", "72bbb.com",
            "vip-shared.com", "someimage.com", "rodfile.com", "mightyupload.com", "pornative.com", "oload.net", "mediafire.bz", "lafiles.com", "kure.tv", "junocloud.me", "fileweed.net", "imgblitz.pw", "fibero.co", "filescloud.co", "fileload.io", "dedifile.com", "anysend.com", "2downloadz.com", "otakushare.com", "uploadbb.co", "bitcasa.com", "imgwet.com", "ziddu.com", "imxd.net", "myvideo.de", "timsah.com", "shared.sx", "stagevu.com", "vip-file.com", "shareflare.net", "moevideo.net", "letitbit.net", "lumload.com", "rosharing.net", "erafile.com", "uploading.com", "cloudsix.me", "zalaa.com", "vodlocker.com", "vidbull.com", "timeload.net", "sendspace.pl", "secureupload.eu", "mp3takeout.com", "imzdrop.com", "idowatch.net", "jpopsuki.tv", "minus.com", "happystreams.net", "kingload.net", "interfile.net", "imgbb.net", "filemoney.com", "auengine.com", "rocketfiles.net", "flashvids.org",
            "yamivideo.com", "divxhosted.com", "4upld.com", "filespart.com", "uploadc.com", "skymiga.com", "shareblue.eu", "videopremium.tv", "veterok.tv", "goo.im", "soundowl.com", "megaxfile.com", "bitshare.com", "promptfile.com", "fireswap.org", "vessel.com", "loadus.net", "gbload.com", "themediastorage.com", "uphere.pl", "megashares.com",
            /** -> */
            "sockshare.ws", "putlocker.ws", "vodu.ch", "watchfreeinhd.com"/** <- these are all the same site */
            , "vidspot.net", "hzfile.al", "video.tt", "tradingporn.com", "pdfcast.org", "mydisk.ge", "mp3hamster.net", "megacache.net", "laoupload.com", "hoodload.com", "filerev.cc", "filepi.com", "filehoot.com", "maxcloud.xyz", "mediaupload.us", "hugefiles.net", "upvast.com", "allmyvideos.net", "filestorm.xyz", "1tube.to", "hdstream.to", "sendfile.pl", "sharesix.net", "sharesix.com", "filenuke.com", "sharing.zone", "filesabc.com", "sendfiles.nl", "firstplanet.eu", "shareneo.net", "vidxtreme.to", "ugoupload.net", "sharingmaster.com", "hexupload.com", "videomega.tv", "speedyshare.com", "uploadbaz.com", "stiahni.si", "imgbar.net", "voooh.com", "filesuniverse.com", "vodbeast.com", "primephile.com", "hzfile.asia", "filenuke.net", "revclouds.com", "upple.it", "filesbomb.in", "livecloudz.com", "uploadscenter.com", "uploadspace.pl", "filebeam.com", "bestreams.net", "zettahost.tv",
            "divxpress.com", "primeshare.tv", "uploadingit.com", "veevr.com", "putstream.com", "zalohuj.to", "yy132.cn", "snakefiles.com", "rapidvideo.tv", "radicalshare.com", "nizfile.net", "novamov.me", "freespace.by", "fastimg.org", "fileflush.com", "downlod.co", "datoteke.com", "animeuploads.com", "cizgifilmlerizle.com", "1000shared.com", "myupload.dk", "vidce.tv", "uploadcoins.com", "fufox.net", "ninjashare.pl", "city-upload.com", "7958.com", "beemp3s.org", "coladrive.com", "megaloads.org", "easydow.org", "easywatch.tv", "vplay.ro", "chayfile.com", "purevid.com", "hdload.info", "devilshare.net", "lolabits.es", "gigafront.de", "5fantastic.pl", "copy.com", "treefiles.com", "mrfile.me", "filepurpose.com", "fexe.com", "avht.net", "24uploading.com", "datagrad.ru", "filesup.co", "steepafiles.com", "yourfiles.to", "hotbytez.com", "fileover.net", "vipup.in", "terafile.co", "vidig.biz",
            "uploadmax.net", "soniclocker.com", "ps3gameroom.net", "ipithos.to", "nowvideo.ws", "fileloby.com", "imgsee.me", "jumbofiles.com", "imgmega.com", "fisierulmeu.ro", "filepost.com", "djvv.com", "creeperfile.com", "bleuup.net", "carrier.so", "nosupload.com", "moviesand.com", "vidgrab.net", "netkups.com", "luckyshare.net", "abelhas.pt", "divshare.com", "letitload.com", "uploadsun.com", "cloudcorner.com", "gasxxx.com", "imageporter.com", "jizzbox.com", "foxytube.com", "fileplaneta.com", "nekaka.com", "vipfileshare.com", "filecore.co.nz", "idrivesync.com", "youporn-deutsch.com", "onlinestoragesolution.com", "oooup.com", "anonymousdelivers.us", "watching.to", "vipfile.in", "teraupload.net", "swiftupload.com", "xddisk.com", "xfiles.ivoclarvivadent.com", "vidd.tv", "up2box.co", "storefiles.co", "songs.to", "nowvideos.eu", "mediaprog.ru", "jumbofile.net", "maxupload.tv",
            "jippyshare.com", "labload.com", "joinfile.com", "hcbit.com", "gigafileupload.com", "filesin.com", "filekee.com", "fileshost.ws", "fileshare.to", "filenium.com", "filer.cx", "exstorage.net", "fileneo.com", "filemeup.net", "data.cod.ru", "cx.com", "cloudsuzy.com", "foxplay.info", "1gbpsbox.com", "banicrazy.info", "veodrop.com", "bigdownloader.com", "beehd.net", "3files.net", "filehost.pw", "storbit.net", "anyfiles.org", "2upfile.com", "ryushare.com", "onevideo.to", "junkyvideo.com", "filecrash.co", "filearning.com", "xenubox.com", "remixshare.com", "filecloud.cc", "4upfiles.com", "plunder.com", "elffiles.com", "shantibit.com", "x-share.ru", "vinupload.com", "linkfile.de", "jumbofiles.net", "freeporn.to", "filebrella.com", "storedrives.com", "uploadblast.com", "skyvids.net", "4uploaded.com", "up4.im", "v-vids.com", "animepremium.tv", "firedrive.com", "sockshare.com",
            "bearfiles.in", "mooshare.biz", "animecloud.me", "aimini.net", "rainy.la", "project-free-upload.com", "netload.in", "riotshare.com", "premiuns.org", "sendfaile.com", "pururin.com", "tumi.tv", "180upload.com", "tuspics.net", "stageflv.com", "shr77.com", "filebulk.com", "speedy-share.com", "sloozie.com", "otr-download.de", "fizy.com", "1tpan.com", "4uploading.com", "dropvideo.com", "filebox.ro", "vidbux.com", "filerock.net", "movreel.com", "megavideoz.eu", "realvid.net", "blip.tv", "gulfup.com", "hardwareclips.com", "uploadnet.co", "files.gw.kz", "files123.net", "free4share.de", "gettyfile.ru", "free-share.ru", "rziz.net", "zinwa.com", "wuala.com", "fileband.com", "megairon.net", "xfileload.com", "free-uploading.com", "fileown.com", "verzend.be", "rapidsonic.com", "filepack.pl", "goldbytez.com", "filemaze.ws", "divxden.com", "vidplay.net", "royalvids.eu", "superromworld.de",
            "n-roms.de", "simpleshare.org", "sharebeast.com", "sanshare.com", "faststream.in", "storedeasy.com", "jumbofiles.org", "uploadlux.com", "linestorage.com", "thefile.me", "muchshare.net", "queenshare.com", "videobam.com", "mp3the.net", "fileforever.net", "quickshare.cz", "privatehomeclips.com", "warped.co", "played.to", "topupload1.com", "creafile.net", "creafile.com", "krotix.net", "akafile.com", "videonan.com", "upload-drive.com", "share.time.mn", "pushfile.com", "fileriio.com", "dropfiles.info", "2drive.net", "400disk.com", "up-loading.net",
            /** turbobit alias' */
            "sharephile.com", "mxua.com", "katzfiles.com", "fsakura.com", "uploadur.com", "wyslijplik.pl", "tishare.com", "share50.com", "med1fire.com", "dumpfiles.org", "videolog.tv", "storeplace.org", "mojoload.com", "ifilehosting.net", "gigfiles.net", "maxisonic.com", "sv-esload.com", "upgaf.com", "gulfdown.com", "imageeer.com", "loombo.com", "upbrasil.info", "filegig.com", "xtraupload.net", "upservmedia.com", "cloudlync.com", "uploadto.us", "loudupload.net", "minoshare.com", "fun-vids.org", "flowload.com", "divxplanet.com", "saryshare.com", "uploadrive.com", "noelshare.com", "yunio.com", "daj.to", "voowl.com", "filerio.im", "boosterking.com", "u-tube.ru", "medifire.net", "galaxy-file.com", "uploadhunt.com", "upload-il.com", "uploadhero.co", "filefeltolto.hu", "przeslij.net", "altervideo.net", "fileswap.com", "safesharing.eu", "japlode.com", "shared.com", "streamit.to", "ilook.to",
            "imgah.com", "grooveshark.com", "mais.me", "megaul.com", "maxshare.pl", "mcupload.com", "mediavalise.com", "uncapped-downloads.com", "deerfile.com", "fupload.net", "zomgupload.com", "disk.84dm.com", "gfssex.com", "7thsky.es", "2download.de", "filezup.net", "fastupload.org", "files2share.ch", "metfiles.com", "1024disk.com", "boojour.eu", "clips-and-pics.org", "axifile.com", "filedust.net", "mixturecloud.com", "1st-files.com", "gigabyteupload.com", "filespeed.net", "midupload.com", "rainupload.com", "kingshare.to", "mejuba.com", "xlocker.net", "xvideohost.com", "rapidshare.com", "dwnshare.pl", "azerfile.com", "sindema.info", "seenupload.com", "sangfile.com", "fileloads.cc", "wdivx.com", "hotsvideos.com", "lovevideo.tv", "transitfiles.com", "xuploading.net", "asfile.com", "roottail.com", "filehostup.com", "freehostina.com", "porntubevidz.com", "upchi.co.il", "fastvideo.eu",
            "hostingbulk.com", "therapide.cz", "yonzy.com", "ravishare.com", "vozupload.com", "getzilla.net", "videodd.net", "dogefile.com", "nakido.com", "filevice.com", "diskfiles.net", "billionuploads.com", "filebite.cc", "gsinfinite.com", "heaven666.org", "saganfiles.com", "theamateurzone.info", "ultrafile.me", "fileb.ag", "k-files.kz", "clicktoview.org", "xvidstream.net", "filemup.com", "miloshare.com", "lomafile.com", "megacrypter.com", "iiiup.com", "digzip.com", "senseless.tv", "henchfile.com", "cometfiles.com", "mijnbestand.nl", "upafile.com", "bl.st", "filthyrx.com", "turbovid.net", "privatefiles.com", "vacishare.com", "xerver.co", "qshare.com", "tuxfile.com", "gigaup.fr", "hddspace.com", "foxishare.com", "filestube.com", "miloyski.com", "upload.hidemyass.com", "divxhosting.net", "sube.me", "quickupload.net", "mydisc.net", "isavelink.com", "filestorm.to", "cruzload.com",
            "bubblefiles.com", "dodane.pl", "myuplbox.com", "updown.bz", "thefilebay.com", "mukupload.com", "box4up.com", "5ilthy.com", "filepom.com", "dizzcloud.com", "cloudfly.us", "vidhog.com", "slingfile.com", "iperupload.com", "oceanus.ch", "mrmkv-fileshare.com", "filejungle.com", "fileserve.com", "zalil.ru", "uploadsat.com", "turbotransfer.pl", "uploadlab.com", "shareyourfile.biz", "tomwans.com", "megabox.ro", "failai.kava.lt", "stahovadlo.cz", "justin.tv", "4share.ws", "pliczek.net", "2gb-hosting.com", "maskfile.com", "rockdizfile.com", "yesload.net", "dotsemper.com", "mixbird.com", "myjizztube.com", "packupload.com", "megaszafa.com", "potload.com", "usefile.com", "nitrobits.com", "uploads.center", "filemonkey.in", "multishared.me", "wallbase.cc", "swankshare.com", "vidbox.yt", "vidzbeez.com", "megafiles.se", "fileom.com", "bluehaste.com", "socifiles.com", "mlfat4arab.com",
            "megashare.by", "flinzy.com", "filesend.net", "fastfileshare.com.ar", "ddlstorage.com", "spaadyshare.com", "sizfile.com", "hostingcup.com", "filecanyon.com", "bytesbox.com", "dropvideos.net", "tubecloud.net", "xrabbit.com", "tubeq.net", "multiupload.com", "pizzaupload.com", "ntupload.com", "magnovideo.com", "files.to", "moidisk.ru", "vidaru.com", "epicshare.net", "filemov.net", "hipfile.com", "zingload.com", "fileove.com", "rnbload.com", "filesaur.com", "wrzuc.to", "hyshare.com", "redload.net", "blitzfiles.com", "qkup.net", "lajusangat.net", "filechum.com", "uploadnetwork.eu", "videoslim.net", "filesfrog.net", "fiberupload.net", "hottera.com", "filego.org", "putme.org", "upshare.me", "uploadzeal.com", "space4file.com", "nzbload.com", "vreer.com", "safashare.com", "shareprofi.com", "crisshare.com", "lemuploads.com", "pandapla.net", "shurload.es", "megaupdown.com",
            "imagehaven.net", "filedap.com", "limevideo.net", "vids.bz", "wallobit.com", "davvas.com", "uploadinc.com", "filelaser.com", "finaload.com", "hostinoo.com", "sinhro.net", "megashare.com", "berofile.com", "uploadizer.net", "megarelease.org", "share-byte.net", "rapidstation.com", "filechin.com", "fujifile.me", "freeuploads.fr", "guizmodl.net", "file1.info", "maximusupload.com", "pandamemo.com", "igetfile.com", "megacloud.com", "egofiles.com", "dupload.net", "cloudvidz.net", "videozed.net", "donevideo.com", "migaload.com", "maxvideo.pl", "tgf-services.com", "albafile.com", "cramit.in", "4savefile.com", "batshare.com", "oleup.com", "filecopter.net", "easyfilesharing.info", "uploadedhd.com", "ilikefile.com", "fileomg.com", "file.am", "navihost.us", "upfile.biz", "dynaupload.com", "freevideo.cz", "file-speed.com", "cloudxeon.com", "hdplay.org", "files2upload.net", "dollyshare.com",
            "filetug.com", "mirorii.com", "fucktube.com", "faceporn.no", "bitload.it", "beatplexity.com", "bandbase.dk", "bananahost.it", "gigamax.ch", "goldfile.eu", "judgeporn.com", "elcorillord.org", "rocketfile.net", "maxisharing.com", "duckload.co", "homesexdaily.com", "pornbanana.com", "celebritycunt.net", "speedvid.tv", "filexb.com", "upsharez.com", "universalfilehosting.com", "4vid.me", "anyap.info", "evominds.com", "topvideo.cc", "zenfiles.biz", "fryhost.com", "turtleshare.com", "uploadjet.net", "devilstorage.com", "videofox.net", "extabit.com", "videofrog.eu", "nirafile.com", "omploader.org", "speedload.org", "sharefiles.co", "upit.in", "dump1.com", "fleon.me", "limelinx.com", "flashstream.in", "hotuploading.com", "filecity.net", "servifile.com", "filegag.com", "sharerun.com", "heftyfile.com", "uploadorb.com", "clipshouse.com", "edoc.com", "fileswappr.com", "filerace.com",
            "cloudzer.net", "netdrive.ws", "file4sharing.com", "rapidapk.com", "docyoushare.com", "kongsifile.com", "vureel.com", "dataport.cz", "uploadoz.com", "warserver.cz", "1clickshare.net", "cepzo.com", "fireuploads.net", "megaup1oad.net", "megaup.me", "ezzfile.com", "skylo.me", "filefolks.com", "videoslasher.com", "filebigz.com", "filezy.net", "vidx.to", "uploadstation.com", "wooupload.com", "ifile.ws", "filesbb.com", "ihostia.com", "youload.me", "ok2upload.com", "bitupload.com", "cobrashare.sk", "enjoybox.in", "share4files.com", "up.msrem.com", "megaload.it", "terafiles.net", "freestorage.ro", "filekai.com", "divxforevertr.com", "filekeeping.de", "livefile.org", "iranfilm16.com", "isharemybitch.com", "shufuni.com", "belgeler.com", "loadhero.net", "ngsfile.com", "1-clickshare.com", "fastsonic.net", "brutalsha.re", "moviesnxs.com", "hotfile.com", "sharedbit.net", "ufox.com",
            "comload.net", "6ybh-upload.com", "cloudnes.com", "fileprohost.com", "cyberlocker.ch", "filebox.com", "x7files.com", "videozer.com", "megabitshare.com", "filestay.com", "uplly.com", "asixfiles.com", "zefile.com", "kingsupload.com", "fileking.co", "sharevid.co", "4fastfile.com", "1-upload.com", "dump.ro", "dippic.com", "uploking.com", "zshare.ma", "book-mark.net", "ginbig.com", "ddl.mn", "syfiles.com", "iuploadfiles.com", "thexyz.net", "zakachali.com", "indianpornvid.com", "hotfiles.ws", "wizzupload.com", "banashare.com", "downupload.com", "putshare.com", "vidbox.net", "filetube.to", "nowveo.com", "uploadic.com", "flashdrive.it", "flashdrive.uk.com", "filewinds.com", "wrzucaj.com", "yourfilestorage.com", "toucansharing.com", "uploaddot.com", "zooupload.com", "uploadcore.com", "spaceha.com", "tubethumbs.com", "peeje.com", "datacloud.to", "xxxmsncam.com", "uploadboxs.com",
            "247upload.com", "fileshare.in.ua", "upload.tc", "filesmall.com", "fileuplo.de", "quakefile.com", "vdoreel.com", "flazhshare.com", "upmorefiles.com", "cloudyload.com", "icyfiles.com", "vidpe.com", "clouds.to", "zuzufile.com", "hostfil.es", "onlinedisk.ru", "fileduct.com", "frogup.com", "filejumbo.com", "dump.ru", "fileshawk.com", "vidstream.us", "filezpro.com", "fileupper.com", "speedy-share.net", "files.ge", "gbitfiles.com", "xtilourbano.info", "allbox4.com", "arab-box.com", "farmupload.com", "filedefend.com", "filesega.com", "kupload.org", "multishare.org", "98file.com", "wantload.com", "esnips.com", "uload.to", "share76.com", "filemates.com", "stahnu.to", "filestock.ru", "uploader.pl", "mach2upload.com", "megaunload.net", "bonpoo.com", "modovideo.com", "bitoman.ru", "maknyos.com", "upgrand.com", "pigsonic.com", "filevelocity.com", "filegaze.com", "ddldrive.com",
            "fileforth.com", "files-save.com", "media-4.me", "backupload.net", "upafacil.com", "filedownloads.org", "filesector.cc", "netuploaded.com", "squillion.com", "sharebees.com", "filetobox.com", "mojedata.sk", "grupload.com", "stickam.com", "gimmedatnewjoint.com", "dup.co.il", "eazyupload.net", "depoindir.com", "own3d.tv", "drop.st", "favupload.com", "anonstream.com", "odsiebie.pl", "shareupload.com", "filebeer.info", "uploadfloor.com", "venusfile.com", "welload.com", "upaj.pl", "shareupload.net", "tsarfile.com", "omegave.org", "fsx.hu", "kiwiload.com", "gbmeister.com", "filesharing88.net", "fileza.net", "filecloud.ws", "filesome.com", "filehost.ws", "filemade.com", "bloonga.com", "zettaupload.com", "aavg.net", "freeporn.com", "bitbonus.com", "sharebeats.com", "vidhost.me", "filetechnology.com", "badongo.com", "uptorch.com", "videoveeb.com", "fileupped.com", "repofile.com",
            "filemsg.com", "dopeshare.com", "filefat.com", "fileplayground.com", "fileor.com", "aieshare.com", "q4share.com", "share-now.net", "1hostclick.com", "mummyfile.com", "hsupload.com", "upthe.net", "ufile.eu", "bitroad.net", "coolshare.cz", "speedfile.cz", "your-filehosting.com", "brontofile.com", "filestrum.com", "filedove.com", "sharpfile.com", "filerose.com", "filereactor.com", "boltsharing.com", "turboupload.com", "glumbouploads.com", "terabit.to", "buckshare.com", "zeusupload.com", "filedino.com", "filedude.com", "uptal.org", "uptal.net", "file-bit.net", "xtshare.com", "cosaupload.org", "sharing-online.com", "filestrack.com", "shareator.net", "azushare.net", "filecosy.com", "monsteruploads.eu", "vidhuge.com", "doneshare.com", "cixup.com", "animegoon.com", "supermov.com", "ufliq.com", "vidreel.com", "deditv.com", "supershare.net", "shareshared.com", "uploadville.com",
            "fileserver.cc", "bebasupload.com", "savefile.ro", "ovfile.com", "divxbase.com", "gptfile.com", "dudupload.com", "eyvx.com", "farshare.to", "azsharing.com", "freefilessharing.com", "elitedisk.com", "freakmov.com", "cloudnator.com", "filesavr.com", "saveufile.in.th", "migahost.com", "fastfreefilehosting.com", "files2k.eu", "shafiles.me", "jalurcepat.com", "divload.org", "refile.net", "oron.com", "wupload.com", "filesonic.com", "xxlupload.com", "cumfox.com", "pyramidfiles.com", "nahraj.cz", "jsharer.com", "annonhost.net", "filekeeper.org", "dynyoo.com", "163pan.com", "imagehost.org", "4us.to", "yabadaba.ru", "madshare.com", "diglo.com", "tubeload.to", "tunabox.net", "yourfilehost.com", "uploadegg.com", "brsbox.com", "amateurboobtube.com", "good.net", "freeload.to", "netporn.nl", "przeklej.pl", "alldrives.ge", "allshares.ge", "holderfile.com", "megashare.vnn.vn", "link.ge",
            "up.jeje.ge", "up-4.com", "cloudcache", "ddlanime.com", "mountfile.com", "platinshare.com", "megavideo.com", "megaupload.com", "megaporn.com", "zshare.net", "uploading4u.com", "megafree.kz", "batubia.com", "upload24.net", "files.namba.kz", "datumbit.com", "fik1.com", "fileape.com", "filezzz.com", "imagewaste.com", "fyels.com", "gotupload.com", "sharehub.com", "sharehut.com", "filesurf.ru", "openfile.ru", "letitfile.ru", "tab.net.ua", "uploadbox.com", "supashare.net", "usershare.net", "skipfile.com", "10upload.com", "x7.to", "uploadking.com", "uploadhere.com", "fileshaker.com", "vistaupload.com", "groovefile.com", "enterupload.com", "xshareware.com", "xun6.com", "yourupload.de", "youshare.eu", "mafiaupload.com", "addat.hu", "archiv.to", "bigupload.com", "biggerupload.com", "bitload.com", "bufiles.com", "cash-file.net", "combozip.com", "duckload.com", "exoshare.com",
            "file2upload.net", "filebase.to", "filebling.com", "filecrown.com", "filefrog.to", "filefront.com", "filehook.com", "filestage.to", "filezup.com", "fullshare.net", "gaiafile.com", "keepfile.com", "kewlshare.com", "lizshare.net", "loaded.it", "loadfiles.in", "megarapid.eu", "megashare.vn", "metahyper.com", "missupload.com", "netstorer.com", "nextgenvidz.com", "piggyshare.com", "profitupload.com", "quickload.to", "quickyshare.com", "share.cx", "sharehoster.de", "shareua.com", "speedload.to", "upfile.in", "ugotfile.com", "upload.ge", "uploadmachine.com", "uploady.to", "uploadstore.net", "vspace.cc", "web-share.net", "yvh.cc", "x-files.kz", "oteupload.com", "vidabc.com", "catshare.org", "javmon.com", "xtwisted.com", "sharenxs.com", "anyfiles.pl", "nowvideo.to", "rapidshare.ru", "watchgfporn.com", "mygirlfriendporn.com", "wrzuta.pl", "jizzhut.com", "fistfast.com", "sexoquente.tv",
            "imgchili.com", "sexix.net", "sexvidx.tv", "pornimagex.com", "porndreamer.com", "wickedcloud.io", "filehd.host", "hulkimge.com" };

    public static String[] getAnnotationNames() {
        return domains;
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String name : domains) {
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + "(?:" + Pattern.quote(name) + ").+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * fullbuild help comment
     *
     * @param wrapper
     */
    public Offline(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls != null) {
            for (final DownloadLink link : urls) {
                link.setAvailable(false);
                link.setComment("Permanently Offline: Host provider no longer exists");
            }
        }
        return true;
    }

    @Override
    public boolean isPremiumEnabled() {
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Permanently Offline: Host provider no longer exists", PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public void reset() {
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.INTERNAL };
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}