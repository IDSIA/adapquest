package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:41
 */
@Entity
@Data
@NoArgsConstructor
public class QuestionLevel implements Comparable<QuestionLevel> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Human name of this difficulty level.
	 */
	private String name;

	/**
	 * Numeric value of this difficulty level. Used for sorting from lower to high.
	 */
	private Double points = 1.;

	public QuestionLevel(String name, Double points) {
		this.name = name;
		this.points = points;
	}

	public QuestionLevel(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(QuestionLevel other) {
		return Long.compare(this.id, other.id);
	}

	@Override
	public String toString() {
		return String.format("%d %.2f %s", id, points, name);
	}
}
