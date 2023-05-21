# Spring 핵심원리_3_객체 지향 원리 적용

### 새로운 할인 정책 개발

![Untitled]<img src='assets/Untitled.png' alt="" />
> `Ctrl + Shift + T` 테스트 클래스 자동만들기
> 
> 
> ![Untitled]<img src='assets/Untitled 1.png' alt="" /> 
> 
- 코드
    
    ```java
    package hello.core.discount;
    
    import hello.core.member.Grade;
    import hello.core.member.Member;
    
    public class RateDiscountPolicy implements DiscountPolicy{
    
        private int discountPercent = 10;
        @Override
        public int discount(Member member, int price) {
            if(member.getGrade() == Grade.VIP){
                return price * discountPercent / 100;
            }else{
                return 0;
            }
        }
    }
    ```
    
    ```java
    package hello.core.discount;
    
    import hello.core.member.Grade;
    import hello.core.member.Member;
    
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;
    //alt+Enter로 Static 으로 올림
    import static org.assertj.core.api.Assertions.*;
    
    class RateDiscountPolicyTest {
        RateDiscountPolicy discountPolicy = new RateDiscountPolicy();
        
        @Test
        @DisplayName("VIP는 10% 할인이 적용되어야 한다")
        void vip_o(){
            //given
            Member member = new Member(1L, "memberVIP", Grade.VIP);
            //when
            int discount = discountPolicy.discount(member, 10000);
            //then
            assertThat(discount).isEqualTo(1000);
        }
    
        @Test
        @DisplayName("VIP가 아니면 할인이 적용되지 않아야 한다")
        void vip_x(){
            //given
            Member member = new Member(2L, "memberBASIC", Grade.BASIC);
            //when
            int discount = discountPolicy.discount(member, 10000);
            //then
            assertThat(discount).isEqualTo(0);
        }
    }
    ```
    

### 새로운 할인 정책 적용과 문제점

새로운 할인 정책을 적용하려면 클라이언트인 OrderServiceImpl의 코드를 변경해야 한다.

```java
//변경   private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
    private final DiscountPolicy discountPolicy = new RateDiscountPolicy();
```

- 우리는 역할과 구현을 충실하게 분리했다 → OK
- 다형성도 활용하고, 인터페이스와 구현 객체를 분리했다 → OK
- OCP, DIP같은 객체지향 설계 원칙을 충실히 준수했다
    
    → 그렇게 보이지만 사실은 아니다
    
    - 추상 인터페이스 뿐만 아니라 구체 클래스에도 의존하고 있다
    
 <img src='assets/Untitled 2.png' alt="" />
    
    이런줄 알았지만! 사실은
    
<img src='assets/Untitled 3.png' alt="" />
    이렇게 의존하고 있다.
    
    코드를 보면 
    
    ```java
    private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
    ```
    
    여기서 구체 클래스인 FixDiscountPolicy에도 의존하고 있는 것을 확인할 수 있다.
    
    ❌문제점❌
    
   <img src='assets/Untitled 4.png' alt="" />
    
    여기서 FixDiscountPolicy를 바꾸는 순간 OrderServiceImpl도 수정해야 한다는 것이 문제이다.
    
    ```java
    //변경   private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
        private final DiscountPolicy discountPolicy = new RateDiscountPolicy();
    ```
    
    이렇게 변경하는 것이 매우 코드가 작긴 했지만, 그래도 OCP를 위반한 것이다.
    

### 어떻게 문제를 해결할 수 있을까?

→ 인터페이스만 의존하도록 의존관계를 변경하자

<aside>
🌟 `private final DiscountPolicy discountPolicy;` 에 `Variable 'discountPolicy' might not have been` 오류가 나는 이유

**`final`** 키워드를 사용하여 변수를 선언하면, 해당 변수는 선언 시점이나 생성자에서 초기화되어야 합니다.

</aside>

```java
package hello.core.order;

import hello.core.discount.DiscountPolicy;
import hello.core.discount.FixDiscountPolicy;
import hello.core.discount.RateDiscountPolicy;
import hello.core.member.MeMoryMemberRepository;
import hello.core.member.Member;
import hello.core.member.MemberRepository;

public class OrderServiceImpl implements OrderService{

    private final MemberRepository memberRepository = new MeMoryMemberRepository();
    private DiscountPolicy discountPolicy;

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int disCountPrice = discountPolicy.discount(member,itemPrice);

        return new Order(memberId, itemName, itemPrice, disCountPrice);
    }
}
```

하지만 이걸 실행하면

**nullPointException이 터질 것이다. discountPolicy 에 아무 값도 할당되어 있지 않기 때문에 문제가 생기는 것이다.**

### 해결 방안

누군가 클라이언트인 OrderServiceImpl에 DiscountPolicy의 구현 객체를 대신 생성하고 주입해 주어야 한다

