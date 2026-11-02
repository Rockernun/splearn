package domaindriven.splearn.application.provided;

import domaindriven.splearn.domain.Member;

/**
 * 회원을 조회한다
 */
public interface MemberFinder {
    Member find(Long memberId);
}
