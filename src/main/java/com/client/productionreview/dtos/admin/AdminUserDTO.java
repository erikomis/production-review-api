package com.client.productionreview.dtos.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {

    private Long id;
    private String name;
    private String username;
    private String email;
    private Boolean active;
    private List<String> roles;
    private Instant createdAt;
    private long reviewsCount;
}
