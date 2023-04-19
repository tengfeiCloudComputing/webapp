package com.tengfei.webapp.controller;

import com.tengfei.webapp.exceptions.UserNotFoundException;
import com.tengfei.webapp.model.User;
import com.tengfei.webapp.repository.UserRepository;
import com.tengfei.webapp.security.BCryptPasswordEncoderBean;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@RestController
public class UserController {
    Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StatsDClient statsDClient;

    private final UserRepository repository;

    public UserController(UserRepository repository){
        this.repository=repository;
    }

    @PostMapping("/v2/user")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user){
        statsDClient.incrementCounter("csye6225.http.post.user");
        logger.info("Creating User...");
        if (user.getUsername()==null || user.getPassword()==null || user.getLastName()==null || user.getFirstName()==null){
            logger.error("null exists in fields");
            return ResponseEntity.status(400).build();
        }

        if (repository.findByUsername(user.getUsername())!=null){
            logger.error("the user has existed:"+ user.getUsername());
            return ResponseEntity.status(403).build();
        }

        //check valid email address
        String emailAddress = user.getUsername();
        String regexPattern = "^(.+)@(\\S+)$";
        boolean patternMatches = patternMatches(emailAddress, regexPattern);
        if (!patternMatches){
            logger.error("not email format");
            return ResponseEntity.status(400).build();
        }

        BCryptPasswordEncoderBean bCryptPasswordEncoder=new BCryptPasswordEncoderBean();
        String encoding= bCryptPasswordEncoder.bCryptPasswordEncoder().encode(user.getPassword());

//        String encoding = Base64.getEncoder().encodeToString((user.getUsername()() + ":" + user.getPassword()).getBytes());
//        String authHeader = "Basic " + encoding;
        user.setPassword(encoding);
        repository.saveAndFlush(user);
        logger.info("User created...");
        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/v2/user/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable int userId,@Valid @RequestBody User userDetails){
        logger.info("updating User...");
        statsDClient.incrementCounter("csye6225.http.update.user");
        if (userDetails.getUsername()!=null || userDetails.getAccount_updated()!=null
                || userDetails.getAccount_created()!=null || userDetails.getId()!=null){
            logger.error("all fields should be provided");
            return ResponseEntity.badRequest().build();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            logger.error("update user not authenticated");
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = repository.findByUsername(username);

        // the same authenticated user
        if (loginUser.getId().equals(userId)){
            User user = repository
                    .findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found on : " + userId));


            if (userDetails.getFirstName()!=null && userDetails.getFirstName().length()>0){
                user.setFirstName(userDetails.getFirstName());
            }
            if (userDetails.getLastName()!=null && userDetails.getLastName().length()>0){
                user.setFirstName(userDetails.getLastName());
            }
            if (userDetails.getPassword()!=null && userDetails.getPassword().length()>3){
                BCryptPasswordEncoderBean bCryptPasswordEncoder=new BCryptPasswordEncoderBean();
                String encoding= bCryptPasswordEncoder.bCryptPasswordEncoder().encode(userDetails.getPassword());
                user.setPassword(encoding);
            }
            logger.info("User Created");
            final User updatedUser =  repository.saveAndFlush(user);
            return ResponseEntity.status(204).build();
        }else{
            logger.error("Not allowed to update other users");
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/v2/user/{userId}")
    public ResponseEntity<User> retrieveUserAccountInfo(@PathVariable int userId) {
        statsDClient.incrementCounter("csye6225.http.retrieve.specific.user");
        logger.info("retrieving user info");
        Optional<User> user = repository.findById(userId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            logger.error("retrieve user info not authenticated");
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = repository.findByUsername(username);

        if (loginUser.getId().equals(userId)){
            if (user.isEmpty())
                throw new UserNotFoundException("id" + userId);
            logger.info("retrieved user info in success");

            return ResponseEntity.ok(user.get());
        }else {
            logger.error("not able to retrieved other user info");
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> getHealth(){
        statsDClient.incrementCounter("csye6225.http.health.check");
        logger.info("doing health check");
        return ResponseEntity.ok("200 OK");
    }

    public static boolean patternMatches(String emailAddress, String regexPattern) {
        return Pattern.compile(regexPattern)
                .matcher(emailAddress)
                .matches();
    }
}
