package com.client.productionreview.repositories.redis;

import com.client.productionreview.model.redis.UserActivationToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivationTokenRepository extends CrudRepository<UserActivationToken, String> {
}
