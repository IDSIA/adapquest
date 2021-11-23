package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.agents.AgentPreciseAdaptiveStructural;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import ch.idsia.adaptive.experiments.utils.ProgressBar;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.inference.sampling.BayesianNetworkSampling;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import ch.idsia.crema.model.io.uai.UAIParser;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    04.10.2021 16:33
 */
public class Experiments {
	private static final Logger logger = LoggerFactory.getLogger(Experiments.class);

	static final int PARALLEL_COUNT = Runtime.getRuntime().availableProcessors();

	static final int N_SAMPLES_PER_PROFILE = 100;

	static final boolean INCLUDE_NEGATIVE_ANS = true;

	static final int N_SKILLS = 17;

	static final int[] SKILLS = IntStream.range(0, N_SKILLS).toArray();
	static final int[] QUESTIONS = IntStream.range(N_SKILLS, 105).toArray();

	static final int[] NODES_PER_QUESTION = new int[]{
			9, 4, 9, 3, 3, 8, 5, 5, 7, 6, 5, 3, 2, 13, 4, 6, 5, 8
	};


	private Map<String, Random> randoms;

	private ExecutorService es;

	private BayesianNetwork model;

	private String filename;

	private int[] generateProfile(int i) {
		final int[] profile = new int[N_SKILLS];

		for (int j = 0; j < N_SKILLS; j++) {
			profile[j] = i % 2;
			i /= 2;
		}

		return profile;
	}

	private TIntIntMap obsSkills(int... v) {
		final TIntIntMap obs = new TIntIntHashMap();
		for (int i = 0; i < v.length; i++) {
			obs.put(i, v[i]);
		}
		return obs;
	}