### 관심사의 분리

> 로미오와 줄리엣 공연을 할 때, 줄리엣 역할은 로미오가 구하는 것이 아니다. 그건 `공연 기획자` 가 할 일이다. 이처럼 관심사를 분리해야 본인의 역할에 충실할 수 있다.
> 

### AppConfig 의 등장

- 애플리케이션의 전체 동작 방식을 구성하기 위해, `구현 객체를 생성`하고, `연결` 하는 책임을 가지는 별도의 설정 클래스를 만들자

<img src='assets/Untitled 5.png' alt="" />
- 객체의 생성과 연결은 AppConfig가 담당한다
- `DIP 완성` MemberServiceImpl은 MemberRepository인 추상에만 의존하면 된다. 이제 구체 클래스를 몰라도 된다
- `관심사의 분리` : 객체를 생성하고 연결하는 역할과 실행하는 역할이 명확이 분리되었다.

<img src='assets/Untitled 6.png' alt="" />

```java
package hello.core.order;

import hello.core.discount.DiscountPolicy;
import hello.core.member.Member;
import hello.core.member.MemberRepository;

public class OrderServiceImpl implements OrderService{

    private final MemberRepository memberRepository;
    private final DiscountPolicy discountPolicy;

    public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
        this.memberRepository = memberRepository;
        this.discountPolicy = discountPolicy;
    }

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int disCountPrice = discountPolicy.discount(member,itemPrice);

        return new Order(memberId, itemName, itemPrice, disCountPrice);
    }
}
```

- OrderServiceImpl의 입장에서 생성자를 통해 어떤 구현 객체가 주입될지는 알 수 없다.

### 새로운 구조와 할인 정책 적용

<img src='assets/Untitled 7.png' alt="" />

```java
package hello.core;

import hello.core.discount.DiscountPolicy;
import hello.core.discount.FixDiscountPolicy;
import hello.core.member.MeMoryMemberRepository;
import hello.core.member.MemberRepository;
import hello.core.member.MemberService;
import hello.core.member.MemberServiceImpl;
import hello.core.order.OrderService;
import hello.core.order.OrderServiceImpl;

public class AppConfig {

    public MemberService memberService(){
        return new MemberServiceImpl(memberRepository());
    }

    private static MemberRepository memberRepository() {
        return new MeMoryMemberRepository();
    }

    public OrderService orderService(){
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    public DiscountPolicy discountPolicy(){
        return new FixDiscountPolicy();
    }
}
```

```java
package hello.core;

import hello.core.discount.DiscountPolicy;
import hello.core.discount.FixDiscountPolicy;
import hello.core.discount.RateDiscountPolicy;
import hello.core.member.MeMoryMemberRepository;
import hello.core.member.MemberRepository;
import hello.core.member.MemberService;
import hello.core.member.MemberServiceImpl;
import hello.core.order.OrderService;
import hello.core.order.OrderServiceImpl;

public class AppConfig {

    public MemberService memberService(){
        return new MemberServiceImpl(memberRepository());
    }

    private static MemberRepository memberRepository() {
        return new MeMoryMemberRepository();
    }

    public OrderService orderService(){
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    public DiscountPolicy discountPolicy(){
        return new RateDiscountPolicy();
    }
}
```

- 이제 할인 정책을 변경해도, 애플리케이션의 구성 역할을 담당하는 AppConfig 만 변경하면 된다. 클라이언트 코드인 `OrderServiceImpl`을 포함해서 `사용 영역` 의 어떤 코드도 변경할 수 없다.

### 좋은 객체 지향 설계의 5가지중 3가지 원칙의 적용

`SRP` 단일 책임 원칙

> 한 클래스는 하나의 책임만 가져야 한다
> 

구현 객체를 생성하고 연결하는 책임은 APPConfig 가 담당

클라이언트 객체는 실행하는 책임만 담당

`DIP` 

> 프로그래머는 추상화에 의존해야지, 구체화에 의존하면 안된다. 의존성 주입은 이 원칙을 따르는 방법 중 하나이다
> 

기존에는 클라이언트 코드도 함께 변경해야 했지만, AppConfig가 객체 인스턴스를 클라이언트 코드 대신 생성해서 의존관계를 주입함으로써, 문제를 해결할 수 있었다

`OCP`

> 소프트웨어 요소는 확장에는 열려 있으나, 변경에는 닫혀 있어야 한다
> 

AppConfig가 의존관계를 `FixDiscount` -> `RateDiscountPolicy` 로 변경해서 클라이언트 코드에 주입하므로, 클라이언트 코드는 변경하지 않아도 됨

소프트웨어 요소를 새롭게 확장해도 사용 영역의 변경은 닫혀 있다

### IoC, DI, 그리고 컨테이너

