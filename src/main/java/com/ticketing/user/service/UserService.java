package com.ticketing.user.service;

import com.ticketing.common.exception.ConflictException;
import com.ticketing.common.exception.ResourceNotFoundException;
import com.ticketing.user.api.UserRequest;
import com.ticketing.user.api.UserResponse;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }
    @Transactional
    public UserResponse create(UserRequest request){
        User user = new User(request.fullname(),request.email());
        try{
           User saved = userRepository.save(user);
           return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email aleady registered: "+ request.email());
        }
    }
    public UserResponse getById(UUID id){
        User user = userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found: "+ id));
        return UserResponse.from(user);
    }
}
