define(['async','log4js' , 'rpc' ],function(async, log4js , rpc ){
	var logger = log4js.getLogger("spider");
	var async  = require('async');
	
	/**
	 * find the beginTag and endTag from the given str
	 * using this method to exclude unnecessary content, to reduce the overhead of jQuery parsing
	 */
	function substrByTag( str , beginTag , endTag , includeEndTag ) {
		var idx = str.indexOf(beginTag);
		if (idx>=0) {
			str = str.substring(idx);
			idx = str.indexOf(endTag);
			if (idx >=0 ) {
				str = str.substring(0, idx + ( includeEndTag ? endTag.length : 0 ) );
				return str;
			}
		}
		return '' ;
	}

	function buildUrl( purl , url ) {
		purl = purl || '' ;
		url  = url || ''  ; 
	    if (url.toUpperCase().indexOf('HTTP://') != 0 && url.toUpperCase().indexOf('HTTPS://') != 0) {
		    if (url[0] === '/') {
			    url = purl.substring(0,8) + purl.substring(8).split('/')[0] + url;
			} else {
			    url = purl + ( purl[purl.length-1] === '/' ? '' : '/' ) + url ;	
			}
			url = url.replace( /\/\.\//ig , '/' );
		}
		return url ;
	}
	
	function scrape(theUrl , parallels , scrapeLimit , callback ) {
		var rpcInstance = rpc.getInstance() ;
		var tagRegExpr  = /\<a href\=\"([^\"]*)\"\>([^\<]*)\<\/a\>\<b\>\(([\d]*)\)\<\/b\>/ ;
		var subRegExpr  = /\<a href\=\"(http\:\/\/movie\.douban\.com\/subject\/\d*\/)\"/   ;
		
		parallels    =  parallels || 2 ;
		scrapeLimit  =  scrapeLimit|| 0;
		
		// STEP 1: get content of the tag page
		rpcInstance.scall('request','', theUrl , function(err,res,strBody){
			var tagsTable = substrByTag(strBody,'<table class="tagCol">','</table>',true);
			// STEP 2: <a href="./tag">tag</a><b>(299010)</b>
			var matches = tagsTable.match( eval(tagRegExpr + 'ig') ) || [] ;
			var tags    = [] ;
			for (var idx=0; idx <matches.length; idx ++ ) {
				var tokens = matches[idx].match(tagRegExpr) || [] ;
				if (tokens.length == 4) {
					var tagName   = tokens[2];
				    var tagUrl    = buildUrl(theUrl , tokens[1] );
				    tags.push({ tagName : tagName , tagUrl : tagUrl , subjects : [] });
				    logger.info( 'tag found:[' + tagName + '] ' + tagUrl );
			    }
			}
			// STEP 3: parallel scrape each tag
			async.map(tags
			    ,
			    function tagFound(tag,cb){
				    cb(null, function scrapeThisTag(cb){
					    var url = tag.tagUrl + '?type=T&start=' + tag.subjects.length ;
					    logger.info( 'start:' + url );
					    
					    rpcInstance.scall('request','', url , function(err,res,strBody){
						    tag.err     = err ;
						    var matches = (strBody||'').match( eval(subRegExpr + 'ig') ) || [] ;
						    for (var idx=0; idx <matches.length; idx ++ ) {
								var tokens = matches[idx].match(subRegExpr) || [] ;
								if (tokens.length == 2) {
									tag.subjects.push( { subUrl:tokens[1] } );
							    }
							}
							
						    logger.info( ( err ? 'failed' : 'finished' ) + ':' + url + ' subjectsFound:' + tag.subjects.length );

							if (matches.length > 0 && ( scrapeLimit <= 0 || tag.subjects.length < scrapeLimit   ) ) {
							    scrapeThisTag(cb) ;	
							} else {
							    cb(null,tag);
							}
						});
					});
				}
				,
				function scrapeTagsParallel(err, tasks) { 
					logger.info( 'start scrape tags in parallel:' + parallels );
					async.parallelLimit( tasks , parallels , function(err,tags){
						// TODO:
					});
				}
			) ;			
		});
	}
	
	return { 
	    scrape   : scrape
	}; 
});