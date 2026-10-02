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
                .andExpect(status().isUnauthorized());
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
}
