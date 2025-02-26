package com.windlogs.authentication.dto.UserProfileDto;

import com.windlogs.authentication.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String image;
    private Role role;
    private String bio;
    private String phone;
    private String location;
    private String company;
    private String pronouns;
    private String lien;

}
