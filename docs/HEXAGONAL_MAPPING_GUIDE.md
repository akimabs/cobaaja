# 🎯 Hexagonal Architecture: Mapping & Separation Guide

## 📌 Golden Rule

```
MAPPING itu technical concern → belongs to ADAPTER!

External Format (DTO/Entity) → ADAPTER → Domain
Domain → ADAPTER → External Format (Response/DTO)

Service HANYA tahu Domain!
```

---

## ❓ Kapan Logic Dianggap "Terlalu Banyak"?

### ✅ **BAGUS** kalau:

1. **Setiap layer punya 1 tanggung jawab jelas**
   - Domain = business rules
   - Service = orchestration + validation
   - Adapter = technical details (API call, DB query, formatting)

2. **Easy to test**
   ```java
   @Test
   void shouldApplyCustomerFee() {
       BillItem item = new BillItem(...);
       BillItem result = item.applyCustomerFee("1000");
       // ✅ Simple, focused test
   }
   ```

3. **Easy to change**
   - Ganti format amount? Ubah di adapter aja
   - Ganti business rule fee? Ubah di domain aja
   - Ganti API endpoint? Ubah di adapter aja

### ❌ **TIDAK BAGUS** kalau:

1. **Semua logic jadi 1 dalam service/helper method**
2. **Mixing technical details + business logic**
3. **Hard to test tanpa mock banyak dependencies**
4. **Method lebih dari 20 baris dengan nested logic**

### 📏 Rule of Thumb

```
Kalau kamu nanya:
"Method ini sudah terlalu panjang ga ya?"

Jawaban:
✅ Pecah kalau method > 20 baris
✅ Pecah kalau ada nested if/for
✅ Pecah kalau mixing concerns
✅ Extract ke domain method untuk business logic
✅ Extract ke helper untuk technical details
```

---

## 🏗️ 3 Layer Architecture

### **1️⃣ Domain Layer** (Pure Business)

**Location:** `domain/entity/`

**Responsibility:**
- Business entities (records/classes)
- Business rules & validation
- Pure Java - NO annotations, NO dependencies

**Example:**
```java
// domain/entity/PulsaBalance.java
public record PulsaBalance(
    String phoneNumber,
    BigDecimal balance,
    String operator,
    String status
) {
    /**
     * ✅ Business rule: Valid phone format
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{12}$");
    }
    
    /**
     * ✅ Business rule: Active balance
     */
    public boolean isActive() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * ✅ Business rule: Apply fee
     */
    public PulsaBalance withFee(BigDecimal fee) {
        return new PulsaBalance(
            this.phoneNumber,
            this.balance.subtract(fee),
            this.operator,
            this.status
        );
    }
}
```

---

### **2️⃣ Application Layer** (Orchestration)

**Location:** 
- `application/port/in/` - Input Ports (Use Cases)
- `application/port/out/` - Output Ports (Dependencies)
- `application/service/` - Service implementations

**Responsibility:**
- Define contracts (ports/interfaces)
- Orchestrate business flow
- Call output ports
- Apply business logic
- **NO technical details!**
- **NO DTO mapping!**

**Example - Input Port:**
```java
// application/port/in/CheckBalanceUseCase.java
public interface CheckBalanceUseCase {
    Mono<PulsaBalance> checkBalance(String phoneNumber);
}
```

**Example - Output Ports:**
```java
// application/port/out/LoadBalancePort.java
public interface LoadBalancePort {
    Mono<PulsaBalance> loadBalance(String phoneNumber);
}

// application/port/out/CacheBalancePort.java
public interface CacheBalancePort {
    Mono<PulsaBalance> get(String phoneNumber);
    Mono<Void> put(String phoneNumber, PulsaBalance balance);
}
```

