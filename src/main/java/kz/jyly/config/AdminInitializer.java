package kz.jyly.config;

import kz.jyly.entity.AppUser;
import kz.jyly.enums.Role;
import kz.jyly.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Әкімші есептік жазбасын конфигурациядан (JYLY_ADMIN_USERNAME / JYLY_ADMIN_PASSWORD) жасайды.
 * Құпиясөз дерекқорда тек BCrypt хеші түрінде сақталады.
 */
@Component
@Order(1)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JylyProperties properties;

    public AdminInitializer(AppUserRepository users, PasswordEncoder encoder, JylyProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = properties.getAdmin().getUsername();
        String password = properties.getAdmin().getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Admin account is not configured. Set JYLY_ADMIN_USERNAME and JYLY_ADMIN_PASSWORD to enable /admin.");
            return;
        }
        if (password.length() < 8) {
            log.warn("Admin password is shorter than 8 characters — use a stronger password outside local demo.");
        }
        AppUser user = users.findByUsername(username).orElseGet(AppUser::new);
        boolean isNew = user.getId() == null;
        if (isNew || !encoder.matches(password, user.getPasswordHash())) {
            user.setUsername(username);
            user.setPasswordHash(encoder.encode(password));
            user.setRole(Role.ADMIN);
            user.setEnabled(true);
            users.save(user);
            log.info("Admin account '{}' {}", username, isNew ? "created" : "password updated from configuration");
        }
    }
}
