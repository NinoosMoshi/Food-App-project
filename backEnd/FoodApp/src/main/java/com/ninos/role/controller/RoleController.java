package com.ninos.role.controller;

import com.ninos.response.Response;
import com.ninos.role.dtos.RoleDTO;
import com.ninos.role.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Response<RoleDTO>> createRole(@RequestBody @Valid RoleDTO roleDTO) {
       return ResponseEntity.ok(roleService.createRole(roleDTO));
    }

    @PutMapping
    public ResponseEntity<Response<RoleDTO>> updateRole(@RequestBody @Valid RoleDTO roleDTO) {
        return ResponseEntity.ok(roleService.updateRole(roleDTO));
    }

    @GetMapping
    public ResponseEntity<Response<List<RoleDTO>>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<?>> deleteRole(@PathVariable("id") Long id) {
       return ResponseEntity.ok(roleService.deleteRole(id));
    }


}
