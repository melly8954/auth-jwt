package com.melly.authjwt.common.auth;

import com.melly.authjwt.domain.entity.UserAuthProviderEntity;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.enums.UserRole;
import com.melly.authjwt.domain.enums.UserStatus;
import com.melly.authjwt.domain.repository.UserAuthProviderRepository;
import com.melly.authjwt.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrincipalOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String provider = userRequest.getClientRegistration().getRegistrationId(); // google, kakao 등
        String providerUserId = oAuth2User.getName(); // OAuth2User.getName()은 provider user id

        UserEntity user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 기존 유저인데 소셜 정보가 없으면 추가
                    if (!userAuthProviderRepository.existsByUserAndProvider(existingUser, provider)) {
                        UserAuthProviderEntity authProvider = UserAuthProviderEntity.builder()
                                .user(existingUser)
                                .provider(provider)
                                .providerId(providerUserId)
                                .build();
                        userAuthProviderRepository.save(authProvider);
                    }
                    return existingUser;
                })
                .orElseGet(() -> registerNewSocialUser(email, provider, providerUserId));

        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }

    private UserEntity registerNewSocialUser(String email, String provider, String providerUserId) {
        String randomPassword = UUID.randomUUID().toString();
        UserEntity newUser = UserEntity.builder()
                .username(email)
                .email(email)
                .password(randomPassword) // encode 필요
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();

        UserEntity savedUser = userRepository.save(newUser);

        // auth_provider 기록
        UserAuthProviderEntity authProvider = UserAuthProviderEntity.builder()
                .user(savedUser)
                .provider(provider)
                .providerId(providerUserId)
                .build();
        userAuthProviderRepository.save(authProvider);

        return savedUser;
    }
}
