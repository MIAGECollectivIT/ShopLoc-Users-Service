package fr.miage.collectivit.usersservice.security;

import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

  public static final String AUTH_HEADER = "Authorization";
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final String SESSION_COOKIE_NAME = "shoploc_token";

  public static final String ATTR_USER_ID = "authenticatedUserId";
  public static final String ATTR_USER_DB_ID = "authenticatedUserDbId";
  public static final String ATTR_USER_EMAIL = "authenticatedUserEmail";
  public static final String ATTR_USER_ROLE = "authenticatedUserRole";
  public static final String ATTR_CLAIMS = "jwtClaims";

  private static final String METHOD_OPTIONS = "OPTIONS";
  private static final String METHOD_POST = "POST";

  private static final Set<String> PUBLIC_POST_PATHS =
      Set.of("/v1/users", "/v1/users/", "/v1/users/login", "/v1/users/login/");
  private static final Set<String> PUBLIC_EXACT_PATHS =
      Set.of(
          "/v1/users/status",
          "/v1/users/status/",
          "/v1/auth/public-key",
          "/v1/auth/public-key/",
          "/v1/auth/jwks.json",
          "/.well-known/jwks.json",
          "/v1/.well-known/jwks.json",
          "/v1/auth/revoked-tokens",
          "/v1/auth/revoked-tokens/",
          "/v1/auth/check-revocation",
          "/v1/auth/check-revocation/",
          "/v1/users/partners",
          "/v1/users/partners/",
          "/swagger-ui.html",
          "/error");
  private static final String DOCS_PREFIX = "/v3/api-docs";
  private static final String SWAGGER_PREFIX = "/swagger-ui";
  private static final String WEBJARS_PREFIX = "/webjars";

  private final JwtTokenService jwtTokenService;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (METHOD_OPTIONS.equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();
    String method = request.getMethod().toUpperCase(Locale.ROOT);

    if (isWhitelisted(path, method)) {
      return true;
    }

    String token = extractToken(request);
    if (token == null || token.isEmpty()) {
      throw new UnauthorizedException(
          "Authentication required. Please provide a valid Bearer JWT token or session cookie.");
    }

    Claims claims = jwtTokenService.validateAndExtractClaims(token);
    populateRequestAttributes(request, claims);

    return true;
  }

  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader(AUTH_HEADER);
    if (authHeader != null
        && authHeader.regionMatches(true, 0, TOKEN_PREFIX, 0, TOKEN_PREFIX.length())) {
      return authHeader.substring(TOKEN_PREFIX.length()).trim();
    }
    return extractTokenFromCookies(request);
  }

  private String extractTokenFromCookies(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (SESSION_COOKIE_NAME.equals(cookie.getName())
          && cookie.getValue() != null
          && !cookie.getValue().isBlank()) {
        return cookie.getValue().trim();
      }
    }
    return null;
  }

  private void populateRequestAttributes(HttpServletRequest request, Claims claims) {
    request.setAttribute(ATTR_USER_ID, claims.get("userId", String.class));
    request.setAttribute(ATTR_USER_DB_ID, extractDbId(claims.get("dbId")));
    request.setAttribute(ATTR_USER_EMAIL, claims.get("email", String.class));
    request.setAttribute(ATTR_USER_ROLE, claims.get("role", String.class));
    request.setAttribute(ATTR_CLAIMS, claims);
  }

  private Long extractDbId(Object dbIdObj) {
    if (dbIdObj instanceof Number n) {
      return n.longValue();
    }
    if (dbIdObj != null) {
      try {
        return Long.parseLong(dbIdObj.toString());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private boolean isWhitelisted(String path, String method) {
    String normalizedPath = path.startsWith("/api/") ? path.substring(4) : path;
    if (METHOD_POST.equals(method) && PUBLIC_POST_PATHS.contains(normalizedPath)) {
      return true;
    }
    if (PUBLIC_EXACT_PATHS.contains(normalizedPath)) {
      return true;
    }
    return normalizedPath.startsWith(DOCS_PREFIX)
        || normalizedPath.startsWith(SWAGGER_PREFIX)
        || normalizedPath.startsWith(WEBJARS_PREFIX);
  }
}
