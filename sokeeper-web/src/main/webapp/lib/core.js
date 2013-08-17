/**
 * http://visualjquery.com/1.1.2.html http://numenzq.javaeye.com/blog/179058
 * http://www.w3schools.com/css/css_reference.asp
 * http://www.blackducksoftware.com/oss
 *
 * ATTENTION: 
 *    1, the html file's encoding(You can set it with CrimsonEditor
 *       Document-->Encoding Type-->UTF-8) must be UTF-8
 * 
 * include:
 * @param className
 *            it could start with 
 *              1, [css.] e.g.:[css.a.b.c], in this case, the function look for stylesheet file 
 *                 {core.root}/css/a/b/c.css and append to the html's head 
 *              2, others, e.g.:[component.a.b.c] in this case, the function look for javascript file
 *                 {core.base}/component/a/b/c.js and append it to html's head
 *
 *
 * bindFeature :
 * @param className
 * @param elem_or_callback
 *            this function will check the required class[className] loaded, if
 *            not it will load it otherwise, it'll enhance the elem with the
 *            required class or execute the callback function
 * 
 * 
 * bindFeatures:
 * @param pElem
 *            this function will go through the element self and all children
 *            elements, for each element check whether each it has css name
 *            started with 
 *            1, [css.] e.g.:[css.a.b.c], in this case, the function look for stylesheet 
 *               file {core.root}/css/a/b/c.css and append to the html's head 
 *
 *            2, [component.] e.g.:[component.a.b.c] in this case, the function look for 
 *               javascript file {core.base}/component/a/b/c.js and append it to html's 
 *               head and execute core["component.a.b.c"]( element ) * 
 *   with these functions, we can 
 *   1, load css file on fly 
 *   2, load javascript library on fly 
 *   3, bind features for elements in the html page with harmless way
 * 
 * Lazy Loading
 *   if the element has attribute srcLazy, the bindFeatures will perform the lazy loading:
 *      elem.src = srcLazy;
 *      elem.srcLazy = null;
 *
 * path problem 
 *   1, img[@src]    : absolute path from core.root 
 *   2, iframe[@src] : absolute path from core.root 
 *   3, form[@action]: absolute path from core.root 
 *   4, TabSet[@href] : relative with current location.href 
 *   5, include[@page]: relative path compare the container page path
 * 
 */
	
