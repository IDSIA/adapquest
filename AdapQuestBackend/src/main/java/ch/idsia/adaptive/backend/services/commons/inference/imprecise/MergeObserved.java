package ch.idsia.adaptive.backend.services.commons.inference.imprecise;

import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    14.04.2021 09:31
 */
// TODO: with crema 0.2.0 delete this class
public class MergeObserved {

	public DAGModel<IntervalFactor> execute(DAGModel<IntervalFactor> original, TIntIntMap evidence) {
		final DAGModel<IntervalFactor> model = original.copy();

		// consider the observed nodes (that are now leaf and binary) which have only one parent.
		final TIntObjectMap<TIntLinkedList> valid = new TIntObjectHashMap<>();
		final TIntHashSet evToRemove = new TIntHashSet();

		for (int key : evidence.keys()) {
			if (!model.getNetwork().containsVertex(key)) {
				evToRemove.add(key);
				continue;
			}

			final int[] parents = model.getParents(key);

			if (parents.length > 1)
				continue;

			final int parent = parents[0];

			if (!valid.containsKey(parent))
				valid.put(parent, new TIntLinkedList());

			valid.get(parent).add(key);
		}

		for (int v : evToRemove.toArray())
			evidence.remove(v);

		// if between these nodes there are two that have the same parent, we replace them with a unique binary node
		for (int y : valid.keys()) {
			final TIntLinkedList children = valid.get(y);
			if (children.size() < 2)
				continue;

			while (children.size() > 1) {
				int x1 = children.get(0);
				int x2 = children.get(1);

				final int ev1 = evidence.get(x1);
				final int ev2 = evidence.get(x2);

				final IntervalFactor f1 = model.getFactor(x1);
				final IntervalFactor f2 = model.getFactor(x2);

				// remove observed variables
				model.removeVariable(x1);
				model.removeVariable(x2);

				// add new variable, merge of the observed, with binary state: observed or not
				final int x = model.addVariable(2);
				model.addParent(x, y);

				final IntervalFactor f = new IntervalFactor(model.getDomain(x), model.getDomain(y));

				// for each state of y
				for (int yi = 0; yi < model.getSize(y); yi++) {
					// the lower and upper probability of the same observed state is the product of the lowers and uppers of the observed states of the two nodes
					double lower_xt_y = f1.getLower(yi)[ev1] * f2.getLower(yi)[ev2];
					double upper_xt_y = f1.getUpper(yi)[ev1] * f2.getUpper(yi)[ev2];

					double[] lower_x_y = new double[]{lower_xt_y, 1.0 - upper_xt_y};
					double[] upper_x_y = new double[]{upper_xt_y, 1.0 - lower_xt_y};

					f.setLower(lower_x_y, yi);
					f.setUpper(upper_x_y, yi);
				}

				model.setFactor(x, f);

				evidence.remove(x1);
				evidence.remove(x2);
				evidence.put(x, 0);

				children.remove(x1);
				children.remove(x2);
				children.add(x);
			}
		}

		return model;
	}
}
