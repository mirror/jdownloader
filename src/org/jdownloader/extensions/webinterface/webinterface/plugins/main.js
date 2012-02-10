(function JDwebUI_main_logic($) {
	$(document).ready(function() {
		// Load theme
		JDwebUI.theme(JDwebUI.theme());

		$(JDwebUI).bind('themeLoaded', function() {
			JDwebUI.linkGrabber.init();
			//JDwebUI.fileList.init();
			JDwebUI.settings.init();
			JDwebUI.init();
		});
	});
})(jQuery);