define(['rpc','async','log4js' ],function(rpc,async, log4js ){
	var logger = log4js.getLogger("spider");
	var async  = require('async');
	
	function spideTags() {
		rpc.request.bind(this)( 'http://movie.douban.com/tag' , function(err,strBody){
			logger.info(strBody);
		});
	}
	
	function sayHello( who ) {
		logger.info('got ' + who );
		return 'Hello ' + who ;
	}
	
	return {
	    sayHello : sayHello	,
	    spideTags: spideTags
	};
//	var CATEGORY_STANDARD_COUPON = 'COUPON' ;
//	var CATEGORY_STANDARD_DEAL   = 'DEAL'   ;
//	var CATEGORY_SCRAPE_CONCURRENT_SIZE = 2 ; 	
//	var HEADERS = {cookie:'dealsplus_view[view]=row; dealsplus_view[num_page]=40'};
//
//	/**
//	 * find the beginTag and endTag from the given str
//	 * using this method to exclude unnecessary content, to reduce the overhead of jQuery parsing
//	 */
//	function substrByTag( str , beginTag , endTag , includeEndTag ) {
//		var idx = str.indexOf(beginTag);
//		if (idx>=0) {
//			str = str.substring(idx);
//			idx = str.indexOf(endTag);
//			if (idx >=0 ) {
//				str = str.substring(0, idx + ( includeEndTag ? endTag.length : 0 ) );
//				return str;
//			}
//		}
//		return '' ;
//	}
//	
//	function categoryFound( cats , standard ) {		
//		var arr = [standard] ;
//		for (var idx=2; idx<arguments.length; idx++) {
//			arr.push(arguments[idx]);
//		}		
//		cats.push(arr) ;
//	}
//	
//	var callbacks  = {
//		'/coupons.*' : function(err,body) { 
//			var categories = []  ;
//			var callback   = this;
//			if (!err) {
//				var $couponCategory= $(substrByTag(body,'<div id="couponCategory">' , '</div>' , true ));
//				$couponCategory.find('> ul > li').each(function(idx,li){
//					var $li    = $(li);
//					var liName = $($li.find('strong')[0]).text();
//					$li.find('> ul > li > a').each(function(idx,a){
//						var $a   = $(a)      ;
//						var aName= $a.text()      ;
//						var aCode= $a.attr('href');
//						categoryFound( categories , CATEGORY_STANDARD_COUPON , aCode , aName , liName );
//					});
//				});
//			}
//			callback(err , categories );
//		}
//		,
//		'/deals.*'   : function(err,body) { 
//			var categories = []  ;
//			var callback   = this;
//			if (!err) {
//				$(substrByTag(body,'<ul class="menu">' , '</ul>' , true )).find('.cat-drop').each(function(idx,li){
//					var $li    = $(li);
//					var a      = $li.find('> a')[0];
//					var $a     = $(a);
//					var liName = $($a.find('>strong')[0]).text();
//					var liCode = $a.attr('href');
//					
//					var pName  = '' ;
//					var pCode  = '' ;
//					$li.find('.submenu-option').each(function(idx,a){
//						var $a    = $(a);
//						var aName = $a.text() ;
//						var aCode = $a.attr('href')   ;
//						if ( $a.css( 'font-weight' ) === 'bold') {
//							pName = aName ;
//							pCode = aCode ;
//						} else {
//							categoryFound( categories , CATEGORY_STANDARD_DEAL , aCode , aName , pName , liName ) ;
//						}
//					}); 
//				});
//			}
//			callback(err , categories );
//		}			
//		,
//		'COUPON' : function findCouponLinkOfVender(body,pageIdx) {
//			var couponLinksOfVender = null ;
//			// STEP 1: check whether given pageIdx's content existed or not
//			var spanOfCurrPage = substrByTag(body,'<span class="box_selected">','</span>' , true );
//			if (spanOfCurrPage) {
//				if ( $(spanOfCurrPage).text().trim() === '' + pageIdx ) {
//	                // STEP 2: scrape the coupon content
//	                couponLinksOfVender = [] ;
//	                // STEP 2.1: find the coupon content's wrapper div and replace all img src with _src avoid load the img to reduce the network traffic
//	                var wrapHTML = substrByTag(body,'<div class="content_left">','<div class="content_right">' , false ).replace(/[ ]src[=]/ig,' _src=');
//	                $(wrapHTML).find('.couponCatBox').each(function(idx,item){
//	                	var couponLinkOfVendor = $(item).find('.couponCatInfo h4 a').attr('href');
//	                	couponLinksOfVender.push( couponLinkOfVendor );
//	                });
//                }
//			}
//			return couponLinksOfVender;
//		}
//		,
//		'DEAL' : function findDealsOfPage(body,pageIdx) {
//			var dealsOfPage = null ;
//			// STEP 1: check whether given pageIdx's content existed or not
//			var spanOfCurrPage = substrByTag(body,'<span class="box_selected">','</span>' , true );
//			if (spanOfCurrPage) {
//				if ( $(spanOfCurrPage).text().trim() === '' + pageIdx ) {
//	                // STEP 2: scrape the deal content
//					dealsOfPage = [] ;
//	                // STEP 2.1: find the deal content's wrapper div and replace all img src with _src avoid load the img to reduce the network traffic
//	                var wrapHTML = substrByTag(body,'<div class="content_left">','<div class="content_right">' , false ).replace(/[ ]src[=]/ig,' _src=');
//	                $(wrapHTML).find('#rowViewRow > tbody > tr').each(function(idx,item){
//	                	var $item   = $(item);
//	                	var smallImgUrl = $item.find('.col1 .deal_image_r_div .img-scaling').attr('_src');
//	                	var shippingFree= null ;
//	                	$item.find('.col2 .rowViewPrice span').each(function(idx,span){
//	                		var $span = $(span);
//	                		if (!$span.attr('class')) {
//	                			shippingFree = $span.text();
//	                		}
//	                	});
//	                	dealsOfPage.push({
//	                		  smallImgUrl : smallImgUrl
//		                	, bigImgUrl   : smallImgUrl.replace('/10000/','/20000/')
//		                	, name        : $item.find('.col2 a.title').text().replace(/\n/ig,'').trim()
//		                	, description : $item.find('.col2 .desc').text().replace(/\n/ig,'').trim()
//		                	, oprice      : $item.find('.col2 .oprice-g').text()
//		                	, nprice      : $item.find('.col2 .nprice-g').text()
//		                	, vender      : $item.find('.col2 .rowViewSite .rdLink').text()
//		                	, sharedBy    : $item.find('.col2 .fleft a').attr('href')
//		                	, shippingFree: shippingFree || ''
//	                	});
//	                });
//                }
//			}
//			return dealsOfPage;
//		}
//	}
//	
//	/**
//	 * usage: scrapeContent.bind(socket)(site,categories)
//	 * @param site: http://www.dealspl.us
//	 * @param categories: the categories scraped by scrapeCagegories call
//	 * @param givenPageIdx: scrape which page content, start from 1
//	 * @param contentFound: callback function(err,jobs){}
//	 * @param contentFound.jobs:
//	 * [
//	 *   { cb : 'DEAL' , err : '' , itemsOrLinks: [ {} , {} , ...... ] , catIdx: 0 , pageIdx: 1 }
//	 *   ......
//	 *   { cb : 'COUPON' , err : '' , itemsOrLinks: [ '' , ''  , ...... ] , catIdx: 1 , pageIdx: 1 }
//	 * ]
//	 */
//	function scrapeContent(site,categories,givenPageIdx,contentFound) {
//		var request = rpc.request.bind(this); 
//		contentFound= contentFound || function(err,jobs) {
//		    if (err) {
//				logger.error('scrape ' + site + ' content failed' , err ) ;
//			} else {
//				logger.debug('itemsOrLinks' , JSON.stringify(jobs,2,2) ) ;
//			}
//		}
//		
//		// STEP 4.2: organize all the job related information into this array as
//		// [
//		//     { err: '' , itemsOrLinks : [] , catIdx : 0 , pageIdx : 1 }
//		//     { err: '' , itemsOrLinks : [] , catIdx : 1 , pageIdx : 1 }
//		//     { err: '' , itemsOrLinks : [] , catIdx : 2 , pageIdx : 1 }
//		//     { err: '' , itemsOrLinks : [] , catIdx : 3 , pageIdx : 1 }
//		// ]
//		var jobs       = [];
//		
//		for (var idx = 0 ; idx < categories.length; idx ++ ) {
//			var cat    = categories[idx] ;
//			// find the callback function by the category URL first, otherwise by COUPON or DEAL
//			var cbName = cat[1];
//			if (!callbacks[cbName]) {
//				cbName = cat[0];
//			}
//			if (callbacks[cbName]) {
//				jobs.push({ cb : cbName , err : '' , itemsOrLinks : [] , catIdx : idx , pageIdx : givenPageIdx || 1 });
//			}
//	    }
//	    
//		async.map(jobs,
//			// convert job object to task functions
//			function toTask(job,callback){ 
//			    callback(null,function scrapeThisPage(callback){
//			    	var arl = categories[job.catIdx][1];
//			    	var purl= site+arl+'?page=' + job.pageIdx;
//			    	
//			    	logger.debug('start scrape page:' + purl ); 
//					request({ uri : purl , headers:HEADERS},function(err,body){
//						if (err) {
//							logger.error('scrape page:' + purl + ' failed' , err );
//							job.err = err     ;
//							callback(null,job);
//						} else {
//							var itemsOrLinks = callbacks[job.cb](body,job.pageIdx);
//							if (itemsOrLinks ) {
//								job.itemsOrLinks = job.itemsOrLinks.concat(itemsOrLinks);
//						    }
//						    						    
//						    // scrape next page when no pageIdx been given and current page exist
//						    if (givenPageIdx == null && itemsOrLinks) {
//								job.pageIdx ++ ; 
//								scrapeThisPage(callback);
//							} else {
//								logger.debug('finished scrape category' , categories[job.catIdx] ) ;
//								callback(null,job) ;
//							}
//						}
//					});  
//			    }); 
//			}
//		    ,
//		    function(err,tasks){
//		    	async.parallelLimit( tasks , CATEGORY_SCRAPE_CONCURRENT_SIZE , function(err,results){
//			    	contentFound(err,results);
//				});
//		    }
//		); 	
//    }
//    
//    /**
//     * @usage: require('scrapeit').scrapeCategories(socket)( site , callback );
//     * @param site: http://www.dealspl.us
//     * @param categoriesFound: the callback function(err,categories){}
//     * @return callback.categories: 
//	 *	[
//	 *	  ['COUPON' , 'url_of_category_1' , 'category_name' , 'parent_category_name' ]
//	 *	  ['COUPON' , 'url_of_category_2' , 'category_name' , 'parent_category_name' ]
//	 *	  ......
//	 *	  ['DEAL'   , 'url_of_category_1' , 'category_name' , 'parent_category_name' , 'parent_category_name' ...... ]
//	 *	  ['DEAL'   , 'url_of_category_2' , 'category_name' , 'parent_category_name' , 'parent_category_name' ...... ]
//	 *	  ......
//	 *	]
//     */
//	function scrapeCategories(site, categoriesFound) {
//		var request = rpc.request.bind(this); 
//		categoriesFound = categoriesFound || function(err,categories) {
//			if (err) {
//				logger.error('scrape ' + site + ' failed' , err ) ;
//			} else {
//				logger.debug('categories found:' , categories);
//			}
//		} ;
//		
//		// STEP 1: get the home page content
//		logger.debug('start scrape home page:' + site);
//		request( { uri : site , headers:HEADERS}, function(err,body){
//			if (err) {
//				categoriesFound('scrape ' + site + ' failed' , err ) ;
//			} else {
//				// STEP 2: find the topLevel menu
//				var $body = $(substrByTag(body,'<ul id="topMenu">','</ul>' , true ));
//				var cbs   = [] ;
//				$body.find('li a').each(function(idx,ahref){ 
//					$ahref  = $(ahref)            ;
//					var arl = $ahref.attr('href') ;
//					for (var pattern in callbacks) {
//						if (new RegExp(pattern).test(arl)) {
//							
//							logger.debug('found top level page:' + site + arl);
//							
//							cbs.push(function(callback){
//								request({ uri : site + arl , headers:HEADERS} ,callbacks[pattern].bind(callback));
//							});
//							break;
//						}
//					}
//				});			
//				// STEP 3: find categories for each topMenu item 
//				async.parallel(cbs,function(err,results){
//					if (err) {
//						categoriesFound(err);
//					} else {
//						// STEP 4: orgalize all categories into one big categories array as
//						var categories = [];
//						
//						for (var idx = 0 ; idx < results.length; idx ++) {
//							for (var ndx = 0 ; ndx < results[idx].length; ndx++) {
//								var cat= results[idx][ndx];
//								categories.push(cat);
//							}
//						} 
//						categoriesFound(null,categories);
//					}
//				});				
//			}
//		});		
//	}
//	
//	return { 
//		scrapeCategories : scrapeCategories ,
//		scrapeContent    : scrapeContent
//	} ;
});