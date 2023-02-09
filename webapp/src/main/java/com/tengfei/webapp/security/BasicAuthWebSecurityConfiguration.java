package com.tengfei.webapp.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import com.tengfei.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class BasicAuthWebSecurityConfiguration{

    @Autowired
    private UserRepository repository;

    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests()
//                .requestMatchers(HttpMethod.GET,"/v1/user/**").permitAll()
                .requestMatchers("/").permitAll()
//                .requestMatchers("/v1/user/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic(Customizer.withDefaults()).csrf().disable();
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(HttpMethod.POST, "/v1/user")
                .requestMatchers(HttpMethod.GET,"/healthz").requestMatchers(HttpMethod.GET,"/v1/product/**");
    }

//    @Bean
//    public InMemoryUserDetailsManager userDetailsService() {
////        List<UserDetails> userDetailsList = new ArrayList<>();
//
//        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
//        Iterable<com.tengfei.webapp.model.User> userList=repository.findAll();
//        for (com.tengfei.webapp.model.User user:userList){
//            String username = user.getUsername();
//            String password = user.getPassword();
//            manager.createUser(User.withUsername(username).password(password).roles("USER").build());
//        }
//        return manager;
//    }

    @Bean
    public UserDetailsService userDetailsService(){
        return new CustomUserDetailsService();
    }
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider=new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        BCryptPasswordEncoderBean bCryptPasswordEncoder=new BCryptPasswordEncoderBean();

        authenticationProvider.setPasswordEncoder(bCryptPasswordEncoder.bCryptPasswordEncoder());
        return authenticationProvider;
    }

}
