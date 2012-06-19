import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.util.Row;

class CsvImporter extends BaseImporter
{
	/**
	 * This is an example of making a field lower case
	 */
	protected void addProperties(Searcher inSearcher, Row inRow, Data inData)
	{
		super.addProperties(inSearcher, inRow, inData);
		//createLookUp(inSearcher.getCatalogId(),inData,"Division","val_divisions");
	}

}


CsvImporter csvimporter = new CsvImporter();
csvimporter.setModuleManager(moduleManager);
csvimporter.setContext(context);
csvimporter.importData();
