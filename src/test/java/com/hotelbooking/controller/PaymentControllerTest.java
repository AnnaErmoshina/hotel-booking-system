package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreatePaymentRequest;
import com.hotelbooking.dto.response.PaymentResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void pay_returns201() {
        PaymentController controller = new PaymentController(paymentService);
        UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.USER).build());
        CreatePaymentRequest request = new CreatePaymentRequest(1L, "card");
        PaymentResponse payment = new PaymentResponse(1L, 1L, BigDecimal.TEN, "PAID", "card", LocalDateTime.now());
        when(paymentService.pay(request, principal)).thenReturn(payment);

        var response = controller.pay(request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(payment);
    }
}