**Example - Service:**
```java
// application/service/PulsaBalanceService.java
@Service
public class PulsaBalanceService implements CheckBalanceUseCase {
    
    private final LoadBalancePort loadBalancePort;
    private final CacheBalancePort cacheBalancePort;
    
    public PulsaBalanceService(
        LoadBalancePort loadBalancePort,
        CacheBalancePort cacheBalancePort
    ) {
        this.loadBalancePort = loadBalancePort;
        this.cacheBalancePort = cacheBalancePort;
    }
    
    @Override
    public Mono<PulsaBalance> checkBalance(String phoneNumber) {
        
        // ✅ Business logic: validation
        if (!PulsaBalance.isValidPhone(phoneNumber)) {
            return Mono.error(new IllegalArgumentException("Invalid phone"));
        }
        
        // ✅ Orchestration: cache-aside pattern
        return cacheBalancePort.get(phoneNumber)
            .switchIfEmpty(
                loadBalancePort.loadBalance(phoneNumber)
                    .flatMap(balance -> 
                        cacheBalancePort.put(phoneNumber, balance)
                            .thenReturn(balance)
                    )
            )
            // ✅ Business logic: filter inactive
            .filter(PulsaBalance::isActive)
            .switchIfEmpty(
                Mono.error(new RuntimeException("Balance inactive"))
            );
    }
}
```

**What Service Knows:**
- ✅ Domain entities (`PulsaBalance`)
- ✅ Ports (interfaces)
- ✅ Business rules

**What Service DOESN'T Know:**
- ❌ DTO classes
- ❌ Database entities
- ❌ Redis
- ❌ WebClient
- ❌ API URLs
- ❌ JSON format

---

### **3️⃣ Infrastructure Layer** (Technical Details + MAPPING)

**Location:**
- `infrastructure/client/` - External API clients (Secondary Adapters)
- `infrastructure/persistence/` - Database repositories (Secondary Adapters)
- `infrastructure/cache/` - Cache implementations (Secondary Adapters)
- `infrastructure/web/` - REST controllers (Primary Adapters)
- `infrastructure/composite/` - Composite adapters (Routing logic)

**Responsibility:**
- Implement output ports
- Call external systems (API, DB, Cache)
- **✅ MAPPING DTO ↔ Domain**
- **✅ Technical details (formatting, parsing, etc)**
- Return domain objects to service

---

#### **A. Secondary Adapter - API Client**

```java
// infrastructure/client/OperatorApiClient.java
@Component
public class OperatorApiClient implements LoadBalancePort {
    
    private final WebClient webClient;
    
    public OperatorApiClient(WebClient webClient) {
        this.webClient = webClient;
    }
    
    @Override
    public Mono<PulsaBalance> loadBalance(String phoneNumber) {
        return webClient.get()
            .uri("/balance?phone={phone}", phoneNumber)
            .retrieve()
            .bodyToMono(BalanceDto.class)  // ← External DTO
            .map(this::toDomain);           // ✅ MAPPING HERE!
    }
    
    /**
     * ✅ MAPPING: External DTO → Domain
     * Technical responsibility
     */
    private PulsaBalance toDomain(BalanceDto dto) {
        return new PulsaBalance(
            dto.getPhone(),
            parseBalance(dto.getBalance()),  // ✅ Parsing here
            dto.getOperator(),
            dto.getStatus()
        );
    }
    
    /**
     * ✅ Technical utility: parse string to BigDecimal
     */
    private BigDecimal parseBalance(String balance) {
        return new BigDecimal(balance);
    }
}

/**
 * DTO - External API format (infrastructure concern)
 */
@Data
class BalanceDto {
    private String phone;
    private String balance;  // String from API
    private String operator;
    private String status;
}
```

---

#### **B. Secondary Adapter - Cache**

```java
// infrastructure/cache/RedisBalanceCache.java
@Component
public class RedisBalanceCache implements CacheBalancePort {
    
    private final RedisTemplate<String, PulsaBalance> redisTemplate;
    
    public RedisBalanceCache(RedisTemplate<String, PulsaBalance> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Mono<PulsaBalance> get(String phoneNumber) {
        return Mono.fromCallable(() -> {
            String key = buildKey(phoneNumber);  // ✅ Technical detail
            return redisTemplate.opsForValue().get(key);
        });
    }
    
    @Override
    public Mono<Void> put(String phoneNumber, PulsaBalance balance) {
        return Mono.fromRunnable(() -> {
            String key = buildKey(phoneNumber);  // ✅ Technical detail
            redisTemplate.opsForValue().set(
                key, 
                balance, 
                Duration.ofMinutes(5)  // ✅ TTL logic
            );
        });
    }
    
    /**
     * ✅ Technical utility: build cache key
     */
    private String buildKey(String phoneNumber) {
        return "balance:" + phoneNumber;
    }
}
```

