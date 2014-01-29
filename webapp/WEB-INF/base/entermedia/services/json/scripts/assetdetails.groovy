package test



import groovy.json.JsonBuilder

import org.openedit.Data

import com.openedit.WebPageRequest

public void handleJSON(){

	WebPageRequest inReq = context;
	Data asset = context.getPageValue("asset");
	
	Collection keys = asset.getProperties().keySet();
	def builder = new groovy.json.JsonBuilder()
	def root = builder.asset {
		name: asset.name
		
		properties(
			keys.collect{
				   String a -> [key: a, value:asset.get(a)]
				}
		)
	}

	String jsondata = builder.toPrettyString();
	log.info(jsondata);
	context.putPageValue("json", jsondata);
}
handleJSON();
