import java.io.OutputStream;
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet;
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Statement

import org.openedit.db.util.ConnectionPool
import org.openedit.entermedia.util.CSVWriter;

import com.openedit.page.Page


class ImportWithSqlHelper extends BaseImporter
{
	
	public void importTables() throws Exception
	{
		String catalogId = context.findValue("catalogid");
		ConnectionPool pool = (ConnectionPool)getModuleManager().getBean("connectionPool");
		Connection con = pool.instance(catalogId);

		DatabaseMetaData allmd = con.getMetaData();
		ResultSet rs = allmd.getTables(null, null, "%", null);
		while (rs.next()) 
		{
			String table = rs.getString(3);
			try{
				Statement st = con.createStatement();
				ResultSet rows = st.executeQuery("SELECT * FROM " + table);
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				System.out.println("Number of Column : "+ col);
				System.out.println("Columns Name: ");
				
				Page page = getPageManager().getPage("/WEB-INF/tmp/exports/" + table + ".csv");
				OutputStream out = getPageManager().saveToStream(page);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(out,"UTF-8") );
				writer.writeAll(rows,true);
				writer.close();
			}
			catch (SQLException s)
			{
				System.out.println("SQL statement is not executed!");
			}
		}
		pool.close(con);
	}
	
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


ImportWithSqlHelper importer = new ImportWithSqlHelper();
importer.setModuleManager(moduleManager);
importer.setContext(context);
importer.importTables();
importer.importData();