---

#### **C. Primary Adapter - Controller**

```java
// infrastructure/web/PulsaController.java
@RestController
@RequestMapping("/api/pulsa")
public class PulsaController {
    
    private final CheckBalanceUseCase checkBalanceUseCase;
    
    public PulsaController(CheckBalanceUseCase checkBalanceUseCase) {
        this.checkBalanceUseCase = checkBalanceUseCase;
    }
    
    @GetMapping("/balance/{phone}")
    public Mono<BalanceResponse> checkBalance(@PathVariable String phone) {
        return checkBalanceUseCase.checkBalance(phone)
            .map(BalanceResponse::from);  // ✅ Domain → Response DTO
    }
}

/**
 * Response DTO - HTTP response format
 */
record BalanceResponse(
    String phoneNumber,
    String balance,
    String operator,
    String status
) {
    /**
     * ✅ MAPPING: Domain → Response DTO
     */
    static BalanceResponse from(PulsaBalance balance) {
        return new BalanceResponse(
            balance.phoneNumber(),
            balance.balance().toString(),  // ✅ BigDecimal → String
            balance.operator(),
            balance.status()
        );
    }
}
```

---

#### **D. Composite Adapter - Multiple Providers**

Ketika ada multiple providers (Telkomsel, XL, Indosat), gunakan **Composite Pattern**:

```java
// infrastructure/composite/OperatorComposite.java
@Component
@Primary  // ← This will be injected by default
public class OperatorComposite implements LoadBalancePort {
    
    private final TelkomselApiClient telkomselClient;
    private final XLApiClient xlClient;
    private final IndosatApiClient indosatClient;
    
    public OperatorComposite(
        TelkomselApiClient telkomselClient,
        XLApiClient xlClient,
        IndosatApiClient indosatClient
    ) {
        this.telkomselClient = telkomselClient;
        this.xlClient = xlClient;
        this.indosatClient = indosatClient;
    }
    
    @Override
    public Mono<PulsaBalance> loadBalance(String phoneNumber) {
        String operator = detectOperator(phoneNumber);
        
        // ✅ Routing logic in adapter, not service!
        return switch(operator) {
            case "TELKOMSEL" -> telkomselClient.loadBalance(phoneNumber);
            case "XL" -> xlClient.loadBalance(phoneNumber);
            case "INDOSAT" -> indosatClient.loadBalance(phoneNumber);
            default -> Mono.error(
                new RuntimeException("Unknown operator: " + operator)
            );
        };
    }
    
    /**
     * ✅ Technical utility: detect operator from phone prefix
     */
    private String detectOperator(String phoneNumber) {
        if (phoneNumber.startsWith("6281")) return "TELKOMSEL";
        if (phoneNumber.startsWith("6287")) return "XL";
        if (phoneNumber.startsWith("6285")) return "INDOSAT";
        throw new IllegalArgumentException("Unknown prefix");
    }
}

// Each provider adapter
@Component
class TelkomselApiClient implements LoadBalancePort {
    // Telkomsel-specific API call + mapping
}

@Component
class XLApiClient implements LoadBalancePort {
    // XL-specific API call + mapping
}

@Component
class IndosatApiClient implements LoadBalancePort {
    // Indosat-specific API call + mapping
}
```

**Service tidak berubah!**
```java
// Service tetap simple
return loadBalancePort.loadBalance(phone);  // Routing handled by composite
```

---

## 🔄 Complete Flow Diagram

