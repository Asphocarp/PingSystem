package app.jyu.common.util;

import java.time.Duration;
import java.time.Instant;

public class RateLimiter {

	private static Duration timeToRegenerate;
	private static Duration timeWindow;

	public static void setRates(long msToRegenerate, long limit) {
		RateLimiter.timeToRegenerate = Duration.ofMillis(msToRegenerate);
		RateLimiter.timeWindow = Duration.ofMillis(msToRegenerate * limit);
	}

	private Instant startTime = null;

	public RateLimiter() {}

	public boolean checkExceeded() {
		if (this.startTime == null) {
			this.startTime = Instant.now().minus(timeWindow).plus(timeToRegenerate);

			return false;
		}

		final var now = Instant.now();
		var elapsed = Duration.between(this.startTime, now);

		if (elapsed.compareTo(timeWindow) > 0) {
			elapsed = timeWindow;
		}

		var leftOver = elapsed.minus(timeToRegenerate);

		if (leftOver.isNegative()) {
			return true;
		}

		this.startTime = now.minus(leftOver);

		return false;
	}
}
