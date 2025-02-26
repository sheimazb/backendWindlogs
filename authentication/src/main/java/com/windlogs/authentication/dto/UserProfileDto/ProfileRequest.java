package com.windlogs.authentication.dto.UserProfileDto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    private String firstname;
    private String lastname;
    private String email;
    private MultipartFile image;
    private String bio;
    private String phone;
    private String location;
    private String company;
    private String pronouns;
    private String lien;
}
