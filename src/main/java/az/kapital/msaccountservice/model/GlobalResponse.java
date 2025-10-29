package az.kapital.msaccountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class GlobalResponse {

    private UUID id;
    private String error_code;
    private String error_message;
    private LocalDateTime time;
}
