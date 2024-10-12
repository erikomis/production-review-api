package com.client.productionreview.model.jpa;

import com.client.productionreview.config.RoleGrantedAuthority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User implements UserDetails, Serializable {

    private static final long serialVersionUID = 18L;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "username", unique = true)
    private String username;

    private String password;

    @Column(name = "active")
    private Boolean active;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id" , referencedColumnName = "id")
    )
    @Builder.Default
    private Collection<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return roles.stream()
//                .map(role -> new SimpleGrantedAuthority(role.getName()))
//                .collect(Collectors.toSet());

//       authorities.addAll(roles.stream()
//                .flatMap(role -> role.getPermissions().stream())
//                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
//                .collect(Collectors.toList());


//        List<Role> aa = roles.stream().collect(Collectors.toList());
//
//        System.out.println("aa = " + aa);
//        Set<GrantedAuthority> authorities = roles.stream()
//                .map(role -> new SimpleGrantedAuthority("ROLE_"+role.getName()))
//                .collect(Collectors.toSet());
//
//        authorities.addAll(roles.stream()
//                .flatMap(role -> role.getPermissions().stream())
//                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
//                .collect(Collectors.toSet()));

        //System.out.println("authorities = " + authorities + " roles = " + roles);
        List<GrantedAuthority> authorities = roles.stream()
                .flatMap(role -> {
                    // Mapeia a role como uma autoridade
                    Stream<GrantedAuthority> roleAuthority = Stream.of(new RoleGrantedAuthority(role));

                    // Mapeia as permissões como autoridades
                    Stream<GrantedAuthority> permissionAuthorities = role.getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getName()));

                    return roleAuthority;
                })
                .collect(Collectors.toList());
       // System.out.println("authorities = " + authorities);
        return authorities;

    }
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
