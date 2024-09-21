package com.client.productionreview.model.redis;

import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

@RedisHash("recoveryCode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecoveryCode  implements Serializable {

    @Id
    private String id;

    @Indexed
    @Email
    private String email;

    @Indexed
    private String code;

    private LocalDateTime createdDate = LocalDateTime.now();
}
