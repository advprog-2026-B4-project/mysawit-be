package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.out.KebunRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KebunQueryUseCaseSecurityTest.TestConfig.class)
class KebunQueryUseCaseSecurityTest {

    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";
    private static final String MANDOR_ID = "00000000-0000-0000-0000-000000000002";
    private static final String OTHER_MANDOR_ID = "00000000-0000-0000-0000-000000000003";

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        KebunRepositoryPort kebunRepositoryPort() {
            return mock(KebunRepositoryPort.class);
        }

        @Bean
        UserQueryUseCase userQueryUseCase() {
            return mock(UserQueryUseCase.class);
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }

        @Bean
        KebunUseCaseService kebunUseCaseService(
                KebunRepositoryPort kebunRepositoryPort,
                UserQueryUseCase userQueryUseCase,
                ApplicationEventPublisher applicationEventPublisher
        ) {
            return new KebunUseCaseService(kebunRepositoryPort, userQueryUseCase, applicationEventPublisher);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private KebunQueryUseCase kebunQueryUseCase;

    @org.springframework.beans.factory.annotation.Autowired
    private KebunRepositoryPort kebunRepositoryPort;

    @org.springframework.beans.factory.annotation.Autowired
    private UserQueryUseCase userQueryUseCase;

    @BeforeEach
    void setUp() {
        reset(kebunRepositoryPort, userQueryUseCase);
    }

    @Test
    @WithMockUser(username = MANDOR_ID, roles = "MANDOR")
    void getSupirListByMandorId_whenMandorRequestsOwnData_returnsSupirList() {
        UUID mandorId = UUID.fromString(MANDOR_ID);
        UUID kebunId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();

        when(kebunRepositoryPort.findKebunIdByMandorId(mandorId)).thenReturn(kebunId);
        when(kebunRepositoryPort.findSupirIdsByKebunId(kebunId)).thenReturn(List.of(supirId));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(
                new UserDTO(supirId, "supir-a", "Supir A", "SUPIR", "supir-a@example.com")
        );

        List<UserDTO> result = kebunQueryUseCase.getSupirListByMandorId(mandorId);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(user -> {
                    assertThat(user.userId()).isEqualTo(supirId);
                    assertThat(user.name()).isEqualTo("Supir A");
                });

        verify(kebunRepositoryPort).findKebunIdByMandorId(mandorId);
        verify(kebunRepositoryPort).findSupirIdsByKebunId(kebunId);
        verify(userQueryUseCase).getUserById(supirId);
    }

    @Test
    @WithMockUser(username = MANDOR_ID, roles = "MANDOR")
    void getSupirListByMandorId_whenMandorRequestsOtherMandorData_isDenied() {
        UUID otherMandorId = UUID.fromString(OTHER_MANDOR_ID);

        assertThatThrownBy(() -> kebunQueryUseCase.getSupirListByMandorId(otherMandorId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(kebunRepositoryPort, userQueryUseCase);
    }

    @Test
    @WithMockUser(username = ADMIN_ID, roles = "ADMIN")
    void getSupirListByMandorId_whenAdminRequestsAnyMandorData_returnsSupirList() {
        UUID otherMandorId = UUID.fromString(OTHER_MANDOR_ID);
        UUID kebunId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();

        when(kebunRepositoryPort.findKebunIdByMandorId(otherMandorId)).thenReturn(kebunId);
        when(kebunRepositoryPort.findSupirIdsByKebunId(kebunId)).thenReturn(List.of(supirId));
        when(userQueryUseCase.getUserById(supirId)).thenReturn(
                new UserDTO(supirId, "supir-b", "Supir B", "SUPIR", "supir-b@example.com")
        );

        List<UserDTO> result = kebunQueryUseCase.getSupirListByMandorId(otherMandorId);

        assertThat(result).extracting(UserDTO::userId).containsExactly(supirId);
        verify(kebunRepositoryPort).findKebunIdByMandorId(otherMandorId);
        verify(kebunRepositoryPort).findSupirIdsByKebunId(kebunId);
        verify(userQueryUseCase).getUserById(supirId);
    }
}
