/* PREMIUM ACCOUNTS START */
var AccountHandler = {
	_queue : [],
	_active_queue : [],
	addAccount : function addAccount(acc) {
		AccountHandler._queue.push(acc);
		$("#prem_table").append('<tr id="prerow_'+acc.id+'"><td><input type="checkbox" class="premiumaccount" value="'+acc.username+'/'+acc.hostname+' //'+acc.id+'" /></td><td><img src="'+$.jd.getURL("content/favicon", [ acc.hostname ])+'"> '+acc.hostname+'</td><td>'+acc.username+'</td><td>'+stamptodate(acc.expireDate)+'</td><td>'+acc.status+'</td><td><div class="jd-progressbar1"><span style="position:absolute;text-align:center;margin-left:270px;font-size:10px;font-weight:bold;">'+do_premium_traffic(acc.trafficLeft,acc.trafficMax)+'</span></div></td><td>'+acc.enabled+'</td></tr>');
	},
	update : function premiumtable(mode) {
				var finalstring = "[";
				var ids = new Array();
				var values = $('input:checkbox:checked.premiumaccount').map(function () {
					$(this).attr('checked', false);
					return this.value;
				})
				for(i=0;values.length>i;i++) {
					var sh = values[i].split("//");
					$("#prerow_"+sh[1]).remove();
					finalstring = finalstring.concat(sh[1]+", ");
					if(mode==1) {
						$.jGrowl("Der Account "+sh[0]+" wurde gel&ouml;scht.", {header: "Premium", position: "bottom-right"});
					} else if(mode==2) {
						$.jGrowl("Der Account "+sh[0]+" wurde aktiviert.", {header: "Premium", position: "bottom-right"});
					} else if(mode==3) {
						$.jGrowl("Der Account "+sh[0]+" wurde deaktiviert.", {header: "Premium", position: "bottom-right"});
					}
				}
				finalstring = finalstring.substr(0,finalstring.length-2);
				finalstring = finalstring.concat("]");
				if(mode==1) {
					$.jd.send("accounts/remove", finalstring );	
				} else if(mode==2) {
					$.jd.send("accounts/setEnabledState", [ "true", finalstring ]);
				} else if(mode==3) {
					$.jd.send("accounts/setEnabledState", [ "false", finalstring ]);
				}
				if(mode!=1) {
					AccountHandler.rload();
				}
		
	},
	getall : function getPremium(mode) {
			$.jd.send("accounts/list", function onAccountList(acc) {
				$.each(acc, function(i, acc) {
					if(mode==2) {
						if(acc.enabled) {
							AccountHandler._active_queue.push(acc);
							$('#dashboard__report_activeprem_val').text(AccountHandler._active_queue.length);
						}
					} else {
						AccountHandler.addAccount(acc);
					}
				});
				if(AccountHandler._active_queue.length == 0) {
					$('#dashboard__report_activeprem_val').text("0");
				}
			});
	},
	rload : function reloadpremium() {
				$("#prem_table").html('');
				AccountHandler.getall(1);
	},
	editdialog : function editdialog() {
				$("#jd-account-dialog").dialog("option", {modal: true, draggable: false, resizable: false}).dialog("open");
	}
	
	
};