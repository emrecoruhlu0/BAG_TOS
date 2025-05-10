package com.bag_tos.roles.naturel;

import com.bag_tos.common.model.RoleType;
import com.bag_tos.roles.Role;

/**
 * Jester (Soytarı) rolü
 */
public class Jester extends Role {
    @Override
    public String getName() {
        return "Jester";
    }

    @Override
    public RoleType getRoleType() {
        return RoleType.JESTER;
    }
}