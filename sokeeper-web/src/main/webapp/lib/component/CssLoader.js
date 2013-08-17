core.create( "component.CssLoader" , {
	css : function() {
	  return [  ] ;
	}
	,
	js : function(){
	  return [] ;
	}
	,
	ready : function() {
	  return true;
	}
	,	
	initialize :  function( elem ){	 
		var csses = ($$(elem,'csses')||'').replace(/\s/ig,'').split(',') || [] ;
		for (var idx=0; idx < csses.length; idx ++) {
			if (csses[idx]) {
				core.include('css.'+csses[idx]);
			}
		}
    } 
} ) ;