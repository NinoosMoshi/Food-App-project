package com.ninos.auth_users.services;

import com.ninos.auth_users.dtos.LoginRequest;
import com.ninos.auth_users.dtos.LoginResponse;
import com.ninos.auth_users.dtos.RegistrationRequest;
import com.ninos.response.Response;

public interface AuthService {

    Response<?> register(RegistrationRequest registrationRequest);
    Response<LoginResponse> login(LoginRequest loginRequest);

}
