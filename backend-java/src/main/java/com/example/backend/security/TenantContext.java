package com.example.backend.security;

public final class TenantContext {
  private static final ThreadLocal<String> TL = new ThreadLocal<>();

  private TenantContext() {
  }

  public static void setTenantId(String tenantId) {
    TL.set(tenantId);
  }

  public static String getTenantId() {
    return TL.get();
  }

  public static void clear() {
    TL.remove();
  }
}
