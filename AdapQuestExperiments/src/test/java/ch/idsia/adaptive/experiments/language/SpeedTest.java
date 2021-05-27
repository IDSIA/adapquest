package ch.idsia.adaptive.experiments.language;

import ch.idsia.crema.entropy.BayesianEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.LoopyBeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    04.02.2021 09:27
 */
public class SpeedTest {

	@Test
	void name() {
		final List<Integer> qIndex = IntStream.range(4, 95).boxed().collect(Collectors.toList());
		final int[] skills = {0, 1, 2, 3};
		final int[] questions = {10, 30, 50, 70, 90};
		final int runs = 10;

		final LanguageTest ltg = new LanguageTest();
		ltg.model();
		final BayesianNetwork bn = ltg.bn;

		final LoopyBeliefPropagation<BayesianFactor> bp = new LoopyBeliefPropagation<>();

		for (int nQuestions : questions) {
//			System.out.println("#questions: " + nQuestions);
			double length = 0L;
			Random rnd = new Random(nQuestions);
			for (int run = 0; run < runs; run++) {

				Collections.shuffle(qIndex, rnd);
				final List<Integer> qList = qIndex.subList(0, nQuestions);

//				System.out.println("Run: " + run);
//				System.out.println("Question list: " + qList.stream().map(Object::toString).collect(Collectors.joining(" ")));

				TIntIntMap obs = new TIntIntHashMap();
				for (Integer qIdx : qList) {
					obs.put(qIdx, rnd.nextInt(2));
				}

				final long start = System.currentTimeMillis();

				for (int s : skills) {
//					System.out.printf("skill: %d %s%n", s, obs.toString());
					final BayesianFactor pS = bp.query(bn, obs, s);
					BayesianEntropy.H(pS);
				}

				final long end = System.currentTimeMillis();

//				System.out.println("Ended in " + (end - start));

				length += (end - start) / (1. * skills.length * runs);
			}
			System.out.printf("questions: %2d runs: %2d time: %f%n", nQuestions, runs, length);
		}

	}
}
