package ma.fstg.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests du flux d'authentification du TP 10 : formulaire personnalise,
 * URL de traitement /authenticate, redirections et controle par role.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /login est public et affiche le formulaire personnalise")
    void loginPageIsPublic() throws Exception {
        MvcResult result = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/authenticate\"")))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Se connecter");
    }

    @Test
    @DisplayName("Une ressource protegee redirige un visiteur vers /login")
    void protectedResourceRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /home est protege")
    void homeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("POST /authenticate avec user/1111 authentifie et redirige vers /home")
    void userLoginSucceeds() throws Exception {
        mockMvc.perform(formLogin("/authenticate").user("user").password("1111"))
                .andExpect(authenticated().withUsername("user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @DisplayName("POST /authenticate avec admin/1234 authentifie l'administrateur")
    void adminLoginSucceeds() throws Exception {
        mockMvc.perform(formLogin("/authenticate").user("admin").password("1234"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @DisplayName("Un mauvais mot de passe renvoie vers /login?error=true")
    void badPasswordRedirectsToErrorUrl() throws Exception {
        mockMvc.perform(formLogin("/authenticate").user("user").password("mauvais"))
                .andExpect(unauthenticated())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("La page de login affiche le message d'erreur avec ?error=true")
    void loginPageShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Identifiants incorrects")));
    }

    @Test
    @DisplayName("La page de login affiche le message de deconnexion avec ?logout=true")
    void loginPageShowsLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deconnecte avec succes")));
    }

    @Test
    @DisplayName("user accede a /home et /user/dashboard")
    void userCanAccessUserSpace() throws Exception {
        mockMvc.perform(get("/home").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));

        mockMvc.perform(get("/user/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user-dashboard"));
    }

    @Test
    @DisplayName("user est refuse (403) sur /admin/dashboard")
    void userCannotAccessAdminSpace() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin accede aux deux espaces (hasAnyRole USER, ADMIN)")
    void adminCanAccessBothSpaces() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));

        mockMvc.perform(get("/user/dashboard").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user-dashboard"));
    }

    @Test
    @DisplayName("POST /logout deconnecte et redirige vers /login?logout=true")
    void logoutInvalidatesSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/logout")
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("La page /home affiche l'utilisateur et ses autorites")
    void homeDisplaysAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/home").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("user")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ROLE_USER")));
    }
}