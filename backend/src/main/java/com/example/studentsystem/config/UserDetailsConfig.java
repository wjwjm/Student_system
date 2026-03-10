package com.example.studentsystem.config;

import com.example.studentsystem.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class UserDetailsConfig {
    @Bean
    UserDetailsService userDetailsService(UserAccountRepository repository) {
        return username -> repository.findByUsername(username)
                .map(account -> new User(account.getUsername(), account.getPassword(),
                        java.util.List.of(new SimpleGrantedAuthority(account.getRole()))))
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));
    }
}
