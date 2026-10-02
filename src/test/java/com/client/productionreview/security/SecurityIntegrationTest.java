package com.client.productionreview.security;

import com.client.productionreview.dtos.auth.AutoSignInDTOResponse;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Permission;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.PermissionRepository;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.CategoryService;
import com.client.productionreview.service.ProductImageService;
import com.client.productionreview.service.ProductService;
import com.client.productionreview.service.ReviewService;
import com.client.productionreview.service.SubCategoryService;
import com.client.productionreview.service.UserDetailsService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sobe o contexto completo com o SecurityConfig e o AuthenticationFilter reais.
 * Os services são mockados para não depender de Redis, Kafka e MinIO.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String CATEGORY_JSON = "{\"name\":\"Eletrônicos\",\"description\":\"d\",\"slug\":\"eletronicos\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private SubCategoryService subCategoryService;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductImageService productImageService;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private UserDetailsService userDetailsService;

    private User admin;
    private User commonUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        Permission write = permissionRepository.save(new Permission(null, "WRITE_PRIVILEGES", null));
        Permission delete = permissionRepository.save(new Permission(null, "DELETE_PRIVILEGES", null));

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setPermissions(Set.of(write, delete));
        adminRole = roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        admin = userRepository.save(User.builder().name("Admin").username("admin").email("admin@mail.com")
                .password("x").active(true).roles(List.of(adminRole)).build());
        commonUser = userRepository.save(User.builder().name("User").username("user").email("user@mail.com")
                .password("x").active(true).roles(List.of(userRole)).build());

        when(categoryService.addCategory(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(categoryService.getCategory(anyLong())).thenReturn(Category.builder().id(1L).name("c").build());
        when(categoryService.getAllCategories()).thenReturn(List.of());
    }

    private Cookie accessCookie(User user) {
        return new Cookie("token", jwtProvider.generateToken(user.getId()).getValue());
    }

    // ---------- rotas públicas ----------

    @Test
    void publicGetRoutes_doNotRequireLogin() throws Exception {
        mockMvc.perform(get("/api/v1/category/list")).andExpect(status().isOk());
        // antes: GET por id exigia login, embora a listagem fosse pública
        mockMvc.perform(get("/api/v1/category/1")).andExpect(status().isOk());
    }

    @Test
    void refreshToken_worksWithoutAccessToken() throws Exception {
        when(userDetailsService.refreshToken(any())).thenReturn(AutoSignInDTOResponse.builder()
                .token(ResponseCookie.from("token", "a").build())
                .refreshToken(ResponseCookie.from("refresh_token", "r").build())
                .build());

        // antes: a rota exigia um access token válido, o que anulava o propósito do refresh
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .cookie(new Cookie("refresh_token", jwtProvider.generateRefreshToken(admin.getId()).getValue())))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    // ---------- autenticação ----------

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/category/").contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isUnauthorized())
                // mesmo formato de erro do restante da API
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.httpStatus").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Faça login para continuar"))
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        String refresh = jwtProvider.generateRefreshToken(admin.getId()).getValue();

        mockMvc.perform(get("/api/v1/user/me").cookie(new Cookie("token", refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUser_returns401InsteadOf500() throws Exception {
        Cookie cookie = accessCookie(commonUser);
        userRepository.delete(commonUser);

        mockMvc.perform(get("/api/v1/user/me").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/user/me").cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.roles[0].name").value("ADMIN"));
    }

    // ---------- autorização ----------

    @Test
    void adminRoute_withCommonUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/category/").cookie(accessCookie(commonUser))
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").cookie(accessCookie(commonUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.httpStatus").value("FORBIDDEN"));
    }

    @Test
    void adminRoute_withAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/category/").cookie(accessCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Eletrônicos"));
    }

    @Test
    void imageUpload_requiresAdmin() throws Exception {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        // antes: qualquer usuário logado podia enviar/apagar imagens de produtos
        mockMvc.perform(multipart("/api/v1/production/file").file(file).param("idProduct", "1")
                        .cookie(accessCookie(commonUser)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/production/file/1").cookie(accessCookie(commonUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSubCategory_withNumericCategoryId_isAccepted() throws Exception {
        when(subCategoryService.addSubCategory(any())).thenAnswer(inv -> {
            SubCategory s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        // antes: @NotBlank/@Pattern em campo Long causavam erro 500 em toda criação
        mockMvc.perform(post("/api/v1/sub-categorie/create").cookie(accessCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Celulares\",\"description\":\"d\",\"slug\":\"celulares\",\"categorieId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorieId").value(1));
    }

    // ---------- CORS ----------

    @Test
    void preflight_onProtectedRoute_fromAllowedOrigin_isAccepted() throws Exception {
        mockMvc.perform(options("/api/v1/category/")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void preflight_fromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/v1/category/")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    // ---------- fase 2: rotas de admin, minhas reviews e usuário inativo ----------

    @Test
    void adminRoutes_withoutLogin_return401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/reviews")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/import/jobs/latest")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/activity")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminRoutes_withCommonUser_return403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").cookie(accessCookie(commonUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").cookie(accessCookie(commonUser))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/users/{id}/admin", commonUser.getId()).cookie(accessCookie(commonUser))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"admin\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/import/open-food-facts").cookie(accessCookie(commonUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoutes_withAdmin_return200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.users").value(2))
                .andExpect(jsonPath("$.reviewsPerDay.length()").value(30));
        mockMvc.perform(get("/api/v1/admin/users").cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/api/v1/admin/reviews").cookie(accessCookie(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/import/jobs/latest").cookie(accessCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminActivity_logsServiceDown_returns503() throws Exception {
        mockMvc.perform(get("/api/v1/admin/activity").cookie(accessCookie(admin)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Serviço de auditoria indisponível"));
    }

    @Test
    void myReviews_requiresLogin() throws Exception {
        when(reviewService.getMyReviews(anyLong(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/review/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/review/me").cookie(accessCookie(commonUser))).andExpect(status().isOk());
    }

    @Test
    void helpful_requiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/review/{id}/helpful", 1L)).andExpect(status().isUnauthorized());
    }

    @Test
    void categoryBySlug_isPublic() throws Exception {
        when(categoryService.getCategoryBySlug("bebidas"))
                .thenReturn(Category.builder().id(1L).name("Bebidas").slug("bebidas").subCategories(List.of()).build());

        mockMvc.perform(get("/api/v1/category/slug/{slug}", "bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("bebidas"));
    }

    @Test
    void inactiveUser_withOldToken_returns401() throws Exception {
        Cookie cookie = accessCookie(commonUser);
        mockMvc.perform(get("/api/v1/user/me").cookie(cookie)).andExpect(status().isOk());

        commonUser.setActive(false);
        userRepository.save(commonUser);

        // desativado por um admin: o token emitido antes deixa de valer
        mockMvc.perform(get("/api/v1/user/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    // ---------- fase 3: Origin e cookies ----------

    @Test
    void post_fromUnknownOrigin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/category/").cookie(accessCookie(admin))
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Origem não permitida"))
                .andExpect(jsonPath("$.httpStatus").value("FORBIDDEN"))
                .andExpect(jsonPath("$.statusCode").value(403));
        mockMvc.perform(post("/api/v1/auth/sign-in").header(HttpHeaders.REFERER, "https://evil.example.com/x")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"a\",\"password\":\"b\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_fromAllowedOrigin_orWithoutOrigin_passes() throws Exception {
        mockMvc.perform(post("/api/v1/category/").cookie(accessCookie(admin))
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/category/").cookie(accessCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void get_withoutOrigin_orFromOtherOrigin_passes() throws Exception {
        mockMvc.perform(get("/api/v1/category/list")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/category/list").header(HttpHeaders.ORIGIN, "https://evil.example.com"))
                .andExpect(status().isForbidden()); // recusado pelo CORS, não pelo filtro de Origin
    }

    @Test
    void sessionCookies_haveSameSiteSecureAndHttpOnly() {
        String token = jwtProvider.generateToken(admin.getId()).toString();
        String refresh = jwtProvider.generateRefreshToken(admin.getId()).toString();
        String cleared = jwtProvider.cleanToken().toString();

        for (String cookie : java.util.List.of(token, refresh, cleared)) {
            org.junit.jupiter.api.Assertions.assertTrue(cookie.contains("SameSite=Lax"), cookie);
            org.junit.jupiter.api.Assertions.assertTrue(cookie.contains("Secure"), cookie);
            org.junit.jupiter.api.Assertions.assertTrue(cookie.contains("HttpOnly"), cookie);
        }
    }
}