var core  = {
  cookieDays  : 7      ,
  lang        : 'en-US',
  base        : null   ,
  root        : null   ,
  relative    : false  ,
  main_css    : true   ,
  absUrl      : null   ,    
  bindFeature : null   ,    
  bindFeatures: null   ,   
  atEndOfBox  : null   ,
  $           : null   ,
  start       : null   ,
  scrollTo    : null   ,
  include     : null   ,
  create      : null   ,
  onUnload    : null   ,
  extAttrs    : {}     ,
  enablePrinter : function( isEnable ){
	  var stylesheet = document.getElementById( 'disableprinter' ) ;
	  if( stylesheet ){
	      document.getElementsByTagName("head")[0].removeChild( stylesheet ) ; 
	  }
	  if( !isEnable ) {
		  stylesheet      = document.createElement('link');
		  stylesheet.id   = 'disableprinter' ;
	      stylesheet.rel  = "stylesheet" ;
	      stylesheet.type = "text/css"   ;	    
          stylesheet.href = core.root + 'css/disableprinter.css' ;
	      document.getElementsByTagName("head")[0].appendChild(stylesheet); 
	  }
  }
  ,
  libraries   : {
    "3rd.swfobject"        : function(){ 
      return window["SWFObject"] != null ; 
    }
    , 
    "3rd.swfupload"        : function(){ 
	    return ((window.FileProgress||{}).prototype||{}).Disappear ;
	} 
    , 
    "3rd.TrimPath"         : function(){ 
	    return window["TrimPath"             ] != null ; 
	} 
	, 
    "3rd.prototype"           : function(){ 
	    if( ( window.Position || {} ).cumulativeOffset ) {  
		    Ajax.getTransport = function() {
                return Try.these(    
                  // put this one as the first one, so when we access file:// will not throw access denied
			      function() {return new ActiveXObject('Microsoft.XMLHTTP')} ,
			      function() {return new XMLHttpRequest()} ,
			      function() {return new ActiveXObject('Msxml2.XMLHTTP')}
			    ) || false ;
		    }
		    return true  ;
	    } else {
		    return false ;
	    }
	} 
    , 
    "3rd.jquery"           : function(){ 
	    if( ( window.jQuery || {} ).noConflict ){
		    window.jQuery.noConflict();
		    return true ;    
	    }
	    return false ;
	} 
    , 
    "3rd.jquerycookie"     : function(){ 
	    return ((window.jQuery||{}).cookie||{}).bind != null ; 
	} 
    , 
    "3rd.jquerydimensions" : function(){ 
	    return (((window.jQuery||{}).fn||{}).offsetParent||{}).bind != null ; 
	} 
    , 
    "3rd.jqueryhoverIntent": function(){ 
	    return (((window.jQuery||{}).fn||{}).hoverIntent||{}).bind != null ; 
	} 
    , 
    "3rd.jquerycluetip"    : function(){ 
	    return (((window.jQuery||{}).fn||{}).cluetip||{}).bind != null ; 
	} 
    , 
    "3rd.jquerydatepicker" : function(){ 
	    return (((window.jQuery||{}).fn||{}).datePicker||{}).bind != null ; 
	} 
    , 
    "3rd.jquerybgiframe"   : function(){ 
	    return (((window.jQuery||{}).fn||{}).bgIframe||{}).bind != null ; 
	} 
    , 
    "3rd.date"             : function(){ 
	    return Date.fromString  != null ; 
	} 
    , 
    "3rd.gridbase"         : function(){ 
	    return (((window.jQuery||{}).fn||{}).jqGrid||{}).bind != null ; 
	} 
    , 
    "3rd.jquerymetadata"   : function(){
	    return (((window.jQuery||{}).fn||{}).metadata||{}).bind != null ; 
	}
    ,
    "3rd.jqueryform"       : function(){ 
	    return (((window.jQuery||{}).fn||{}).ajaxForm||{}).bind != null ; 
	}
	,
	"3rd.jqueryvalidate"   : function(){
	    return (((window.jQuery||{}).fn||{}).validate||{}).bind != null ; 
	}
	,
	"3rd.additional-methods":function(){
	    return (((window.jQuery||{}).validator||{}).methods||{})['url2'] != null ; 
	}
	,
	"3rd.gears":function(){
		return ( window.google || {} ).gears != null ;
	}	
	,
	"3rd.datetimepicker":function(){
		return window.dropIt != null ;
	}
	,
	"3rd.raphael":function(){
		return window.Raphael != null ;
	}
  }     
  ,
  registerIsLibraryLoaded : function( library , func ){
    core.libraries[library] = func ;  	  
  }
  ,       
  isLibraryLoaded : function( library  ){
	if( core.libraries[library] == true ) {
		return true ;
	}
	
	if( core.libraries[library] == null ) {
	    core.registerIsLibraryLoaded( library , function(){
		    return core[ library ] ;
		} );	
	}
	  
	if( ( core.libraries[library] || {} ).bind ) {
	    if( core.libraries[library]() ) {
		    core.libraries[library] = true ;
	    } else {
		    return false ;
	    }    	
	}
	
	return true ;
  } 
  ,
  whereIsLibrary : function( className ) {
    return ( window[ '__where_is_library__' ] || {} )[className] || className ;
  }   
  , 
  debug : {
    debug : function( msg , element ) {
	    
    }
    ,
    info : function( msg , element ) {
	    
    }
    ,
    warnning : function( msg , element ) {
	    
    }
    ,
    fatal : function( msg , element ) {
    }
  }                 
} ;

function $$( elemId , attrName , attrVal ){	
	if( elemId ) {
		
		if( ( elemId.constructor != String ) && !elemId.id ) {
		    elemId.id = 'elem_' + Math.random( ) ;	
		}
		
		elemId = elemId.id || elemId ;

		switch( arguments.length ) {
		    case 1 : {
		    	return core.extAttrs[elemId];
		    } ;
		    case 2 : {
		    	return (core.extAttrs[elemId] || {})[attrName] || document.getElementById(elemId).getAttribute(attrName);	
		    } ;
		    case 3 : {
		    	core.extAttrs[elemId]           = core.extAttrs[elemId] || {} ;
		    	core.extAttrs[elemId][attrName] = attrVal ;
		    } ;
		}
		
	}
}

