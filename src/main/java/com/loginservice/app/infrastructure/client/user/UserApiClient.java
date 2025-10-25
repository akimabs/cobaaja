package com.loginservice.app.infrastructure.client.user;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.loginservice.app.application.port.out.UserPort;
import com.loginservice.app.domain.entity.User;
import com.loginservice.app.domain.entity.Location;
import com.loginservice.app.domain.entity.Coordinates;
import com.loginservice.app.domain.entity.Organization;

import reactor.core.publisher.Mono;


@Component
public class UserApiClient implements UserPort {

    private final WebClient webClient;

    public UserApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<User> loadById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDto.class)
                .map(this::toDomainEntity);
    }

    private User toDomainEntity(UserDto dto) {
        return new User(
            dto.getId(),
            dto.getName(),
            dto.getUsername(),
            dto.getEmail(),
            toLocation(dto.getAddress()),
            dto.getPhone(),
            dto.getWebsite(),
            toOrganization(dto.getCompany())
        );
    }

    private Location toLocation(UserDto.AddressDto addressDto) {
        if (addressDto == null) {
            return null;
        }

        return new Location(
            buildFullAddress(addressDto),
            addressDto.getZipcode(),
            toCoordinates(addressDto.getGeo())
        );
    }

    private String buildFullAddress(UserDto.AddressDto addressDto) {
        return String.format("%s, %s, %s",
            addressDto.getStreet(),
            addressDto.getSuite(),
            addressDto.getCity()
        );
    }

    private Coordinates toCoordinates(UserDto.GeoDto geoDto) {
        if (geoDto == null) {
            return null;
        }

        return new Coordinates(
            geoDto.getLat(),
            geoDto.getLng()
        );
    }

    private Organization toOrganization(UserDto.CompanyDto companyDto) {
        if (companyDto == null) {
            return null;
        }

        return new Organization(
            companyDto.getName(),
            companyDto.getCatchPhrase(),
            companyDto.getBs()
        );
    }
}