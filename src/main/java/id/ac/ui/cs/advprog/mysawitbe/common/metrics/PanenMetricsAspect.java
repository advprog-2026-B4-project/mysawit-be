package id.ac.ui.cs.advprog.mysawitbe.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;

@Aspect
@Component
@Slf4j
public class PanenMetricsAspect {

    private final Timer createPanenTimer;
    private final Timer approvePanenTimer;
    private final Counter panenSubmittedCounter;
    private final Counter panenApprovedCounter;
    private final Counter panenRejectedCounter;
    private final DistributionSummary panenWeightSummary;

    public PanenMetricsAspect(MeterRegistry registry) {
        this.createPanenTimer = Timer.builder("panen.create.duration")
                .description("Latency of createPanen (NFR target p95 < 300ms)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.approvePanenTimer = Timer.builder("panen.approve.duration")
                .description("Latency of approvePanen — triggers payroll creation pipeline")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.panenSubmittedCounter = Counter.builder("panen.submitted.total")
                .description("Jumlah panen masuk (cumulative). Use rate() for per-hour.")
                .register(registry);

        this.panenApprovedCounter = Counter.builder("panen.approved.total")
                .description("Jumlah panen disetujui mandor (cumulative).")
                .register(registry);

        this.panenRejectedCounter = Counter.builder("panen.rejected.total")
                .description("Jumlah panen ditolak mandor (cumulative).")
                .register(registry);

        this.panenWeightSummary = DistributionSummary.builder("panen.weight.grams")
                .description("Berat panen per laporan (grams). sum() = total berat masuk.")
                .baseUnit("grams")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service.PanenCommandImpl.createPanen(..))")
    public Object timeCreatePanen(ProceedingJoinPoint pjp) throws Throwable {
        return createPanenTimer.recordCallable(() -> {
            PanenDTO result = (PanenDTO) pjp.proceed();
            panenSubmittedCounter.increment();
            if (result != null) {
                panenWeightSummary.record(result.weight());
            }
            return result;
        });
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service.PanenCommandImpl.approvePanen(..))")
    public Object timeApprovePanen(ProceedingJoinPoint pjp) throws Throwable {
        return approvePanenTimer.recordCallable(() -> {
            Object result = pjp.proceed();
            panenApprovedCounter.increment();
            return result;
        });
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.service.PanenCommandImpl.rejectPanen(..))")
    public Object countRejectPanen(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        panenRejectedCounter.increment();
        return result;
    }
}
