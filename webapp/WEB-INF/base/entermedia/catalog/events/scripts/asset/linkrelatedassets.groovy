import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery


public void init(){
	WebPageRequest req = context;
	String ok = req.findValue("save");
	if (!Boolean.parseBoolean(ok)){
		log.info("not save, aborting")
		return;
	}
	log.info("*** saving modifications to related assets ***")
	MediaArchive archive = req.getPageValue("mediaarchive");
	String external = req.getRequestParameter("fieldexternalid");
	if (!external){
		external = req.getRequestParameter("fieldexternalid_module");
	}
	if (!external){
		log.info("no fieldexternalid, aborting");
		return;
	}
	Asset asset = null;
	//need to check situation where external id is shown on the view as well as on the form
	String [] ids = req.getRequestParameters("${external}.value");
	if (!ids || ids.length == 0){
		return;
	}
	boolean hasempty = false;
	for (String id:ids){
		if (id && id.trim() == ""){
			hasempty = true;
			continue;
		}
		if (id && id.trim() != "" && !asset && !id.startsWith("multiedit:")) {
			asset = archive.getAsset(id);
			break;
		}
	}
	if (hasempty){
		//error out here
		log.info("configuration error: externalid ${external} cannot be shown on this view");
		return;
	}
	if (!asset){
		log.info("no asset found, aborting");
		return;
	}
	String searchtype = req.getRequestParameter("searchtype");
	if (!searchtype || searchtype == "asset"){ //no op
		log.info("searchtype problem $searchtype, aborting");
		return;
	}
	Searcher searcher = archive.getSearcher(searchtype);
	Data data = req.getPageValue("data");
	if (!data){
		log.info("no data saved, aborting");
		return;// nothing saved
	}
	//handle delete
	boolean deleterow = Boolean.parseBoolean(req.getRequestParameter("delete"));
	//iterate through fields
	String [] fields = req.getRequestParameters("field");//these are the fields on the view
	log.info("Processing : ${fields}");
	//need to search for a field that is not empty and references an asset; this will be the related field
	boolean update = false;
	for (String field:fields){
		String val = req.getRequestParameter("${field}.value");
		PropertyDetail detail = searcher.getDetail(field);
		if(detail.isList()){
			String remotesearcherid = detail.getListId();
			//only look at those details configured as lists, and foreign list id is asset
			if ("asset".equals(remotesearcherid) == false){
				continue;
			}
			Searcher remotesearcher = archive.getSearcher(remotesearcherid);
			Data remotedata = val ? remotesearcher.searchById(val) : null;
			//for now, use dependson field since not used here
			String dependson = detail.get("dependson");//should be found in only one of the pairs
			String dependsonval = dependson ? data.get("${dependson}") : null; //get the value of dependson field
			//handle delete request
			if (dependsonval && deleterow){
				val = null;//set depends on field to null, so that paired data entry will be deleted
				data.setProperty("${detail.getId()}",val);
			} else {
				//update related fields
				if (updateRelatedFields(req,archive,searcher,detail,data,remotedata)){
					update = true;
				}
			}
			if (dependson){
				PropertyDetail detail2 = searcher.getDetail(dependson);
				if (detail2 && detail2.isList() && detail2.getListId() == "asset"){
					//dependson should be same as external
					if (external != dependson){
						log.info("@@@@@@@ configuration error: dependson should be the same as external, not creating second entry @@@@@@@");
						continue;
					}
					//get original values
					String original1 = req.getRequestParameter("${dependson}.original")
					String original2 = req.getRequestParameter("${field}.original")
					log.info("${dependson}: current = $dependsonval, original = $original1");
					log.info("${field}: current = $val, original = $original2");
					boolean delete = false;
					boolean add = true;
					if (original2){
						if (!val){
							//if val is null, then the related asset was deleted
							//so need to search for old value, delete entry, but don't add a new one
							delete = true;
							add = false;
						} else if (val!=original2){
							//if original1 is not equal to val, then the related asset changed (was replaced)
							//so need to search for the old value first, delete, then add the new one
							delete = true;
						}
					}
					//create search query
					SearchQuery query = searcher.createSearchQuery();
					if (delete){
						log.info("delete query: $query");
						query.addExact("$dependson",original2);
						query.addExact("$field",dependsonval);
						HitTracker hits = searcher.search(query);
						if (hits.isEmpty() == false){
							//should only be one to delete
							Data entry = (Data) searcher.searchById(((Data) hits.first()).getId());
							searcher.delete(entry,null);
						}
						//need to create new query in case add is required
						query = searcher.createSearchQuery();
					}
					if (add){
						query.addExact("$dependson",val);
						query.addExact("$field",dependsonval);
						log.info("add query: $query");
						HitTracker hits = searcher.search(query);
						Data entry = null;
						if (hits.isEmpty()){
							entry = searcher.createNewData();
							entry.setProperty("$dependson",val);
							entry.setProperty("$field",dependsonval);
							log.info("Search was empty:" + query);
						} else {
							entry = (Data) searcher.loadData( ((Data) hits.first()));
							log.info("Search had one:" + query + " " + entry);
						}
						//need the remote data
						Data remotedata2 = remotesearcher.searchById(dependsonval);
						//swap detail w detail2, use corresponding remote datas
						updateRelatedFields(req,archive,searcher,detail2,entry,remotedata);//detail2 w remotedata
						updateRelatedFields(req,archive,searcher,detail,entry,remotedata2);//detail w remotedata2
						searcher.saveData(entry,null);
					}
				}
			}
		}
	}
	if (deleterow){
		searcher.delete(data,null);
	} else if (update){
		searcher.saveData(data,null);
	}
	String redirect = req.getRequestParameter("redirect");
	if (redirect){
		req.redirect(redirect);
	}
}

public boolean updateRelatedFields(WebPageRequest req, MediaArchive archive, Searcher searcher, PropertyDetail detail, Data data, Data remotedata){
	log.info("Updating: ${detail.getId()} on  ${data} from Asset:${remotedata}");
	boolean updated = false;
	Collection details = searcher.getPropertyDetails();
	for (Iterator iterator = details.iterator(); iterator.hasNext();){
		PropertyDetail propdetail = (PropertyDetail) iterator.next();
		String id = propdetail.getId();
		if (id == detail.getId() || !id.startsWith(detail.getId()+".") ){
			continue;
		}
//		String refvalue = data.get(detail.getId());
		String input = data.get(id);//current value
		String field = id.substring(id.indexOf(".")+1);//remote field
		String refvalue = remotedata ? remotedata.get(field) : null;//if remotedata is present, get value from it
		if (refvalue == input){
			continue;
		}
		data.setProperty(id,refvalue);
		updated = true;
	}
	return updated;
}


init();