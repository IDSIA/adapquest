package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:10
 */
public class KSection {

	final int sectionId;
	final String name;

	public KSection(int sectionId, String name) {
		this.sectionId = sectionId;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KSection section = (KSection) o;
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
