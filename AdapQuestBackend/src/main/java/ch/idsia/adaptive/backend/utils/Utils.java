package ch.idsia.adaptive.backend.utils;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    05.02.2021 16:03
 */
public class Utils {

	// TODO: this should be inside crema...
	public static double H(double[] d) {
		double h = 0.0;

		for (double v : d) {
			// log base 4
			double logXv = Math.log(v) / Math.log(d.length);
			h += v * logXv;
		}

		return -h;
	}

}
