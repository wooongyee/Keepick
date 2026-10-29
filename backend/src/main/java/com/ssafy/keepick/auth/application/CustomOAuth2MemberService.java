package com.ssafy.keepick.auth.application;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.ssafy.keepick.auth.application.dto.CustomOAuth2Member;
import com.ssafy.keepick.auth.application.dto.GoogleProvider;
import com.ssafy.keepick.auth.application.dto.KakaoProvider;
import com.ssafy.keepick.auth.application.dto.MemberDto;
import com.ssafy.keepick.auth.application.dto.NaverProvider;
import com.ssafy.keepick.auth.application.dto.OAuth2Provider;
import com.ssafy.keepick.member.application.MemberService;
import com.ssafy.keepick.member.domain.Member;
import com.ssafy.keepick.member.persistence.MemberRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class CustomOAuth2MemberService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Provider user details are intentionally excluded from application logs.

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 시도: 제공자 = {}", registrationId);
        
        OAuth2Provider oAuth2Response = null;
        if (registrationId.equals("naver")) {
            oAuth2Response = NaverProvider.from(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = GoogleProvider.from(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            oAuth2Response = KakaoProvider.from(oAuth2User.getAttributes());
        } else {
            log.warn("지원하지 않는 OAuth2 제공자: {}", registrationId);
            return null;
        }

        String email = oAuth2Response.getEmail();
        log.debug("OAuth2 이메일: {}", email);
        Member existMember = memberRepository.findByEmail(email).orElse(null);

        // 존재하지 않는 회원이면 회원 생성
        if (existMember == null) {
            log.info("새 회원 생성: 이메일 = {}, 제공자 = {}", email, registrationId);

            // 고유한 닉네임 생성
            String uniqueNickname = memberService.generateUniqueNicknameFromEmail(email);
            log.info("생성된 고유 닉네임: {}", uniqueNickname);
            
            Member member = Member.builder()
            .name(oAuth2Response.getName())
            .email(email)
            .nickname(uniqueNickname)
            .profileUrl(oAuth2Response.getProfileUrl())
            .provider(oAuth2Response.getProvider())
            .providerId(oAuth2Response.getProviderId())
            .build();

            Member savedMember = memberRepository.save(member);
            log.info("회원 생성 완료: ID = {}, 닉네임 = {}", savedMember.getId(), savedMember.getNickname());

            MemberDto memberDto = MemberDto.from(savedMember);

            return CustomOAuth2Member.from(memberDto);
        }
        // 존재하는 회원이면 기존 정보 그대로 사용
        else {
            log.info("기존 회원 로그인: ID = {}, 이메일 = {}", existMember.getId(), email);
            MemberDto memberDto = MemberDto.from(existMember);

            return CustomOAuth2Member.from(memberDto);
        }
    }


}