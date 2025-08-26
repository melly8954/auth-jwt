package com.melly.authjwt.service;

import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.enums.UserRole;
import com.melly.authjwt.domain.enums.UserStatus;
import com.melly.authjwt.domain.repository.UserRepository;
import com.melly.authjwt.dto.request.SignUpRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 단위 테스트")
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("signUp() 메서드 테스트")
    class SignUp {
        private SignUpRequestDto dto;

        @BeforeEach
        void setUp() {
            dto = new SignUpRequestDto();
            dto.setUsername("testid");
            dto.setEmail("testid@example.com");
            dto.setPassword("testpassword");
            dto.setConfirmPassword("testpassword");
        }

        @Test
        @DisplayName("성공 - 회원가입 성공")
        void signUp_success() {
            // given
            when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");

            // when
            userService.signUp(dto);

            // then
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository, times(1)).save(captor.capture());
            UserEntity savedUser = captor.getValue();

            assertThat(savedUser.getUsername()).isEqualTo(dto.getUsername());
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedUser.getEmail()).isEqualTo(dto.getEmail());
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("예외 - 중복된 username 사용")
        void signUp_duplicateUsername_throwsException() {
            when(userRepository.existsByUsername("testid")).thenReturn(true);

            assertThatThrownBy(() -> userService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.DUPLICATE_USERNAME);
        }

        @Test
        @DisplayName("예외 - 중복된 email 사용")
        void signUp_duplicateEmail_throwsException() {
            when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail("testid@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("예외 - 비밀번호 불일치")
        void signUp_passwordMismatch_throwsException() {
            dto.setConfirmPassword("passwordmismatch");
            when(userRepository.existsByUsername("testid")).thenReturn(false);
            when(userRepository.existsByEmail("testid@example.com")).thenReturn(false);

            assertThatThrownBy(() -> userService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.PASSWORD_MISMATCH);
        }
    }
}
