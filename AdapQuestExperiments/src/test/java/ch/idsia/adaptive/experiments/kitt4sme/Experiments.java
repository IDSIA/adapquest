package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.inference.sampling.BayesianNetworkSampling;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.UAIParser;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    04.10.2021 16:33
 */
public class Experiments {

	static class Progress {
		private final int total;
		private int completed;
		private long time;

		private long lastPrint = 0;

		public Progress(int total) {
			this.total = total;
			completed = 0;
			time = 0;
		}

		void print() {
			final long now = System.currentTimeMillis();
			if (now - lastPrint < 1000)
				return;

			lastPrint = now;

			if (completed > 0)
				System.out.print("\r");

			final double perc = 1.0 * time / completed;

			System.out.printf("%10d/%10d %4.2f it/s", completed, total, perc);
		}

		synchronized void update(long time) {
			completed += 1;
			this.time += time;

			print();
		}
	}

	static final String filename = "adaptive.results.tsv";

	static final int PARALLEL_COUNT = Runtime.getRuntime().availableProcessors();

	static final int N_SAMPLES_PER_PROFILE = 100;

	static final boolean INCLUDE_NEGATIVE_ANS = true;

	static final int N_SKILLS = 17;

	static final int[] SKILLS = IntStream.range(0, N_SKILLS).toArray();
	static final int[] QUESTIONS = IntStream.range(N_SKILLS, 105).toArray();

	static final double[] P_FEATURES = {
			0.240952381, 0.394285714, 0.372380952,
			0.313461538, 0.344230769, 0.405769231, 0.359615385,
			0.281730769, 0.271153846, 0.276923077, 0.303883495, 0.170192308,
			0.290291262, 0.217307692, 0.1875, 0.161538462, 0.196153846
	};

	static final int[] NODES_PER_QUESTION = new int[]{
			9, 4, 9, 3, 3, 8, 5, 5, 7, 6, 5, 3, 2, 13, 4, 6, 5, 8
	};

	private static int[] generateProfile(int i) {
		final int[] profile = new int[N_SKILLS];

		for (int j = 0; j < N_SKILLS; j++) {
			profile[j] = i % 2;
			i /= 2;
		}

		return profile;
	}

	private static TIntIntMap obsSkills(int... v) {
		final TIntIntMap obs = new TIntIntHashMap();
		for (int i = 0; i < v.length; i++) {
			obs.put(i, v[i]);
		}
		return obs;
	}

	private static synchronized void write(List<String> lines) {
		try {
			Files.write(Paths.get(filename), lines, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (Exception e) {
			System.err.println("Could not write to file");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Files.write(Paths.get(filename), new ArrayList<String>(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		// both models are the same
		final BayesianNetwork model = UAIParser.read("AdaptiveQuestionnaire.multiple.model.uai");

		final Random random = new Random(42);
		RandomUtil.setRandom(random);

		//	TintIntMap profile = obsSkills(0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0);

		final BayesianNetworkSampling bns = new BayesianNetworkSampling();

		final List<TIntIntMap> samples = IntStream.range(0, (int) Math.pow(2, N_SKILLS))
				.mapToObj(Experiments::generateProfile)
				.map(Experiments::obsSkills)
				.map(profile -> bns.samples(model, profile, N_SAMPLES_PER_PROFILE))
				.flatMap(Arrays::stream)
				.collect(Collectors.toList());

		System.out.println("Cores used:  " + PARALLEL_COUNT);
		System.out.println("Total tasks: " + samples.size());

		final Progress p = new Progress(samples.size());
		p.print();

		final ExecutorService es = Executors.newFixedThreadPool(PARALLEL_COUNT);

		final List<Callable<List<Void>>> tasks = IntStream.range(0, samples.size())
				.mapToObj(i -> (Callable<List<Void>>) () -> {
					final long startTime = System.currentTimeMillis();

					final TIntIntMap sample = samples.get(i);
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

					p.update(endTime - startTime);

					write(content);

					return null;
				})
				.collect(Collectors.toList());

		es.invokeAll(tasks);
		es.shutdown();
	}

}
