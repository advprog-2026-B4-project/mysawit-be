package id.ac.ui.cs.advprog.mysawitbe.common.event;

import id.ac.ui.cs.advprog.mysawitbe.common.port.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringApplicationEventAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Override
    public void publish(Object event) {
        delegate.publishEvent(event);
    }
}
