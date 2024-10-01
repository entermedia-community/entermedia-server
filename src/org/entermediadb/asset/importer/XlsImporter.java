package org.entermediadb.asset.importer;

public class XlsImporter extends BaseImporter {

	public void importData() throws Exception {
		
		/*
		InputStream file = getImportPage().getInputStream();

		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);

		// Get first/desired sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);

		// Iterate through each rows one by one
		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {

			Row row = rowIterator.next();

			// For each row, iterate through all the columns
			Iterator<Cell> cellIterator = row.cellIterator();

			while (cellIterator.hasNext()) {

				Cell cell = cellIterator.next();

				// Check the cell type and format accordingly
				switch (cell.getCellType()) {
				//case Cell.CELL_TYPE_NUMERIC:
				default:
					System.out.print(cell.getNumericCellValue() + "t");
					break;
				//case Cell.CELL_TYPE_STRING:
//					System.out.print(cell.getStringCellValue() + "t");
//					break;
				}
			}
			System.out.println("");
		}
		file.close();
		*/
	}
}