```
┌──────────────────────────────────────────────────────────┐
│  REQUEST: GET /api/pulsa/balance/628123456789           │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│  PRIMARY ADAPTER: PulsaController                        │
│  - Receives HTTP request                                 │
│  - Extract: phone = "628123456789"                       │
│  - Call: checkBalanceUseCase.checkBalance(phone)         │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│  APPLICATION: PulsaBalanceService                        │
│  ✅ Validate: isValidPhone(phone)                        │
│  ✅ Try cache: cacheBalancePort.get(phone)               │
│  ✅ If miss → loadBalancePort.loadBalance(phone)         │
│  ✅ Filter: balance.isActive()                           │
│  ✅ Return: Mono<PulsaBalance> (domain)                  │
└────────────────────┬─────────────────────────────────────┘
                     ↓
         ┌───────────┴───────────┐
         ↓                       ↓
┌─────────────────────┐  ┌─────────────────────┐
│ RedisBalanceCache   │  │ OperatorApiClient   │
│ (Secondary Adapter) │  │ (Secondary Adapter) │
│                     │  │                     │
│ Technical:          │  │ Technical:          │
│ - Redis ops         │  │ - WebClient call    │
│ - Key format        │  │ ✅ MAPPING:         │
│ - TTL logic         │  │    BalanceDto       │
│                     │  │    → PulsaBalance   │
│ Return: Domain      │  │ Return: Domain      │
└─────────────────────┘  └─────────────────────┘
         ↓                       ↓
    [Redis Cache]          [External API]
                     ↓
         (back to Service, then Controller)
                     ↓
┌──────────────────────────────────────────────────────────┐
│  PRIMARY ADAPTER: PulsaController                        │
│  - Receives: Mono<PulsaBalance> (domain)                 │
│  ✅ MAPPING: PulsaBalance → BalanceResponse              │
│  - Return: JSON                                          │
└──────────────────────────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│  RESPONSE: JSON to client                                │
│  {                                                       │
│    "phoneNumber": "628123456789",                        │
│    "balance": "50000",                                   │
│    "operator": "TELKOMSEL",                              │
│    "status": "ACTIVE"                                    │
│  }                                                       │
└──────────────────────────────────────────────────────────┘
```

---

## 📋 Mapping Locations Summary

| Layer | What | Where | Example |
|-------|------|-------|---------|
| **Secondary Adapter (OUT)** | External → Domain | API Client, DB Repository | `BalanceDto → PulsaBalance` |
| **Service** | ❌ NO MAPPING | - | Only Domain objects |
| **Primary Adapter (IN)** | Domain → Response | Controller | `PulsaBalance → BalanceResponse` |

### **Key Points:**

1. **DTO is infrastructure concern** → Lives in `infrastructure/`
2. **Mapping is technical concern** → Done in Adapters
3. **Service never touches DTO** → Only Domain
4. **Formatting/Parsing** → Done in Adapters
5. **Business Rules** → In Domain or Service

---

## 🎯 Real-World Example: Bill Payment

### ❌ **WRONG** - Everything in Service

```java
@Service
public class BillPaymentService {
    
    protected List<BillItemDto> constructDenomUBP(
        List<BillItemDto> listDenomUBP, 
        String fee
    ) {
        List<BillItemDto> listDenom = new ArrayList<>();
        
        // ❌ Mixing: validation + mapping + formatting
        if (listDenomUBP != null && listDenomUBP.size() > 0) {
            for (BillItemDto data : listDenomUBP) {
                if (!StringUtils.isEmpty(data.getBillAmount())) {
                    BillItemDto obj = BillItemDto.builder()
                        .billCode(data.getBillCode())
                        .billAmount(data.getBillAmount())
                        // ❌ Formatting in service
                        .billName(AmountUtil.formatAmount(...))
                        // ❌ Business logic mixed with mapping
                        .billCustomerChargeAmount(
                            fee.equalsIgnoreCase("0") 
                                ? data.getBillCustomerChargeAmount() 
                                : fee
                        )
                        // ... more fields
                        .build();
                    
                    listDenom.add(obj);
                }
            }
        }
        return listDenom;
    }
    
    private List<BillItemDto> doInquiryDenom(BaseTransactionDataDto transaction) {
        // ❌ Too many responsibilities:
        // - External API calls
        // - Branching logic
        // - Mapping
        // - Sorting
        // - Caching
        
        List<BillItemDto> listDenom;
        
        if (transaction.getUbpHost().equalsIgnoreCase("SV")) {
            var denomRs = svBillPaymentService.inquiryDenomV2(transaction);
            listDenom = constructDenomSV(denomRs.getListDenom(), ...);
        } else {
            BillInquiryRs inquiryRs = billPaymentService.inquiry(transaction);
            listDenom = constructDenomUBP(inquiryRs.getBody().getBillItemList(), ...);
        }
        
        // Sorting
        Collections.sort(listDenom, ...);
        
        // Caching
        flowService.putCacheDenomList(...);
        
        return listDenom;
    }
}
```

