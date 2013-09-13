requirejs.config({
	catchError : true,
	paths : {
		'socket.io' : '/socket.io/socket.io',
		'async'     : '/async/lib/async',
		'log4js'    : 'log4javascript'  
	},
	shim : {
		'log4js' : {
			deps : [],
			exports : 'log4javascript',
			init : function() {
				var logger = log4javascript.getLogger('rpc');
				logger.addAppender(new log4javascript.PopUpAppender());
				logger.setLevel(log4javascript.Level.INFO);

				logger = log4javascript.getLogger('spider');
				logger.addAppender(new log4javascript.PopUpAppender());
				logger.setLevel(log4javascript.Level.DEBUG);

				logger = log4javascript.getDefaultLogger();
				logger.setLevel(log4javascript.Level.INFO);
			}
		}
		,
		'jquery-ui' : {
			deps    : ['jquery'] ,
			exports : 'jQuery.ui'
		}
	}
});

requirejs.onError = function(e) {
	console.error(e);
}

define(['jquery', 'socket.io', 'rpc' , 'log4js' , 'spider' ], function($, io,rpc,log4js,spider) {
	// STEP 1: initialize Logger
    var logger = log4js.getDefaultLogger();
	logger.info('Logger Initialed');	
	
	// STEP 2: initialize socket.io
	var socket = io.connect();
	
	// STEP 3: initialize the rpc
	var rpcInstance = rpc.getInstance(socket); 
	
	// STEP 4: example rpc calls
	rpcInstance.scall('fs','stat','rpc.log', function(err,stats){ 
		logger.info(stats);
	});
	
	// STEP 5: start scrape
	//spider.scrape('http://movie.douban.com/tag' , 5, 40 , function(err,msg){
	//});
	
	rpcInstance.call('spider','scrape','http://movie.douban.com/tag' , 5 , 40 ,function(err,msg){
	});
		
});
