package id.ac.ui.cs.advprog.mysawitbe.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tracks APDEX (Application Performance Index) metrics.
 *
 * <p>APDEX classifies each request into one of three buckets based on
 * response time relative to a configurable threshold {@code T}:
 * <ul>
 *   <li><b>Satisfied</b> — response time &le; T</li>
 *   <li><b>Tolerating</b> — T &lt; response time &le; 4T</li>
 *   <li><b>Frustrated</b> — response time &gt; 4T</li>
 * </ul>
 *
 * <p>The score is computed as:
 * {@code apdex = (satisfied + tolerating/2) / total}.
 *
 * <p>Exposed Micrometer meters:
 * <ul>
 *   <li>{@code apdex.satisfied.total} — Counter</li>
 *   <li>{@code apdex.tolerating.total} — Counter</li>
 *   <li>{@code apdex.frustrated.total} — Counter</li>
 *   <li>{@code apdex.score} — Gauge (computed on scrape)</li>
 * </ul>
 */
@Component
public class ApdexMetrics {

    private final Counter satisfiedCounter;
    private final Counter toleratingCounter;
    private final Counter frustratedCounter;
    private final long thresholdMs;

    public ApdexMetrics(MeterRegistry registry,
                        @Value("${app.apdex.threshold-ms:500}") long thresholdMs) {
        this.thresholdMs = thresholdMs;

        this.satisfiedCounter = Counter.builder("apdex.satisfied.total")
                .description("APDEX satisfied requests (response time <= T)")
                .register(registry);

        this.toleratingCounter = Counter.builder("apdex.tolerating.total")
                .description("APDEX tolerating requests (T < response time <= 4T)")
                .register(registry);

        this.frustratedCounter = Counter.builder("apdex.frustrated.total")
                .description("APDEX frustrated requests (response time > 4T)")
                .register(registry);

        Gauge.builder("apdex.score", this, ApdexMetrics::computeScore)
                .description("APDEX score = (satisfied + tolerating/2) / total. 1.0 = all satisfied, 0.0 = no data.")
                .register(registry);
    }

    /**
     * Record a request response time, categorizing it into the appropriate APDEX bucket.
     *
     * @param responseTimeMs response time in milliseconds
     */
    public void logRequest(long responseTimeMs) {
        if (responseTimeMs <= thresholdMs) {
            satisfiedCounter.increment();
        } else if (responseTimeMs <= thresholdMs * 4) {
            toleratingCounter.increment();
        } else {
            frustratedCounter.increment();
        }
    }

    private double computeScore() {
        double satisfied = satisfiedCounter.count();
        double tolerating = toleratingCounter.count();
        double total = satisfied + tolerating + frustratedCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return (satisfied + (tolerating / 2.0)) / total;
    }
}
