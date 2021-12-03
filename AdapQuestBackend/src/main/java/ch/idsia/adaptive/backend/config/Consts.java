package ch.idsia.adaptive.backend.config;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 15:56
 */
public class Consts {

	/**
	 * Stop the skill query when its precision is above this threshold.
	 */
	public static double STOP_THRESHOLD = 0.9;

	/**
	 * Stop the multi skill query when its precision is below this threshold.
	 */
	public static double SCORE_LEVEL = 0.35;

	/**
	 * Minimal number of questions for each skill.
	 */
	public static int QUESTION_NUM_MIN = 15;

	/**
	 * Minimal number of questions to start check for the validity of a skill.
	 */
	public static int QUESTION_CHECK_MIN = 20;

	/**
	 * Minimal number of questions for every skill in total.
	 */
	public static int QUESTION_TOT_MIN = 30;

	public static int QUESTION_SKILL_MAX = 35;

	public static String NO_SKILL = "NoSkill";
}
