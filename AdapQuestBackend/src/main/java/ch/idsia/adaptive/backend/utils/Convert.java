package ch.idsia.adaptive.backend.utils;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.responses.*;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 15:58
 * <p>
 * These methods are used to convert object of the model layer in object used in response.
 */
public class Convert {

	public static ResponseQuestion toResponse(Question question) {
		ResponseQuestion rq = new ResponseQuestion();

		rq.id = question.getId();
		rq.explanation = question.getExplanation();
		rq.question = question.getQuestion();
		rq.name = question.getName();
		rq.score = question.getScore();

		rq.isExample = question.getIsExample();
		rq.isMandatory = question.getMandatory();
		rq.randomAnswers = question.getRandomAnswers();
		rq.multipleChoice = question.getMultipleChoice();

		rq.answers = question.getAnswersAvailable().stream()
				.map(Convert::toResponse)
				.collect(Collectors.toList());

		return rq;
	}

	public static ResponseAnswer toResponse(QuestionAnswer answer) {
		ResponseAnswer ra = new ResponseAnswer();

		ra.id = answer.getId();
		ra.text = answer.getText();
		ra.state = answer.getState();

		return ra;
	}

	public static ResponseData toResponse(SurveyData data) {
		ResponseData rd = new ResponseData();

		rd.code = data.getAccessCode();
		rd.token = data.getToken();
		rd.startTime = data.getStartTime();

		return rd;
	}

	public static ResponseSkill toResponse(Skill skill) {
		ResponseSkill rs = new ResponseSkill();

		rs.name = skill.getName();
		rs.variable = skill.getVariable();
		rs.states = skill.getStates().stream().map(Convert::toResponse).collect(Collectors.toList());
		rs.states.sort(Comparator.comparingInt(x -> x.state));

		return rs;
	}

	public static ResponseSkillState toResponse(SkillState state) {
		ResponseSkillState rsl = new ResponseSkillState();

		rsl.name = state.getName();
		rsl.state = state.getState();

		return rsl;
	}

	public static ResponseResult toResponse(Session session, State status) {
		ResponseResult rr = new ResponseResult();

		rr.data = session.getStartTime();
		rr.seconds = session.getElapsedSeconds();
		rr.ended = session.getEndTime() != null;
		rr.state = toResponse(status);

		return rr;
	}

	public static ResponseState toResponse(State state) {
		ResponseState rs = new ResponseState();

		rs.skillDistribution = state.getProbabilities();
		rs.scoreDistribution = state.getScore();
		rs.scoreAverage = state.getScoreAverage();
		rs.skillCompleted = state.getSkillCompleted();
		rs.questionsPerSkill = state.getQuestionsPerSkill();
		rs.creationTime = state.getCreation();
		rs.totalAnswers = state.getTotalAnswers();
		rs.skills = state.skills.values().stream().map(Convert::toResponse).collect(Collectors.toList());
		rs.skills.sort(Comparator.comparingInt(x -> x.variable));

		return rs;
	}

}
