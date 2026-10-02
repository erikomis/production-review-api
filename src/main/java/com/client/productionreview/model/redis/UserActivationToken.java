package com.client.productionreview.model.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

/**
 * Token enviado por e-mail para ativar a conta. Expira em 24 horas.
 */
@RedisHash(value = "activationToken", timeToLive = 86400)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivationToken implements Serializable {

    @Id
    private String token;

    private String email;
}
