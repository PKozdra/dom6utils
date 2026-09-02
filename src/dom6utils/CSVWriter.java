package dom6utils;
/* This file is part of dom6utils.
*
* dom6utils is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* dom6utils is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with dom6utils.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class CSVWriter {

	enum Delimiter {
		COMMA, TAB,
	};
	
	//Spreadsheet type
	enum SSType {
		CSV, XLSX,
	}
	
	private static String getFilePathWithExtension(String basename, SSType ssType) {
		if (basename == null || basename.isEmpty()) {
			return null;
		}
		switch (ssType) {
		case CSV:
			return Paths.get(CSV_OUTPUT_DIR_NAME, basename + ".csv").toString();
		case XLSX:
		default:
			return Paths.get(basename + ".xlsx").toString();
		}
	}
	
	
	public static void createCSVOutputDirectory() throws IOException {
		Files.createDirectories(Paths.get(CSV_OUTPUT_DIR_NAME));
	}
	
	public static FileOutputStream getFOS (String basename, SSType ssType) throws IOException {
		if (basename != null && !basename.isEmpty()) {
			return new FileOutputStream(CSVWriter.getFilePathWithExtension(basename, ssType));
		}
		return null;
	}
	
	public static BufferedWriter getBFW (String basename, SSType ssType) throws IOException {
		if (basename != null && !basename.isEmpty()) {
			return new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(CSVWriter.getFilePathWithExtension(basename, ssType)),
					StandardCharsets.ISO_8859_1));
		}
		return null;
	}
	
	public static void writeSimpleCSV(XSSFSheet sheet, BufferedWriter writer, Delimiter delim) throws IOException {
		final char delimChar = getDelimeterChar(delim);
		
		if (sheet != null)
		{
			int columnsInWidestRow = 0;
			
			//first, we need to get the number of columns in the row that has the most defined columns
			for (Row curRow : sheet) {
				columnsInWidestRow = Math.max(columnsInWidestRow, curRow.getLastCellNum());
			}
			
			// Locale-independent number rendering
			DataFormatter df = new DataFormatter(Locale.US, true);
			for (Row curRow : sheet) {
				if (curRow != null) {
					boolean firstColumn = true;
					for (short cellNum = 0; cellNum < columnsInWidestRow; ++cellNum) {
						Cell curCell = curRow.getCell(cellNum); //it's safe to call this even for empty cells... we're just passed null back
						if (firstColumn) {
							//for the first column, we don't need to prepend a delimiter
							firstColumn = false;
						}
						else {
							//for all non-first columns, we need to add a delimiter
							writer.write(delimChar);
						}
						
						if (curCell != null) {
							writer.write(utf8Safe(df.formatCellValue(curCell)));
						}
					}
				}
				///The dom5inspector is expecting unix newlines for the CSV files
				writer.write('\n');
			}
		}
	}
	
	/**
	 * Cell text arrives as one char per exe byte (every indexer reads the exe as
	 * ISO-8859-1) and the CSV writer emits it as ISO-8859-1, so UTF-8 strings in the
	 * exe round-trip byte for byte. A few strings (the fixed names) are stored as
	 * Latin-1 in the exe; those would come out as invalid UTF-8, so we re-encode them.
	 */
	private static String utf8Safe(String s) {
		byte[] raw = s.getBytes(StandardCharsets.ISO_8859_1);
		try {
			StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
				.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
				.decode(java.nio.ByteBuffer.wrap(raw));
			return s;
		} catch (java.nio.charset.CharacterCodingException e) {
			return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		}
	}

	private static char getDelimeterChar(Delimiter delim) {
		switch (delim) {
		case COMMA:
			return ',';
		case TAB:
		default:
			return '\t';
			
		}
	}
	
	private static final String CSV_OUTPUT_DIR_NAME = "csv_output";
	
	
}
