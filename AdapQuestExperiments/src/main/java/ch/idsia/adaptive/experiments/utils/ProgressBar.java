package ch.idsia.adaptive.experiments.utils;

import java.time.Duration;
import java.util.Collections;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    06.10.2021 14:27
 */
public class ProgressBar {
	private final int total;
	private int completed;

	private long time;
	private long delay = 100; // ms
	private long lastPrint = 0L;
	private long startTime = 0L;

	private final int digits;

	private String prefix = "Progress:";
	private String suffix = "";

	public ProgressBar(int total) {
		this.total = total;
		this.digits = Double.valueOf(Math.log10(total)).intValue() + 1;
		reset();
	}

	public ProgressBar setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public ProgressBar setSuffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public ProgressBar setDelay(long delay) {
		this.delay = delay;
		return this;
	}

	public void reset() {
		completed = 0;
		time = 0;
		lastPrint = 0;
		startTime = 0;
	}

	public void print() {
		final long now = System.currentTimeMillis();

		if (startTime == 0)
			startTime = now;

		if (now - lastPrint < delay)
			return;

		lastPrint = now;

		final double perc = 1.0 * completed / total;
		final int fill = (int) (30 * perc);
		final int rest = 30 - fill;

		String itText = "it/s";
		double its = 0;

		if (completed > 0) {
			its = 1000.0 * completed / time;

			if (its < 1.) {
				its = 1.0 / its;
				itText = "s/it";
			}
		}

		final long elapsed = System.currentTimeMillis() - startTime;
		if (completed == total) {
			System.out.printf("\r%s %" + digits + "d \u001B[33mcompleted\u001B[0m in %s (%6.4f %4s)%n",
					prefix,
					total,
					formatDuration(Duration.ofMillis(elapsed)),
					its,
					itText
			);
			return;
		}

		final Duration eta = Duration.ofMillis(completed == 0 ? 0 : (elapsed * (total - completed) / completed));

		System.out.printf("\r%s |%s%s| %" + digits + "d/%" + digits + "d %6.4f %4s ETA: %s \u001B[33m%5.1f%%\u001B[0m %s\u001B[33m \u001B[0m",
				prefix,
				String.join("", Collections.nCopies(fill, "█")),
				String.join("", Collections.nCopies(rest, "-")),
				completed,
				total,
				its,
				itText,
				formatDuration(eta),
				perc * 100,
				suffix
		);
	}

	private String formatDuration(Duration duration) {
		final long seconds = duration.getSeconds();
		final long absSeconds = Math.abs(seconds);
		final String positive = String.format(
				"%02d:%02d:%02d",
				absSeconds / 3600,          // hours
				(absSeconds % 3600) / 60,   // minutes
				absSeconds % 60);           // seconds
		return seconds < 0 ? "-" + positive : positive;
	}

	public synchronized void update(long time) {
		completed += 1;
		this.time += time;

		print();
	}
}
