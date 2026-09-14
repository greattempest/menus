package com.tempest.menus.config;

import com.tempest.menus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

  private final AuthService authService;

  public PermissionInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String path = request.getRequestURI();
    if ((!path.startsWith("/api/menus") && !path.equals("/api/uploads"))
      || HttpMethod.GET.matches(request.getMethod())) {
      return true;
    }

    String permission = switch (request.getMethod()) {
      case "POST" -> path.equals("/api/uploads") ? "menu:create" : "menu:create";
      case "PUT", "PATCH" -> "menu:update";
      case "DELETE" -> "menu:delete";
      default -> null;
    };
    HttpSession session = request.getSession(false);
    if (permission == null || session == null || !authService.hasPermission(session, permission)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "没有执行此操作的权限");
      return false;
    }
    return true;
  }
}