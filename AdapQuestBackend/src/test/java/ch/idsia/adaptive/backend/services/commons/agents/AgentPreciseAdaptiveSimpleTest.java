package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.SurveyStructureRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.State;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    15.10.2021 13:49
 */
class AgentPreciseAdaptiveSimpleTest {

	@Test
	void directEvidence() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure3S2QMultiChoiceDirect("");
		final Survey survey = InitializationService.parseSurveyStructure(structure);

		final AgentPreciseAdaptiveSimple agent = new AgentPreciseAdaptiveSimple(survey, 42L, new ScoringFunctionExpectedEntropy());

		Question q;
		State s;
		q = agent.next();
		agent.check(new Answer(q.getAnswersAvailable().get(1)));
		s = agent.getState();

		Assertions.assertTrue(agent.observations.containsKey(3));
		Assertions.assertTrue(agent.observations.containsKey(2));
		Assertions.assertEquals(1, agent.observations.get(3));
		Assertions.assertEquals(0, agent.observations.get(2));
		Assertions.assertEquals(1.0, s.getProbabilities().get("S2")[0]);

		q = agent.next();
		agent.check(new Answer(q.getAnswersAvailable().get(1)));
		s = agent.getState();

		Assertions.assertTrue(agent.observations.containsKey(4));
		Assertions.assertEquals(1, agent.observations.get(4));
		Assertions.assertEquals(0.0, s.getProbabilities().get("S1")[0]);
		Assertions.assertEquals(1.0, s.getProbabilities().get("S1")[1]);
		Assertions.assertEquals(1.0, s.getProbabilities().get("S2")[0]);
		Assertions.assertEquals(0.0, s.getProbabilities().get("S2")[1]);
	}

}