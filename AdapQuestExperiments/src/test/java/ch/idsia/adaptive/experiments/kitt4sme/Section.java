package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:10
 */
public class Section {

	final int sectionId;
	final String name;

	public Section(int sectionId, String name) {
		this.sectionId = sectionId;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Section section = (Section) o;
		return name.equals(section.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Section{" +
				"sectionId=" + sectionId +
				", name='" + name + '\'' +
				'}';
	}
}