	private synchronized void write(List<String> lines) {
		try {
			Files.write(Paths.get(filename), lines, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (Exception e) {
			System.err.println("Could not write to file");
			e.printStackTrace();
		}
	}

	private long starTime;

	@BeforeEach
	void setUp() throws Exception {
		starTime = System.currentTimeMillis();
		randoms = new ConcurrentHashMap<>();
		RandomUtil.setRandom(() -> randoms.get(Thread.currentThread().getName()));

		System.out.println("Cores used:  " + PARALLEL_COUNT);
		es = Executors.newFixedThreadPool(PARALLEL_COUNT);

		// both models are the same
		model = UAIParser.read("AdaptiveQuestionnaire.model.uai");
	}

	@AfterEach
	void tearDown() {
		RandomUtil.reset();
		es.shutdown();

		final long seconds = Duration.ofMillis(System.currentTimeMillis() - starTime).getSeconds();
		final String txt = String.format(
				"%02d:%02d:%02d",
				seconds / 3600,          // hours
				(seconds % 3600) / 60,   // minutes
				seconds % 60);           // seconds
		logger.info("experiment duration: {}", txt);
	}

	@Disabled
	@Test
	void sampleSingleProfile() throws Exception {
		filename = "adaptive.results.single_profile_1.0.tsv";
		model = UAIParser.read("AdaptiveQuestionnaire.model.1.0.uai");

		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		final TIntIntMap profile = obsSkills(0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0);

		final ProgressBar p = new ProgressBar(N_SAMPLES_PER_PROFILE);
		p.print();

		final List<Callable<Void>> tasks = IntStream.range(0, N_SAMPLES_PER_PROFILE)
				.mapToObj(i -> (Callable<Void>) () -> {
					final String tName = "Thread" + i;
					Thread.currentThread().setName(tName);
					randoms.put(tName, new Random(i));

					final BayesianNetworkSampling bns = new BayesianNetworkSampling();
					final TIntIntMap sample = bns.samples(model, profile, 1)[0];

					final long startTime = System.currentTimeMillis();

					final BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>();

					final TIntIntMap obs = new TIntIntHashMap();
					final List<String> content = new ArrayList<>();

					for (int k = 0; k < NODES_PER_QUESTION.length; k++) {
						int j = N_SKILLS + k;

						for (int o = 0; o < NODES_PER_QUESTION[k]; o++) {
							final int v = sample.get(j);
							obs.put(j, v);
							j++;
						}

						final List<String> output = new ArrayList<>();
						output.add("" + i);
						output.add("" + k);
						for (int s = 0; s < N_SKILLS; s++) {
							output.add("" + sample.get(s));
						}
						for (int s = 0; s < N_SKILLS; s++) {
							final BayesianFactor q = inf.query(model, obs, s);
							final double d = q.getValue(1);
							output.add("" + d);
						}

						content.add(String.join("\t", output));
					}

					final long endTime = System.currentTimeMillis();

					p.update(endTime - startTime);

					write(content);

					randoms.remove(tName);
					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
	}

	@Disabled // This will take SO MUCH time...
	@Test
	public void sampleAllProfiles() throws Exception {
		filename = "adaptive.results.all_profiles.tsv";
		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		final List<TIntIntMap> profiles = IntStream
				.range(0, (int) Math.pow(2, N_SKILLS))
				.mapToObj(this::generateProfile)
				.map(this::obsSkills)
				.collect(Collectors.toList());

		System.out.println("Total tasks: " + profiles.size());

		final ProgressBar p1 = new ProgressBar(profiles.size()).setPrefix("Sampling");
		p1.print();

		final List<Callable<TIntIntMap[]>> tasksSampling = IntStream.range(0, profiles.size())
				.mapToObj(i -> (Callable<TIntIntMap[]>) () -> {
					final String tName = "Thread" + i;
					Thread.currentThread().setName(tName);
					randoms.put(tName, new Random(i));

					final TIntIntMap profile = profiles.get(i);

					final long startTime = System.currentTimeMillis();

					final BayesianNetworkSampling bns = new BayesianNetworkSampling();
					final TIntIntMap[] samples = bns.samples(model, profile, N_SAMPLES_PER_PROFILE);

					final long endTime = System.currentTimeMillis();
					p1.update(endTime - startTime);

					randoms.remove(tName);
					return samples;
				})
				.collect(Collectors.toList());

		final List<Future<TIntIntMap[]>> futSampling = es.invokeAll(tasksSampling);

		final List<TIntIntMap> samples = new ArrayList<>();

		for (Future<TIntIntMap[]> future : futSampling) {
			final TIntIntMap[] sample = future.get();
			Collections.addAll(samples, sample);
		}

		final ProgressBar p2 = new ProgressBar(samples.size()).setPrefix("Process ");
		p2.print();

		final List<Callable<Void>> tasks = IntStream.range(0, samples.size())
				.mapToObj(i -> (Callable<Void>) () -> {
					final String tName = "Thread" + i;
					Thread.currentThread().setName(tName);
					randoms.put(tName, new Random(i));

					final long startTime = System.currentTimeMillis();

					final TIntIntMap sample = profiles.get(i);
					final BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>();

					final TIntIntMap obs = new TIntIntHashMap();
					final List<String> content = new ArrayList<>();

					for (int k = 0; k < NODES_PER_QUESTION.length; k++) {
						int j = N_SKILLS + k;

						for (int o = 0; o < NODES_PER_QUESTION[k]; o++) {
							final int v = sample.get(j);
							if (INCLUDE_NEGATIVE_ANS || v == 1) {
								obs.put(j, v);
							}
							j++;
						}

						final List<String> profile = new ArrayList<>();
						profile.add("" + i);
						profile.add("" + k);
						for (int s = 0; s < N_SKILLS; s++) {
							profile.add("" + sample.get(s));
						}
						for (int s = 0; s < N_SKILLS; s++) {
							final BayesianFactor q = inf.query(model, obs, s);
							final double d = q.getValue(1);
							profile.add("" + d);
						}

						content.add(String.join("\t", profile));
					}

					final long endTime = System.currentTimeMillis();

					p2.update(endTime - startTime);

					write(content);

					randoms.remove(tName);
					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
	}

	@Disabled
	@Test
	public void testPilotProfilesNonAdaptive() throws Exception {
		final Survey survey = InitSurvey.init("AdaptiveQuestionnaire.multiple.survey.json");
		final List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());
		final Set<Skill> skills = survey.getSkills();

		model = new BayesUAIParser(lines).parse();
		final List<KProfile> profiles = KProfile.read();

		logger.info("Found {} profiles", profiles.size());

		filename = "adaptive.results.given_profiles_18.tsv";

		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		final ProgressBar p = new ProgressBar(profiles.size() * 18);
		p.print();

		final List<Callable<Void>> tasks = profiles.stream()
				.map(profile -> (Callable<Void>) () -> {
					try {
						final BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>();

						final TIntIntMap obs = new TIntIntHashMap();
						final List<String> content = new ArrayList<>();

						List<String> output = new ArrayList<>();
						output.add("" + profile.name);
						output.add("" + -1);
						for (Skill skill : skills)
							output.add("" + profile.skills.get(skill.getName()));
						for (Skill skill : skills) {
							final BayesianFactor f = inf.query(model, obs, skill.getVariable());
							final double d = f.getValue(1);
							output.add("" + d);
						}
						content.add(String.join("\t", output));

						for (int q = 0; q < NODES_PER_QUESTION.length; q++) {
							final long startTime = System.currentTimeMillis();

							int j = N_SKILLS + q;

							for (int a = 0; a < NODES_PER_QUESTION[q]; a++) {
								final int v = profile.answer("Q" + (q + 1), "A" + (a + 1));
								if (v == 1)
									obs.put(j, v);
								j++;
							}

							output = new ArrayList<>();
							output.add("" + profile.name);
							output.add("" + q);
							for (Skill skill : skills)
								output.add("" + profile.skills.get(skill.getName()));
							for (Skill skill : skills) {
								final BayesianFactor f = inf.query(model, obs, skill.getVariable());
								final double d = f.getValue(1);
								output.add("" + d);
							}

							content.add(String.join("\t", output));

							final long endTime = System.currentTimeMillis();

							p.update(endTime - startTime);
						}

						write(content);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				})
				.collect(Collectors.toList());

		p.print();

		es.invokeAll(tasks);
	}

	public void setup1(Survey s) {
		for (Question question : s.getQuestions()) {
			question.setYesOnly(true);
		}
	}

	public void setup2(Survey s) {
		for (Question question : s.getQuestions()) {
			switch (question.getName()) {
				case "Q1":
				case "Q2":
				case "Q4":
				case "Q12":
				case "Q13":
					question.setYesOnly(false);
					break;
				default:
					question.setYesOnly(true);
			}
		}
	}

	@Disabled
	@Test
	public void testPilotProfilesAdaptive2Setup() throws Exception {
		final List<KProfile> profiles = KProfile.read();
		logger.info("Found {} profiles", profiles.size());

		final Survey survey = InitSurvey.init("AdaptiveQuestionnaire.multiple.survey.json");
		survey.setQuestionTotalMin(18);

		setup1(survey);
		filename = "adaptive.results.given_profiles_18_setup1.tsv";
		defaultAdaptiveExperiment(profiles, survey);

		setup2(survey);
		filename = "adaptive.results.given_profiles_18_setup2.tsv";
		defaultAdaptiveExperiment(profiles, survey);
	}

	@Disabled
	@Test
	public void testPilotProfilesAdaptive() throws Exception {
		final List<KProfile> profiles = KProfile.read();
		logger.info("Found {} profiles", profiles.size());

		final Survey survey = InitSurvey.init("AdaptiveQuestionnaire.multiple.survey.json");
		survey.setQuestionTotalMin(18);

		filename = "adaptive.results.6profiles_fixedStrength.tsv";
		defaultAdaptiveExperiment(profiles, survey);
	}

	private void defaultAdaptiveExperiment(List<KProfile> profiles, Survey survey) throws IOException, InterruptedException {
		logger.info("Experiment {}", filename);
		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

//		final ProgressBar p = new ProgressBar(profiles.size() * 18);
//		p.print();

		final List<Callable<Void>> tasks = profiles.stream()
				.map(profile -> (Callable<Void>) () -> {
					final List<String> content = new ArrayList<>();
					final ExecutorService e = Executors.newFixedThreadPool(PARALLEL_COUNT);
					final AgentPreciseAdaptiveStructural agent = new AgentPreciseAdaptiveStructural(survey, 42L, new ScoringFunctionExpectedEntropy());
					agent.setExecutor(e);
					final Set<String> skills = profile.skills.keySet();

					List<String> output;

					try {
						Question question;
						State state;
						double avgScore;

						state = agent.getState();

						output = new ArrayList<>();
						output.add("" + profile.name); // profile name
						output.add("INIT"); // question
						output.add(""); // answer
						output.add(""); // answer given
						for (String skill : skills) {
							final double d = state.probabilities.get(skill)[1];
							output.add("" + d); // P(skill)
						}
						avgScore = 0.0;
						for (String skill : skills) {
							final double score = state.score.get(skill);
							avgScore += score / skills.size();
						}
						output.add("" + avgScore); // H(avg)
						output.add("" + agent.getObservations()); // observations

						content.add(String.join("\t", output));

						while ((question = agent.next()) != null) {
							content.add("");
							final long startTime = System.currentTimeMillis();

							final String q = question.getName();

							final List<QuestionAnswer> checked = new ArrayList<>();
							for (QuestionAnswer qa : question.getAnswersAvailable()) {
								final String a = qa.getName();
								final int ans = profile.answer(q, a);

								if (ans == qa.getState()) {
									checked.add(qa);
									logger.debug("{} {} {} {}", profile.name, q, a, ans);

									output = new ArrayList<>();
									output.add("" + profile.name); // profile
									output.add("" + q); // question
									output.add("" + a); // answer

									double v;
									if (question.getYesOnly()) {
										v = ans == 0 ? 0 : +1;
									} else {
										v = ans == 0 ? -1 : +1;
									}
									output.add("" + v); // answer given
									for (double d : profile.weights.get(q).get(a)) {
										output.add("" + (v * d)); // P(x)
									}
									output.add(""); // H(avg)
									content.add(String.join("\t", output));
								}

							}

							checked.forEach(qa -> agent.check(new Answer(qa)));

							state = agent.getState();

							output = new ArrayList<>();
							output.add("" + profile.name); // profile
							output.add("" + q); // question
							output.add(""); // answer
							output.add(""); // answer given

							for (String s : skills) {
								final double d = state.getProbabilities().get(s)[1];
								output.add("" + d); // P(x)
							}
							avgScore = 0.0;
							for (String skill : skills) {
								final double score = state.score.get(skill);
								avgScore += score / skills.size();
							}
							output.add("" + avgScore); // H(avg)
							output.add("" + agent.getObservations());

							content.add(String.join("\t", output));

//							final long endTime = System.currentTimeMillis();

							logger.debug("{}", output);

//							p.update(endTime - startTime);
						}

					} catch (Exception ex) {
						final String message = ex.getMessage();
						if (!"Finished".equals(message)) {
							logger.warn("{}", message);
							ex.printStackTrace();
						}
					}

					output = new ArrayList<>();
					output.add("" + profile.name); // profile
					output.add("TRUE"); // question
					output.add(""); // answer
					output.add(""); // answer given
					for (String s : skills) {
						output.add("" + profile.skills.get(s)); // P(x)
					}
					content.add("");
					content.add(String.join("\t", output));

					write(content);

//					p.print();
					e.shutdown();

					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
	}

	@Disabled
	@Test
	public void testPilotProfiles105() throws Exception {
		// reading answers and profiles
		final Map<Integer, String> names = new HashMap<>();
		final Map<String, int[]> profiles = new HashMap<>();
		final Map<String, int[][]> answers = new HashMap<>();

		final File file = new File("AdaptiveQuestionnaire.xlsx");

		final Workbook workbook = new XSSFWorkbook(file);

		final Sheet sheetSkills = workbook.getSheet("Pilot Skill");
		final Sheet sheetPilotAnswers = workbook.getSheet("Pilot Answers");

		// profiles parsing
		for (Row row : sheetSkills) {
			if (row.getRowNum() == 0) {
				for (int j = 2; j < row.getLastCellNum(); j++) {
					final String profile = row.getCell(j).getStringCellValue();
					names.put(j, profile);
					profiles.put(profile, new int[N_SKILLS]);
				}

				continue;
			}

			for (int j = 2, k = 0; k < names.size(); j++, k++) { // two columns on the left
				final int s = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
				profiles.get(names.get(j))[row.getRowNum() - 1] = s;
			}
		}

		// answers parsing
		names.clear();
		int Q = -1;
		for (Row row : sheetPilotAnswers) {
			if (row.getRowNum() == 0)
				continue;

			if (row.getRowNum() == 1) {
				for (int j = 5, k = 0; k < profiles.size(); j++, k++) {
					final String profile = row.getCell(j).getStringCellValue();
					names.put(j, profile);
					int[][] ans = new int[NODES_PER_QUESTION.length][];
					for (int m = 0; m < NODES_PER_QUESTION.length; m++) {
						ans[m] = new int[NODES_PER_QUESTION[m]];
					}
					answers.put(profile, ans);
				}

				continue;
			}

			if (row.getRowNum() == 107) {
				break;
			}

			final int A = Double.valueOf(row.getCell(3).getNumericCellValue()).intValue();
			if (A == 1) {
				Q = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue();
			}

			final int a = A - 1;
			final int q = Q - 1;

			for (int j = 5, k = 0; k < names.size(); j++, k++) {
				answers.get(names.get(j))[q][a] = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
			}
		}

//		for (String name : names.values()) {
//			System.out.println(name);
//			System.out.println("Skills: " + Arrays.toString(profiles.get(name)));
//			final int[][] ans = answers.get(name);
//			for (int[] an : ans) {
//				System.out.println(Arrays.toString(an));
//			}
//		}

		filename = "adaptive.results.given_profiles_105.tsv";
		model = UAIParser.read("AdaptiveQuestionnaire.model.uai");

		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		final ProgressBar p = new ProgressBar(names.size() * 105);
		p.print();

		final List<Callable<Void>> tasks = names.values().stream()
				.map(name -> (Callable<Void>) () -> {
					final int[] profile = profiles.get(name);

					final BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>();

					final TIntIntMap obs = new TIntIntHashMap();
					final List<String> content = new ArrayList<>();

					List<String> output = new ArrayList<>();
					output.add("" + name);
					output.add("" + -1);
					output.add("" + -1);
					for (int s = 0; s < N_SKILLS; s++)
						output.add("" + profile[s]);
					for (int s = 0; s < N_SKILLS; s++) {
						final BayesianFactor f = inf.query(model, obs, s);
						final double d = f.getValue(1);
						output.add("" + d);
					}
					content.add(String.join("\t", output));

					for (int q = 0; q < NODES_PER_QUESTION.length; q++) {
						final long startTime = System.currentTimeMillis();

						int j = N_SKILLS + q;

						for (int a = 0; a < NODES_PER_QUESTION[q]; a++) {
							final int v = answers.get(name)[q][a];
							obs.put(j, v);
							j++;

							output = new ArrayList<>();
							output.add("" + name);
							output.add("" + q);
							output.add("" + a);
							for (int s = 0; s < N_SKILLS; s++)
								output.add("" + profile[s]);
							for (int s = 0; s < N_SKILLS; s++) {
								final BayesianFactor f = inf.query(model, obs, s);
								final double d = f.getValue(1);
								output.add("" + d);
							}

							content.add(String.join("\t", output));

							final long endTime = System.currentTimeMillis();
							p.update(endTime - startTime);
						}
					}

					write(content);

					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
	}

}
