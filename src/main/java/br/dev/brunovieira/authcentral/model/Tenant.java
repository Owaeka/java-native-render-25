package br.dev.brunovieira.authcentral.model;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @EqualsAndHashCode.Include
    private Long id;

    private String tenantKey;

    private String tenantName;

    private String realmName;

    private String clientId;

    private String clientSecret;

    private String keycloakBaseUrl;

    private Boolean isActive = true;
}
