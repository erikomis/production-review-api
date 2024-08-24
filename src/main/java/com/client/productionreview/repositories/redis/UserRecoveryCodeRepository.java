package com.client.productionreview.repositories.redis;

import com.client.productionreview.model.redis.UserRecoveryCode;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRecoveryCodeRepository extends CrudRepository<UserRecoveryCode, String> {



    Optional<UserRecoveryCode> findByEmail(String email);

    Optional<UserRecoveryCode> findByRecoveryCode(String recoveryCode);
}
