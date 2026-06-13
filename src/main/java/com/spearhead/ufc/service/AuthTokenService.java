package com.spearhead.ufc.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.spearhead.ufc.model.AuthToken;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.AuthTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthTokenService {

	private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

	@Autowired
	private AuthTokenRepository authTokenRepository;

	public void saveRefreshToken(Employee user, String refreshToken) {
		log.info("Enter into saveRefreshToken method -AuthTokenService");
		try {
			AuthToken token = new AuthToken();
			token.setEmployee(user);
			token.setRefreshToken(refreshToken);
			token.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));
			authTokenRepository.save(token);
		} catch (Exception e) {
			log.error("Error Occured into the saveRefreshToken method - AuthTokenService", e.getMessage(), e);
		} finally {
			log.error("Exited saveRefreshToken method - AuthTokenService");
		}
	}

	public Optional<AuthToken> findByToken(String refreshToken) {
		log.info("Enter into findByToken method - AuthTokenService");
		try {
			return authTokenRepository.findByRefreshToken(refreshToken);
		} catch (Exception e) {
			log.error("Error Occured into findByToken method - AuthTokenService");
			return null;
		} finally {
			log.info("Exited into findByToken method - AuthTokenService");
		}
	}

}
