package com.client.productionreview.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AutoSignInDTOResponse {


    private Long id;

    private String name;

    private String username;

    private String email;


    private String token;

    private String type;

    private String refreshToken;





}
