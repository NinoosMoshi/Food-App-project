package com.ninos.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // If a field is null, it will not appear in the resulting JSON.
@JsonIgnoreProperties(ignoreUnknown = true) // If the JSON contains fields not present in the Java class, they are ignored
public class UserDTO {

    private Long id;
    private String name;
    private String phoneNumber;
    private String profileUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // password can be written in the request body, but will not be included in the response
    private String password;

    private boolean isActive;
    private String address;
    private List<Role> roles;
    private MultipartFile imageFile;



}
