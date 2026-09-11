package com.ortakpay.core.repository;

import com.ortakpay.core.domain.GroupMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {}
