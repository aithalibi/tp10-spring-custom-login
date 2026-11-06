# TP 10 — Formulaire de connexion personnalisé avec Spring Security

Application Spring Boot / Thymeleaf qui **remplace la page de connexion par défaut** de Spring Security par un formulaire HTML maîtrisé, en gérant les messages d'erreur (`?error=true`), le message de déconnexion (`?logout=true`), les redirections après succès et la différenciation des accès par rôle.

> Cours : *Développement JakartaEE : Spring* — TP 10 (lab 209)

---

## 1. Objectifs

- Décrire le **flux complet d'authentification** de Spring Security.
- Brancher un formulaire HTML personnalisé sur une URL de traitement contrôlée par le framework (`/authenticate`).
- Gérer les **redirections** après succès (`/home`) ou échec (`/login?error=true`).
- Comprendre le rôle de chaque composant : `HttpSecurity`, `UserDetailsService`, `SecurityFilterChain`.
- Implémenter une **déconnexion propre** avec message de confirmation.

---

## 2. Flux d'authentification

```
1. GET /user/dashboard
        │  (non authentifié)
        ▼
2. SecurityFilterChain intercepte → 302 /login
        ▼
3. GET /login  (AuthController → login.html)
        ▼
4. POST /authenticate  (username, password, _csrf)
        ▼
5. UsernamePasswordAuthenticationFilter
        ├─ charge l'utilisateur via UserDetailsService
        ├─ compare le mot de passe via PasswordEncoder (BCrypt)
        └─ crée l'objet Authentication → SecurityContext (+ session)
        ▼
6. succès : 302 /home        échec : 302 /login?error=true
        ▼
7. POST /logout → session invalidée → 302 /login?logout=true
```

---

## 3. Configuration de la sécurité

