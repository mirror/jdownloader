//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.JoinPeerTubeOrgConfig;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class JoinPeerTubeOrg extends antiDDoSForHost {
    public JoinPeerTubeOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               mp4dllink         = null;
    private String               m3u8dllink        = null;
    private boolean              server_issues     = false;
    /* Plugin/Packagizer properties */
    private static final String  PROPERTY_TITLE    = "title";
    private static final String  PROPERTY_UPLOADER = "uploader";
    private static final String  PROPERTY_DATE     = "date";

    @Override
    public String getAGBLink() {
        return "https://instances.joinpeertube.org/instances";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), true) + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
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
                "peertube.pi2.dev", "video.pony.gallery", "tube.skrep.in", "tube.others.social", "tube-poitiers.beta.education.fr", "peertube.satoshishop.de", "streamsource.video", "vid.wildeboer.net", "battlepenguin.video", "peertube.cloud.sans.pub", "tube.vraphim.com", "refuznik.video", "tube.shela.nu", "video.1146.nohost.me", "peertube.davigge.com", "videos.tankernn.eu", "vod.ksite.de", "tube.grin.hu", "peertube.swarm.solvingmaz.es", "videos.fsci.in", "media.inno3.cricket", "video.livecchi.cloud", "tube.cryptography.dog", "peertube.zergy.net", "vid.ncrypt.at", "watch.krazy.party", "videos.tcit.fr", "video.valme.io", "peertube.patapouf.xyz", "video.violoncello.ch", "peertube.gidikroon.eu", "tubedu.org", "tv.netwhood.online", "watch.breadtube.tv", "video.exodus-privacy.eu.org", "peertube.social", "vidcommons.org", "auf1.tv", "tube.porn3dx.com", "framatube.org", "gegenstimme.tv",
                "tube.network.europa.eu", "alttube.fr" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:videos/(?:watch|embed)|w)/([\\w+\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Debug function which can find new instances compatible with this code/plugin/template from:
     * https://instances.joinpeertube.org/instances </br>
     * Important: Do NOT overwrite old entries with these ones! Looks like this list is not reliably collecting "all" peertube instances
     * and/or single peertube instances can turn off some kind of "allow my instance to appear on tht list" setting!
     */
    private static ArrayList<String> findNewScriptInstances() {
        if (false) {
            // don't compile into class file
            final ArrayList<String> existingInstances = getAllSupportedPluginDomainsFlat();
            final ArrayList<String> newInstances = new ArrayList<String>();
            final Browser br = new Browser();
            final int itemsPerRequest = 50;
            int index = 0;
            int totalNumberofItems = 0;
            List<Object> ressourcelist = null;
            do {
                try {
                    br.getPage("https://instances.joinpeertube.org/api/v1/instances?start=" + index + "&count=" + itemsPerRequest + "&sort=-createdAt");
                    Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (index == 0) {
                        totalNumberofItems = ((Number) entries.get("total")).intValue();
                    }
                    ressourcelist = (List<Object>) entries.get("data");
                    for (final Object siteO : ressourcelist) {
                        entries = (Map<String, Object>) siteO;
                        String host = (String) entries.get("host");
                        // final String siteVersion = (String) entries.get("version");
                        if (StringUtils.isEmpty(host)) {
                            continue;
                        }
                        host = host.replace("www.", "");
                        if (!existingInstances.contains(host)) {
                            newInstances.add(host);
                        }
                    }
                } catch (final Throwable e) {
                    /* Stop on unknown errors */
                    e.printStackTrace();
                    break;
                }
                index += itemsPerRequest;
            } while (ressourcelist != null && ressourcelist.size() == itemsPerRequest);
            String hostsStr = "ret.add(new String[] { ";
            for (final String newHost : newInstances) {
                /* Outputs Java code to easily add new instances */
                // System.out.println(newHost + ",");
                hostsStr += "\"" + newHost + "\", ";
            }
            hostsStr += " });";
            System.out.println(hostsStr);
            /* TODO: Maybe add a check for missing/offline instances too */
            return newInstances;
        } else {
            return null;
        }
    }

    /** Using API: https://docs.joinpeertube.org/api-rest-reference.html */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(this.getFID(link) + ".mp4");
        }
        mp4dllink = null;
        m3u8dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        final String host = Browser.getHost(link.getPluginPatternMatcher(), true);
        final Browser brc = br.cloneBrowser();
        getPage(brc, "https://" + host + "/api/v1/videos/" + this.getFID(link));
        if (brc.getHttpConnection().getResponseCode() == 400) {
            /* Can e.g. happen when bad video parameter is used e.g. https://alttube.fr/w/p */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (brc.getHttpConnection().getResponseCode() == 404) {
            /* 2020-07-03: E.g. {"error":"Video not found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        checkErrorsAPI(brc);
        final Map<String, Object> root = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        String description = (String) root.get("description");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            /* Description is truncated sometimes --> Grab full description if needed */
            if (description.endsWith("...")) {
                logger.info("Description seems to be truncated -> Trying to fetch full description");
                brc.getPage(brc.getURL() + "/description");
                final Map<String, Object> descriptionInfo = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                final Object descriptionFull = descriptionInfo.get("description");
                if (descriptionFull instanceof String) {
                    description = descriptionFull.toString();
                } else {
                    logger.warning("Failed to fetch full description");
                }
            }
            link.setComment(description);
        }
        parseMetadataAndSetFilename(link, root);
        /* Grab highest quality downloadurl + filesize */
        final List<Map<String, Object>> oficialDownloads = (List<Map<String, Object>>) root.get("files");
        if (!oficialDownloads.isEmpty()) {
            final Map<String, Object> downloadInfo = oficialDownloads.get(0);
            this.mp4dllink = (String) downloadInfo.get("fileDownloadUrl");
            final Number filesize = (Number) downloadInfo.get("size");
            if (filesize != null) {
                link.setDownloadSize(filesize.longValue());
            }
        } else {
            logger.info("Official download impossible, checking stream variants");
            final List<Map<String, Object>> streamingPlaylists = (List<Map<String, Object>>) root.get("streamingPlaylists");
            if (!streamingPlaylists.isEmpty()) {
                playlistLoop: for (final Map<String, Object> streamPlaylist : streamingPlaylists) {
                    m3u8dllink = (String) streamPlaylist.get("playlistUrl");
                    /* Expect array to be pre-sorted best to worst quality. */
                    final List<Map<String, Object>> streams = (List<Map<String, Object>>) streamPlaylist.get("files");
                    for (final Map<String, Object> stream : streams) {
                        final Map<String, Object> resolution = (Map<String, Object>) stream.get("resolution");
                        final String thisDownloadlink = (String) stream.get("fileDownloadUrl");
                        if (!StringUtils.isEmpty(thisDownloadlink)) {
                            this.mp4dllink = thisDownloadlink;
                            final long filesize = ((Number) stream.get("size")).longValue();
                            if (filesize > 0) {
                                if (PluginJsonConfig.get(JoinPeerTubeOrgConfig.class).isPreferHLS()) {
                                    /* HLS download size will be different. */
                                    link.setDownloadSize(filesize);
                                } else {
                                    link.setVerifiedFileSize(filesize);
                                }
                            }
                            logger.info("Selected quality: " + resolution.get("label"));
                            break playlistLoop;
                        }
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static void parseMetadataAndSetFilename(final DownloadLink link, final Map<String, Object> entries) {
        final String host = Browser.getHost(link.getPluginPatternMatcher());
        String title = (String) entries.get("name");
        final String createdAt = (String) entries.get("createdAt");
        final String uploader = (String) JavaScriptEngineFactory.walkJson(entries, "account/name");
        final String formattedDate = new Regex(createdAt, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String description = (String) entries.get("description");
        if (description != null && true) {
            link.setComment(description);
        }
        if (!StringUtils.isEmpty(title)) {
            link.setProperty(PROPERTY_TITLE, title);
            String filename = "";
            if (!StringUtils.isEmpty(formattedDate)) {
                link.setProperty(PROPERTY_DATE, formattedDate);
                filename += formattedDate + "_";
            }
            filename += host.replace(".", "_") + "_";
            if (!StringUtils.isEmpty(uploader)) {
                link.setProperty(PROPERTY_UPLOADER, uploader);
                filename += uploader + "_";
            }
            filename += title + ".mp4";
            link.setFinalFileName(filename);
        }
    }

    protected void checkErrorsAPI(final Browser br) throws PluginException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String errorMsg = (String) entries.get("error");
        if (!StringUtils.isEmpty(errorMsg)) {
            if (errorMsg.equalsIgnoreCase("Video not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Unknown error */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (PluginJsonConfig.get(JoinPeerTubeOrgConfig.class).isPreferHLS()) {
            if (StringUtils.isEmpty(m3u8dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(this.m3u8dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            if (StringUtils.isEmpty(mp4dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, mp4dllink, free_resume, free_maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PeerTube;
    }

    @Override
    public String getHost(final DownloadLink link, final Account account, boolean includeSubdomain) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), includeSubdomain);
        } else if (account != null) {
            return account.getHoster();
        } else {
            return super.getHost(link, account, includeSubdomain);
        }
    }

    @Override
    public Class<? extends JoinPeerTubeOrgConfig> getConfigInterface() {
        return JoinPeerTubeOrgConfig.class;
    }
}