1. `IoC` : 제어의 역전
    - 기존 객체는 클라이언트 구현 객체가 ㅅ스로 서버 구현 객체를 생성하고 연결하고 실행했다. 한마디로, `구현 객체가` 프로그램의 제어 흐름을 스스로 조종했다.
        
        → 개발자 입장에서는 자유로움
        
    - 반면에, AppConfig가 등장한 이후에 `구현 객체`는 자신의 로직을 실행하는 역할만 담당한다
    - 프로그램에 제어 흐름에 대한 권한은 모두 `AppConfig`가 가지고 있다. AppConfig는 OrderServiceImple 이 아닌 OrderService 인터페이스의 구현 객체를 생성하고 실행할 수도 있다. 그 사실을 모른 채 `구현 객체` 는 묵묵히 자신의 로직을 실행한다
    
    프레임워크 vs 라이브러리
    
    - 프레임워크가 내가 작성한 코드를 제어하고, 대신 실행하면 그것은 프레임워크다.
    - 반면에, 내가 작성한 코드가 직접 제어 흐름을 담당한다면 그것은 프레임워크가 아니라 라이브러리다.
    
    1. `DI` : 의존관계 주입
        - `OrderServiceImpl`은 실제로 어떤 구현 객체가 사용될지 알지 못한다.
        
        **“정적인 클래스 의존관계”**
        
        - 클래스가 사용하는 import 코드만 보고 의존관계를 쉽게 판단할 수 있다. 정적인 의존관계는 어플리케이션을 실행하지 않아도 분석할 수 있다는 것이다.
            
            <img src='assets/Untitled 9.png' alt="" />
        
        **“동적인 객체 인스턴스 의존 관계”**
        
        - 애플리케이션 실행 시점에 실제 생성된 객체 인스턴스의 참조가 연결된 의존 관계
            
            <img src='assets/Untitled 8.png' alt="" />
            
        - 런타임에 외부에서 실제 구현 객체를 생성하고 클라이언트에 전달해서 클라이언트와 서버의 실제 의존관계가 연결되는 것을 `의존관계 주입` 이라 한다.
        - 객체 인스턴스를 생성하고, 그 참조값을 전달해서 연결된다
        - 의존관계 주입을 사용하면 클라이언트 코드를 변경하지 않고, 클라이언트가 호출하는 대상의 타입 인스턴스를 변경할 수 있다.
        - **의존관계 주입을 사용하면, 정적인 클래스 의존관계를 변경하지 않고 동적인 객체 인스턴스 의존관계를 쉽게 변경할 수 있다.**
        
        ### IoC 컨테이너, DI컨테이너
        
        - AppConfig 처럼 객체를 생성하고 관리하면서 의존관계를 연결해 주는 것을 `IoC 컨테이너`, 또는 `DI 컨테이너` 라고 한다.
        - 의존관계 주입에 초점을 맞추어 최근에는 주로 **DI컨테이너**라고 한다.
        - 또는 어셈블러, 오브젝트 팩토리 등으로 불리기도 한다
        
        ### Spring 으로 전환하기
        
        ```java
        package hello.core;
        
        import hello.core.discount.DiscountPolicy;
        import hello.core.discount.FixDiscountPolicy;
        import hello.core.member.MeMoryMemberRepository;
        import hello.core.member.MemberRepository;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        import hello.core.order.OrderService;
        import hello.core.order.OrderServiceImpl;
        
        public class AppConfig {
        
            public MemberService memberService(){
                return new MemberServiceImpl(memberRepository());
            }
        
            public static MemberRepository memberRepository() {
                return new MeMoryMemberRepository();
            }
        
            public OrderService orderService(){
                return new OrderServiceImpl(memberRepository(), discountPolicy());
            }
        
            public DiscountPolicy discountPolicy(){
                return new FixDiscountPolicy();
            }
        }
        ```
        
        →
        
        ```java
        package hello.core;
        
        import hello.core.discount.DiscountPolicy;
        import hello.core.discount.FixDiscountPolicy;
        import hello.core.member.MeMoryMemberRepository;
        import hello.core.member.MemberRepository;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        import hello.core.order.OrderService;
        import hello.core.order.OrderServiceImpl;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        
        @Configuration // 설정 정보
        public class AppConfig {
        
            @Bean // 스프링 컨테이너에 등록됨
            public MemberService memberService(){
                return new MemberServiceImpl(memberRepository());
            }
        
            @Bean
            public static MemberRepository memberRepository() {
                return new MeMoryMemberRepository();
            }
        
            @Bean
            public OrderService orderService(){
                return new OrderServiceImpl(memberRepository(), discountPolicy());
            }
        
            @Bean
            public DiscountPolicy discountPolicy(){
                return new FixDiscountPolicy();
            }
        }
        ```
        
        ---
        
        ```java
        package hello.core;
        
        import hello.core.member.Grade;
        import hello.core.member.Member;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        
        public class MemberApp {
        
            public static void main(String[] args) {
                AppConfig appConfig = new AppConfig();
                MemberService memberService = appConfig.memberService();
                Member member = new Member(1L, "memberA", Grade.VIP);
                memberService.join(member);
        
                Member findMember = memberService.findMember(1L);
                System.out.println("new member = " + member.getName());
                System.out.println("find member = " + findMember.getName());
            }
        }
        ```
        
        →
        
        ```java
        package hello.core;
        
        import hello.core.member.Grade;
        import hello.core.member.Member;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        import org.springframework.context.ApplicationContext;
        import org.springframework.context.annotation.AnnotationConfigApplicationContext;
        
        public class MemberApp {
        
            public static void main(String[] args) {
        //        AppConfig appConfig = new AppConfig();
        //        MemberService memberService = appConfig.memberService();
        
                //스프링 컨테이너가 관리하도록 설정
                ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
                //스프링 컨테이너를 통해서 가져오도록
                MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
                Member member = new Member(1L, "memberA", Grade.VIP);
                memberService.join(member);
        
                Member findMember = memberService.findMember(1L);
                System.out.println("new member = " + member.getName());
                System.out.println("find member = " + findMember.getName());
            }
        }
        ```
        
        ```java
        1:22:19.556 [main] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'appConfig'
        21:22:19.561 [main] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'memberService'
        21:22:19.576 [main] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'memberRepository'
        21:22:19.576 [main] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'orderService'
        21:22:19.578 [main] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'discountPolicy'
        ```
        
        이런 것들이 스프링 컨테이너 내에서 관리되는 것
        
        ---
        
        ```java
        package hello.core;
        
        import hello.core.member.Grade;
        import hello.core.member.Member;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        import hello.core.order.Order;
        import hello.core.order.OrderService;
        import hello.core.order.OrderServiceImpl;
        
        public class OrderApp {
        
            public static void main(String[] args) {
                AppConfig appConfig = new AppConfig();
                MemberService memberService = appConfig.memberService();
                OrderService orderService = appConfig.orderService();
        
                
                Long memberId = 1L;
                Member member = new Member(memberId, "memberA", Grade.VIP);
                memberService.join(member);
        
                Order order = orderService.createOrder(memberId,"itemA", 10000);
        
                System.out.println("order = " + order);
            }
        }
        ```
        
        →
        
        ```java
        package hello.core;
        
        import hello.core.member.Grade;
        import hello.core.member.Member;
        import hello.core.member.MemberService;
        import hello.core.member.MemberServiceImpl;
        import hello.core.order.Order;
        import hello.core.order.OrderService;
        import hello.core.order.OrderServiceImpl;
        import org.springframework.context.ApplicationContext;
        import org.springframework.context.annotation.AnnotationConfigApplicationContext;
        
        public class OrderApp {
        
            public static void main(String[] args) {
        //        AppConfig appConfig = new AppConfig();
        //        MemberService memberService = appConfig.memberService();
        //        OrderService orderService = appConfig.orderService();
        
                ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        
                MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
                OrderService orderService  = applicationContext.getBean("orderService",OrderService.class);
        
                Long memberId = 1L;
                Member member = new Member(memberId, "memberA", Grade.VIP);
                memberService.join(member);
        
                Order order = orderService.createOrder(memberId,"itemA", 10000);
        
                System.out.println("order = " + order);
            }
        
        }
        ```
        
        ### 스프링 컨테이너
        
        - ApplicationContext를 `스프링 컨테이너`라 한다.
        - 기존에는 개발자가 AppConfig 를 사용해서 직접 객체를 생성하고 DI를 했지만, 이제부터는 스프링 컨테이너를 통해서 사용한다.
        - 스프링 컨테이너는 @Configuration이 붙은 `appConfig`를 설정정보를 사용한다. 이때 @Bean이라 적힌 메서드를 모두 호출해서 반환된 객체를 스프링 컨테이너에 등록한다. 이렇게 스프링 컨테이너에 등록된 객체를 스프링 빈이라 한다.
        - 스프링 빈(@Bean)이 붙은 메서드의 이름을 스프링 빈의 이름으로 사용한다. 물론 @Bean(”새이름”) 처럼 바꿀 수 있다.
        - 이전에는 개발자가 필요한 객체를 AppConfig 를 사용해서 직접 조회했지만, 스프링 컨테이너를 통해서 필요한 스프링 빈을 찾을 수 있음(ApplicationContext.getBean())
        
        **근데, 코드가 오히려 더 복잡한 것 같은데 스프링 컨테이너를 사용하는 이점이 뭘까????**
        
        → 차차 알아보자. 엄~청 많다!😲