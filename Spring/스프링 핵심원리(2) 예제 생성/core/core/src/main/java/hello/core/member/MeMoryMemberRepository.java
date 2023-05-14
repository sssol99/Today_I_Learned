package hello.core.member;

import java.util.HashMap;
import java.util.Map;

public class MeMoryMemberRepository implements MemberRepository{
    //사실은 동시성 이슈 때문에 ConcurrnetHashMap를 쓰는게 맞음
    private static Map<Long, Member> store= new HashMap<>();

    @Override
    public void save(Member member) {
        store.put(member.getId(), member);
    }

    @Override
    public Member findById(Long memberId) {
        return store.get(memberId);
    }
}
