package com.ninos.payment.controller;

import com.ninos.payment.dtos.PaymentDTO;
import com.ninos.payment.services.PaymentService;
import com.ninos.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<Response<?>> initializePayment(@RequestBody PaymentDTO paymentDTO) {
        return ResponseEntity.ok(paymentService.initializePayment(paymentDTO));
    }

    @PutMapping("/update")
    public void updatePaymentForOrder(@RequestBody PaymentDTO paymentDTO) {
        paymentService.updatePaymentForOrder(paymentDTO);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<List<PaymentDTO>>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<PaymentDTO>> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }


}
