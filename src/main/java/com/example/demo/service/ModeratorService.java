package com.example.demo.service;

import com.example.demo.config.CustomUserDetails;
import com.example.demo.repository.ModeratorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ModeratorService {
    @Autowired
    private ModeratorRepository moderatorRepository;
    
    //Получаем айди текущего авторизованного модератора из токена
    public int getModeratorId() {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int userId = customUserDetails.getId();
        return moderatorRepository.findByUser_Id(userId).getIdModerator();
    }
}
