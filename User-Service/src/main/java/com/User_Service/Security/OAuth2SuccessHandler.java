package com.User_Service.Security;

import com.User_Service.Model.User;
import com.User_Service.Repository.UserRepository;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository repository;

    // ✅ Make frontend URL configurable via application.properties
    @Value("${frontend.url:http://localhost:8081}")
    private String frontendUrl;

    public OAuth2SuccessHandler(JwtUtil jwtUtil, UserRepository repository) {
        this.jwtUtil = jwtUtil;
        this.repository = repository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // ✅ Get email from OAuth provider
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // ✅ Create or find user in database
        User user = repository.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name != null ? name : email);
                    u.setPassword("OAUTH2_USER"); // Placeholder password for OAuth users
                    return repository.save(u);
                });

        // ✅ Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // ✅ URL encode the token to handle special characters
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

        // ✅ Redirect to frontend dashboard with token
        // Frontend will extract this token and save it to localStorage
        response.sendRedirect(frontendUrl + "/dashboard?token=" + encodedToken);
    }
}