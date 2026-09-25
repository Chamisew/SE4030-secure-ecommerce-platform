package com.example.backend.auth.security.oauth2;

import com.example.backend.entity.Role;
import com.example.backend.entity.Users;
import com.example.backend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsersRepo usersRepo;
    private final PasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        return processOAuth2User(oAuth2UserRequest, oAuth2User);
    }

    public OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<Users> userOptional = usersRepo.findByEmail(oAuth2UserInfo.getEmail());
        Users user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private Users registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        String fullName = oAuth2UserInfo.getName();
        String firstName = "User";
        String lastName = "OAuth";
        if (StringUtils.hasText(fullName)) {
            String[] parts = fullName.trim().split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1];
            }
        }

        Users user = Users.builder()
                .email(oAuth2UserInfo.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ROLE_USER) // Strict default role: defense against V03 role elevation
                .emailVerified(true)  // Google accounts have verified email
                .enabled(true)
                .profileImageUrl(oAuth2UserInfo.getImageUrl())
                .build();

        return usersRepo.save(user);
    }

    private Users updateExistingUser(Users existingUser, OAuth2UserInfo oAuth2UserInfo) {
        if (StringUtils.hasText(oAuth2UserInfo.getImageUrl()) && existingUser.getProfileImageUrl() == null) {
            existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        }
        if (!existingUser.isEmailVerified()) {
            existingUser.setEmailVerified(true);
        }
        existingUser.setEnabled(true);
        return usersRepo.save(existingUser);
    }
}
