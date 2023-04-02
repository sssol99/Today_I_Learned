# MySQL 성능최적화

날짜: 2023년 4월 2일

### 1️⃣ DB의 성능 최적화를 어디서 시켜야 할까

DB의 성능 최적화는 **디스크 I/O**와 관련이 많습니다. 

데이터를 읽기 위해서 디스크가 돌아야 하고 바늘이 돌아야 하는데 이게 물리적으로 이동하기 때문에 시간이 오래 걸립니다.

<img src='assets/Untitled.png' alt="" />


디스크

실제로 `HDD I/O`와 `메모리 I/O`의 속도차이는 **10만~15만 배**가 납니다. 이는 달팽이와 전투기의 차이과 같습니다. SSD가 많이 보편화되긴 했지만 그래도 메모리보다는 느립니다.

결국 성능을 줄이는 핵심은 디스크 I/O를 줄이는 것이 핵심입니다.

### 2️⃣ 인덱스를 쓰면 조회는 빨라지지만 수정/삭제/생성은 느려지지 않나요?

맞습니다. 하지만 저희는 인덱스를 생성해서 써야 합니다.

일반적인 웹사이트의 경우 R과 CUD의 비율이 8:2에서 9:1이라고 합니다.

조회에서 성능 최적화를 하고, 데이터 수정 삭제에서 조금 손해를 보는 게 훨씬 이득입니다.

이 뿐만 아니라, ORDER BY와 GROUP BY에서도 이득을 볼 수 있습니다.

```java
SELECT *
FROM crew
WHERE nickname >= "매트" AND nickname <="토르"
ORDER BY nickname;
```

만약에 이경우, 인덱스가 없다면 모든 데이터를 읽어온 뒤 DB에서 정렬을 해야 합니다.

하지만 인덱스가 있다면, 이미 정렬되어 있기 때문에 인덱스 순서대로 파일을 읽기만 하면 됩니다.

GROUPBY 또한 마찬가지입니다. nickname 이 가장 빠른 사람들을 가져오는 쿼리를 날린다고 가정해 봅시다.

```java
CREATE INDEX IDX_CREW_TRACK_NICKNAME ON CREW (TRACK, NICKNAME);
SELECT TRACK, MIN(NICKNAME) FROM CREW GROUP BY TRACK;
```

<img src='assets/Untitled 1.png' alt="" />


인덱스가 걸려 있는 경우에는 최상단의 꼬재만 읽고, 백엔드로 넘어가서 백엔드의 매트만 읽으면 됩니다. 

여기서 중간의 것들을 읽지 않기 때문에 디스트 I/O를 줄일 수 있습니다.

### 3️⃣ 인덱스 실행 계획

1. `all`: 테이블 전체 스캔
    - full table scan 을 하면 성능이 좋지 않습니다.
    - full table scan을 하는 경우
        1. 인덱스가 있는 경우 : 데이터 전체의 개수가 그렇게 많지 않거나 읽거나 하는 데이터가 전체 데이터의 25%를 넘어갈 때
        2. 인덱스가 없는 경우 : 검색하려면 다 돌아야 합니다.
    
   <img src='assets/Untitled 2.png' alt="" />

    
2. `range` :인덱스를 사용하여 범위 검색을 할 때
    - index range scan은 성능이 가장 좋습니다.
    - 이상적으로 인덱스를 걸었을 때(id가 19이상, 27이하를 검색할 때)
    - 필요한 데이터만 읽기 때문에 I/O를 줄일 수 있습니다.
    
   <img src='assets/Untitled 3.png' alt="" />

    

1. `index`
    - index full scan은 full table scan 보다는 성능이 좋습니다. 왜냐하면 인덱스는 데이터 파일보다는 크기가 작기 때문입니다.
    - 하지만! index range scan 보다는 성능이 나쁘겠죠?
    
  <img src='assets/Untitled 4.png' alt="" />

    
    ### 4️⃣ 인덱스 적용 사례I - 기본 칼럼에 인덱스 적용
    
    대체 어느 컬럼에 인덱스를 걸어야 할까?
    
   <img src='assets/Untitled 5.png' alt="" />

    
<aside>
🌟  1. **서비스의 특성상 무엇에 대한 조회가 많이 일어나는지 파악**

1. **카디널리티가 높은 칼럼에 대해 인덱스를 생성**
</aside>

