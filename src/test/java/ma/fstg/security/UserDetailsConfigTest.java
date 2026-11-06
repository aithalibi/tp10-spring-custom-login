package ma.fstg.security;

import ma.fstg.security.config.UserDetailsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {CustomLoginDemoApplication.class, UserDetailsConfig.class})
class UserDetailsConfigTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("L'encodeur est un BCrypt")
    void encoderIsBcrypt() {
        assertThat(passwordEncoder.getClass().getSimpleName()).isEqualTo("BCryptPasswordEncoder");
    }

    @Test
    @DisplayName("Les mots de passe sont stockes haches en BCrypt et restent verifiables")
    void passwordsAreBcryptHashed() {
        UserDetails user = userDetailsService.loadUserByUsername("user");
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(admin.getPassword()).startsWith("$2a$");
        assertThat(user.getPassword()).isNotEqualTo("1111");
        assertThat(admin.getPassword()).isNotEqualTo("1234");

        assertThat(passwordEncoder.matches("1111", user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("1234", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("0000", user.getPassword())).isFalse();
    }

    @Test
    @DisplayName("Aucun mot de passe n'utilise le prefixe {noop} du TP d'origine")
    void noNoopPrefix() {
        assertThat(userDetailsService.loadUserByUsername("user").getPassword()).doesNotContain("{noop}");
        assertThat(userDetailsService.loadUserByUsername("admin").getPassword()).doesNotContain("{noop}");
    }

    @Test
    @DisplayName("user possede ROLE_USER uniquement")
    void userHasOnlyUserRole() {
        UserDetails user = userDetailsService.loadUserByUsername("user");

        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("admin possede ROLE_ADMIN et ROLE_USER")
    void adminHasBothRoles() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("Un identifiant inconnu leve UsernameNotFoundException")
    void unknownUserThrows() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}