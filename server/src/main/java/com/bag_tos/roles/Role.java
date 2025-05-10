package com.bag_tos.roles;

import com.bag_tos.common.model.RoleType;

/**
 * Oyunda kullanılan rollerin temel sınıfı
 */
public abstract class Role {
    /**
     * Rolün adını döndürür
     * @return Rol adı
     */
    public abstract String getName();

    /**
     * Rolün tipini döndürür
     * @return Rol tipi
     */
    public abstract RoleType getRoleType();
}