1. 만약 nickname 에 대한 조회가 많다고 보면,  Cardinality가 높은 칼럼에 인덱스를 생성해야 합니다.
2. nickname 에는 중복이 일어나지 않지만 track 의 경우 중복이 일어나므로  track의 경우 cardinality가 낮습니다.
3. 그렇다면 nickname 을 조회하는 것이 타당합니다.
    
    `인덱스 카디널리티` : 고유한 값의 개수가 유일할수록 높다.
    
<img src='assets/Untitled 6.png' alt="" />

        
    
이 경우, id 의 경우 PK니까 `클러스터형  인덱스`가 자동 생성되었을 것입니다.
    
`클러스터형 인덱스` 

- 가나다 순으로 찾기 좋게 정렬되어 있음
- 실제 데이터 페이지가 클러스터 인덱스를 기준으로 재정렬됨
- 테이블당 하나
- PK에는 자동으로 인덱스 생성

`보조 인덱스` 

- 정렬기능 없음
- 테이블당 여러 개 사용 가능
    
### 5️⃣ 인덱스 적용 사례II - 복합 인덱스 적용

`복합 인덱스` `결합 인덱스` `다중 컬럼 인덱스` `Composite Index` : 두 개 이상의 컬럼을 합쳐서 인덱스를 만드는 것

- 하나의 칼럼으로 인덱스를 만들었을 때 보다 더 적은 데이터 분포를 보여 탐색할 데이터 수가 줄어듬
    
<img src='assets/Untitled 7.png' alt="" />


이 테이블의 경우 나이 순, 닉네임 순으로 정렬되어 있습니다. 이렇게 정렬된 기준을 통해 

```java
SELECT * FROM crew WHERE age >= 26
```

이 쿼리를 날렸다고 생각해 봅시다. 어떻게 탐색 범위를 줄일 수 있을까요?

나이 순으로 정렬되어 있기 때문에 26이상의 사람들을 가져오려고 하면 26살부터 데이터를 가져오면 되죠. 

```java
SELECT * FROM crew WHERE age >= AND nickname >='토르'
```

다음으로 닉네임이 ‘토르’ 보다 뒤에 나오는 사람들을 가져오려고 합니다. 마찬가지로 나이 순으로 정렬되어 있고 닉네임 순으로 정렬되어 있기 때문에 탐색 범위가 위처럼 줄어들게 되죠.

```java
SELECT * FROM crew WHERE nickname >='동키콩'
```

그럼 닉네임을 기준으로 탐색하려고 하면요?

여기서는 닉네임과 age가 섞여 있기 때문에 탐색 범위를 줄일 수 없습니다. 따라서 `full table scan` 을 하게 되죠.

이러한 상황을 잘 고려해서 첫번째와 두번째 경우에는 복합 인덱스를, 세번째 경우에는 복합 인덱스를 사용하지 않으면 되겠죠?
    
### 6️⃣ 인덱스 적용 사례III - 커버링 인덱스
    
`커버링 인덱스` 인덱스로 설정한 컬럼만 읽어 쿼리를 모두 처리할 수 있는 인덱스

`디스크 I/O` 인덱스를 사용하여 처리하는 쿼리 중 가장 큰 부하를 차지하는 부분은 인덱스 검색에서 일치하는 키 값의 레코드를 읽는 것입니다. 

n개의 인덱스를 읽을 때 n번의 디스크I/O가 발생할 수도 있는 것이죠.

<img src='assets/Untitled 8.png' alt="" />


쿼리 최적화의 가장 큰 목적은 이런 디스크 I/O를 줄이는 것입니다.

