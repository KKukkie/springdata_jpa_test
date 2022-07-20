package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.datajpa.entity.member.Member;
import study.datajpa.entity.member.dto.MemberDto;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {

    List<Member> findByUserNameAndAgeGreaterThan(String userName, int age);

    /**
     * 권장하는 기능
     */
    @Query("select m from Member m where m.userName = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    @Query("select m.userName from Member m")
    List<String> findUserNameList();

    @Query("select new study.datajpa.entity.member.dto.MemberDto(m.id, m.userName, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    @Query("select m from Member m where m.userName in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);

    @Query("select m from Member m where m.age = :age order by m.userName desc")
    List<Member> findByPage(@Param("age") int age);

    List<Member> findListByUserName(String userName); // 컬렉션 반환
    Member findMemberByUserName(String userName); // 객체 반환
    Optional<Member> findOptionalByUserName(String userName); // Optional 객체 반환

    /**
     * Paging > total count 쿼리도 같이 발생
     * >> 성능에 문제가 생길 수 있다.
     * 아래와 같이 수정하지 않으면 count query 에서도 left outer join 이 발생해서 성능에 문제가 생길 수 있다.
     *
     * select * from ( select member0_.member_id as member_id1_0_, member0_.age as age2_0_,
     *      member0_.team_id as team_id4_0_, member0_.user_name as user_name3_0_ from member member0_
     *      left outer join team team1_ on member0_.team_id=team1_.team_id order by member0_.user_name desc ) where rownum <= 3;
     *
     * select count(member0_.member_id) as col_0_0_ from member member0_
     *      left outer join team team1_ on member0_.team_id=team1_.team_id;
     *
     * >> 그래서 count query 문을 별개로 작성 가능하다. (성능 최적화 가능)
     */
    @Query(value = "select m from Member m left join m.team t",
            countQuery = "select count(m) from Member m")
    /**
     * select count(member0_.member_id) as col_0_0_ from member member0_;
     */
    Page<Member> findByAge(int age, Pageable pageable);
    List<Member> findTop3ByAge(int age);

    // Slicing > select 만 실행
    //Slice<Member> findByAge(int age, Pageable pageable);

    // Bulk operation

    /**
     * JPA 에서는 벌크 연산을 조심해서 사용해야 한다.
     * 영속성 컨텍스트를 무시하고 DB 와 직접 연산한다.
     * 그래서 영속성 컨텍스트와 데이터 싱크가 안맞을 수 있다.
     */
    @Modifying(clearAutomatically = true) // 영속성 컨텍스트에 즉각 반영한다.
    @Query("update Member  m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    /**
     * Fetch Join
     * 연관된 객체를 모두 들고온다.
     */
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    @Override
    @EntityGraph(attributePaths = "team") // Fetch join 으로 변경
    //@Query("select m from Member m")
    List<Member> findAll();

    @EntityGraph
    List<Member> findEntityGraphByUserName(@Param("userName") String userName);

    /**
     * QueryHint
     *
     * JPA 순수 라이브러리에는 없다.
     * Hibernate 에서 구현되어 있다.
     */
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUserName(String userName);

}
