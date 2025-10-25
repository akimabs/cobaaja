package com.loginservice.app.infrastructure.client.user;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private AddressDto address;
    private String phone;
    private String website;
    private CompanyDto company;
    
    @Data
    @NoArgsConstructor
    public static class AddressDto {
        private String street;
        private String suite;
        private String city;
        private String zipcode;
        private GeoDto geo;
    }
    
    @Data
    @NoArgsConstructor
    public static class GeoDto {
        private String lat;
        private String lng;
    }
    
    @Data
    @NoArgsConstructor
    public static class CompanyDto {
        private String name;
        private String catchPhrase;
        private String bs;
    }
}
