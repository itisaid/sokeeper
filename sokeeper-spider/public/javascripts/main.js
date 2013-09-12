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

define(['jquery', 'socket.io', 'rpc' , 'log4js' ], function($, io,rpc,log4js) {
	// STEP 1: initialize Logger
    var logger = log4js.getDefaultLogger();
	logger.info('Logger Initialed');	
	
	// STEP 2: initialize socket.io
	var socket = io.connect();
	
	// STEP 3: 
	var rpcInstance = rpc.getInstance(socket); 
	rpcInstance.start();
	rpcInstance.call('spider','sayHello','James Fu',function(err,msg){
		logger.info(msg);
		rpcInstance.request( 'http://movie.douban.com/tag' , function(err,strBody){
		    logger.info(strBody);	
		});
	});
});
