package ch.idsia.adaptive.backend.services.commons.profiles;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 14:15
 */
public class Content {

	private static enum CType {
		STRING, DOUBLE, INTEGER
	}

	private static class Cell {
		String s;
		Double d;
		Integer i;
		final CType t;

		public Cell(String s) {
			this.s = s;
			t = CType.STRING;
		}

		public Cell(Double d) {
			this.d = d;
			t = CType.DOUBLE;
		}

		public Cell(Integer i) {
			this.i = i;
			t = CType.INTEGER;
		}
	}

	private final List<List<Cell>> content;

	public Content() {
		content = new ArrayList<>();
		content.add(new ArrayList<>());
	}

	private void add(Cell c) {
		content.get(content.size() - 1).add(c);
	}

	public void add(String s) {
		add(new Cell(s));
	}

	public void add(Double d) {
		add(new Cell(d));
	}

	public void add(Integer i) {
		add(new Cell(i));
	}

	public void newLine() {
		content.add(new ArrayList<>());
	}

	public int row(Sheet sheet, int r) {
		for (List<Cell> cells : content) {
			final Row row = sheet.createRow(r++);

			int i = 0;
			for (Cell cell : cells) {
				switch (cell.t) {
					case DOUBLE:
						row.createCell(i++, CellType.NUMERIC).setCellValue(cell.d);
						break;
					case INTEGER:
						row.createCell(i++, CellType.NUMERIC).setCellValue(cell.i);
						break;
					case STRING:
					default:
						row.createCell(i++, CellType.STRING).setCellValue(cell.s);
						break;
				}
			}
		}

		return r;
	}

	public static int header(Row row, Set<String> skills) {
		int i = 0;
		row.createCell(i++, CellType.STRING).setCellValue("Profile");
		row.createCell(i++, CellType.STRING).setCellValue("Q");
		row.createCell(i++, CellType.STRING).setCellValue("A");
		row.createCell(i++, CellType.STRING).setCellValue("Answer");

		for (String skill : skills) {
			row.createCell(i++, CellType.STRING).setCellValue(skill);
		}

		row.createCell(i++, CellType.STRING).setCellValue("Havg");
		row.createCell(i, CellType.STRING).setCellValue("Observed skills");

		return i;
	}
}
