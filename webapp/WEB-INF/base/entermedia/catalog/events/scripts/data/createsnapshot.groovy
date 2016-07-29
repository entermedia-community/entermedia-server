import org.openedit.node.NodeManager

public void init() {
	
	String catalogid = context.findValue("catalogid");
	log.info("starting snapshot ");
	
	NodeManager nodeManager = moduleManager.getBean(catalogid,"nodeManager");
	String snapshot = nodeManager.createSnapShot(catalogid);
	log.info("created " + snapshot);
	
}


init();