( function( ){
  {	
	// 1, the bind method  
	Function.prototype.bind = function() {
      var __method = this;
      var args     = []  ;
      Array.prototype.push.apply( args, arguments );  
      var object   = args.shift() ;
      return function() {
	    var _args  = [];
	    Array.prototype.push.apply( _args, arguments );  
	    return __method.apply(object, args.concat( _args ) );
      }
    }
    
    Array.prototype.inject=function(memo, iterator) {
	  for( var i=0 ; i<this.length; i++ ){
	    memo = iterator(memo, this[i], i );
	  }  
      return memo;
    }
  
    String.prototype.strip=function(){
	  return this.replace(/^\s+/, '').replace(/\s+$/, '');
    }
  
    String.prototype.parseQuery=function(separator) {
      var match = this.strip().match(/([^?#]*)(#.*)?$/);
      if (!match) return {};

      return match[1].split(separator || '&').inject({}, function(hash, pair) {
        if ((pair = pair.split('='))[0]) {
          var key = decodeURIComponent(pair.shift());
          var value = pair.length > 1 ? pair.join('=') : pair[0];
          if (value != undefined) value = decodeURIComponent(value);

          if (key in hash) {
            if (hash[key].constructor != Array) hash[key] = [hash[key]];
            hash[key].push(value);
          }
          else hash[key] = value;
        }
        return hash;
      });
    }
    
    String.prototype.encode=function( ){
    	var intLen=this.length;
    	var strOut="";
    	var strTemp;

    	for(var i=0; i<intLen; i++)
    	{
    		strTemp=this.charCodeAt(i);
    		if (strTemp>255)
    		{
    			tmp = strTemp.toString(16);
    			for(var j=tmp.length; j<4; j++) tmp = "0"+tmp;
    			strOut = strOut+"^"+tmp;
    		}
    		else
    		{
    			if (strTemp < 48 || (strTemp > 57 && strTemp < 65) || (strTemp > 90 && strTemp < 97) || strTemp > 122)
    			{
    				tmp = strTemp.toString(16);
    				for(var j=tmp.length; j<2; j++) tmp = "0"+tmp;
    				strOut = strOut+"~"+tmp;
    			}
    			else
    			{
    				strOut=strOut+this.charAt(i);
    			}
    		}
    	}
    	return (strOut);    
    }

    String.prototype.decode=function( ){
    	var intLen = this.length;
    	var strOut = "";
    	var strTemp;
        for(var i=0; i<intLen; i++)
    	{
    		strTemp = this.charAt(i);
    		switch (strTemp)
    		{
    			case "~":{
    				strTemp = this.substring(i+1, i+3);
    				strTemp = parseInt(strTemp, 16);
    				strTemp = String.fromCharCode(strTemp);
    				strOut  = strOut+strTemp;
    				i += 2;
    				break;
    			}
    			case "^":{
    				strTemp = this.substring(i+1, i+5);
    				strTemp = parseInt(strTemp,16);
    				strTemp = String.fromCharCode(strTemp);
    				strOut  = strOut+strTemp;
    				i += 4;
    				break;
    			}
    			default:{
    				strOut  = strOut+strTemp;
    				break;
    			}
    		}

    	}
    	return (strOut);
    }
    
	function cumulativeOffset(element) {
	    var valueT = 0, valueL = 0;
	    do {
	      valueT += element.offsetTop  || 0;
	      valueL += element.offsetLeft || 0;
	      element = element.offsetParent;
	    } while (element);
	    return [valueL, valueT];
    }
    
    core.scrollTo   = function ( element ){
	    var pos = cumulativeOffset(element) ;
	    window.scrollTo( pos[0] , pos[1] );    
    }
    
    core.atEndOfBox = function ( input_box , mouseClientX ){
		return (  cumulativeOffset(input_box)[0] + parseInt( input_box.width || input_box.style.width )-20 <= mouseClientX )
	}
	
	core.$=function(element) {
	  if (arguments.length > 1) {
	    for (var i = 0, elements = [], length = arguments.length; i < length; i++)
	      elements.push(core.$(arguments[i]));
	    return elements;
	  }
	  if (typeof element == 'string'){
	    element = document.getElementById(element) ;
	  }
	  return element ;
	} 
	
    // 2, initialize the core.base and core.root path variables
    var scripts = document.getElementsByTagName("script") ;
    var rePkg1  = /(core[^\.]*)\.js([\?\.]|$)/i;	  
    for( var i=0 ; i < scripts.length ; i ++ ) {
      if( scripts[i].src != null ) {
    	    var m = scripts[i].src.match(rePkg1);
    	    if(m) {
    		    var str = scripts[i].src.substring(0, m.index) ;
    			if( str.toLowerCase().indexOf( "http" ) != 0 
    			      && str.toLowerCase().indexOf( "file" ) != 0  
    			        && location.href.lastIndexOf("/") > 0 ){
    				var pathname = location.pathname;      
    				var prefix   = location.href.substring(0,location.href.indexOf(pathname)) ; 
    				if( !prefix && location.pathname.match( /[/][a-z|A-Z][:].*/ ) ){
	    				pathname = pathname.substring(1).replace( /\\/g, '/' );
	    				prefix = location.href.substring(0,location.href.indexOf(pathname) ) ;
    				}
    				if( str.indexOf('/') == 0 && prefix.toLowerCase().match( /^(http[s]{0,1}[:][/][/][^/]*)[/]{0,1}.*$/ ) ) {
	    				
	    			} else {
    				    prefix+= pathname ;
    				    prefix = prefix.substring(0,prefix.lastIndexOf("/")+1);	
				    }				    				
    				str = prefix + str ;
    			}
    			while( str.match( /[/][.][.][/]/ ) ){
    			    str = str.replace( /[^\/]*[/][.][.][/]/g,'');
			    }
    			core.base=str;
    			str = str.substring(0,str.length-1);
    			core.root=str.substring(0,str.lastIndexOf("/")+1);    
    			core.relative=location.href.split( '?' )[0].substring( core.root.length ).match( /[/]/ );
    			core.main_css=scripts[i].getAttribute("main_css") !== "false" ;
    			core.version=scripts[i].getAttribute("version") || new Date().getTime() ;
    			var lang =navigator.language ? navigator.language : navigator.userLanguage ;
    			lang = lang.replace(/_/, '-').toLowerCase();
				if (lang.length > 3) {
					lang = lang.substring(0, 3) + lang.substring(3).toUpperCase();
				}
				core.lang=lang;
				
    			break ;
    		} 
    	}
    }
  }
  
  // 3, class include function
  var __classes__ = [] ;
  function include( fullClassName ) {
	fullClassName = ( fullClassName || "__does__not_exist__" ).replace( /\r\n/g,'').replace(/ /g,'') ;
	if( !__classes__[fullClassName] ) {	
	  // 1, avoid cycle	
	  __classes__[fullClassName] = core.whereIsLibrary(fullClassName);
	  
	  // 2, if the class path is full path like: http://xxxx/a.js, /a.js, http://xxx/a.css, /a.css
      var is_abs =  __classes__[fullClassName].toLowerCase().match( /^file.*$|^http.*$/ ) ;
      var is_js  =  false ;
      
	  if( __classes__[fullClassName].toLowerCase().match( /^.*\.css([#|?].*){0,1}$/ ) ){
	      if( !is_abs ) {
		      __classes__[fullClassName] = core.root + __classes__[fullClassName] ;
	      }
      } else if( __classes__[fullClassName].toLowerCase().match( /^.*\.js([#|?].*){0,1}$/ ) ){
	      if( !is_abs ) {
		      __classes__[fullClassName] = core.base + __classes__[fullClassName] ;
	      }
	      is_js = true ;
      } 
      // 3, is package name style
      else {
	      __classes__[fullClassName] = __classes__[fullClassName].replace(/[.]/g,'/') ;
	      // css.a.b.c
	      if( __classes__[fullClassName].indexOf("css/") == 0 ){
		      __classes__[fullClassName] = core.root + __classes__[fullClassName] + ".css" ; 
	      } else {
		      __classes__[fullClassName] = core.base + __classes__[fullClassName] + ".js"  ;   
		      is_js  = true ; 
	      }
      }
      
      // 4, check it is css or js
      
      // javascript
      if( is_js ){
          xscript = document.createElement('script');
          xscript.src =  __classes__[fullClassName] + ( __classes__[fullClassName].indexOf('?') > 0 ? '' : '?') + '&version=' + core.version ;
	      document.getElementsByTagName("head")[0].appendChild(xscript);
	  } 
      // css    
      else if( !( window['__css_ignores__']||{})[fullClassName] ){
	      stylesheet      = document.createElement('link');
		  stylesheet.rel  = "stylesheet" ;
		  stylesheet.type = "text/css"   ;	    
	      stylesheet.href = __classes__[fullClassName] + '?version=' + core.version ;
	      try {
		      document.getElementsByTagName("head")[0].appendChild(stylesheet);
	      } catch (e) {} 
      }
    }
    return true;
  } 
  core.include = include ;
  
  // 6, bindFeature  
  core.bindFeature=function( className, elem_or_callback ) {
	  if( !core[className] ) {
	      core.include(className) ;        
      }	
      if( className.match( /css[.].*/ ) ){
	      return ;    
      }
      
      var execute = null ;
      if( core[className] ) {
	      if( core[className].prototype.ready ){
		      if( core[className].prototype.ready() ){
			      delete core[className].prototype.ready; 
			      execute = core[className] ;
			  }   
	      } else {
		      execute = core[className] ;    
	      }    
      }
      if( execute ) {
	      if( elem_or_callback.bind ){
		      execute = elem_or_callback ;    
	      }    
      }
      
      if( execute ){
	      if( execute.prototype.initialize ){
		      elem_or_callback.instance = new execute( elem_or_callback );
		      elem_or_callback.instances= elem_or_callback.instances || [] ;
		      elem_or_callback.instances[className] = elem_or_callback.instance ; 
	      } else {
		      execute();    
	      }   
	  } else {
          setTimeout( function( ){ core.bindFeature.apply( {} , [ className, elem_or_callback ] ) ; } , 10 ) ;    
	  }
  } ;
  
  core.create=function( vClassName , classBody ){
  	  if( !vClassName || !classBody || !classBody.initialize ) {
  		  return ;
  	  }
  	  
  	  // avoid include vClassName.js
  	  __classes__[vClassName] = core.whereIsLibrary(vClassName);
  	  
	  var oClassName  = vClassName ;
	  var vSplitName  = vClassName.split(".");
      var vNameLength = vSplitName.length-1;
	  var vTempObject = window;
	
	  for (var i=0; i<vNameLength; i++){
	    if (typeof vTempObject[vSplitName[i]] === "undefined" ) {
	      vTempObject[vSplitName[i]] = {};
	    }
	    vTempObject = vTempObject[vSplitName[i]];
	    vClassName  = vSplitName[i+1];
	  }
	
	  if ( !( typeof vTempObject[vClassName] === "undefined" ) ) {
		return ;
	  }
	  
	  var proto = {
	      css : function() {
		      return [] ;
	      }
	      ,
	      js : function(){
		      return [] ;
	      }
	      ,
	      ready : function() {
		      return false ;
	      }
      } ;
	  for( var key in classBody ) {
	   	  proto[key] = classBody[key] ;
	  }
	  
	  // load css files and dependend js files
	  var css = proto.css() ;
	  proto.js= proto.js()  ;
	  for( var i=0; i<css.length; i++){
	      core.include( css[i] ) ;	  
	  } 
	  delete proto.css ;
	  
	  vTempObject[vClassName] = classBody.initialize ;
	  vTempObject[vClassName].prototype=proto ;
	  
	  core.delayCreate( proto , oClassName ) ;
  }
  
  core.delayCreate = function( proto , oClassName ) {    	  
	  for( var i=0; i<proto.js.length; i++){
		  if( !core.isLibraryLoaded( proto.js[i] ) ){ 
	          core.include( proto.js[i] ) ;	 
	          setTimeout(  core.delayCreate.bind( {} , proto , oClassName ) , 100 ) ;
	          return ;
          } else {
	          // avoid include proto.js[i] twice(if they already in compressed js file)
  	          __classes__[proto.js[i]] = core.whereIsLibrary(proto.js[i]);    
          }
	  }
	  delete proto.js  ;	  
	  core[oClassName] = proto.initialize ;
  }
  
  core.absUrl = function( url , pUrl ){
	  if( !url ) return url      ;
      pUrl = pUrl || core.root ;
      if( url.match( /^[/].*$/ ) ){
	      url    = core.root + url.substring(1) ;
	  } else if( url.toLowerCase().indexOf( 'http' ) != 0 && url.toLowerCase().indexOf( 'file' ) != 0  ){
		  url    = pUrl.match( /(.*[/]).*/ )[1] + url ;
	  }
	  return url ;
  }
  
  // 7, bindFeatures
  core.bindFeatures=function( pElem , currentUrl ){
	  var executes = [] , elements = [] , child , className ;
	  pElem = (pElem || document.body ) ;
	  var children = pElem.getElementsByTagName('include');
	  
	  if( children.length > 0 ) {
		  pElem.currentUrl = currentUrl ;
       	  executes = [ [ 'component.PageInclude' , pElem ] ];
      } else {
	      children = pElem.getElementsByTagName('*');
	      for (var i = 0, length = children.length; i < length; i++) {
            child    = children[i];
            className= child.className || "" ;
            if( className.indexOf( "component." ) >= 0 || className.indexOf( "css." ) >= 0 ){
	            var names = className.split( ' ' ) ;
	            className = "" ;
	            
	            for( var j=0; j<names.length; j++ ){
		          if( names[j].indexOf( "component." ) >= 0 || names[j].indexOf( "css." ) >= 0 ){
		    	      executes.push( [ names[j] , child ] );
		    	  } else className += names[j] + " " ;   
	            }
	            child.className = className ;
            }             
            // lazy loading for images
            if( child.getAttribute && child.getAttribute( 'srcLazy' ) ){
	            child.setAttribute( 'src' , child.getAttribute( 'srcLazy' ) ) ;
	            child.removeAttribute( 'srcLazy' ) ;
	        }
          }
      }
           
      for( var i=0 ; i<executes.length;i++){
	      core.bindFeature( executes[i][0] , executes[i][1] );
      }
      
  }  
  
  core.navigateTo = function( srcUrl , pElem , replaceParentElem ){
	  if( !srcUrl || !pElem || ( typeof srcUrl !== 'string' ) ) {
	      return false ;	  
	  }
	  if( typeof pElem === 'string' ) {
	      pElem = document.getElementById( pElem ) ;	  
	  }
	  if( !pElem ) {
	      return false ;	  
	  }
	  
	  if( replaceParentElem ){
		  pElem.className = 'included_page'    ; 
		  pElem.setAttribute( 'page' , srcUrl );
		  pElem = pElem.parentNode ;
	  } else {
	      //clear the pElem's innerHTML with <include tag
	      pElem.innerHTML = '<div class="included_page" page="' + srcUrl + '" ></div>' ;
      }
	  //call the component.PageInclude
	  core.bindFeature( 'component.PageInclude' , pElem );
	  
	  return true ;
  }
  
  // 8 remove unused elements
  function removeUnusedElements(){	  
	  try {
	      var unusedElements = [] ;
		  var children = document.body.getElementsByTagName('*');
	      for (var i = 0, length = children.length; i < length; i++) {
		      var forAttr = children[i].getAttribute( 'depend' ) || children[i].htmlFor;
		      if( forAttr && !document.getElementById( forAttr ) ) {
			      unusedElements.push( children[i] );
			  }else if( document.getElementById( forAttr ) ){
			      var el      = document.getElementById( forAttr ) ;
			      var display = '' ;
			      while( el && ( el != document.body ) ){
				      display = (( el.style || {} ).display || '').toLowerCase() ;
				      if( display == 'none' ) {
					      break ;   
				      }
				      el = el.parentNode ;
			      }
			      if( ( children[i].style || {} ).display != display ) {
			          ( children[i].style || {} ).display = display ;
		          }
			  }   
	      }	
	          
	      for( var i=0 ; i<unusedElements.length; i++ ){
		      unusedElements[i].parentNode.removeChild( unusedElements[i] );
		  }
      } catch( e ) {} ;
      
	  setTimeout( removeUnusedElements , 100 );	  
  }
  
  /**
   * this function will be call once the container's content will be lost
   * @return true - if the container screen can be unload, otherwise false
   */
  core.onUnload=function( container , is_browser_refresh ){	  
	  container = core.$( container ) ;
	  if( container && container.getElementsByTagName ) {
	      var children = container.getElementsByTagName('*');
	      for (var i = 0, length = children.length; i < length; i++) {
		      if( children[i].instance ) {
			      for( var instanceKey in children[i].instances ) {
				      if( children[i].instances[instanceKey].onUnload ) {
					      try {
						      var not_leavable_msg = children[i].instances[instanceKey].onUnload( is_browser_refresh ) ;
						      if( typeof not_leavable_msg == 'string' ) {
							      return is_browser_refresh ? not_leavable_msg : ( children[i].instances[instanceKey].confirm || window.confirm )( not_leavable_msg ) ;	
						      }
					      } catch( e ) {} ;
					  }    
			      }
		      }   
	      }	  
	  }
	  return true ;
  }
  
  if( core.main_css ) {
	  if( window['__css_ignores__'] == null ) {
	      core.include(  'css.main') ;
	  } else {
	      core.include(  'css.main_compress'  ) ;
	  }
  }
  
  core.start=function( pElem , bUrl ){
	  core.bindFeatures(  ( pElem ||  document.body ) , ( bUrl || location.href )  );
	  removeUnusedElements( );	  
  } ;
  
  core.onbeforeunload = function(){
      var not_leavable_msg = core.onUnload( document.body , true ) ;
      if( typeof not_leavable_msg == 'string' ) {
	      return not_leavable_msg ;    
      }
  }
} )( ) ;