**Problems:**
- Service tahu tentang DTO
- Mapping logic di service
- Technical details (caching, formatting) di service
- Hard to test
- Hard to maintain

---

### ✅ **CORRECT** - Separated by Layer

#### **Domain:**
```java
// domain/entity/BillItem.java
public record BillItem(
    String code,
    BigDecimal amount,
    String formattedName,
    BigDecimal customerFee,
    BigDecimal companyFee
) {
    public boolean isValid() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BillItem withCustomerFee(BigDecimal fee) {
        return new BillItem(
            this.code,
            this.amount,
            this.formattedName,
            fee,  // ✅ Business logic
            this.companyFee
        );
    }
    
    public int compareByAmount(BillItem other) {
        return this.amount.compareTo(other.amount);
    }
}
```

#### **Application:**
```java
// application/port/out/InquiryBillPort.java
public interface InquiryBillPort {
    Mono<List<BillItem>> inquiryByProvider(String provider, String customerId);
}

// application/service/BillInquiryService.java
@Service
public class BillInquiryService implements InquiryBillUseCase {
    
    private final InquiryBillPort inquiryBillPort;
    private final CacheBillPort cacheBillPort;
    
    @Override
    public Mono<List<BillItem>> inquiryDenom(InquiryCommand command) {
        return inquiryBillPort.inquiryByProvider(
                command.provider(), 
                command.customerId()
            )
            // ✅ Business logic: filter & sort
            .map(items -> items.stream()
                .filter(BillItem::isValid)
                .sorted(BillItem::compareByAmount)
                .toList()
            )
            // ✅ Orchestration: cache result
            .flatMap(items -> 
                cacheBillPort.cache(command.payeeCode(), items)
                    .thenReturn(items)
            );
    }
}
```

#### **Infrastructure:**
```java
// infrastructure/client/UbpBillClient.java
@Component
public class UbpBillClient implements InquiryBillPort {
    
    @Override
    public Mono<List<BillItem>> inquiryByProvider(String provider, String customerId) {
        if (!"UBP".equalsIgnoreCase(provider)) {
            return Mono.empty();
        }
        
        return webClient.post()
            .uri("/ubp/inquiry")
            .bodyValue(new UbpRequest(customerId))
            .retrieve()
            .bodyToMono(UbpResponse.class)
            .map(response -> response.getItems().stream()
                .map(this::toDomain)  // ✅ MAPPING HERE
                .toList()
            );
    }
    
    /**
     * ✅ MAPPING: BillItemDto → BillItem
     */
    private BillItem toDomain(BillItemDto dto) {
        BigDecimal amount = new BigDecimal(dto.getBillAmount());
        
        return new BillItem(
            dto.getBillCode(),
            amount,
            formatAmount(amount),  // ✅ Formatting in adapter
            new BigDecimal(dto.getBillCustomerChargeAmount()),
            new BigDecimal(dto.getBillCompanyChargeAmount())
        );
    }
    
    /**
     * ✅ Technical utility
     */
    private String formatAmount(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }
}

// infrastructure/client/SvBillClient.java
@Component
public class SvBillClient implements InquiryBillPort {
    // Similar structure but for SV provider
    // Each adapter has its own mapping logic
}

// infrastructure/composite/BillInquiryComposite.java
@Component
@Primary
public class BillInquiryComposite implements InquiryBillPort {
    // Routes to correct adapter based on provider
}
```

