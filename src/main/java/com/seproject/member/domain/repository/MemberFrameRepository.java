package com.seproject.member.domain.repository;

import com.seproject.member.domain.Member;
import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.MemberFrame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberFrameRepository extends JpaRepository<MemberFrame, Long> {

    @Query("select mf from MemberFrame mf join fetch mf.frame where mf.member = :member")
    List<MemberFrame> findByMemberWithFrame(@Param("member") Member member);

    boolean existsByMemberAndFrame(Member member, Frame frame);
}