```java
SELCT *
FROM crew
WHERE nickname BETWEEN 'a` AND 'd' AND track = 'BACKEND'
```

```java
ALTER TABLE crew ADD INDEX idx_crew_nickname_track(nickname, track);
```

옵티마이저는 전체 데이터의 20-25% 이상을 조회하는 경우 인덱스를 통해 조회하는 것보다 데이터 파일을 바로 읽는 것이 효율적이라 판단합니다. 그래서 full table scan이 발생하게 되죠.

`옵티마이저` DBMS 에서 SQL 쿼리의 실행 계획을 최적화하는 컴포넌트

이것을 **`커버링 인덱스`**로 개선할 수 있습니다.

```java
SELCT *
FROM crew
WHERE nickname BETWEEN 'a` AND 'd' AND track = 'BACKEND'
```

🔽

```java
SELCT nickname, track
FROM crew
WHERE nickname BETWEEN 'a` AND 'd' AND track = 'BACKEND'
```

이렇게 하면 데이터 파일을 읽지 않고 인덱스만 읽어 불필요한 디스크 I/O 시간을 단축할 수 있습니다.

<img src='assets/Untitled 9.png' alt="" />


그런데 PK를 같이 조회한다면 어떻게 나올까요?

```java
SELCT id, nickname, track
FROM crew
WHERE nickname BETWEEN 'a` AND 'd' AND track = 'BACKEND'
```

이전과 동일하게 커버링 인덱스를 활용하고 있습니다. pk를 복합 인덱스로 설정하지도 않았는데 이상하죠?

왜냐하면 PK는 실제 레코드 주소가 아닌 클러스터 인덱스가 걸린 PK를 주소로 가지기 때문입니다.
    
### 7️⃣ 인덱스 적용 사례IV - 커버링 컨디션 푸쉬다운

<img src='assets/Untitled 10.png' alt="" />



type 을 기준으로 조회가 많이 일어난다고 가정하고, 인덱스를 생성하겠습니다.

```java
ALTER TABLE study_log ADD INDEX idx_study_log_type(type)
```

Question 목적을 가진 일정 기간 사이의 학습 로그를 조회해 보겠습니다.

```java
SELECT *
FROM study_log
WHERE type = 'QUESTION'
AND created_at BETWEEN '2022-10-07 00:00' AND '2022-10-13 00:00'
```

<img src='assets/Untitled 11.png' alt="" />


이렇게 보면 인덱스가 잘 적용된 것처럼 보입니다.

그런데 Extra 컬럼에 Using Where 이 있죠.

Extra 칼럼에는 쿼리의 실행 계획에서 성능에 관련된 중요한 내용이 표시됩니다. 내부적인 처리 알고리즘에 대해 조금 더 깊은 내용을 포함하는 것이죠.

using where 란 InnoDB 스토리지 엔진을 통해 테이블에서 행을 가져온 뒤 MySQL 엔진에서 추가적인 체크 조건을 활용하여 행의 범위를 축소한 것입니다.

`InnoDB` MYSQL의 DBMS 스토리지 엔진, 트랜잭션, 락, 외래 키 같은 기능을 제공

<img src='assets/Untitled 12.png' alt="" />


이렇게 InnoDB스토리지 엔진이 쓸데없이 너무 많은 데이터를 디스크에서 가져오게 됩니다.

이걸 복합 인덱스를 통해 개선할 수 있습니다.

```java
ALTER TABLE study_log ADD INDEX idx_study_log_type_Created_at(type,created_at)
```

<img src='assets/Untitled 13.png' alt="" />

<img src='assets/Untitled 14.png' alt="" />

<img src='assets/Untitled 15.png' alt="" />


### 8️⃣ 그럼 모든 컬럼에 인덱스를 다는 게 좋지 않나요?

    자칫 본체보다 인덱스가 더 큰 용량을 필요로 할 수 있습니다. 거대한 데이터를 처리하기 위해 DB를 사용하는 건데 본체보다 더 거대한 인덱스가 생긴다면 곤란할 것입니다.

    그리고 아무 컬럼에 인덱스를 거는 것은 일단 효율적이지 못합니다.  

    만약 어떤 게임회사의 DB에서 지난 주에 접속한 유저를 찾는 쿼리를 날린다고 가정해 보겠습니다.

    > 게임 플레이 컬럼에 인덱스를 건다면, 10000명의 유저가 하루 100번 플레이했다고 가정했을 때 그 컬럼의 개수는 백만개입니다. 그리고 1년이면 25억개가 됩니다. 25억개 중에서 데이터를 찾아야 하는 것입니다.
    하지만 플레이 위크(week) 컬럼에 인덱스를 건다면, 키는 25억개에서 52개 정도로 줄어들게 될 것입니다.
> 

    출처
    
    ---
    
    [https://www.youtube.com/watch?v=nvnl9YgnON8](https://www.youtube.com/watch?v=nvnl9YgnON8)
    
    [https://mongyang.tistory.com/75](https://mongyang.tistory.com/75)