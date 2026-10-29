package com.ssafy.keepick.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.RequestEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestTemplate;
import com.ssafy.keepick.member.application.MemberService;
import com.ssafy.keepick.member.domain.Member;
import com.ssafy.keepick.member.persistence.MemberRepository;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2MemberServiceTest {
    @Mock private MemberRepository memberRepository;
    @Mock private MemberService memberService;
    @Mock private RestTemplate restTemplate;
    @InjectMocks private CustomOAuth2MemberService service;

    private OAuth2UserRequest request(String provider, Map<String,Object> attributes) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(provider)
            .clientId("test-client").clientSecret("test-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback").authorizationUri("https://provider.invalid/auth")
            .tokenUri("https://provider.invalid/token").userInfoUri("https://provider.invalid/me")
            .userNameAttributeName("sub").build();
        service.setRestOperations(restTemplate);
        given(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class))).willReturn(ResponseEntity.ok(attributes));
        return new OAuth2UserRequest(registration, new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            "test-access-token", Instant.now(), Instant.now().plusSeconds(60)));
    }

    @Test @DisplayName("새로운 회원 소셜 로그인 - 고유한 닉네임 생성")
    void loadUser_NewMember_GenerateUniqueNickname() {
        String email="test@example.com";
        given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
        given(memberService.generateUniqueNicknameFromEmail(email)).willReturn("test123");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation->invocation.getArgument(0));
        OAuth2User result=service.loadUser(request("google",Map.of("sub","123456789","name","Test User","email",email,"picture","https://example.com/profile.jpg")));
        assertThat(result).isNotNull();
        verify(memberService).generateUniqueNicknameFromEmail(email);
        verify(memberRepository).save(any(Member.class));
    }

    @Test @DisplayName("기존 회원 소셜 로그인 - 기존 정보 사용")
    void loadUser_ExistingMember_UseExistingInfo() {
        String email="existing@example.com";
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(Member.builder().name("Existing User")
            .email(email).nickname("existing").provider("google").providerId("123456789").build()));
        OAuth2User result=service.loadUser(request("google",Map.of("sub","123456789","name","Existing User","email",email,"picture","https://example.com/profile.jpg")));
        assertThat(result).isNotNull();
        verify(memberService,never()).generateUniqueNicknameFromEmail(anyString());
        verify(memberRepository,never()).save(any(Member.class));
    }

    @Test @DisplayName("지원하지 않는 OAuth2 제공자")
    void loadUser_UnsupportedProvider() {
        assertThat(service.loadUser(request("unsupported",Map.of("sub","test")))).isNull();
        verify(memberRepository,never()).findByEmail(anyString());
        verify(memberService,never()).generateUniqueNicknameFromEmail(anyString());
    }
}