---

## 🧪 Testing Benefits

### **Before (Without Hexagonal):**

```java
@Test
void testCheckBalance() {
    // ❌ Need to:
    // - Mock RestTemplate
    // - Mock RedisTemplate
    // - Setup test data in specific DTO format
    // - Mock JSON parsing
    // - Mock formatting utilities
    
    // Too many mocks, fragile test!
}
```

### **After (With Hexagonal):**

```java
@Test
void checkBalance_shouldReturnFromCache() {
    // ✅ Mock ports only
    PulsaBalance cached = new PulsaBalance(
        "628123",
        new BigDecimal(50000),
        "XL",
        "ACTIVE"
    );
    
    when(cacheBalancePort.get("628123"))
        .thenReturn(Mono.just(cached));
    
    // Test
    Mono<PulsaBalance> result = service.checkBalance("628123");
    
    // Verify - SIMPLE!
    StepVerifier.create(result)
        .expectNext(cached)
        .verifyComplete();
    
    // ✅ No Redis, no API, no DTO mocking needed!
    verify(loadBalancePort, never()).loadBalance(any());
}
```

---

## 🎁 Benefits

### **1. Easy to Replace Adapters**

```java
// Want to change from Redis to Hazelcast?
// Just create new adapter, service UNCHANGED!

@Component
public class HazelcastBalanceCache implements CacheBalancePort {
    // Different implementation, same contract
}
```

### **2. Multiple Implementations**

```java
// Can have multiple adapters for same port
@Component
class TelkomselClient implements LoadBalancePort { }

@Component
class XLClient implements LoadBalancePort { }

@Component
@Primary  // Default
class CompositeClient implements LoadBalancePort {
    // Routes to correct adapter
}
```

### **3. Clear Boundaries**

```
Domain      ← Pure business, no tech
Application ← Orchestration, uses ports
Infrastructure ← Technical details + MAPPING
```

### **4. Testability**

```java
// Mock ports, not implementations
when(port.method()).thenReturn(domain);
```

### **5. Maintainability**

- Change formatting? → Change adapter only
- Change business rule? → Change domain/service only
- Add new provider? → Add new adapter only

---

## 📖 Summary

### **Where Mapping Happens:**

```
┌─────────────────────────────────────────────────────────┐
│  External System (API/DB)                               │
│  Returns: DTO/Entity (external format)                  │
└────────────────────┬────────────────────────────────────┘
                     ↓
          ✅ MAPPING HAPPENS HERE
                     ↓
┌─────────────────────────────────────────────────────────┐
│  SECONDARY ADAPTER                                      │
│  - Calls external system                                │
│  - Receives DTO/Entity                                  │
│  ✅ Maps: DTO → Domain                                  │
│  - Returns Domain to Service                            │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  SERVICE                                                │
│  - Works ONLY with Domain objects                       │
│  - Business logic                                       │
│  - Orchestration                                        │
│  ❌ NO DTO, NO MAPPING                                  │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  PRIMARY ADAPTER (Controller)                           │
│  - Receives Domain from Service                         │
│  ✅ Maps: Domain → Response DTO                         │
│  - Returns JSON/HTTP response                           │
└─────────────────────────────────────────────────────────┘
```

### **3 Golden Rules:**

1. **Domain = Business Rules** (Pure, no dependencies)
2. **Service = Orchestration** (Uses ports, works with domain)
3. **Adapter = Technical + MAPPING** (DTO ↔ Domain conversion)

---

## 🚀 Next Steps

1. **Read:** `PURE_HEXAGONAL_ARCHITECTURE.md` for overall structure
2. **Read:** `COMPOSITE_ADAPTER_GUIDE.md` for multiple providers pattern
3. **Read:** `TDD_HEXAGONAL_WORKFLOW.md` for testing approach
4. **Practice:** Start refactoring helper methods to adapters
5. **Practice:** Extract business rules to domain entities

---

**🎯 Remember: MAPPING belongs to ADAPTER, NOT SERVICE!**

*If you find yourself doing DTO conversion in Service, it's a code smell.* 🚨

