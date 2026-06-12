package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class UserServiceEmailDuplicationTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("이메일 중복 검사시 유저가 존재하지 않으면 예외가 발생하지 않는다.")
    @Test
    void shouldNotThrowException_whenUserDoesNotExistDuringDuplicateCheck() {
        //given
        String email = "test@test.com";

        given(userRepository.findByEmailAndStatusIn(eq(email), anyList())).willReturn(Optional.empty());

        //when, then
        assertDoesNotThrow(() -> userService.checkEmailDuplication(email));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        //given
        String email = "test@test.com";
        User user = mock(User.class);

        given(userRepository.findByEmailAndStatusIn(eq(email), anyList())).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.checkEmailDuplication(email))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("Email already exists.");
    }

}
