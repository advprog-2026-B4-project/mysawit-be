package id.ac.ui.cs.advprog.mysawitbe.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class NotificationMetricsAspect {

    private static final String BASE = "notification.sent.total";
    private static final String BASE_BY_EVENT = "notification.sent.by.event.total";

    private final Counter sentTotalCounter;
    private final Counter sentPanenApprovedCounter;
    private final Counter sentPanenRejectedCounter;
    private final Counter sentPengirimanTibaCounter;
    private final Counter sentPengirimanApprovedMandorCounter;
    private final Counter sentPengirimanProcessedAdminCounter;
    private final Counter sentPayrollProcessedCounter;
    private final Counter sentBuruhAssignedCounter;
    private final Counter sentMandorAssignedCounter;

    public NotificationMetricsAspect(MeterRegistry registry) {
        this.sentTotalCounter = Counter.builder(BASE)
                .description("Total notifikasi terkirim ke repository (cumulative)")
                .register(registry);

        this.sentPanenApprovedCounter   = eventCounter(registry, "PANEN_APPROVED");
        this.sentPanenRejectedCounter   = eventCounter(registry, "PANEN_REJECTED");
        this.sentPengirimanTibaCounter  = eventCounter(registry, "PENGIRIMAN_TIBA");
        this.sentPengirimanApprovedMandorCounter = eventCounter(registry, "PENGIRIMAN_APPROVED_MANDOR");
        this.sentPengirimanProcessedAdminCounter = eventCounter(registry, "PENGIRIMAN_PROCESSED_ADMIN");
        this.sentPayrollProcessedCounter = eventCounter(registry, "PAYROLL_PROCESSED");
        this.sentBuruhAssignedCounter   = eventCounter(registry, "BURUH_ASSIGNED");
        this.sentMandorAssignedCounter  = eventCounter(registry, "MANDOR_ASSIGNED_KEBUN");
    }

    private Counter eventCounter(MeterRegistry registry, String trigger) {
        return Counter.builder(BASE_BY_EVENT)
                .tag("trigger", trigger)
                .description("Notifikasi terkirim per jenis event pemicu")
                .register(registry);
    }

    private static final String SVC =
            "id.ac.ui.cs.advprog.mysawitbe.modules.notification.application.service.NotificationUseCaseImpl";

    @Around("execution(* " + SVC + ".sendNotification(..))")
    public Object countSend(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentTotalCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPanenApproved(..))")
    public Object countPanenApproved(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPanenApprovedCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPanenRejected(..))")
    public Object countPanenRejected(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPanenRejectedCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPengirimanStatusTiba(..))")
    public Object countPengirimanTiba(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPengirimanTibaCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPengirimanApprovedByMandor(..))")
    public Object countPengirimanApprovedMandor(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPengirimanApprovedMandorCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPengirimanProcessedByAdmin(..))")
    public Object countPengirimanProcessedAdmin(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPengirimanProcessedAdminCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onPayrollProcessed(..))")
    public Object countPayrollProcessed(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentPayrollProcessedCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onBuruhAssigned(..))")
    public Object countBuruhAssigned(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentBuruhAssignedCounter.increment();
        return result;
    }

    @Around("execution(* " + SVC + ".onMandorAssignedToKebun(..))")
    public Object countMandorAssigned(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        sentMandorAssignedCounter.increment();
        return result;
    }
}
