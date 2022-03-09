//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.JoinPeerTubeOrg;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PeerTubeCrawler extends PluginForDecrypt {
    public PeerTubeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Sync this list between hoster- and decrypterplugin! */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "joinpeertube.org", "thevideoverse.com", "tube.emy.plus", "video.jigmedatse.com", "video.amiga-ng.org", "video.berdi.xyz", "peertube.ssvo.de", "video.stackmatic.org", "peertube.espace.si", "videos.supertuxkart.net", "video.oliwaisfreeman.nohost.me", "pt.borgcube.eu", "tube.chaouane.xyz", "mp-tube.de", "oldpcmuseum.ru", "video.file.tube", "skeptube.fr", "socialwebtube.com", "tube.vvv.cash", "zergud.com", "peertube.soykaf.org", "kraut.zone", "video.asgardius.company", "tube.borked.host", "birkeundnymphe.de", "birkeundnymphe.de", "video.artist.cx", "v.kisombrella.top", "peertube.w.utnw.de", "tube.sp-codes.de", "tube.apolut.net", "myworkoutarenapeertube.cf", "tpaw.video", "peertube.dtmf.ca", "stream.shahab.nohost.me", "tube.mfraters.net", "tube.pyngu.com", "peertube.troback.com", "peertube.ucy.de", "peertube.aukfood.net", "peertube.bridaahost.ynh.fr",
                "peertube.myrasp.eu", "peertube.freetalklive.com", "watch.softinio.com", "peertube.plataformess.org", "vid.pravdastalina.info", "peertube.bitsandlinux.com", "tv1.gomntu.space", "phijkchu.com", "tube.arthack.nz", "tv.atmx.ca", "peertube.cybercirujas.club", "toob.bub.org", "pt.maciej.website", "tube.superseriousbusiness.org", "videos.petch.rocks", "kino.kompot.si", "play.rosano.ca", "tube.kockatoo.org", "peertube.cabaal.net", "nastub.cz", "sovran.video", "v.xxxapex.com", "tube.odat.xyz", "stream.k-prod.fr", "peertube.fenarinarsa.com", "peertube.art3mis.de", "tube.tylerdavis.xyz", "video.ethantheenigma.me", "arcana.fun", "peertube.ecologie.bzh", "peertube.atsuchan.page", "peertube.vlaki.cz", "video-cave-v2.de", "peertube.keazilla.net", "vids.tekdmn.me", "piraten.space", "offenes.tv", "peertube.arbleizez.bzh", "tube.bstly.de", "web-fellow.de", "peertube.mattone.net",
                "ptb.lunarviews.net", "ovaltube.codinglab.ch", "video.wilkie.how", "video.wsf2021.info", "videorelay.co", "auf1.eu", "tube.toontoet.nl", "libertynode.tv", "video.gyt.is", "peertube.0x5e.eu", "turkum.me", "videos.denshi.live", "peertube.jensdiemer.de", "peertube.bubbletea.dev", "tube.futuretic.fr", "libra.syntazia.org", "peertube.thenewoil.xyz", "peertube.beeldengeluid.nl", "tv.lumbung.space", "vid.dascoyote.xyz", "peertube.cuatrolibertades.org", "orgdup.media", "pocketnetpeertube1.nohost.me", "live.toobnix.org", "videos.hush.is", "tube.ebin.club", "tube.wien.rocks", "tube.tpshd.de", "tube.cowfee.moe", "video.ozgurkon.org", "video.progressiv.dev", "tube.s1gm4.eu", "s1.gegenstimme.tv", "peertube.nz", "pocketnetpeertube4.nohost.me", "comf.tube", "peertube.orthus.link", "pocketnetpeertube6.nohost.me", "peertube.freenet.ru", "pocketnetpeertube5.nohost.me", "peertube.biz",
                "peertube.radres.xyz", "video.rw501.de", "darkvapor.nohost.me", "tube.chaoszone.tv", "pt.fedi.tech", "peertube.rtnkv.cloud", "media.over-world.org", "tube.avensio.de", "peertube.klaewyss.fr", "videos.npo.city", "tube.azkware.net", "video.cpn.so", "sender-fm.veezee.tube", "peertube.takeko.cyou", "hyperreal.tube", "peertube.us.to", "peertube.kalua.im", "tv.undersco.re", "video.cm-en-transition.fr", "twctube.twc-zone.eu", "video.balsillie.net", "tv.neue.city", "tv.piejacker.net", "peertube.nebelcloud.de", "videos.shmalls.pw", "peertube.chrisspiegl.com", "tube.tardis.world", "video.shitposter.club", "tv.mattchristiansenmedia.com", "tube.dsocialize.net", "tube.hackerscop.org", "videos.capas.se", "peertube.kx.studio", "v.sil.sh", "videos.3d-wolf.com", "tube.octaplex.net", "video.076.ne.jp", "stream.elven.pw", "re-wizja.re-medium.com", "my.bunny.cafe", "videos.rampin.org",
                "peertube.le5emeaxe.fr", "bitcointv.com", "videos.lucero.top", "media.gzevd.de", "video.resolutions.it", "tube.cms.garden", "a.metube.ch", "peertube.luckow.org", "peertube.donnadieu.fr", "mani.tube", "video.linuxtrent.it", "video.demokratischer-sommer.de", "tube.bachaner.fr", "video.comune.trento.it", "peertube.red", "tube.org.il", "tv.generallyrubbish.net.au", "tv.pirateradio.social", "watch.rt4mn.org", "peertube.eu.org", "peertube.chevro.fr", "tube.frischesicht.de", "videos.traumaheilung.net", "videos.alexandrebadalo.pt", "conspiracydistillery.com", "peertube.chemnitz.freifunk.net", "lolitube.freedomchan.moe", "myfreetube.de", "video.apps.thedoodleproject.net", "peertube.lavallee.tech", "hpstube.fr", "video.blast-info.fr", "peertube.bubuit.net", "tube.aerztefueraufklaerung.de", "tube.vigilian-consulting.nl", "video.cybre.town", "peertube.portaesgnos.org",
                "the.jokertv.eu", "climatejustice.video", "wikileaks.video", "video.cnt.social", "fair.tube", "tube.lokad.com", "videos.benjaminbrady.ie", "peertube.bgzashtita.es", "vid.rajeshtaylor.com", "video.binarydad.com", "tube.as211696.net", "pierre.tube", "tube.pmj.rocks", "peertube.boba.best", "gary.vger.cloud", "video.guerredeclasse.fr", "tube.wehost.lgbt", "tube.novg.net", "ptmir2.inter21.net", "ptmir5.inter21.net", "ptmir4.inter21.net", "ptmir3.inter21.net", "peertube.habets.house", "tube.arkhalabs.io", "ptube.xmanifesto.club", "tube.yapbreak.fr", "peertube.ctseuro.com", "spectra.video", "live.nanao.moe", "peertube.inapurna.org", "peertube.librenet.co.za", "watch.libertaria.space", "video.triplea.fr", "videos.monstro1.com", "tube.darknight-coffee.org", "video.catgirl.biz", "peertube.westring.digital", "peertube.get-racing.de", "peertube.hackerfraternity.org",
                "vulgarisation-informatique.fr", "videos.thisishowidontdisappear.com", "video.islameye.com", "tube.kotur.org", "livegram.net", "tube.motuhake.xyz", "v.szy.io", "video.veloma.org", "video.liege.bike", "regarder.sans.pub", "tube.rhythms-of-resistance.org", "tube-bordeaux.beta.education.fr", "peertube.am-networks.fr", "video.lespoesiesdheloise.fr", "peertube.luga.at", "peertube.fomin.site", "peertube.joffreyverd.fr", "peertube.swrs.net", "wwtube.net", "tube.shanti.cafe", "videos.cloudron.io", "vid.werefox.dev", "tube.seditio.fr", "video.thinkof.name", "video.p3x.de", "video.codingfield.com", "tv.adn.life", "peertube.functional.cafe", "peertube.br0.fr", "video.bards.online", "peertube.semweb.pro", "video.toot.pt", "videos.archigny.net", "peertube.logilab.fr", "peertube.gruezishop.ch", "tube.nchoco.net", "videos.pzelawski.xyz", "peertube.zoz-serv.org", "vid.qorg11.net",
                "pt.apathy.top", "videos.stadtfabrikanten.org", "archive.vidicon.org", "peertube.gargantia.fr", "peertube.ignifi.me", "tube.melonbread.xyz", "tube.grap.coop", "webtv.vandoeuvre.net", "peertube.european-pirates.eu", "video.potate.space", "video.lunasqu.ee", "video.fhtagn.org", "tube.bmesh.org", "videos.scanlines.xyz", "kirche.peertube-host.de", "v.lor.sh", "beertube.epgn.ch", "peertube.be", "grypstube.uni-greifswald.de", "wiwi.video", "peertube.cats-home.net", "peertube.tv", "video.soi.ch", "peertube.newsocial.tech", "peertube.cpge-brizeux.fr", "sleepy.tube", "tube.distrilab.fr", "kinowolnosc.pl", "videos.trom.tf", "advtv.ml", "videos.john-livingston.fr", "htp.live", "melsungen.peertube-host.de", "evangelisch.video", "tube.anufrij.de", "tube.foxarmy.ml", "videos.mastodont.cat", "captain-german.com", "graeber.video", "flim.txmn.tk", "media.undeadnetwork.de",
                "peertube.kathryl.fr", "eduvid.org", "tube.dragonpsi.xyz", "veezee.tube", "peertube.nicolastissot.fr", "tube.alexx.ml", "s2.veezee.tube", "tubes.jodh.us", "lucarne.balsamine.be", "tube.lucie-philou.com", "video.odayacres.farm", "tube.schule.social", "unfilter.tube", "tube.systest.eu", "tube.xd0.de", "tube.xy-space.de", "peertube.lagob.fr", "studios.racer159.com", "exo.tube", "mirametube.fr", "fediverse.tv", "video.kicik.fr", "xxivproduction.video", "sdmtube.fr", "digitalcourage.video", "media.kaitaia.life", "tvox.ru", "video.skyn3t.in", "video.kuba-orlik.name", "video.vaku.org.ua", "peer.azurs.fr", "sickstream.net", "video.ecole-89.com", "tube.kai-stuht.com", "video.fbxl.net", "live.libratoi.org", "video.p1ng0ut.social", "vid.samtripoli.com", "watch.deranalyst.ch", "videos.sibear.fr", "video.discord-insoumis.fr", "video.kyushojitsu.ca", "peertube.forsud.be",
                "video.pcf.fr", "kumi.tube", "tube.rsi.cnr.it", "peertube.dc.pini.fr", "befree.nohost.me", "vs.uniter.network", "watch.ignorance.eu", "tube.schleuss.online", "tube.saumon.io", "videos.judrey.eu", "theater.ethernia.net", "alimulama.com", "watch.tubelab.video", "lastbreach.tv", "tube.abolivier.bzh", "video.coales.co", "tuba.lhub.pl", "truetube.media", "film.k-prod.fr", "peertube.francoispelletier.org", "videos.danksquad.org", "v.phreedom.club", "peertube.tweb.tv", "peertube.lestutosdeprocessus.fr", "videos-passages.huma-num.fr", "video.mycrowd.ca", "kodcast.com", "video.altertek.org", "ruraletv.ovh", "videos.weblib.re", "tube.oisux.org", "peertube.louisematic.site", "tv2.cocu.cc", "tv1.cocu.cc", "tube.dev.lhub.pl", "periscope.numenaute.org", "clap.nerv-project.eu", "peertube.kodcast.com", "tube.lacaveatonton.ovh", "peertube.tspu.edu.ru", "p.lu", "serv3.wiki-tube.de",
                "serv1.wiki-tube.de", "video.lavolte.net", "peertube.r5c3.fr", "mountaintown.video", "ptmir1.inter21.net", "tube.foxden.party", "vod.lumikko.dev", "fotogramas.politicaconciencia.org", "peertube.manalejandro.com", "tube.mrbesen.de", "www4.mir.inter21.net", "tgi.hosted.spacebear.ee", "peertube.genma.fr", "video.csc49.fr", "tube.wolfe.casa", "tube.linkse.media", "video.dresden.network", "peertube.zapashcanon.fr", "maindreieck-tv.de", "40two.tube", "tube.amic37.fr", "video.comptoir.net", "peertube.tux.ovh", "kino.schuerz.at", "peertube.tiennot.net", "tututu.tube", "peertube.interhop.org", "tube.picasoft.net", "wiki-tube.de", "video.stuartbrand.co.uk", "video.internet-czas-dzialac.pl", "peertube.cythin.com", "thecool.tube", "thaitube.in.th", "tv.bitma.st", "peertube.chtisurel.net", "videos.testimonia.org", "peertube.informaction.info", "video.mass-trespass.uk",
                "v.lastorder.xyz", "daschauher.aksel.rocks", "video.mstddntfdn.online", "tube.cyano.at", "media.skewed.de", "visionon.tv", "peertube.securitymadein.lu", "video.linux.it", "peertube.aventer.biz", "tuktube.com", "v.basspistol.org", "libremedia.video", "mojotube.net", "mytube.kn-cloud.de", "video.nogafam.es", "peertube.stream", "videos.leslionsfloorball.fr", "player.ojamajo.moe", "ftsi.ru", "video.cigliola.com", "xxx.noho.st", "peertube.stefofficiel.me", "video.eradicatinglove.xyz", "canard.tube", "videos.jordanwarne.xyz", "video.anartist.org", "tube.jeena.net", "video.mundodesconocido.com", "video.screamer.wiki", "tube.cloud-libre.eu", "videos.coletivos.org", "videos.wakkerewereld.nu", "peertube.travelpandas.eu", "peertube.runfox.tk", "video.pourpenser.pro", "video.sdm-tools.net", "peertube.anzui.dev", "video.up.edu.ph", "video.igem.org", "worldofvids.com",
                "peertube.pi2.dev", "video.pony.gallery", "tube.skrep.in", "tube.others.social", "tube-poitiers.beta.education.fr", "peertube.satoshishop.de", "streamsource.video", "vid.wildeboer.net", "battlepenguin.video", "peertube.cloud.sans.pub", "tube.vraphim.com", "refuznik.video", "tube.shela.nu", "video.1146.nohost.me", "peertube.davigge.com", "videos.tankernn.eu", "vod.ksite.de", "tube.grin.hu", "peertube.swarm.solvingmaz.es", "videos.fsci.in", "media.inno3.cricket", "video.livecchi.cloud", "tube.cryptography.dog", "peertube.zergy.net", "vid.ncrypt.at", "watch.krazy.party", "videos.tcit.fr", "video.valme.io", "peertube.patapouf.xyz", "video.violoncello.ch", "peertube.gidikroon.eu", "tubedu.org", "tv.netwhood.online", "watch.breadtube.tv", "video.exodus-privacy.eu.org", "peertube.social", "vidcommons.org", "auf1.tv", "tube.porn3dx.com", "framatube.org", "gegenstimme.tv" });
        return ret;
    }

    /** Returns content of getPluginDomains as single dimensional Array. */
    public static ArrayList<String> getAllSupportedPluginDomainsFlat() {
        ArrayList<String> allDomains = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            for (final String singleDomain : domains) {
                allDomains.add(singleDomain);
            }
        }
        return allDomains;
    }

    public static String[] getAnnotationNames() {
        return new String[] { "joinpeertube.org" };
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:a|c)/([^/]+)/videos");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String host = Browser.getHost(param.getCryptedUrl(), true);
        final String channelName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String channelNameUrlencoded = Encoding.urlEncode(channelName);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("Referer", "https://" + host + "/c/" + channelNameUrlencoded + "/videos?s=1");
        br.getPage("https://" + host + "/api/v1/video-channels/" + channelNameUrlencoded);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> channel = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        br.getPage("/api/v1/video-channels/" + channelName + "/videos?start=0&count=0&sort=-publishedAt");
        final Map<String, Object> channelVideosInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final int totalNumberofVideos = ((Number) channelVideosInfo.get("total")).intValue();
        if (totalNumberofVideos == 0) {
            final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "EMPTY_CHANNEL_" + channelName, "This channel contains no videos.");
            decryptedLinks.add(dummy);
            return decryptedLinks;
        }
        final String channelDescription = (String) channel.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(channelName);
        if (!StringUtils.isEmpty(channelDescription)) {
            fp.setComment(channelDescription);
        }
        final int maxItemsPerRequest = 25;
        int page = 1;
        int index = 0;
        final Set<String> dupes = new HashSet<String>();
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        channelLoop: do {
            final UrlQuery query = new UrlQuery();
            query.add("start", Integer.toString(index));
            query.add("count", Integer.toString(maxItemsPerRequest));
            query.add("sort", "-publishedAt");
            query.add("skipCount", "true");
            query.add("nsfw", "both");
            br.getPage("/api/v1/video-channels/" + channelNameUrlencoded + "/videos?" + query.toString());
            final Map<String, Object> channelVideosResponse = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Map<String, Object>> videoMaps = (List<Map<String, Object>>) channelVideosResponse.get("data");
            for (final Map<String, Object> videoMap : videoMaps) {
                String videoURL = (String) videoMap.get("url");
                if (videoURL == null) {
                    /* Not always given e.g. visionon.tv */
                    final String embedPath = (String) videoMap.get("embedPath");
                    if (!StringUtils.isEmpty(embedPath)) {
                        videoURL = br.getURL(embedPath).toString();
                    }
                }
                if (!plg.canHandle(videoURL)) {
                    logger.warning("Stopping because: Found unsupported URL! Developer work needed! URL: " + videoURL);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!dupes.add(videoURL)) {
                    /* Additional fail-safe */
                    logger.info("Stopping because: Detected dupe");
                    break channelLoop;
                }
                final DownloadLink video = this.createDownloadlink(videoURL);
                JoinPeerTubeOrg.parseMetadataAndSetFilename(video, videoMap);
                video.setAvailable(true);
                video._setFilePackage(fp);
                decryptedLinks.add(video);
                distribute(video);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + decryptedLinks.size() + "/" + totalNumberofVideos);
            if (decryptedLinks.size() >= totalNumberofVideos) {
                logger.info("Stopping because: Found all items");
                break;
            } else if (videoMaps.size() < maxItemsPerRequest) {
                /* Additional fail-safe */
                logger.info("Stopping because: Reached last page");
                break;
            } else if (this.isAbort()) {
                break;
            }
            page++;
            index += videoMaps.size();
        } while (true);
        if (decryptedLinks.size() < totalNumberofVideos) {
            /* This should never happen */
            logger.warning("Some videos are missing: " + (totalNumberofVideos - decryptedLinks.size()));
        }
        return decryptedLinks;
    }
}
