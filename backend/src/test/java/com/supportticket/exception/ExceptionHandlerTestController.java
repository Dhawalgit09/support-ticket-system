package com.supportticket.exception;

import com.supportticket.dto.request.CreateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/exceptions")
class ExceptionHandlerTestController {

    @GetMapping("/not-found")
    void notFound() {
        throw new TicketNotFoundException(42L);
    }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody CreateTicketRequest request) {
    }
}
