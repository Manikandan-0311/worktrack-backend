package com.spearhead.ufc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/authController/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            String username = jwtUtil.getUsername(token);
//
//            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                if (jwtUtil.validateToken(token, username)) {
//                    UsernamePasswordAuthenticationToken authToken =
//                            new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
//                    SecurityContextHolder.getContext().setAuthentication(authToken);
//                }
//            }
        	
        	// try {
        	// 	String token = authHeader.substring(7);
        	//     String username = jwtUtil.getUsername(token);
        	//     if (username != null && jwtUtil.validateToken(token, username)) {
        	//         UsernamePasswordAuthenticationToken auth =
        	//             new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
        	//         SecurityContextHolder.getContext().setAuthentication(auth);
        	//     }
        	// } catch (JwtException e) {
        	//     response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        	//     response.getWriter().write("Invalid or expired JWT token");
        	//     return;
        	// }

        }
        
        System.out.println("JwtFilter triggered for URI: " + request.getRequestURI());

        filterChain.doFilter(request, response);
    }

}
