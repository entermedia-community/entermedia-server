import org.entermediadb.asset.importer.BaseImporter
import org.entermediadb.asset.util.Row
import org.openedit.Data

class CsvImporter extends BaseImporter
{
	/**
	 * This is an example of making a field lower case
	 */
	protected void addProperties( Row inRow, Data inData)
	{
		super.addProperties( inRow, inData);
		//createLookUp(inSearcher.getCatalogId(),inData,"Division","val_divisions");
	}

}


CsvImporter csvimporter = new CsvImporter();
csvimporter.setModuleManager(moduleManager);
csvimporter.setContext(context);
csvimporter.setLog(log);
csvimporter.setMakeId(false);
csvimporter.importData();
