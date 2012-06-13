import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.util.Row;

class CsvImporter extends BaseImporter
{
	/**
	 * This is an example of making a field lower case
	 */
	/*
	protected void addProperties(Searcher inSearcher, Row inRow, Data inData)
	{

		String code = inRow.get("projectcode");
		code = code.toLowerCase();
		inRow.set("projectcode",code);
		super.addProperties(inSearcher, inRow, inData);
	}
	*/
}


CsvImporter csvimporter = new CsvImporter();
csvimporter.setModuleManager(moduleManager);
csvimporter.setContext(context);
csvimporter.importData();
