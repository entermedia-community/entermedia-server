import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.convert.ConvertInstructions
import org.entermediadb.elemental.ElementalManager
import org.openedit.Data
import org.openedit.data.BaseData

public void init() {
	
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	ElementalManager manager = archive.getBean("elementalManager");
	manager.getJobs();
	ConvertInstructions ins = new ConvertInstructions(archive);
	ins.setProperty("preset", "10");
	manager.createJob(ins);
	Data task = new BaseData();
	task.setProperty("externalid", "12722");
	manager.updateJobStatus(task);
}



init();
