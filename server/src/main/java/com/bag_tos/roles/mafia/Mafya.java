package com.bag_tos.roles.mafia;

import com.bag_tos.common.model.RoleType;
import com.bag_tos.roles.Role;

/**
 * Mafya rolü
 */
public class Mafya extends Role {
    @Override
    public String getName() {
        return "Mafya";
    }

    @Override
    public RoleType getRoleType() {
        return RoleType.MAFYA;
    }
}