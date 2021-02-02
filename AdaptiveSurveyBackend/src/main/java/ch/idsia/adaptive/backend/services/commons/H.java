package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    02.02.2021 14:23
 */
final class H {
	final Skill skill;
	final Question question;

	public H(Skill skill, Question question) {
		this.skill = skill;
		this.question = question;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		H h = (H) other;
		return Objects.equals(skill, h.skill) && Objects.equals(question, h.question);
	}

	@Override
	public int hashCode() {
		return Objects.hash(skill, question);
	}
}
