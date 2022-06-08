package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String eiosLogin) throws UsernameNotFoundException {
        User user = userService.findByEiosLogin(eiosLogin);
        return CustomUserDetails.fromUserToCustomUserDetails(user);
    }

    public UserDetails loadUserById(int id) throws UsernameNotFoundException {
        User user = userService.findUser(id);
        return CustomUserDetails.fromUserToCustomUserDetails(user);
    }
}
