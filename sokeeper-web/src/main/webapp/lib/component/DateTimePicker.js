/**
 * @attr[target]: could be empty, indicate which element will receive the picked datetime value
 * @attr[showTime]:whether show the time(hours minutes and seconds)
 */
core.create( "component.DateTimePicker" , {
	css : function() {
	  return [ ] ;
	}
	,
	js : function(){
	  return [ "3rd.datetimepicker" ] ;
	}
	,
	ready : function() {
	  if( core.isLibraryLoaded( "3rd.datetimepicker" ) ){
	    if(!datetimepicker.images_path){
		  datetimepicker.images_path=core.base+'3rd/datetimepicker/'    
	    }
	    return true;	  
	  }
	  return false;
	}
	,	
	initialize :  function( elem ){	  
	  var callback=function(evt){ 
		var srcElem=evt.target||evt.srcElement;   
		srcElem.id=srcElem.id||('elem_'+Math.random()); 
		pickIt(evt);
	    NewCssCal(srcElem.getAttribute("target")||srcElem.id,'yyyymmdd','arrow',srcElem.getAttribute("showTime")=="true",24,false);	  
	  }
	  if(elem.addEventListener){
	    elem.addEventListener('click',callback,false);	  
	  }else if(elem.attachEvent){
	    elem.attachEvent('onclick',callback);		  
	  }
    } 
} ) ;