define(['socket.io','log4js'],function(io,log4js){
	var logger       = log4js.getLogger("rpc");
	var socketsCache = {} ;
	var socketId     = 0 ;
	var clientEnabled= typeof window !== 'undefined';
	
	function RpcWrapper(socket) {
		this.socket = socket ;
		this.acks   = {}     ;
	    this.ackId  = 0      ;
		var selfThis= this   ;
		
		socket.on('rpc.call',function(cfg){ 
			var module  = null         ;
			var errMsg  = ''           ;
			cfg = cfg || {}            ;
			cfg.args = cfg.args || []  ;
			
			if (!errMsg && !cfg.module) {
				errMsg = 'module not found from the call' ;
			}
			if (!errMsg && !cfg.method) {
				errMsg = 'method not found from the call' ;
			}
			if (!errMsg) {
				requirejs([cfg.module],(function(errMsg,cfg,socket,module){
					var theFunc = null ;
					var response= null ;					
					if (!module) {
						errMsg = 'can not find module:' + cfg.module ;
					} 
					if (!errMsg && !module[cfg.method]) {						
						if (cfg.method) {
						    errMsg = 'no method: ' + cfg.method + ' found from module:' + cfg.module ;
						} else {
							if (typeof module === 'function') {
							    theFunc= module ;
						    } else {
						        errMsg = 'no method specified to call module:' + cfg.module ;
							}
						} 
					} 					
					if (!errMsg) {  
						theFunc = theFunc || module[cfg.method];
						
					    cfg.args.push( (function(cfg,errMsg){
							this.emit('rpc.response' , {ackId: cfg.ackId , response : Array.prototype.slice.call(arguments,2) , error : errMsg });
						}).bind(socket,cfg) ) ;
					    
						try {
							theFunc.apply(selfThis,cfg.args) ;
						} catch (e) {
						    socket.emit('rpc.response' , {ackId: cfg.ackId , response : [] , error : e.toString() });	
						}
					} else {
						socket.emit('rpc.response' , { ackId: cfg.ackId , response : response , error : errMsg});
					}
				}).bind({} , errMsg,cfg,socket));
			} else {
				socket.emit('rpc.response' , { ackId: cfg.ackId , error : errMsg});
			}
		});		
		socket.on('rpc.response' , function(res){ 
			if (res.ackId != null && selfThis.acks[res.ackId]) {
				var func = selfThis.acks[res.ackId];
				delete selfThis.acks[res.ackId] ; 
				func.apply( this , [res.error].concat(res.response) );
			} else if (res.error){
				logger.error(res.error); 
			}
		});		
	}
	
	RpcWrapper.prototype.call=function call(module,method) {
		if (!module) {
			throw 'module cannot be null' ;
		}
		if (method === null) {
			throw 'method cannot be null' ;
		}		
		
		var cb   = null;
		var args = []  ;
		for (var i=2; i<arguments.length; i++) {
			if (typeof arguments[i] === 'function') {
				if ( i == arguments.length - 1) {
					cb = arguments[i] ;
				} else {
					args.push(arguments[i].toString() );
				}
			} else {
				args.push(arguments[i]);
			}
		}
		var cfg = {
			module : module , method: method , args : args
		};
		if (cb) {
			cfg.ackId            = ++ this.ackId ;
			this.acks[cfg.ackId] = cb            ;
		}
		this.socket.emit('rpc.call', cfg );
	}
	
	/**
	 * always call on server side
	 */
	var scallFunc = RpcWrapper.prototype.scall = (function(){
		if (clientEnabled) {
			return function() { 
				this.call.apply(this,['rpc', '__internal__scall__'].concat(Array.prototype.slice.call(arguments)));
			}
		} else {
			var method = function() {
				var module = arguments[0] ;
				var mName  = arguments[1] ;
				var args   = Array.prototype.slice.call(arguments,2);
				
			    var m = require(module);
			    if (!m) {
				    throw 'module ' + module + ' not found' ;   
			    }
			    if (mName){
				    if (!m[mName] || !m[mName].apply ) {
					    throw 'method ' + mName + ' not found from module ' + module ;     
					}
				    m[mName].apply(this,args);
				} else {
					if (m.apply) {
					    m.apply(this,args);
					} else {
						throw 'method not specified for call module ' + module ; 
					}
			    }
			}
			return method;
		}
	})();
	
	function getInstance(socket) {
		var sid = 0 ;
	    if (socket != null) {
		    if (socket.id == null) {
			    socket.id = socketId ++ ;    
			}
		    if (!socketsCache[socket.id]) {
			    socketsCache[socket.id] = new RpcWrapper(socket);
			}
			sid = socket.id ;
		} else {
			for (var id in socketsCache){
				if (typeof socketsCache[id] === 'object') {
				    sid = id ;
				    break;	
				}
			}
		}	
		var instance = socketsCache[sid];
		if (instance == null) {
			if (clientEnabled) {
			    throw 'socket.io not initialized' ;
			}
		    instance = {
			    scall   : scallFunc 
			    ,
			    call    : function(){
					throw 'socket.io not initialized' ;
				}
			}	
		}
		return instance ; 
	}
	
	return {
	    getInstance               : getInstance 
	    ,
	    __internal__scall__       : scallFunc	
	} ;
});