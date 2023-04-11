# 4. Spring Security를 이용한 회원 가입 및 로그인
## 4-1. 인증 VS 인가
- 인증 : 해당 리소스에 대해 작업을 수행할 수 있는 주체인지 확인하는 것
- 인가 : 접근하는 사용자가 해당 URL에 대해 인가된 회원인지를 검사하는 것

## 4-2. Spring Security 설정 추가
- 각 페이지 별 필요한 권한
  - 인증 필요 X : 상품 상세 페이지 조회
  - 인증 필요 O : 상품 주문
  - 관리자 권한 필요 O : 상품 등록
- security dependency 추가
    ```xml
    <!--porm.xml-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    ```
- SecurityConfig 클래스 작성
  ```java
  @Configuration
  @EnableWebSecurity // (A) 
  public class SecurityConfig extends WebSecurityConfigurerAdapter { // (B)
  
      @Override
      protected void configure(HttpSecurity http) throws Exception { // (C)
  
      }
  
      @Bean
      public PasswordEncoder passwordEncoder(){ // (D)
          return new BCryptPasswordEncoder();
      }
  }
  ```
  - (A)(B) : WebSecurityConfigurerAdapter를 상속받는 클래스에 @EnableWebSecurity 어노테이션을 선언 시 SpringSecurityFilterChain이 자동으로 포함되며 WebSecurityConfigurerAdapter를 상속받아 메소드 오버라이딩을 통해 보안 설정을 커스터마이징하는 것이 가능 
  - (C) : http 요청에 대한 보안을 설정하며 페이지 권한 설정, 로그인 페이지 설정, 로그아웃 메소드 등에 대한 코드를 작성
  - (D) : 비밀번호를 DB에 그대로 저장 시 보안의 위험이 있으므로 BCryptPasswordEncoder의 해시 함수를 이용하여 비밀번호를 암호화하여 저장

## 4-3. 회원 가입 기능 구현
- Role 작성
  ```java
  /* com.shop.constant.Role.java */
  public enum Role {
    USER, ADMIN
  }
  ```
- MemberFormDto 작성
  ```java
  /* com.shop.dto.MemberFormDto.java */
  @Getter
  @Setter
  public class MemberFormDto {
      private String name;
      private String email;
      private String password;
      private String address;
  }
  ```
- Member 엔티티 작성
  ```java
  /* com.shop.entity.Member.java */
  @Entity
  @Table(name = "member")
  @Getter
  @Setter
  @ToString
  public class Member {
      @Id
      @Column(name = "member_id")
      @GeneratedValue(strategy = GenerationType.AUTO)
      private Long id;
  
      private String name;
  
      @Column(unique = true)
      private String email;
  
      private String password;
  
      private String address;
  
      @Enumerated(EnumType.STRING)
      private Role role;
  
      public static Member createMember(MemberFormDto memberFormDto, PasswordEncoder passwordEncoder){
          Member member = new Member();
          member.setName(memberFormDto.getName());
          member.setEmail(memberFormDto.getEmail());
          member.setAddress(memberFormDto.getAddress());
  
          String password = passwordEncoder.encode(memberFormDto.getPassword());
          member.setPassword(password);
          member.setRole(Role.USER);
          return member;
      }
  }
  ```
- MemberRepository 작성
  ```java
  /* com.shop.repository.MemberRepository.java */
  public interface MemberRepository extends JpaRepository<Member, Long> {
      Member findByEmail(String email);
  }
  ```
