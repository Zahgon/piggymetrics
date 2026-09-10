package com.piggymetrics.auth.oauth;

import io.smallrye.jwt.util.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Exposes the RSA public key that matches the signing key as a JSON Web Key Set, and the
 * key id used both in the JWKS and in the {@code kid} header of every issued token, so that
 * resource servers (and this module's own smallrye-jwt verifier) can resolve the key.
 */
@ApplicationScoped
public class JwkService {

	private final String publicKeyLocation;

	private Map<String, Object> jwk;

	@Inject
	public JwkService(@ConfigProperty(name = "mp.jwt.verify.publickey.location") String publicKeyLocation) {
		this.publicKeyLocation = publicKeyLocation;
	}

	@PostConstruct
	void init() {
		try {
			PublicKey publicKey = KeyUtils.readPublicKey(publicKeyLocation);
			if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
				throw new IllegalStateException(
						"expected an RSA public key at " + publicKeyLocation + " but got " + publicKey.getAlgorithm());
			}
			String n = base64Url(toUnsignedBytes(rsaPublicKey.getModulus().toByteArray()));
			String e = base64Url(toUnsignedBytes(rsaPublicKey.getPublicExponent().toByteArray()));
			this.jwk = Map.of(
					"kty", "RSA",
					"alg", "RS256",
					"use", "sig",
					"kid", thumbprint(n, e),
					"n", n,
					"e", e);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("unable to read the JWT public key from " + publicKeyLocation, ex);
		}
	}

	public String keyId() {
		return (String) jwk.get("kid");
	}

	/** RFC 7517 JSON Web Key Set holding the single active signing key. */
	public Map<String, Object> jwks() {
		return Map.of("keys", List.of(jwk));
	}

	/** RFC 7638 thumbprint of the RSA key, used as a stable {@code kid}. */
	private static String thumbprint(String n, String e) throws Exception {
		String canonical = "{\"e\":\"" + e + "\",\"kty\":\"RSA\",\"n\":\"" + n + "\"}";
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
		return base64Url(digest);
	}

	/** BigInteger#toByteArray may prepend a zero sign byte; JWK wants the raw magnitude. */
	private static byte[] toUnsignedBytes(byte[] twosComplement) {
		if (twosComplement.length > 1 && twosComplement[0] == 0) {
			byte[] trimmed = new byte[twosComplement.length - 1];
			System.arraycopy(twosComplement, 1, trimmed, 0, trimmed.length);
			return trimmed;
		}
		return twosComplement;
	}

	private static String base64Url(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
