package com.client.productionreview.dtos;


import com.client.productionreview.model.jpa.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {


    private Long id;


    private String name;

    private String email;

    private String username;

    private Boolean Active;

    private List<RoleDTO> roles;



}
