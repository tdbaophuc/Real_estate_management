package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.CustomerSummaryResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.repository.CustomerFavoriteListingRepository;
import com.javaweb.customer.repository.CustomerNoteRepository;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.customer.repository.CustomerRequirementRepository;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.listing.enums.ListingPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerSummaryServiceTest {
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerRequirementRepository requirementRepository = mock(CustomerRequirementRepository.class);
    private final CustomerNoteRepository noteRepository = mock(CustomerNoteRepository.class);
    private final CustomerFavoriteListingRepository favoriteRepository = mock(CustomerFavoriteListingRepository.class);
    private final LeadRepository leadRepository = mock(LeadRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final CustomerSummaryService service = new CustomerSummaryService(
            customerRepository,
            requirementRepository,
            noteRepository,
            favoriteRepository,
            leadRepository,
            aiService,
            new ObjectMapper()
    );

    @Test
    void shouldReturnFallbackSummaryWhenAiIsSkipped() {
        Customer customer = customer();
        CustomerRequirement requirement = new CustomerRequirement(ListingPurpose.SALE);
        requirement.setDescription("Can ho Quan 1");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(requirementRepository.findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(requirement));
        when(noteRepository.findAllByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(favoriteRepository.findAllByCustomerIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(leadRepository.findAllByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());
        when(aiService.complete(any())).thenReturn(AiCompletionResponse.skipped(
                "noop",
                "not-configured",
                "AI provider is disabled or API key is not configured"
        ));

        CustomerSummaryResponse response = service.summarize(1L, manager());

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.needsSummary()).contains("Can ho Quan 1");
        assertThat(response.nextBestAction()).contains("Create or qualify");
    }

    private Customer customer() {
        User agent = new User("agent@example.test", "password", "Agent");
        ReflectionTestUtils.setField(agent, "id", 10L);
        Customer customer = new Customer("CUS-SUM", "Nguyen Van C", agent);
        customer.setAssignedAgent(agent);
        ReflectionTestUtils.setField(customer, "id", 1L);
        return customer;
    }

    private AuthUserPrincipal manager() {
        return new AuthUserPrincipal(
                10L,
                "manager@example.test",
                "password",
                "Manager",
                UserStatus.ACTIVE,
                null,
                List.of("MANAGER"),
                List.of(),
                List.of()
        );
    }
}
