package ch.idsia.adaptive.backend.services.commons;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    26.11.2021 09:18
 */
public class OutFile {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final File file;

	public OutFile(File file) {
		this.file = file;
	}

	public String name() {
		return file.getName();
	}

	public String date() {
		final Date date = new Date(file.lastModified());
		return SDF.format(date);
	}
}
