package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.inference.sampling.BayesianNetworkSampling;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.UAIParser;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Random;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    04.10.2021 16:33
 */
public class Experiments {

	static final int N_COMPANIES = 10;
	static final int N_QUESTIONS = 40;

	static final boolean MULTI = true;

	public static void main(String[] args) throws Exception {

		// both models are the same
		final BayesianNetwork model = UAIParser.read("AdaptiveQuestionnaire.multiple.model.uai");


		final int N_SKILLS = 17;

		final Random random = new Random(42);
		RandomUtil.setRandom(random);

//		final int[] skills = IntStream.range(0, N_SKILLS).toArray();
		final int[] nodesPerQuestion = new int[]{9, 4, 9, 3, 3, 8, 5, 5, 7, 6, 5, 3, 2, 13, 4, 6, 5, 8};

		final BayesianNetworkSampling bns = new BayesianNetworkSampling();
		final TIntIntMap[] samples = bns.samples(model, N_COMPANIES);

		final BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>();

//		for (int v : model.getVariables()) {
//			final int[] parents = model.getParents(v);
//			System.out.printf("%2s: %s%n", v, Arrays.toString(parents));
//		}

		for (int i = 0; i < N_COMPANIES; i++) {
			System.out.printf("=========== %2d ===========%n", i);

			final TIntIntMap sample = samples[i];

			final TIntIntMap obs = new TIntIntHashMap();

			if (MULTI) {
				for (int j = N_SKILLS, k = 0; k < nodesPerQuestion.length; k++) {

					for (int o = 0; o < nodesPerQuestion[k]; o++) {
						final int v = sample.get(j);
						if (v == 1) {
							System.out.printf("%2d = %d ", j, v);
							obs.put(j, v);
						}
						j++;
					}
					System.out.println();

					final BayesianFactor[] q = new BayesianFactor[N_SKILLS];
					for (int s = 0; s < N_SKILLS; s++) {
						q[s] = inf.query(model, obs, s);
						System.out.printf("%.4f ", q[s].getValue(1));
					}

					if (Double.isNaN(q[0].getValue(1))) {
						System.out.println("BROKEN!");
						break;
					}

					System.out.println();
				}

			} else {
				for (int j = N_SKILLS; j < N_QUESTIONS; j++) {
					final int o = sample.get(j);
					if (o == 0)
						continue;

					obs.put(j, o);

					System.out.printf("%2d = %d : ", j, o);
					final BayesianFactor[] q = new BayesianFactor[N_SKILLS];
					for (int s = 0; s < N_SKILLS; s++) {
						q[s] = inf.query(model, obs, s);
						System.out.printf("%.4f ", q[s].getValue(1));
					}

					if (Double.isNaN(q[0].getValue(1))) {
						System.out.println("BROKEN!");
						break;
					}

					System.out.println();
				}
			}
			System.out.println();
		}

	}

}
