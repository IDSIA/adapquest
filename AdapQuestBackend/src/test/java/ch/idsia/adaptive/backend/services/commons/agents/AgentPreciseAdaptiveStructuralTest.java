package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.SurveyStructureRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.State;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    19.10.2021 09:57
 */
class AgentPreciseAdaptiveStructuralTest {

	@Test
	void structuralModelMultiChoice() {
		final ImportStructure structure = SurveyStructureRepository.structure3S3Q4C("");
		final Survey survey = InitializationService.parseSurveyStructure(structure);

		final AgentPreciseAdaptiveStructural agent = new AgentPreciseAdaptiveStructural(survey, 42L, new ScoringFunctionExpectedEntropy());
		final int[][] answers = {
				{0, 2},
				{1, 2},
				{0, 3}
		};

		Question q;
		State state;

		state = agent.getState();
		state.probabilities.forEach((k, v) -> System.out.println(k + ": " + Arrays.toString(v)));
		System.out.println();

		try {
			int i = 0;
			while ((q = agent.next()) != null) {
				agent.check(new Answer(q.getQuestionAnswer(answers[i][0])));
				agent.check(new Answer(q.getQuestionAnswer(answers[i++][1])));
				state = agent.getState();
				System.out.println(q);
				state.probabilities.forEach((k, v) -> System.out.println(k + ": " + Arrays.toString(v)));
				System.out.println();
			}
		} catch (SurveyException ignored) {
		}
	}
}