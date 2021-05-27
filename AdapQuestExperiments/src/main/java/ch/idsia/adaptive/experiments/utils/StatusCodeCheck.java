package ch.idsia.adaptive.experiments.utils;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 17:04
 * <p>
 * This is an utility class that can identify a class of a return code of an {@link java.net.http.HttpResponse} object.
 */
public class StatusCodeCheck {

	/**
	 * @param statusCode return code of an {@link java.net.http.HttpResponse}.
	 * @return true if the code is an Informational Response, otherwise false
	 */
	public static Boolean is1xxSuccessful(Integer statusCode) {
		return statusCode >= 100 && statusCode < 200;
	}

	/**
	 * @param statusCode return code of an {@link java.net.http.HttpResponse}.
	 * @return true if the code is a Success, otherwise false
	 */
	public static Boolean is2xxSuccessful(Integer statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

	/**
	 * @param statusCode return code of an {@link java.net.http.HttpResponse}.
	 * @return true if the code is a Redirection, otherwise false
	 */
	public static Boolean is3xxSuccessful(Integer statusCode) {
		return statusCode >= 300 && statusCode < 400;
	}

	/**
	 * @param statusCode return code of an {@link java.net.http.HttpResponse}.
	 * @return true if the code is a Client Error, otherwise false
	 */
	public static Boolean is4xxSuccessful(Integer statusCode) {
		return statusCode >= 400 && statusCode < 500;
	}

	/**
	 * @param statusCode return code of an {@link java.net.http.HttpResponse}.
	 * @return true if the code is a Server Error, otherwise false
	 */
	public static Boolean is5xxSuccessful(Integer statusCode) {
		return statusCode >= 500 && statusCode < 600;
	}

}
