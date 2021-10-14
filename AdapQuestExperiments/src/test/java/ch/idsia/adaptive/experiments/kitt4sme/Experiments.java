package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.experiments.utils.ProgressBar;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.inference.sampling.BayesianNetworkSampling;
import ch.idsia.crema.model.graphical.BayesianNetwork;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

	@BeforeEach
	void setUp() throws Exception {
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
	}

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

	@Test
	public void testPilotProfiles18() throws Exception {
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

		filename = "adaptive.results.given_profiles_18.tsv";
		model = UAIParser.read("AdaptiveQuestionnaire.model.uai");

		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		final ProgressBar p = new ProgressBar(names.size());
		p.print();

		final List<Callable<Void>> tasks = names.values().stream()
				.map(name -> (Callable<Void>) () -> {
					final long startTime = System.currentTimeMillis();
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
						int j = N_SKILLS + q;

						for (int a = 0; a < NODES_PER_QUESTION[q]; a++) {
							final int v = answers.get(name)[q][a];
							obs.put(j, v);
							j++;
						}

						output = new ArrayList<>();
						output.add("" + name);
						output.add("" + q);
						for (int s = 0; s < N_SKILLS; s++) {
							output.add("" + profile[s]);
						}
						for (int s = 0; s < N_SKILLS; s++) {
							final BayesianFactor f = inf.query(model, obs, s);
							final double d = f.getValue(1);
							output.add("" + d);
						}

						content.add(String.join("\t", output));
					}

					final long endTime = System.currentTimeMillis();

					p.update(endTime - startTime);

					write(content);

					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
	}


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
