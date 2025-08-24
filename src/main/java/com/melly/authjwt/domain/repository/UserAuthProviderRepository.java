package com.melly.authjwt.domain.repository;

import com.melly.authjwt.domain.entity.UserAuthProviderEntity;
import com.melly.authjwt.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProviderEntity, Long> {
    @Query("select ap from UserAuthProviderEntity ap join fetch ap.user u " +
            "where u.userId = :userId and ap.provider = :provider")
    Optional<UserAuthProviderEntity> findByUserIdAndProviderFetchJoin(@Param("userId") Long userId,
                                                                      @Param("provider") String provider);

    boolean existsByUserAndProvider(UserEntity existingUser, String provider);
}
