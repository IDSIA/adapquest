package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.AdaptiveModel;
import ch.idsia.adaptive.backend.persistence.model.Status;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.persistence.model.SurveyData;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    03.12.2020 09:48
 */
@Service
public class AdaptiveModelService {

	private final SurveyRepository surveyRepository;

	// TODO: add cache for models
	private final Map<String, BeliefPropagation<BayesianFactor>> activeInference = new HashMap<>();
	private final Map<String, BayesianNetwork> activeModels = new HashMap<>();

	@Autowired
	public AdaptiveModelService(SurveyRepository surveyRepository) {
		this.surveyRepository = surveyRepository;
	}

	private AdaptiveModel getModel(SurveyData data) {
		Survey survey = surveyRepository.findById(data.getSurveyId())
				.orElseThrow(() -> new IllegalArgumentException("No model associated with SurveyId=" + data.getSurveyId()));
		return survey.getModel();
	}

	private BayesianNetwork getNetwork(SurveyData data) {
		AdaptiveModel model = getModel(data);

		List<String> lines = Arrays.stream(model.getData().split("\n")).collect(Collectors.toList());
		return new BayesUAIParser(lines).parse();
	}

	/**
	 * Load the {@link AdaptiveModel} associated with the active session stored in the given {@link SurveyData}.
	 *
	 * @param data the {@link SurveyData} passed must be initialized correctly from a {@link SessionService}.
	 * @throws IllegalArgumentException when the survey id is not valid
	 */
	public void init(SurveyData data) {
		BayesianNetwork bn = getNetwork(data);

		BeliefPropagation<BayesianFactor> bp = new BeliefPropagation<>(bn);
		bp.fullPropagation();

		activeModels.put(data.getToken(), bn);
		activeInference.put(data.getToken(), bp);
	}

	public Status getState(SurveyData data) {
		String token = data.getToken();
		if (!activeModels.containsKey(token))
			throw new IllegalArgumentException("Cannot load status: no model for token=" + token);
		if (!activeInference.containsKey(token))
			throw new IllegalArgumentException("Cannot load status: no inference for token=" + token);

		BeliefPropagation<BayesianFactor> bp = activeInference.get(token);
		AdaptiveModel am = getModel(data);

		Map<String, double[]> state = am.getSkillToVariable()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> bp.query(e.getValue()).getData()
				));

		return new Status()
				.setState(state);
	}
}