- MemberService 작성
  ```java
  /* com.shop.service.MemberService.java */
  @Service
  @Transactional // (A)
  @RequiredArgsConstructor // (B)
  public class MemberService {
  
    private final MemberRepository memberRepository; // (C)
  
    public Member saveMember(Member member){
      validateDuplicateMember(member);
      return memberRepository.save(member);
    }
  
    private void validateDuplicateMember(Member member){ // (D)
      Member findMember = memberRepository.findByEmail(member.getEmail());
      if(findMember != null) {
        throw new IllegalStateException("이미 가입된 회원입니다.");
      }
    }
  }
  ```
  - (A) : 비즈니스 로직을 담당하는 서비스 계층 클래스에 @Transactional 어노테이션을 선언하여 로직을 처리 중 오류가 발생 시 변경된 데이터를 로직 수행 전의 상태로 콜백 
  - (B)(C) : @RequiredArgsConstructor 어노테이션을 통해 final 또는 @NonNull이 붙은 필드에 생성자를 생성하여 빈을 주입하는 방법이며 빈에 생성자가 1개이고 생성자의 파라미터 타입이 빈으로 등록 가능하면 @Autowired 어노테이션 없이 의존성 주입이 가능
  - (D) : 이미 가입된 회원의 경우 IllegalStateException 예외를 발생
- 회원가입 기능 테스트를 위한 MemberServiceTest 작성
  ```java
  /* com.shop.service.MemberServiceTest.java */
  @SpringBootTest
  @Transactional
  @TestPropertySource(locations = "classpath:application-test.properties")
  class MemberServiceTest {
    @Autowired
    MemberService memberService;
  
    @Autowired
    PasswordEncoder passwordEncoder;
  
    public Member createMember(){
      MemberFormDto memberFormDto = new MemberFormDto();
      memberFormDto.setEmail("test@gmail.com");
      memberFormDto.setName("홍길동");
      memberFormDto.setAddress("성남시 분당구 분당동");
      memberFormDto.setPassword("1234");
      return Member.createMember(memberFormDto, passwordEncoder);
    }
  
    @Test
    @DisplayName("회원가입 테스트")
    public void saveMemberTest() {
      Member member = createMember();
      Member savedMember = memberService.saveMember(member);
  
      assertEquals(member.getEmail(), savedMember.getEmail());
      assertEquals(member.getName(), savedMember.getName());
      assertEquals(member.getAddress(), savedMember.getAddress());
      assertEquals(member.getPassword(), savedMember.getPassword());
      assertEquals(member.getRole(), savedMember.getRole());
    }
  
    @Test
    @DisplayName("중복 회원가입 테스트")
    public void saveDuplicateMemberTest(){
      Member member1 = createMember();
      Member member2 = createMember();
      memberService.saveMember(member1);
  
      Throwable e = assertThrows(IllegalStateException.class, () ->{
        memberService.saveMember(member2);
      });
  
      assertEquals("이미 가입된 회원입니다.", e.getMessage());
    }
  }
  ```
- MemberController 작성
  ```java
  /* com.shop.controller.MemberController.java */
  @RequestMapping("/members")
  @Controller
  @RequiredArgsConstructor
  public class MemberController {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
  
    @GetMapping(value = "/new")
    public String memberForm(Model model) {
      model.addAttribute("memberFormDto", new MemberFormDto());
      return "member/memberForm";
    }
  
    @PostMapping(value = "/new")
    public String memberForm(MemberFormDto memberFormDto) {
      Member member = Member.createMember(memberFormDto, passwordEncoder);
      memberService.saveMember(member);
  
      return "redirect:/";
    }
  }
  ```