`src/main/java/ma/fstg/security/config/SecurityConfig.java`

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/login", "/css/**", "/error").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
    )
    .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/authenticate")
        .defaultSuccessUrl("/home", true)
        .failureUrl("/login?error=true")
        .permitAll()
    )
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?logout=true")
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID")
        .permitAll()
    );
```

| Élément | Rôle |
|---|---|
| `loginPage("/login")` | Page affichée aux visiteurs non authentifiés |
| `loginProcessingUrl("/authenticate")` | URL **traitée par Spring**, aucun contrôleur à écrire |
| `defaultSuccessUrl("/home", true)` | Redirection **systématique** vers `/home` après succès (`alwaysUse = true`) |
| `failureUrl("/login?error=true")` | Paramètre lu par la vue pour afficher le message d'erreur |
| `logoutSuccessUrl("/login?logout=true")` | Message de confirmation de déconnexion |
| `hasAnyRole("USER", "ADMIN")` | `/user/**` ouvert aux deux rôles |

### Comptes de démonstration

| Identifiant | Mot de passe | Rôles | Accès |
|---|---|---|---|
| `user` | `1111` | `ROLE_USER` | `/home`, `/user/dashboard` |
| `admin` | `1234` | `ROLE_ADMIN`, `ROLE_USER` | tout, dont `/admin/dashboard` |

> Le TP d'origine utilise `{noop}1234` / `{noop}1111`. Ici les mots de passe sont **encodés en BCrypt** au démarrage (`UserDetailsConfig`) : le comportement fonctionnel est identique, la sécurité est meilleure.

---

## 4. Contrôleur de vues

`src/main/java/ma/fstg/security/web/AuthController.java`

| Méthode | URL | Vue | Accès |
|---|---|---|---|
| GET | `/login` | `login.html` | public |
| GET | `/home` | `home.html` | authentifié |
| GET | `/user/dashboard` | `user-dashboard.html` | `USER` ou `ADMIN` |
| GET | `/admin/dashboard` | `admin-dashboard.html` | `ADMIN` |

---

## 5. Formulaire de connexion

```html
<form th:action="@{/authenticate}" method="post">
    <input type="text" name="username" required>
    <input type="password" name="password" required>
    <button type="submit">Se connecter</button>
</form>

<p th:if="${param.error}">Identifiants incorrects, veuillez réessayer.</p>
<p th:if="${param.logout}">Vous êtes déconnecté avec succès.</p>
```

- `th:action="@{/authenticate}"` : l'URL interceptée par le filtre d'authentification.
- Thymeleaf injecte automatiquement le jeton **CSRF** caché dans tout formulaire `method="post"` avec `th:action`.
- `param.error` / `param.logout` : lecture des paramètres de l'URL pour afficher les messages.

---

## 6. Structure du projet

```
tp10-spring-login
├── pom.xml
├── README.md
├── docs
│   ├── build.log
│   ├── tests.log
│   └── images/
└── src
    ├── main
    │   ├── java/ma/fstg/security
    │   │   ├── CustomLoginDemoApplication.java
    │   │   ├── config
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── UserDetailsConfig.java
    │   │   └── web
    │   │       └── AuthController.java
    │   └── resources
    │       ├── application.properties
    │       ├── static/css/style.css
    │       └── templates
    │           ├── fragments.html
    │           ├── login.html
    │           ├── home.html
    │           ├── user-dashboard.html
    │           ├── admin-dashboard.html
    │           └── error.html
    └── test/java/ma/fstg/security
        ├── SecurityConfigTest.java   (13 tests)
        └── UserDetailsConfigTest.java (6 tests)
```

---

## 7. Lancer l'application

```bash
mvn clean package
java -jar target/custom-login-demo-1.0.0.jar
# ou
mvn spring-boot:run
```

→ http://localhost:8080/login

Dépendances : `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-thymeleaf`, `spring-boot-devtools` (comme demandé dans l'énoncé), plus `thymeleaf-extras-springsecurity6` (attributs `sec:`) et `spring-security-test` (tests).

---

## 8. Comportements observés (Étape 8 du TP)

| Action | Comportement attendu | Constat |
|---|---|---|
| Accès sans login à `/user/dashboard` | redirection vers `/login` | ✅ 302 → `/login` |
| Connexion réussie (`user / 1111`) | redirection vers `/home` | ✅ 302 → `/home` |
| Mauvais mot de passe | retour sur `/login?error=true` | ✅ message affiché |
| Déconnexion | `/login?logout=true` | ✅ message affiché, session invalidée |
| `user` sur `/admin/dashboard` | 403 — accès refusé | ✅ page d'erreur explicite |
| `admin` sur `/admin/dashboard` | 200 | ✅ |
| `admin` sur `/user/dashboard` | 200 (`hasAnyRole`) | ✅ |

### Captures

| Fichier | Contenu |
|---|---|
| `docs/images/01-login.png` | Formulaire personnalisé |
| `docs/images/02-login-erreur.png` | Message « Identifiants incorrects » |
| `docs/images/03-home-user.png` | Accueil après connexion `user` |
| `docs/images/04-user-dashboard.png` | Espace utilisateur (`user`) |
| `docs/images/05-admin-refuse-403.png` | 403 pour `user` sur `/admin/dashboard` |
| `docs/images/06-logout.png` | Message de déconnexion |
| `docs/images/07-home-admin.png` | Accueil après connexion `admin` |
| `docs/images/08-admin-dashboard.png` | Espace administrateur (`admin`) |
| `docs/images/09-admin-user-dashboard.png` | `admin` dans l'espace utilisateur |

---

## 9. Tests automatisés

```bash
mvn test
```

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- SecurityConfigTest
Tests run:  6, Failures: 0, Errors: 0, Skipped: 0 -- UserDetailsConfigTest
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`SecurityConfigTest` couvre : page de login publique, redirection des ressources protégées, `/home` protégé, connexion `user` et `admin` via `formLogin("/authenticate")`, mauvais mot de passe, messages `?error=true` / `?logout=true`, accès `user` et `admin` aux espaces, **403** pour `user` sur `/admin/dashboard`, déconnexion avec invalidation de session.

`UserDetailsConfigTest` vérifie l'encodage BCrypt, l'absence de `{noop}`, la composition des rôles et le `UsernameNotFoundException`.

---

## 10. Points pédagogiques

- **Découplage logique / vue** : le traitement reste côté serveur (filtre Spring), la présentation est librement personnalisable.
- **Un seul formulaire** suffit pour tout le processus d'authentification : Spring gère session, cookie et CSRF.
- **`defaultSuccessUrl("/home", true)`** force la destination, alors que `false` respecterait l'URL initialement demandée.
- **`loginProcessingUrl`** ne nécessite aucun contrôleur : c'est le filtre `UsernamePasswordAuthenticationFilter` qui traite la requête.
- **Flexibilité** : on peut ajouter une redirection conditionnelle selon le rôle avec un `AuthenticationSuccessHandler`.