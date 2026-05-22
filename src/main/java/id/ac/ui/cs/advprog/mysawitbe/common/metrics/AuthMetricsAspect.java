package id.ac.ui.cs.advprog.mysawitbe.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;

@Aspect
@Component
public class AuthMetricsAspect {

    private static final String METRIC_LOGIN_TOTAL = "auth.login.total";
    private static final String METRIC_REGISTER_TOTAL = "auth.register.total";
    private static final String TAG_METHOD = "method";
    private static final String TAG_RESULT = "result";
    private static final String TAG_ROLE = "role";

    private final Timer loginEmailTimer;
    private final Counter loginEmailSuccessCounter;
    private final Counter loginEmailFailureCounter;
    private final Counter registerBuruhCounter;
    private final Counter registerSupirCounter;
    private final Counter registerMandorCounter;
    private final Counter googleCallbackCounter;
    private final Counter googleCompleteCounter;

    public AuthMetricsAspect(MeterRegistry registry) {
        this.loginEmailTimer = Timer.builder("auth.login.email.duration")
                .description("Latency loginWithEmail (authentication + DB lookup)")
                .publishPercentiles(0.5, 0.95)
                .register(registry);

        this.loginEmailSuccessCounter = Counter.builder(METRIC_LOGIN_TOTAL)
                .tag(TAG_METHOD, "EMAIL").tag(TAG_RESULT, "success")
                .description("Login email berhasil")
                .register(registry);

        this.loginEmailFailureCounter = Counter.builder(METRIC_LOGIN_TOTAL)
                .tag(TAG_METHOD, "EMAIL").tag(TAG_RESULT, "failure")
                .description("Login email gagal (wrong password / not found)")
                .register(registry);

        this.registerBuruhCounter = Counter.builder(METRIC_REGISTER_TOTAL)
                .tag(TAG_ROLE, "BURUH")
                .description("Registrasi user baru per role")
                .register(registry);

        this.registerSupirCounter = Counter.builder(METRIC_REGISTER_TOTAL)
                .tag(TAG_ROLE, "SUPIR")
                .register(registry);

        this.registerMandorCounter = Counter.builder(METRIC_REGISTER_TOTAL)
                .tag(TAG_ROLE, "MANDOR")
                .register(registry);

        this.googleCallbackCounter = Counter.builder(METRIC_LOGIN_TOTAL)
                .tag(TAG_METHOD, "GOOGLE").tag(TAG_RESULT, "callback")
                .description("Google OAuth callback berhasil diproses")
                .register(registry);

        this.googleCompleteCounter = Counter.builder(METRIC_LOGIN_TOTAL)
                .tag(TAG_METHOD, "GOOGLE").tag(TAG_RESULT, "complete")
                .description("Google OAuth registration completion berhasil")
                .register(registry);
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service.AuthCommandUseCaseImpl.loginWithEmail(..))")
    public Object timeLoginWithEmail(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return loginEmailTimer.recordCallable(() -> {
                try {
                    Object result = pjp.proceed();
                    loginEmailSuccessCounter.increment();
                    return result;
                } catch (Throwable t) {
                    if (t instanceof Exception e) throw e;
                    throw new RuntimeException(t);
                }
            });
        } catch (Exception ex) {
            loginEmailFailureCounter.increment();
            throw ex;
        }
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service.AuthCommandUseCaseImpl.registerUser(..))")
    public Object countRegister(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (result instanceof UserDTO user) {
            Counter counter = switch (user.role()) {
                case "BURUH"  -> registerBuruhCounter;
                case "SUPIR"  -> registerSupirCounter;
                case "MANDOR" -> registerMandorCounter;
                default -> null;
            };
            if (counter != null) counter.increment();
        }
        return result;
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service.AuthCommandUseCaseImpl.handleGoogleOAuthCallback(..))")
    public Object countGoogleCallback(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        googleCallbackCounter.increment();
        return result;
    }

    @Around("execution(* id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service.AuthCommandUseCaseImpl.completeGoogleOAuthRegistration(..))")
    public Object countGoogleComplete(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        googleCompleteCounter.increment();
        return result;
    }
}
