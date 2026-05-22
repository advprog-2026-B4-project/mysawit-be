package id.ac.ui.cs.advprog.mysawitbe.common.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringApplicationEventAdapterTest {

    @Mock
    private ApplicationEventPublisher delegate;

    @InjectMocks
    private SpringApplicationEventAdapter adapter;

    // =========================================================================
    // publish()
    // =========================================================================
    @Nested
    class Publish {

        @Test
        void publish_stringEvent_delegatesToApplicationEventPublisher() {
            String event = "test-event";

            adapter.publish(event);

            verify(delegate).publishEvent(event);
        }

        @Test
        void publish_recordEvent_delegatesToApplicationEventPublisher() {
            TestEvent event = new TestEvent("id-123", "message");

            adapter.publish(event);

            verify(delegate).publishEvent(event);
        }

        @Test
        void publish_customObjectEvent_delegatesToApplicationEventPublisher() {
            Object event = new Object();

            adapter.publish(event);

            verify(delegate).publishEvent(event);
        }

        @Test
        void publish_multipleCalls_eachDelegatedCorrectly() {
            TestEvent event1 = new TestEvent("id-1", "msg1");
            TestEvent event2 = new TestEvent("id-2", "msg2");

            adapter.publish(event1);
            adapter.publish(event2);

            verify(delegate).publishEvent(event1);
            verify(delegate).publishEvent(event2);
        }
    }

    // Simple test event record
    private record TestEvent(String id, String message) {}
}
