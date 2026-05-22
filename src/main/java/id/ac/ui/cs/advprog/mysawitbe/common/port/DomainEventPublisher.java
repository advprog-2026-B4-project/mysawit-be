package id.ac.ui.cs.advprog.mysawitbe.common.port;

public interface DomainEventPublisher {
    void publish(Object event);
}
