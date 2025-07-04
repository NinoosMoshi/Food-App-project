package com.ninos.auth_users.services;

import com.ninos.auth_users.dtos.UserDTO;
import com.ninos.auth_users.entity.User;
import com.ninos.response.Response;

import java.util.List;

public interface UserService {

    User getCurrentLoggedInUser();
    Response<List<UserDTO>> getAllUsers();
    Response<UserDTO> getOwnAccountDetails();
    Response<?> updateOwnAccount(UserDTO userDTO);
    Response<?> deactivateOwnAccount();


}
