package com.windlogs.authentication.dto.UserProfileDto;

import com.windlogs.authentication.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnerResponse {

        private Long id;
        private String firstname;
        private String lastname;
        private String email;
        private String image;
        private Role role;
        private String phone;
        private String location;
        private String company;
        private String lien;
        private Boolean accountLocked;


}
