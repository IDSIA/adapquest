package ch.idsia.adaptive.experiments;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 17:04
 */
public class StatusCodeCheck {

	public static Boolean is1xxSuccessful(Integer statusCode) {
		return statusCode >= 100 && statusCode < 200;
	}

	public static Boolean is2xxSuccessful(Integer statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

	public static Boolean is3xxSuccessful(Integer statusCode) {
		return statusCode >= 300 && statusCode < 400;
	}

	public static Boolean is4xxSuccessful(Integer statusCode) {
		return statusCode >= 400 && statusCode < 500;
	}

	public static Boolean is5xxSuccessful(Integer statusCode) {
		return statusCode >= 500 && statusCode < 600;
	}


}