- memberForm.html 작성
  ```html
  <!-- resources/templates/member/memberForm.html -->
  <!DOCTYPE html>
  <html xmlns:th="http://www.thymeleaf.org"
        xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
        layout:decorate="~{layouts/layout1}">
  
      <!-- 사용자 CSS 추가 -->
      <th:block layout:fragment="css">
          <style>
              .fieldError {
                  color : #bd2130;
              }
          </style>
      </th:block>
  
      <!-- 사용자 스크립트 추가 -->
      <th:block layout:fragment="script">
          <script th:inline="javascript">
              $(document).ready(function(){
                  var errorMessage = [[${errorMessage}]];
                  if(errorMessage != null) {
                      alert(errorMessage);
                  }
              });
          </script>
      </th:block>
      <div layout:fragment="content">
          <form action="/members/new" role="form" method="post"  th:object="${memberFormDto}">
              <div class="form-group">
                  <label th:for="name">이름</label>
                  <input type="text" th:field="*{name}" class="form-control" placeholder="이름을 입력해주세요">
                  <p th:if="${#fields.hasErrors('name')}" th:errors="*{name}" class="fieldError">Incorrect data</p>
              </div>
              <div class="form-group">
                  <label th:for="email">이메일주소</label>
                  <input type="email" th:field="*{email}" class="form-control" placeholder="이메일을 입력해주세요">
                  <p th:if="${#fields.hasErrors('email')}" th:errors="*{email}" class="fieldError">Incorrect data</p>
              </div>
              <div class="form-group">
                  <label th:for="password">비밀번호</label>
                  <input type="password" th:field="*{password}" class="form-control" placeholder="비밀번호 입력">
                  <p th:if="${#fields.hasErrors('password')}" th:errors="*{password}" class="fieldError">Incorrect data</p>
              </div>
              <div class="form-group">
                  <label th:for="address">주소</label>
                  <input type="text" th:field="*{address}" class="form-control" placeholder="주소를 입력해주세요">
                  <p th:if="${#fields.hasErrors('address')}" th:errors="*{address}" class="fieldError">Incorrect data</p>
              </div>
              <div style="text-align: center">
                  <button type="submit" class="btn btn-primary" style="">Submit</button>
              </div>
              <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
          </form>
      </div>
  </html>
  ```
- 회원가입 후 메인 페이지로 이동하는 메인 컨트롤러 작성
  ```java
  /* com.shop.controller.MainController.java */
  ```
- main 페이지 작성
  ```html
  <!-- resources/templates/main.html -->
  <!DOCTYPE html>
  <html xmlns:th="http://www.thymeleaf.org"
        xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
        layout:decorate="~{layouts/layout1}">
  <div layout:fragment="content">
  
    <h1>메인페이지</h1>
  
  </div>
  ```
- spring-boot-starter-validation 의존성 추가
  ```xml
  <!-- pom.xml -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  ```
  - javax.validation 어노테이션
    - @NotEmpty : NULL 체크 및 문자열의 경우 길이가 0인지 검사
    - @NotBlank : NULL 체크 및 문자열의 경우 길이 0 및 빈 문자열 검사
    - @Length(min=, max=) : 최소, 최대 길이 검사
    - @Email : 이메일 형식인지 검사
    - @Max(숫자) : 지정한 값보다 작은지 검사
    - @Min(숫자) : 지정한 값보다 큰지 검사
    - @Null : 값이 NULL인지 검사
    - @NotNull : 값이 NULL이 아닌지 검사
- MemberFormDto 수정(유효성 검증 부분)
  ```java
  @Getter
  @Setter
  public class MemberFormDto {
      @NotBlank(message = "이름은 필수 입력값입니다.")
      private String name;
  
      @NotEmpty(message = "이메일은 필수 입력값입니다.")
      @Email(message = "이메일 형식으로 입력해주세요.")
      private String email;
  
      @NotEmpty(message = "비밀번호는 필수 입력값입니다.")
      @Length(min = 8, max = 16, message = "비밀번호는 8자 이상, 16자 이하로 입력해주세요.")
      private String password;
  
      @NotEmpty(message = "주소는 필수 입력값입니다.")
      private String address;
  }
  ```
- MemberController 수정
  ```java
  @RequestMapping("/members")
  @Controller
  @RequiredArgsConstructor
  public class MemberController {
      
      ...
  
      @PostMapping(value = "/new")
      public String newMember(@Valid MemberFormDto memberFormDto, BindingResult bindingResult, Model model) { // (A)
          if(bindingResult.hasErrors()) { // (B)
              return "member/memberForm";
          }
  
          try {
              Member member = Member.createMember(memberFormDto, passwordEncoder);
              memberService.saveMember(member);
          } catch (IllegalStateException e) {
              model.addAttribute("errorMessage", e.getMessage());
              return "member/memberForm";
          }
  
          return "redirect:/";
      }
  }
  ```
  - (A) : 검증하려는 객체의 앞에 @Valid 어노테이션을 선언 및 파라미터로 bindingResult 객체를 추가하며 검사 후 결과는 bindingResult에 저장
  - (B) : bindingResult.hasErrors()를 호출하여 에러가 존재 시 회원가입 페이지로 이동
