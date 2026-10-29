package com.ssafy.keepick.group.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.keepick.external.redis.RedisService;
import com.ssafy.keepick.global.config.QueryDslConfig;
import com.ssafy.keepick.global.config.RedisConfig;
import com.ssafy.keepick.global.exception.BaseException;

import com.ssafy.keepick.group.controller.request.GroupInviteRequest;
import com.ssafy.keepick.group.domain.Group;
import com.ssafy.keepick.group.domain.GroupMember;
import com.ssafy.keepick.group.domain.GroupMemberStatus;
import com.ssafy.keepick.member.domain.Member;
import com.ssafy.keepick.group.persistence.GroupMemberRepository;
import com.ssafy.keepick.group.persistence.GroupRepository;
import com.ssafy.keepick.member.persistence.MemberRepository;
import com.ssafy.keepick.group.application.dto.GroupMemberDto;
import com.ssafy.keepick.support.RedisTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({
        QueryDslConfig.class,
        RedisConfig.class,
        RedisService.class,
        GroupInvitationService.class,
})
@DataJpaTest
public class GroupInvitationServiceTest extends RedisTestContainer {

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    GroupInvitationService groupInvitationService;

    @Autowired
    RedisService redisService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @DisplayName("회원을 그룹에 초대한다")
    @Test
    void inviteTest() {
        // given
        Member member1 = createMember(1);
        Member member2 = createMember(2);
        Member member3 = createMember(3);
        Member member4 = createMember(4);
        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);
        memberRepository.save(member4);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        GroupMember groupMemberRejected = GroupMember.createGroupMember(group, member3); groupMemberRejected.reject();
        GroupMember groupMemberLeft = GroupMember.createGroupMember(group, member4); groupMemberLeft.accept(); groupMemberLeft.leave();
        groupMemberRepository.save(groupMemberRejected);
        groupMemberRepository.save(groupMemberLeft);


        GroupInviteRequest request = GroupInviteRequest.builder().inviteeIds(List.of(member1.getId(), member2.getId(), member3.getId(), member4.getId())).build();

        // when
        List<GroupMemberDto> groupMemberDtos = groupInvitationService.createInvitation(request, group.getId());
        GroupMember groupMember1 = groupMemberRepository.findByGroupIdAndMemberId(group.getId(), member1.getId()).get();
        GroupMember groupMember2 = groupMemberRepository.findByGroupIdAndMemberId(group.getId(), member2.getId()).get();
        GroupMember groupMember3 = groupMemberRepository.findByGroupIdAndMemberId(group.getId(), member3.getId()).get();
        GroupMember groupMember4 = groupMemberRepository.findByGroupIdAndMemberId(group.getId(), member4.getId()).get();

        // then
        assertThat(groupMember1.getStatus()).isEqualTo(GroupMemberStatus.PENDING);
        assertThat(groupMember2.getStatus()).isEqualTo(GroupMemberStatus.PENDING);
        assertThat(groupMember3.getStatus()).isEqualTo(GroupMemberStatus.PENDING);
        assertThat(groupMember4.getStatus()).isEqualTo(GroupMemberStatus.PENDING);
    }

    @DisplayName("이미 그룹에 가입한 회원을 초대할 수 없다")
    @Test
    void inviteDuplicateMemberTest() {
        // given
        Member member = createMember(1);
        memberRepository.save(member);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        GroupMember groupMember = GroupMember.createGroupMember(group, member);
        groupMember.accept();
        groupMemberRepository.save(groupMember);

        GroupInviteRequest request = GroupInviteRequest.builder().inviteeIds(List.of(member.getId())).build();
        List<GroupMemberDto> groupMemberDtos = groupInvitationService.createInvitation(request, group.getId());

        // when & then
        assertThat(groupMemberDtos.get(0).getStatus()).isEqualTo(GroupMemberStatus.ACCEPTED);
    }

    @DisplayName("그룹 초대를 수락한다")
    @Test
    void acceptInvitationTest() {
        // given
        Member member = createMember(1);
        memberRepository.save(member);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        GroupMember groupMember = GroupMember.createGroupMember(group, member);
        groupMemberRepository.save(groupMember);

        // when
        GroupMemberDto result = groupInvitationService.acceptInvitation(groupMember.getId(), member.getId());
        List<Long> joinedMemberIds = groupMemberRepository.findJoinedMembersById(group.getId()).stream().map(gm -> gm.getMember().getId()).toList();

        // then
        assertThat(result.getStatus()).isEqualTo(GroupMemberStatus.ACCEPTED);
        assertThat(joinedMemberIds).containsExactly(member.getId());

        assertThrows(BaseException.class, () -> groupInvitationService.acceptInvitation(1234567L, member.getId()));
    }

    @DisplayName("그룹 초대를 거절한다")
    @Test
    void rejectInvitationTest() {
        // given
        Member member = createMember(1);
        memberRepository.save(member);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        GroupMember groupMember = GroupMember.createGroupMember(group, member);
        groupMemberRepository.save(groupMember);

        // when
        GroupMemberDto result = groupInvitationService.rejectInvitation(groupMember.getId(), member.getId());
        List<Long> rejectGroupIds = groupMemberRepository.findGroupsByMember(member.getId(), GroupMemberStatus.REJECTED).stream().map(gm -> gm.getGroup().getId()).toList();

        // then
        assertThat(result.getStatus()).isEqualTo(GroupMemberStatus.REJECTED);
        assertThat(rejectGroupIds).containsExactly(group.getId());

        assertThrows(BaseException.class, () -> groupInvitationService.rejectInvitation(1234567L, member.getId()));
    }

    @DisplayName("그룹 초대 링크를 생성한다")
    @Test
    void createInvitationLinkTest() throws Exception {
        // given
        Member member = createMember(1);
        memberRepository.save(member);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        // when
        String link = groupInvitationService.createInvitationLink(group.getId());
        String encodedToken = link.substring(link.lastIndexOf("/invite/") + "/invite/".length());

        // then
        String inviteToken = decodeInviteToken(encodedToken);
        String value = redisService.getValue("invite:" + group.getId());
        assertThat(value).isEqualTo(inviteToken);
    }

    @DisplayName("그룹 초대 링크로 그룹에 가입한다")
    @Test
    void joinGroupByInvitationLinkTest() {
        // given
        Member member = createMember(1);
        memberRepository.save(member);

        Group group = Group.createGroup("테스트 그룹", null);
        groupRepository.save(group);

        String link = groupInvitationService.createInvitationLink(group.getId());
        String encodedToken = link.substring(link.lastIndexOf("/invite/") + "/invite/".length());
        String inviteToken = decodeInviteToken(encodedToken);

        // when
        GroupMemberDto result = groupInvitationService.joinGroupByInvitationLink(group.getId(), member.getId(), inviteToken);

        // then
        List<Group> groups = groupMemberRepository.findGroupsByMember(member.getId(), GroupMemberStatus.ACCEPTED).stream().map(gm -> gm.getGroup()).toList();
        assertThat(groups).extracting("id").contains(group.getId());
    }

    Member createMember(int i) {
        return Member.builder().name("test" + i).email("email" + i).nickname("nick" + i).provider("google" + i).providerId("pid" + i).identificationUrl("url" + i).build();
    }

    String decodeInviteToken(String encoded) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            Map<String, String> decoded = objectMapper.readValue(json, Map.class);
            return decoded.get("token");
        } catch (Exception e) {
            throw new RuntimeException("초대토큰 디코딩 실패", e);
        }
    }

}
