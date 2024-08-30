package com.client.productionreview.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AutoSignInDTOResponse {





    private ResponseCookie token;
    private ResponseCookie refreshToken;





}
