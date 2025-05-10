package com.bag_tos.roles.town;

import com.bag_tos.common.model.RoleType;
import com.bag_tos.roles.Role;

/**
 * Şerif rolü
 */
public class Serif extends Role {
    @Override
    public String getName() {
        return "Serif";
    }

    @Override
    public RoleType getRoleType() {
        return RoleType.SERIF;
    }
}