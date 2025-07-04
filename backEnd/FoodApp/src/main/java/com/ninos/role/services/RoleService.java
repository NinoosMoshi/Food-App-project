package com.ninos.role.services;

import com.ninos.response.Response;
import com.ninos.role.dtos.RoleDTO;

import java.util.List;

public interface RoleService {

    Response<RoleDTO> createRole(RoleDTO roleDTO);
    Response<RoleDTO> updateRole(RoleDTO roleDTO);
    Response<List<RoleDTO>> getAllRoles();
    Response<?> deleteRole(Long id);

}
