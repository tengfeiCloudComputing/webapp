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

@RestController
public class UserController {
    private UserRepository repository;

    public UserController(UserRepository repository){
        this.repository=repository;
    }

    @PostMapping("/v1/user")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user){

        if (repository.findByEmail(user.getEmail())!=null){
            return ResponseEntity.status(400).build();
        }

        BCryptPasswordEncoderBean bCryptPasswordEncoder=new BCryptPasswordEncoderBean();
        String encoding= bCryptPasswordEncoder.bCryptPasswordEncoder().encode(user.getPassword());

//        String encoding = Base64.getEncoder().encodeToString((user.getEmail() + ":" + user.getPassword()).getBytes());
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
        if (userDetails.getEmail()!=null || userDetails.getAccount_updated()!=null
                || userDetails.getAccount_created()!=null || userDetails.getId()!=null){
            return ResponseEntity.badRequest().build();
        }
//        if (userDetails.getFirstName()!=null){
//
//        }
//

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null){
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User loginUser = repository.findByEmail(username);

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
    public User retrieveUserAccountInfo(@PathVariable int userId) {
        Optional<User> user = repository.findById(userId);
        if (user.isEmpty())
            throw new UserNotFoundException("id" + userId);
        User displayedUser=new User();

        //hide password
        displayedUser.setId(user.get().getId());
        displayedUser.setEmail(user.get().getEmail());
        displayedUser.setFirstName(user.get().getFirstName());
        displayedUser.setLastName(user.get().getLastName());
        displayedUser.setAccount_created(user.get().getAccount_created());
        displayedUser.setAccount_updated(user.get().getAccount_updated());

        return displayedUser;
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> getHealth(){
        return ResponseEntity.ok("200 OK");
    }
}
