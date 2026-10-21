package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.AdminProfileDto;
import com.ksiracare.backend.entity.Admin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CommonMappings.class)
public interface AdminMapper {

    @Mapping(source = "id", target = "adminId")
    AdminProfileDto toProfile(Admin admin);
}
