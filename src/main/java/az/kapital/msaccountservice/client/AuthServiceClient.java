package az.kapital.msaccountservice.client;

import az.kapital.msaccountservice.model.TokenValidationRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-auth-service", url = "${client.ms-auth-service.url}")
public interface AuthServiceClient {


    @PostMapping("/api/auth/validate")
    String validateToken(@RequestBody @Valid TokenValidationRequest request);


}