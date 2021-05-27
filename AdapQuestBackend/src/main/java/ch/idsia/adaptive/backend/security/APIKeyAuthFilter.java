package ch.idsia.adaptive.backend.security;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    28.01.2021 17:22
 */
public class APIKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final String principalRequestHeader;

	public APIKeyAuthFilter(String principalRequestHeader) {
		this.principalRequestHeader = principalRequestHeader;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		return request.getHeader(principalRequestHeader);
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return "N/A";
	}
}
