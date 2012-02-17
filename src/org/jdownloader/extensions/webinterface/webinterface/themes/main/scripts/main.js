$(document).ready(function() {
	function helpers() {	
		Handlebars.registerHelper('list', function(context, block) {
			var ret = "";
			for(var i=0, j=context.length; i<j; i++) {
				ret = ret + block(context[i]);
			}
			return ret;
		});
	}
	function get_page() {
		var head = Handlebars.compile($("#head").html());
		pagecontent = { 
			"scripts": [
				{"src": "jquery.ui.js"},
				{"src": "jquery.jdapi.js"},
				{"src": "jquery.jgrowl.js"},
				{"src": "jquery.history.js"},
				{"src": "handlers/AccountHandler.js"},
				{"src": "handlers/CaptchaHandler.js"},
				{"src": "handlers/LoginHandler.js"},
				{"src": "main.jd.js"}
			],
			"css": [
				{"src": "fluid.css"},
				{"src": "jui/jquery.ui.css"},
				{"src": "jd.theme.css"},
				{"src": "jd.style.css"},
				{"src": "icons/icons.css"},
				{"src": "jquery.jgrowl.css"}
				
			],
			"nav": [
				{ "id": "nav_dashboard", "href": "#dashboard", "icon": "i-home", "value": $.i18n._('nav.dashboard'), "extra": "" },
				{ "id": "nav_downloads", "href": "#downloads", "icon": "i-arrow-down", "value": $.i18n._('nav.downloads'), "extra": "" },
				{ "id": "nav_linkadd", "href": "#linkadd", "icon": "i-plus", "value": $.i18n._('nav.grabber'), "extra": "" },
				{ "id": "nav_passwd", "href": "#passwd", "icon": "i-unlocked", "value": $.i18n._('nav.passwords'), "extra": "" },
				{ "id": "nav_accounts", "href": "#accounts", "icon": "i-key-2", "value": $.i18n._('nav.accounts'), "extra": "" },
				{ "id": "nav_settings", "href": "#settings", "icon": "i-cog-5", "value": $.i18n._('nav.settings'), "extra": "" }
			],
			"dashboard_reports": [
				{ "icon": "ic-accept", "title": $.i18n._('dashboard.reports.successfull'), "value": "324" },
				{ "icon": "ic-cancel", "title": $.i18n._('dashboard.reports.failed'), "value": "101" },
				{ "icon": "ic-arrow-down", "title": $.i18n._('jd.speed'), "value": "1.9MB/s" },
				{ "icon": "ic-time", "title": $.i18n._('dashboard.reports.done'), "value": "06:47:01" },
				{ "icon": "ic-star", "title": $.i18n._('dashboard.reports.accounts'), "value": "100" }
			],
			"downloads_reports": [
				{ "icon": "ic-control-play-blue", "title": $.i18n._('jd.jd'), "value": $.i18n._('downloads.reports.start') },
				{ "icon": "ic-control-pause-blue", "title": $.i18n._('jd.jd'), "value": $.i18n._('downloads.reports.pause') },
				{ "icon": "ic-control-stop-blue", "title": $.i18n._('jd.jd'), "value": $.i18n._('downloads.reports.stop') },
				{ "icon": "ic-control-repeat-blue", "title": $.i18n._('jd.jd'), "value": $.i18n._('downloads.reports.update') },
				{ "icon": "ic-cancel", "title": $.i18n._('jd.jd'), "value": $.i18n._('downloads.reports.shutdown') }
			],
			"account_reports": [
				{ "icon": "ic-add", "title": $.i18n._('accounts.reports.title'), "value": $.i18n._('jd.add') },
				{ "icon": "ic-basket", "title": $.i18n._('accounts.reports.title'), "value": $.i18n._('accounts.reports.buy') },
				{ "icon": "ic-add", "title": $.i18n._('accounts.reports.title'), "value": $.i18n._('accounts.reports.buy') },
				{ "icon": "ic-add", "title": $.i18n._('accounts.reports.title'), "value": $.i18n._('accounts.reports.buy') },
				{ "icon": "ic-add", "title": $.i18n._('accounts.reports.title'), "value": $.i18n._('accounts.reports.buy') }
			],
			"dashboard__downloads_title" : $.i18n._('dashboard.downloads.title'),
			"accounts__account_title" : $.i18n._('accounts.account.title'),
			"accounts__account_table_hoster" : $.i18n._('accounts.account.hoster'),
			"accounts__account_table_user" : $.i18n._('accounts.account.user'),
			"accounts__account_table_expire" : $.i18n._('accounts.account.expire'),
			"accounts__account_table_status" : $.i18n._('accounts.account.status'),
			"accounts__account_table_lefttraffic" : $.i18n._('accounts.account.traffic'),
			"accounts__account_table_active" : $.i18n._('accounts.account.active'),
			"downloads__download_title" : $.i18n._('downloads.download.title'),
			"downloads__download_package" : $.i18n._('downloads.download.package'),
			"downloads__download_size" : $.i18n._('downloads.download.size'),
			"downloads__download_hoster" : $.i18n._('downloads.download.hoster'),
			"downloads__download_progress" : $.i18n._('downloads.download.progress'),
			"dashboard_downloads": [
				{ "title": $.i18n._('dashboard.downloads.all'), "value": "100" },
				{ "title": $.i18n._('dashboard.downloads.extracted'), "value": "10" },
				{ "title": $.i18n._('dashboard.downloads.fail_extracted'), "value": "1" },
				{ "title": $.i18n._('dashboard.downloads.done'), "value": "70" },
				{ "title": $.i18n._('dashboard.downloads.downloading'), "value": "5" },
				{ "title": $.i18n._('dashboard.downloads.fail'), "value": "16" }
			],
			"mini_table": [
				{ "icon": "ic-accept", "title": $.i18n._('jd.activate') },
				{ "icon": "ic-cross", "title": $.i18n._('jd.deactivate') },
				{ "icon": "ic-edit", "title": $.i18n._('jd.edit') },
				{ "icon": "ic-bin-closed", "title": $.i18n._('jd.delete') },
				{ "icon": "ic-arrow-refresh", "title": $.i18n._('jd.renew') }
			],
			"options" : $.i18n._('jd.options'), 
			"kbps" : $.i18n._('jd.speed.kps'),
			"speed" : $.i18n._('jd.speed'),
			"logo" : themepath+"images/core/logo.png",
			"title" : "JDownloader"
		};
		$.globalEval(head(pagecontent));
		TemplateAjax("themes/main/tpl/header.tpl.html","body","append");
		TemplateAjax("themes/main/tpl/page.tpl.html","body","append");
	}
	$("body").append('<div id="black" style="opacity:0.8;background:#000;height:100%;width:100%;z-index:28234;position:absolute;"><center><img style="margin-top:200px;" src="'+themepath+'images/core/load.gif" alt="loading.." /></center></div>');
	helpers();
	get_page();
	setTimeout('$("#black").fadeOut()',2900);
});
function TemplateAjax(path, page, mode) {
	var source;
	var template;
	$.ajax({
		url: path,
		cache: true,
		success: function(data) {
			source    = data;
			template  = Handlebars.compile(source);
			if(mode=="html") {
				$(page).html(template(pagecontent));
			}else if(mode == "append") {
				$(page).append(template(pagecontent));
			}
		}               
	});         
}
