package com.tengfei.webapp.controller;

import com.tengfei.webapp.exceptions.UserNotFoundException;
import com.tengfei.webapp.model.User;
import com.tengfei.webapp.repository.UserRepository;
import com.tengfei.webapp.security.BCryptPasswordEncoderBean;
import jakarta.validation.Valid;
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
    private final UserRepository repository;

    public UserController(UserRepository repository){
        this.repository=repository;
    }

    @PostMapping("/v1/user")
    public ResponseEntity<String> createUser(@Valid @RequestBody User user){

        if (user.getUsername()==null || user.getPassword()==null || user.getLastName()==null || user.getFirstName()==null){
            return ResponseEntity.status(400).build();
        }

        if (repository.findByUsername(user.getUsername())!=null){
            return ResponseEntity.status(400).build();
        }

        //check valid email address
        String emailAddress = user.getUsername();
        String regexPattern = "^(.+)@(\\S+)$";
        boolean patternMatches = patternMatches(emailAddress, regexPattern);
        if (!patternMatches){
            return ResponseEntity.status(400).build();
        }

        BCryptPasswordEncoderBean bCryptPasswordEncoder=new BCryptPasswordEncoderBean();
        String encoding= bCryptPasswordEncoder.bCryptPasswordEncoder().encode(user.getPassword());

//        String encoding = Base64.getEncoder().encodeToString((user.getUsername()() + ":" + user.getPassword()).getBytes());
//        String authHeader = "Basic " + encoding;
        user.setPassword(encoding);
        repository.saveAndFlush(user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/v1/user/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable int userId,@Valid @RequestBody User userDetails){
        if (userDetails.getUsername()!=null || userDetails.getAccount_updated()!=null
                || userDetails.getAccount_created()!=null || userDetails.getId()!=null){
            return ResponseEntity.badRequest().build();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
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

            final User updatedUser =  repository.saveAndFlush(user);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/v1/user/{userId}")
    public ResponseEntity<User> retrieveUserAccountInfo(@PathVariable int userId) {
        Optional<User> user = repository.findById(userId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = repository.findByUsername(username);

        if (loginUser.getId().equals(userId)){
            if (user.isEmpty())
                throw new UserNotFoundException("id" + userId);

            return ResponseEntity.ok(user.get());
        }else {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> getHealth(){
        return ResponseEntity.ok("200 OK");
    }

    public static boolean patternMatches(String emailAddress, String regexPattern) {
        return Pattern.compile(regexPattern)
                .matcher(emailAddress)
                .matches();
    }
}
