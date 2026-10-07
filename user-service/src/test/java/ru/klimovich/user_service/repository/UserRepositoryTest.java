package ru.klimovich.user_service.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.klimovich.user_service.model.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Фиксирует фактические ограничения таблицы users в PostgreSQL
 * (openspec/specs/user-profile/spec.md, as-is).
 */
class UserRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    private static User user(String keycloakId, String email, String phone) {
        User user = new User();
        user.setKeycloakUserId(keycloakId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPhone(phone);
        return user;
    }

    @Test
    @DisplayName("Профиль сохраняется и находится по keycloakUserId и email, id и date_create заполнены")
    void saveAndFind() {
        User saved = userRepository.saveAndFlush(user("kc-1", "a@example.com", "+79990000001"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<User> byKeycloakId = userRepository.findByKeycloakUserId("kc-1");
        assertThat(byKeycloakId).isPresent();
        assertThat(byKeycloakId.get().getEmail()).isEqualTo("a@example.com");
        assertThat(userRepository.findByEmail("a@example.com")).isPresent();
        assertThat(userRepository.existsByKeycloakUserId("kc-1")).isTrue();
    }

    @Test
    @DisplayName("Дубль email нарушает уникальное ограничение БД (OBS-03 as-is: проверка только в БД)")
    void duplicateEmail_violatesUniqueConstraint() {
        userRepository.saveAndFlush(user("kc-1", "dup@example.com", null));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("kc-2", "dup@example.com", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Дубль keycloakUserId нарушает уникальное ограничение БД")
    void duplicateKeycloakUserId_violatesUniqueConstraint() {
        userRepository.saveAndFlush(user("kc-dup", "a@example.com", null));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("kc-dup", "b@example.com", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("first_name NOT NULL (OBS-02 as-is: нет family_name/given_name -> ошибка БД)")
    void nullFirstName_violatesNotNull() {
        User user = user("kc-1", "a@example.com", null);
        user.setFirstName(null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("last_name NOT NULL (OBS-02 as-is)")
    void nullLastName_violatesNotNull() {
        User user = user("kc-1", "a@example.com", null);
        user.setLastName(null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("email NOT NULL (OBS-02 as-is)")
    void nullEmail_violatesNotNull() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(user("kc-1", null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Телефон не уникален: два профиля с одним phone сохраняются (OBS-09 as-is)")
    void samePhone_isAllowed() {
        userRepository.saveAndFlush(user("kc-1", "a@example.com", "+79990000000"));
        userRepository.saveAndFlush(user("kc-2", "b@example.com", "+79990000000"));

        assertThat(userRepository.count()).isEqualTo(2);
    }
}
